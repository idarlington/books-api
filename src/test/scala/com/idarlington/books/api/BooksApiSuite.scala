package com.idarlington.books.api

import cats.effect.{IO, Resource}
import com.idarlington.books.services.NewYorkTimesService
import com.idarlington.books.suite.Server
import com.idarlington.books.suite.TestSuite._
import com.twitter.finagle.http.Status.BadRequest
import io.finch.Error.ParamNotPresent
import io.finch.Input
import weaver.IOSuite

object BooksApiSuite extends IOSuite {

  override type Res = NewYorkTimesService[IO]

  override def sharedResource: Resource[IO, NewYorkTimesService[IO]] = {
    for {
      clientCache   <- Resource.eval(cacheResults)
      cache         <- Resource.eval(cacheIO(None))
      client        <- new Server(clientCache).service
      serviceConfig <- Resource.eval(serviceConfig)
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
    } yield nytService
  }

  test("responds to correct queries") { nytService =>
    val requestRyan: Input   = Input.get("/books", ("author" -> ryan.value))
    val requestHaruki: Input = Input.get("/books", ("author" -> haruki.value))

    val booksApi = new BooksApi[IO](nytService)

    for {
      ryanRes   <- booksApi.route(requestRyan).value
      harukiRes <- booksApi.route(requestHaruki).value
    } yield {
      expect(ryanRes.books.nonEmpty) &&
      expect(harukiRes.books.nonEmpty) &&
      expect(ryanRes.author == ryan.value) &&
      expect(harukiRes.author == haruki.value)
    }
  }

  test("responds to incorrect queries") { nytService =>
    val firstInvalidReq: Input  = Input.get("/books")
    val secondInvalidReq: Input = Input.get("/books", ("author" -> ""))
    val thirdInvalidReq: Input  = Input.get("/books", ("year" -> "20"))
    val fourthInvalidReq: Input =
      Input.get("/books", ("year" -> "20"), ("author" -> ryan.value), ("year" -> "464"))

    val booksApi = new BooksApi[IO](nytService)

    for {
      firstInvalid  <- booksApi.route(firstInvalidReq).outputAttempt
      secondInvalid <- booksApi.route(secondInvalidReq).output
      thirdInvalid  <- booksApi.route(thirdInvalidReq).outputAttempt
      fourthInvalid <- booksApi.route(fourthInvalidReq).outputAttempt
    } yield {
      assert(secondInvalid.status == BadRequest) &&
      assert(fourthInvalid.isLeft) &&
      assert(firstInvalid.isLeft) &&
      assert(thirdInvalid.isLeft) &&
      assert(firstInvalid == Left(ParamNotPresent("author")))
    }
  }

  test("responds to queries with year params") { nytService =>
    val requestRyan: Input = Input.get("/books", ("author" -> ryan.value))
    val requestRyanWithYear: Input =
      Input.get("/books", ("author" -> ryan.value), ("year" -> "2022"))
    val requestRyanWithYears: Input =
      Input.get("/books", ("author" -> ryan.value), ("year" -> "2022"), ("year" -> "2021"))
    val booksApi = new BooksApi[IO](nytService)

    for {
      books         <- booksApi.route(requestRyan).value
      filtered      <- booksApi.route(requestRyanWithYear).value
      multipleYears <- booksApi.route(requestRyanWithYears).value
    } yield {
      expect(books.books.nonEmpty) &&
      expect(filtered.books.nonEmpty) &&
      expect(multipleYears.books.nonEmpty) &&
      expect(books.author == ryan.value)
      expect(filtered.author == ryan.value) &&
      expect(multipleYears.author == ryan.value) &&
      expect(books.books.size > filtered.books.size)
      expect(filtered.books.size < multipleYears.books.size)
      expect(
        books != filtered &&
          books != multipleYears &&
          filtered != multipleYears
      )
    }
  }

}
