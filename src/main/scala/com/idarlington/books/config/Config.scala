package com.idarlington.books.config

import cats.implicits._
import ciris._
import ciris.refined._
import com.idarlington.books.config.types._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.PortNumber

object Config {
  private val retryConfig: ConfigValue[Effect, RetryConfig] =
    (default(10: PositiveInt), default(10: PositiveInt))
      .parMapN((backOffMin, backOffMax) => RetryConfig(backOffMin, backOffMax))

  private val sessionConfig: ConfigValue[Effect, SessionConfig] =
    (
      default(1: PositiveInt),
      default(5: PositiveInt),
      default(10: PositiveInt),
      default(30: PositiveInt)
    ).parMapN((minSize, maxSize, maxWaiters, ttl) =>
      SessionConfig(minSize, maxSize, maxWaiters, ttl)
    )

  val nytClient: ConfigValue[Effect, NewYorkTimesClient] =
    (
      env("NYT_HOST").as[Host].default("api.nytimes.com"),
      retryConfig,
      sessionConfig,
      default(30: PositiveInt)
    ).parMapN(NewYorkTimesClient)

  val nytService: ConfigValue[Effect, NewYorkTimesService] = (
    nytClient,
    env("NYT_API_KEY").as[String].secret,
    env("NYT_HISTORY_PATH").as[Path].default("/svc/books/v3/lists/best-sellers/history.json")
  ).parMapN(NewYorkTimesService)

  val serverConfig: ConfigValue[Effect, ServerConfig] = (
    env("APP_PORT").as[PortNumber].default(8087),
    env("MAX_CONCURRENT_REQUESTS").as[Int].default(1000),
    env("MAX_WAITERS").as[Int].default(100)
  ).parMapN(ServerConfig)

  val appConfig: ConfigValue[Effect, AppConfig] =
    (
      nytService,
      serverConfig
    ).parMapN(AppConfig)
}
