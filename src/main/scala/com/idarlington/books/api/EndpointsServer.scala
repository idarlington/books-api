package com.idarlington.books.api

import cats.effect.{Async, Resource}
import com.idarlington.books.model.Books
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import ExceptionEncoder._

object EndpointsServer {

  final def service[F[_]: Async](
      books: Endpoint[F, Books],
      loggingMiddleWare: Endpoint.Compiled[F] => Endpoint.Compiled[F],
      requestMeter: Endpoint.Compiled[F] => Endpoint.Compiled[F]
  ): Resource[F, Service[Request, Response]] = {
    Bootstrap[F]
      .serve[Application.Json](books)
      .middleware(Function.chain(Seq(requestMeter, loggingMiddleWare)))
      .toService
  }
}
