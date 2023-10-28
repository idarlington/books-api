package com.idarlington.books.api

import com.idarlington.books.config.ServerConfig
import com.twitter.finagle.Http
import com.twitter.util.Duration

import java.util.concurrent.TimeUnit

object BaseHttpServer {

  def server(serverConfig: ServerConfig): Http.Server =
    Http.server.withAdmissionControl
      .concurrencyLimit(
        maxConcurrentRequests = serverConfig.maxConcurrentRequests,
        maxWaiters = serverConfig.maxWaiters
      )
      .withAdmissionControl
      .deadlines
      .withRequestTimeout(Duration(60, TimeUnit.SECONDS))
}
