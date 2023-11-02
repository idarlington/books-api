package com.idarlington.books.resources

import cats.effect.Async
import com.idarlington.books.model.NewYorkTimesBooks
import io.chrisdavenport.mules.{MemoryCache, TimeSpec}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

object Cache {
  def booksCache[F[_]: Async]: F[MemoryCache[F, (String, Int), NewYorkTimesBooks]] =
    MemoryCache.ofConcurrentHashMap[F, (String, Int), NewYorkTimesBooks](
      defaultExpiration = TimeSpec.fromDuration(Duration(3, TimeUnit.MINUTES))
    )

}
