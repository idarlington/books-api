package com.idarlington.books.model

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import eu.timepit.refined.string.MatchesRegex
import shapeless.Witness

import java.time.Year

object Book {
  type Author = String Refined NonEmpty
  type BookYear = String Refined
    MatchesRegex[Witness.`"""^(\\d{4})$"""`.T]
}

case class Book(name: String, publisher: Option[String], publicationYear: Option[Year])

case class Books(author: String, books: List[Book])
