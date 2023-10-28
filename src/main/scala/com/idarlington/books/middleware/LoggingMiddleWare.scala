package com.idarlington.books.middleware
import cats.effect.Sync
import cats.implicits._
import io.finch._
import org.typelevel.log4cats.Logger

class LoggingMiddleWare[F[_]: Sync](logger: Logger[F]) {
  val route: Endpoint.Compiled[F] => Endpoint.Compiled[F] = compiled =>
    compiled.tapWithF { (req, res) =>
      for {
        _ <- logger.info(s"Request: $req")
        _ <- logger.info(s"Response: $res")
      } yield (res)
    }
}
