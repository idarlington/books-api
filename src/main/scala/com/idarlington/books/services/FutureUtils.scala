package com.idarlington.books.services

import cats.effect.kernel.Async
import cats.implicits._
import com.twitter.util

object FutureUtils {
  implicit class LiftFuture[A](f: => util.Future[A]) {
    def liftF[F[_]: Async]: F[A] = {
      Async[F].async[A] { cb =>
        Async[F]
          .pure(f)
          .map {
            _.respond {
              case util.Return(a) => cb(Right(a))
              case util.Throw(ex) => cb(Left(ex))
            }
          }
          .as(None)
      }
    }
  }
}
