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

class Server(cache: MemoryCache[IO, String, Int]) extends EndpointModule[IO] {

  def route: Endpoint[IO, Json] =
    get(
      "svc" :: "books" :: "v3" :: "lists" ::
        "best-sellers" :: "history.json" :: param[Author]("author")
    ) { (author: Author) =>
      val name = author.value.toLowerCase.split(" ").mkString("-")

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
              curr <- cache.lookup(author.value)
              _    <- cache.insert(author.value, curr.getOrElse(0) + 1)
            } yield Ok(parse(string).getOrElse(Json.Null))
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
