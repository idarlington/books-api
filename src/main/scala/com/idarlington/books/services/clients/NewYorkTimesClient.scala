package com.idarlington.books.services.clients

import com.idarlington.books.config
import com.twitter.finagle.http.{ Request, Response, Status }
import com.twitter.finagle.service.{ RetryBudget, RetryFilter, RetryPolicy }
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.finagle.{ Backoff, Http, Service }
import com.twitter.util.{ Duration, Return, Try }
import eu.timepit.refined.auto._

import java.util.concurrent.TimeUnit

object NewYorkTimesClient {
  final def client(cfg: config.NewYorkTimesClient): Service[Request, Response] = {
    val policy: RetryPolicy[(Request, Try[Response])] =
      RetryPolicy.backoff(
        Backoff
          .equalJittered(
            Duration(cfg.retry.backOffMin.value, TimeUnit.MILLISECONDS),
            Duration(cfg.retry.backOffMax.value, TimeUnit.SECONDS)
          )
      ) {
        case (_, Return(rep)) if rep.status == Status.InternalServerError => true
        case (_, Return(rep)) if rep.status == Status.TooManyRequests     => false
      }

    val retryFilter = new RetryFilter[Request, Response](
      retryPolicy = policy,
      timer = DefaultTimer,
      statsReceiver = NullStatsReceiver,
      retryBudget = RetryBudget()
    )

    val baseClient = Http.client.withSessionPool
      .minSize(cfg.sessionPool.minSize)
      .withSessionPool
      .maxSize(cfg.sessionPool.maxSize)
      .withSessionPool
      .maxWaiters(cfg.sessionPool.maxWaiters)
      .withSessionPool
      .ttl(Duration.fromSeconds(cfg.sessionPool.ttl))
      .withRequestTimeout(Duration.fromSeconds(cfg.requestTimeout))
      .withTls(s"${cfg.host}")
      .newClient(s"${cfg.host}:443")
      .toService

    retryFilter andThen baseClient
  }
}
