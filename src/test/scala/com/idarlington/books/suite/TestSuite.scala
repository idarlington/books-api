package com.idarlington.books.suite

import cats.effect._
import ciris.Secret
import com.idarlington.books.config
import com.idarlington.books.config.{Config, NewYorkTimesService}
import com.idarlington.books.model.Book.Author
import com.idarlington.books.model.NewYorkTimesBooks
import eu.timepit.refined.auto._
import io.chrisdavenport.mules.MemoryCache

object TestSuite {
  val ryan: Author   = "Ryan holiday"
  val haruki: Author = "haruki murakami"

  val cacheIO: IO[MemoryCache[IO, (String, Int), NewYorkTimesBooks]] = MemoryCache
    .ofConcurrentHashMap[IO, (String, Int), NewYorkTimesBooks](None)
  val cacheResults: IO[MemoryCache[IO, (String, Int), Int]] = MemoryCache
    .ofConcurrentHashMap[IO, (String, Int), Int](None)

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

}
