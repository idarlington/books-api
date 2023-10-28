package com.idarlington.books.services

import cats.effect._
import com.idarlington.books.model.{RateLimitError, UnExpectedStatusCode}
import com.idarlington.books.suite.Server
import com.idarlington.books.suite.TestSuite._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import eu.timepit.refined.auto._
import io.chrisdavenport.mules.MemoryCache
import weaver._

object NewYorkTimesServiceSuite extends IOSuite {

  type Res = (MemoryCache[IO, String, Int], Service[Request, Response])

  override def sharedResource: Resource[IO, Res] = {
    for {
      cache   <- Resource.eval(cacheResults)
      service <- new Server(cache).service
    } yield (cache, service)
  }

  test("gets books from service") { counterWithClient =>
    val (_, client) = counterWithClient
    for {
      cache         <- cacheIO(None)
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      books <- nytService.booksByAuthor(haruki, List())
    } yield {
      expect(books.books.nonEmpty) &&
      expect(books.author == haruki.value)
    }
  }

  test("stores books in cache") { counterWithClient =>
    val (_, client) = counterWithClient
    for {
      cache         <- cacheIO(None)
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      books        <- nytService.booksByAuthor(ryan, List())
      cacheContent <- cache.lookup(ryan.value.toLowerCase)
    } yield {
      expect(cacheContent.isDefined) &&
      expect(cacheContent.exists(_.results.size >= books.books.size))
    }
  }

  test("catches unexpected status codes") { (counterWithClient) =>
    val (_, client) = counterWithClient
    for {
      cache         <- cacheIO(None)
      serviceConfig <- serviceConfig
      newClient    = { client.map[Request](a => a.uri("wrong/wrong")) }
      firstService = new NewYorkTimesService[IO](serviceConfig, cache, newClient)
      firstError <- firstService.booksByAuthor(ryan, List()).attempt
      rateLimit     = { client.map[Request](a => a.uri("/rate-limit")) }
      secondService = new NewYorkTimesService[IO](serviceConfig, cache, rateLimit)
      secondError <- secondService.booksByAuthor(ryan, List()).attempt
    } yield {
      expect(firstError == Left(UnExpectedStatusCode(404))) &&
      expect(secondError == Left(RateLimitError()))
    }
  }

  test("filters by years") { counterWithClient =>
    val (counter, client) = counterWithClient
    for {
      cache         <- cacheIO(None)
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      firstCall          <- nytService.booksByAuthor(ryan, List())
      serviceHits        <- counter.lookup(ryan.value)
      secondCall         <- nytService.booksByAuthor(ryan, List("2022"))
      thirdCall          <- nytService.booksByAuthor(ryan, List("2022", "2021"))
      updatedServiceHits <- counter.lookup(ryan.value)
    } yield {
      expect(serviceHits.get == updatedServiceHits.get) &&
      expect(firstCall.books != secondCall.books) &&
      expect(firstCall.books.size > secondCall.books.size) &&
      expect(secondCall.books.size < thirdCall.books.size)
    }
  }

  test("reads from cache when data is present") { counterWithClient =>
    val (counter, client) = counterWithClient
    for {
      cache         <- cacheIO(None)
      serviceConfig <- serviceConfig
      nytService = new NewYorkTimesService[IO](serviceConfig, cache, client)
      firstCall          <- nytService.booksByAuthor(ryan, List())
      serviceHits        <- counter.lookup(ryan.value)
      secondCall         <- nytService.booksByAuthor(ryan, List())
      updatedServiceHits <- counter.lookup(ryan.value)
    } yield {
      expect(firstCall == secondCall) &&
      expect(serviceHits.get == updatedServiceHits.get)
    }
  }

}
