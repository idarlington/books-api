package com.idarlington.books.services

import io.circe._
object ExceptionEncoder {

  implicit val encodeExceptionCirce: Encoder[Exception] =
    Encoder.instance(e =>
      Json.obj("message" -> Option(e.getMessage).fold(Json.Null)(Json.fromString))
    )
}
