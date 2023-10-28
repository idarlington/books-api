package com.idarlington.books.services

import cats.effect.kernel.Async
import cats.implicits._
import com.twitter.util.{Return, Throw, Future => TFuture}

import scala.concurrent.{ExecutionContext, Future => SFuture, Promise => SPromise}

object FutureUtils {
  implicit class RichTFuture[A](f: TFuture[A]) {
    def asScala(implicit e: ExecutionContext): SFuture[A] = {
      val p: SPromise[A] = SPromise()
      f.respond {
        case Return(value)    => p.success(value)
        case Throw(exception) => p.failure(exception)
      }

      p.future
    }

    def liftF[F[_]: Async]: F[A] = {
      for {
        executionContext <- Async[F].executionContext
        fa               <- Async[F].fromFuture(Async[F].delay(f.asScala(executionContext)))
      } yield fa
    }
  }
}
