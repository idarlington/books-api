package com.idarlington.books

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.implicits._
import com.idarlington.books.api.{BooksApi, EndpointsServer, HttpServer}
import com.idarlington.books.config.Config
import com.idarlington.books.middleware.{LoggingMiddleWare, RequestMeter}
import com.idarlington.books.resources.{Cache, Meters}
import com.idarlington.books.services.NewYorkTimesService
import com.idarlington.books.services.clients.NewYorkTimesClient
import com.twitter.server.TwitterServer
import io.finch.EndpointModule
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends TwitterServer with EndpointModule[IO] {

  private lazy val adminServer =
    Resource
      .make(IO(adminHttpServer))(listeningServer => IO(listeningServer.close()))

  def main(): Unit =
    Config.appConfig
      .load[IO]
      .flatMap { cfg =>
        Slf4jLogger.create[IO].flatMap { logger =>
          Cache.booksCache[IO].flatMap { cache =>
            val serverConfig    = cfg.serverConfig
            val logging         = new LoggingMiddleWare[IO](logger)
            val requestMeter    = new RequestMeter[IO](Meters.all.minute, Meters.all.daily, cache)
            val nytClient       = NewYorkTimesClient.client(cfg.nytService.client)
            val nytBooksService = new NewYorkTimesService[IO](cfg.nytService, cache, nytClient)
            val booksApi        = new BooksApi[IO](nytBooksService)
            val service         = EndpointsServer.service(booksApi.route, logging.route, requestMeter.meter)

            (
              HttpServer.make(serverConfig, statsReceiver, service).useForever,
              adminServer.useForever
            ).parTupled
          }
        }
      }
      .onError(e => IO(exitOnError(e)))
      .unsafeRunSync()
}
