package com.idarlington.books.config

import ciris._
import com.idarlington.books.config.types._
import enumeratum.{CirisEnum, Enum, EnumEntry}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.string.Uri
import eu.timepit.refined.types.net.PortNumber

import scala.collection.immutable

object types {
  type PositiveInt = Int Refined Positive
  type Host        = String Refined Uri
  type Path        = String Refined Uri
}

sealed trait AppEnvironment extends EnumEntry

object AppEnvironment extends Enum[AppEnvironment] with CirisEnum[AppEnvironment] {
  case object Production extends AppEnvironment

  val values: immutable.IndexedSeq[AppEnvironment] = findValues
}

final case class RetryConfig(
    backOffMin: PositiveInt,
    backOffMax: PositiveInt
)

final case class SessionConfig(
    minSize: PositiveInt,
    maxSize: PositiveInt,
    maxWaiters: PositiveInt,
    ttl: PositiveInt
)

final case class NewYorkTimesClient(
    host: Host,
    retry: RetryConfig,
    sessionPool: SessionConfig,
    requestTimeout: PositiveInt
)

final case class NewYorkTimesService(
    client: NewYorkTimesClient,
    apiKey: Secret[String],
    historyPath: Path
)

final case class ServerConfig(
    port: PortNumber,
    maxConcurrentRequests: Int,
    maxWaiters: Int
)

final case class AppConfig(
    nytService: NewYorkTimesService,
    serverConfig: ServerConfig
)
