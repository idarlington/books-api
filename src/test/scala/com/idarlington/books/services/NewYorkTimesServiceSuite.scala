package com.idarlington.books.services

import cats.effect._
import com.idarlington.books.model.Book.Page
import com.idarlington.books.model.{RateLimitError, UnExpectedStatusCode}
import com.idarlington.books.suite.Server
import com.idarlington.books.suite.TestSuite._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import eu.timepit.refined.auto._
import io.chrisdavenport.mules.MemoryCache
import weaver._

object NewYorkTimesServiceSuite extends IOSuite {

  type Res = (MemoryCache[IO, (String, Int), Int], Service[Request, Response])

  override def sharedResource: Resource[IO, Res] = {
    for {
      cache   <- Resource.eval(cacheResults)
      service <- new Server(cache).service
    } yield (cache, service)
  }

  test("gets books from service") { counterWithClient =>
    val (_, client) = counterWithClient
    for {
      cache         <- cacheIO
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      books <- nytService.booksByAuthor(haruki, List(), None)
    } yield {
      expect(books.books.nonEmpty) &&
      expect(books.author == haruki.value)
    }
  }

  test("stores books in cache") { counterWithClient =>
    val (_, client) = counterWithClient
    for {
      cache         <- cacheIO
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      books        <- nytService.booksByAuthor(ryan, List(), None)
      cacheContent <- cache.lookup((ryan.value.toLowerCase, 0))
    } yield {
      expect(cacheContent.isDefined) &&
      expect(cacheContent.exists(_.results.size >= books.books.size))
    }
  }

  test("catches unexpected status codes") { (counterWithClient) =>
    val (_, client) = counterWithClient
    for {
      cache         <- cacheIO
      serviceConfig <- serviceConfig
      newClient    = { client.map[Request](a => a.uri("wrong/wrong")) }
      firstService = new NewYorkTimesService[IO](serviceConfig, cache, newClient)
      firstError <- firstService.booksByAuthor(ryan, List(), None).attempt
      rateLimit     = { client.map[Request](a => a.uri("/rate-limit")) }
      secondService = new NewYorkTimesService[IO](serviceConfig, cache, rateLimit)
      secondError <- secondService.booksByAuthor(ryan, List(), None).attempt
    } yield {
      expect(firstError == Left(UnExpectedStatusCode(404))) &&
      expect(secondError == Left(RateLimitError()))
    }
  }

  test("filters by years") { counterWithClient =>
    val (counter, client) = counterWithClient
    for {
      cache         <- cacheIO
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      firstCall          <- nytService.booksByAuthor(ryan, List(), None)
      serviceHits        <- counter.lookup((ryan.value, 0))
      secondCall         <- nytService.booksByAuthor(ryan, List("2022"), None)
      thirdCall          <- nytService.booksByAuthor(ryan, List("2022", "2021"), None)
      updatedServiceHits <- counter.lookup((ryan.value, 0))
    } yield {
      expect(serviceHits.get < updatedServiceHits.get) &&
      expect(firstCall.books != secondCall.books) &&
      expect(firstCall.books.size > secondCall.books.size) &&
      expect(secondCall.books.size < thirdCall.books.size)
    }
  }

  test("reads from cache when data is present") { counterWithClient =>
    val (counter, client) = counterWithClient
    for {
      cache         <- cacheIO
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      firstCall          <- nytService.booksByAuthor(ryan, List(), None)
      serviceHits        <- counter.lookup((ryan.value, 0))
      secondCall         <- nytService.booksByAuthor(ryan, List(), None)
      updatedServiceHits <- counter.lookup((ryan.value, 0))
    } yield {
      expect(firstCall == secondCall) &&
      expect(serviceHits.get < updatedServiceHits.get)
    }
  }

  test("pagination") { counterWithClient =>
    val (counter, client) = counterWithClient
    for {
      cache         <- cacheIO
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      firstCall      <- nytService.booksByAuthor(ryan, List(), Option(1: Page))
      serviceHits    <- counter.lookup((ryan.value, 0))
      secondCall     <- nytService.booksByAuthor(ryan, List(), Option(2: Page))
      secondCallHits <- counter.lookup((ryan.value, 20))
      thirdCall      <- nytService.booksByAuthor(ryan, List(), Option(3: Page))
      thirdCallHits  <- counter.lookup((ryan.value, 40))
    } yield {
      expect {
        (firstCall.books != secondCall.books) &&
        (firstCall.books.nonEmpty) &&
        (secondCall.books.isEmpty) &&
        (thirdCall.books.isEmpty)
      } &&
      expect {
        (serviceHits.get >= 1) &&
        (secondCallHits.get >= 1) &&
        (thirdCallHits.get >= 1)
      }
    }
  }

}
