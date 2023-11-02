package com.idarlington.books.api

import cats.effect.Async
import cats.implicits._
import com.idarlington.books.model.Book.{Author, BookYear, Page}
import com.idarlington.books.model.{Books, RateLimitError}
import com.idarlington.books.services.NewYorkTimesService
import com.twitter.finagle.http.Status
import fs2.data.json.JsonException
import io.finch._
import io.finch.refined._

class BooksApi[F[_]: Async](
    nytBooksService: NewYorkTimesService[F]
) extends Endpoint.Module[F] {

  private def getBooks: Endpoint[F, Books] =
    get(
      "books" :: param[Author]("author") :: params[BookYear]("year") :: paramOption[Page]("page")
    ) { (author: Author, year: List[BookYear], page: Option[Page]) =>
      nytBooksService
        .booksByAuthor(author, year, page)
        .map(Ok)
    }.handle {
      case ex: JsonException   => InternalServerError(ex)
      case ex: Error.NotParsed => BadRequest(new Exception(ex.getMessage()))
      case ex: RateLimitError =>
        Output.failure(new Exception(ex.getMessage), Status.FailedDependency)
    }

  val route: Endpoint[F, Books] = getBooks

}
