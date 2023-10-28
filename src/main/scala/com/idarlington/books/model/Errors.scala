package com.idarlington.books.model

import scala.util.control.NoStackTrace

case class RateLimitError() extends NoStackTrace {
  override def getMessage: String = {
    "Unable to serve a response at this moment. Please try again later"
  }
}

case class UnExpectedStatusCode(code: Int) extends NoStackTrace
