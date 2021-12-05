package com.app.ticker.core

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import com.app.ticker.client.SymbolTickerClient
import com.app.ticker.client.model.Request.WsConnectionDetails
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

object MainApp extends App {
  val logger = LoggerFactory.getLogger(getClass.getName)

  logger.info("Init Akka")
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val mat: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val http: HttpExt = Http()

  logger.info("Init App Components")
  val config = AppConfig()
  val tickerApi = new SymbolTickerClient(config.tickerApi)
  // val producer = new KafkaProducer(config.kafka.producer)

  logger.info("Running main flow")
  val res = for {
    publicDetails <- tickerApi.obtainBulletPublicDetails
    wsConnection   = WsConnectionDetails(
                       publicDetails.data.token,
                       publicDetails.data.instanceServers.head.endpoint,
                       publicDetails.data.instanceServers.head.pingInterval.millis
                     )
    _             <- tickerApi.connectToWs(wsConnection, "myTickerClient-" + Random.nextInt(1000))
  } yield ()

  res.onComplete {
    case Success(())        => logger.info("successRes")
    case Failure(exception) => logger.error(exception.getMessage)
  }

  logger.info("Finish main flow")

  // TODO https://doc.akka.io/docs/akka/2.6/coordinated-shutdown.html
}
