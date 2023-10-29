package com.idarlington.books.middleware

import cats.NonEmptyParallel
import cats.effect.Async
import cats.syntax.all._
import com.idarlington.books.model.{NewYorkTimesBooks, RateLimitError}
import com.twitter.concurrent.AsyncMeter
import com.twitter.finagle.Failure
import com.twitter.finagle.http.{Request, Response}
import io.chrisdavenport.mules.MemoryCache
import io.finch._
import io.finch.internal.TwitterFutureConverter

class RequestMeter[F[_]: Async: NonEmptyParallel](
    minuteMeter: AsyncMeter,
    dailyMeter: AsyncMeter,
    cache: MemoryCache[F, String, NewYorkTimesBooks]
) {

  val meter: Endpoint.Compiled[F] => Endpoint.Compiled[F] = compiled => {
    Endpoint.Compiled[F] {
      case req if !req.params.contains("author") =>
        compiled(req)

      case req =>
        filter(req.params.get("author"), req, compiled)
    }
  }

  private def get(key: String): F[Option[NewYorkTimesBooks]] =
    cache.lookup(key.toLowerCase)

  private def filter(
      maybeAuthor: Option[String],
      req: Request,
      compiled: Endpoint.Compiled[F]
  ): F[(Trace, Either[Throwable, Response])] = {
    for {
      maybeContent <- maybeAuthor.traverse(key => get(key)).map(_.flatten)
      response <- maybeContent match {
        case Some(_) =>
          compiled(req)
        case None =>
          for {
            minute <- minuteMeter.await(1).toAsync.attempt
            daily  <- dailyMeter.await(1).toAsync.attempt

            resp <- (minute, daily) match {
              case (Right(_), Right(_)) => compiled(req)
              case _ =>
                Async[F].pure(
                  Trace.empty -> (Failure
                    .rejected(RateLimitError())
                    .asLeft: Either[Throwable, Response])
                )
            }
          } yield (resp)
      }
    } yield response
  }

}
