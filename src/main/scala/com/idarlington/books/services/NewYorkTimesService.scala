package com.idarlington.books.services

import cats.effect.Async
import cats.implicits._
import com.idarlington.books.config
import com.idarlington.books.model.Book.{Author, BookYear}
import com.idarlington.books.model._
import com.idarlington.books.services.FutureUtils._
import com.twitter.finagle.Service
import com.twitter.finagle.http._
import eu.timepit.refined.auto._
import fs2.Stream
import fs2.data.json.circe._
import fs2.data.json.{codec, tokens}
import io.chrisdavenport.mules.MemoryCache

import java.time.Year

class NewYorkTimesService[F[_]: Async](
    cfg: config.NewYorkTimesService,
    cache: MemoryCache[F, String, NewYorkTimesBooks],
    client: Service[Request, Response]
) {

  def booksByAuthor(
      author: Author,
      years: List[BookYear]
  ): F[Books] = {
    val stream = for {
      maybeContent <- Stream.eval(cache.lookup(author.toLowerCase))

      nytBooks <- maybeContent match {
        case Some(content) => Stream.eval(Async[F].pure(content))
        case None          => requestBooks(author)
      }

      transformed <- transform(author, nytBooks)
    } yield (transformed)

    stream
      .filter(book => filterByYears(years, book))
      .compile
      .toList
      .map { books =>
        Books(author.value, books)
      }
  }

  private def requestBooks(
      author: Author
  ): Stream[F, NewYorkTimesBooks] = {
    val request = Request(
      s"${cfg.historyPath.value}",
      ("author", s"${author.value}"),
      ("api-key", s"${cfg.apiKey.value}")
    )
    for {
      resp <- Stream.eval(client(request).liftF)
      _    <- Stream.eval(Async[F].pure(checkStatusCode(resp.status))).rethrow

      nytBooks <- Stream
        .emit(resp.getContentString())
        .through(tokens[F, String])
        .through(codec.deserialize[F, NewYorkTimesBooks])

      _ <- Stream.attemptEval(cache.insert(author.value.toLowerCase, nytBooks)).spawn

    } yield nytBooks
  }

  private def transform(
      author: Author,
      books: NewYorkTimesBooks
  ): Stream[F, Book] = {

    val stream = for {
      nytBook <- Stream.emits(books.results)
      maybeDate = nytBook.ranksHistory.headOption.map(_.publishedDate)
    } yield (nytBook, maybeDate)

    stream
      .filter { case (book, _) =>
        book.author.toLowerCase.contains(author.value.toLowerCase)
      }
      .map { case (nytBook, maybeDate) =>
        Book(
          name = nytBook.title,
          publisher = nytBook.publisher,
          publicationYear = maybeDate.map(date => Year.of(date.getYear))
        )
      }
  }

  private def filterByYears(years: List[BookYear], book: Book): Boolean = {
    years.isEmpty || book.publicationYear.exists(pubYear =>
      years.exists(_.value == pubYear.toString)
    )
  }

  private def checkStatusCode(status: Status): Either[Throwable, Status] = {
    status.code match {
      case 200 => status.asRight[Throwable]
      case 429 => RateLimitError().asLeft
      case _   => UnExpectedStatusCode(status.code).asLeft
    }
  }

}
