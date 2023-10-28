package com.idarlington.books.suite

import cats.effect._
import ciris.Secret
import com.idarlington.books.config
import com.idarlington.books.config.{Config, NewYorkTimesService}
import com.idarlington.books.model.Book.Author
import com.idarlington.books.model.NewYorkTimesBooks
import eu.timepit.refined.auto._
import io.chrisdavenport.mules.{MemoryCache, TimeSpec}

object TestSuite {
  val ryan: Author   = "Ryan holiday"
  val haruki: Author = "haruki murakami"

  val cacheIO: Option[TimeSpec] => IO[MemoryCache[IO, String, NewYorkTimesBooks]] = MemoryCache
    .ofSingleImmutableMap[IO, String, NewYorkTimesBooks]
  val cacheResults: IO[MemoryCache[IO, String, Int]] = MemoryCache
    .ofSingleImmutableMap[IO, String, Int](None)

  val serviceConfig: IO[NewYorkTimesService] = {
    for {
      clientCfg <- Config.nytClient.load[IO]
      serviceConfig = config.NewYorkTimesService(
        client = clientCfg,
        apiKey = Secret(""),
        historyPath = "/svc/books/v3/lists/best-sellers/history.json"
      )
    } yield (serviceConfig)
  }

//   val nytClientConfig: config.NewYorkTimesClient = config.NewYorkTimesClient(host = "localhost")
//   val serviceConfig: config.NewYorkTimesService =
//    config.NewYorkTimesService(
//      client = nytClientConfig,
//      apiKey = Secret(""),
//      historyPath = "/svc/books/v3/lists/best-sellers/history.json"
//    )
}
