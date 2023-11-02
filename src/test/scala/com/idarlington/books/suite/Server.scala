package com.idarlington.books.suite

import cats.effect.{IO, Resource}
import com.idarlington.books.model.Book.Author
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import io.chrisdavenport.mules.MemoryCache
import io.circe.Json
import io.circe.parser.parse
import io.finch._
import io.finch.circe._
import io.finch.refined._

import scala.io.Source
import scala.util.Using

class Server(cache: MemoryCache[IO, (String, Int), Int]) extends EndpointModule[IO] {

  val empty =
    "{\"status\":\"OK\",\"copyright\":\"Copyright (c) 2023 The New York Times Company.  All Rights Reserved.\",\"num_results\":0,\"results\":[]}"

  def route: Endpoint[IO, Json] =
    get(
      "svc" :: "books" :: "v3" :: "lists" ::
        "best-sellers" :: "history.json" :: param[Author]("author") :: paramOption[Int]("offset")
    ) { (author: Author, offset: Option[Int]) =>
      val name       = author.value.toLowerCase.split(" ").mkString("-")
      val pageOffset = offset.getOrElse(0)

      IO(
        Using
          .resource(Source.fromResource(s"${name}.json"))(source =>
            source.getLines().toStream.mkString
          )
      ).attempt
        .flatMap {
          case Left(error) =>
            IO(BadRequest(new Exception(error)))
          case Right(string) =>
            for {
              curr <- cache.lookup((author.value, pageOffset))
              _    <- cache.insert((author.value, pageOffset), curr.getOrElse(0) + 1)

              res =
                if (pageOffset == 0) parse(string)
                else parse(empty)
            } yield Ok(res.getOrElse(Json.Null))
        }
    }

  def rateLimit: Endpoint[IO, String] = get("rate-limit") {
    Output.failure[String](
      new Exception("Rate limit quota violation"),
      Status.TooManyRequests
    )
  }

  def service: Resource[IO, Service[Request, Response]] =
    Bootstrap[IO]
      .serve[Application.Json](route :+: rateLimit)
      .toService
}
