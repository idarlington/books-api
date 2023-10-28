package com.idarlington.books.model

import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

import java.time.LocalDate

case class RanksHistory(
    publishedDate: LocalDate,
    displayName: String,
    rank: BigInt
)

case class NewYorkTimesBook(
    title: String,
    author: String,
    publisher: Option[String],
    ranksHistory: List[RanksHistory]
)

case class NewYorkTimesBooks(status: String, results: List[NewYorkTimesBook])

object NewYorkTimesBooks {
  implicit val config: Configuration =
    Configuration.default.withSnakeCaseMemberNames

  implicit val nytRanksDecoder: Decoder[RanksHistory] =
    deriveConfiguredDecoder[RanksHistory]
  implicit val nytRanksEncoder: Encoder[RanksHistory] =
    deriveConfiguredEncoder[RanksHistory]

  implicit val nytBookDecoder: Decoder[NewYorkTimesBook] =
    deriveConfiguredDecoder[NewYorkTimesBook]
  implicit val nytBookEncoder: Encoder[NewYorkTimesBook] =
    deriveConfiguredEncoder[NewYorkTimesBook]

  implicit val nytRespDecoder: Decoder[NewYorkTimesBooks] =
    deriveConfiguredDecoder
  implicit val nytRespEncoder: Encoder[NewYorkTimesBooks] =
    deriveConfiguredEncoder
}
