package com.idarlington.books.api

import cats.effect.{ Async, Resource }
import com.idarlington.books.Main
import com.idarlington.books.config.ServerConfig
import com.twitter.finagle.http.{ Request, Response }
import com.twitter.finagle.param.Label
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{ Http, ListeningServer, Service }
import com.twitter.util.Duration
import io.finch.internal.TwitterFutureConverter

import java.util.concurrent.TimeUnit

object HttpServer {

  private def base(serverConfig: ServerConfig): Http.Server =
    Http.server.withAdmissionControl
      .concurrencyLimit(
        maxConcurrentRequests = serverConfig.maxConcurrentRequests,
        maxWaiters = serverConfig.maxWaiters
      )
      .withAdmissionControl
      .deadlines
      .withRequestTimeout(Duration(60, TimeUnit.SECONDS))

  def make[F[_]: Async](
      serverConfig: ServerConfig,
      statsReceiver: StatsReceiver,
      serviceResource: Resource[F, Service[Request, Response]]
  ): Resource[F, ListeningServer] = {

    serviceResource.flatMap { service =>
      val server = Async[F]
        .delay(
          base(serverConfig)
            .configured(Label(Main.name))
            .withStatsReceiver(statsReceiver)
            .serve(s":${serverConfig.port.value}", service)
        )

      Resource.make(server)(listeningServer => Async[F].defer(listeningServer.close().toAsync))
    }
  }
}
