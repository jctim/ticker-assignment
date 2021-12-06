package com.app.ticker.core

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.app.ticker.client.SymbolTickerClient
import com.app.ticker.client.model.Request.WsConnectionDetails
import com.app.ticker.client.model.Response.TickerData
import com.app.ticker.pubsub.{TickerAlertingStream, TickerDataPublisher}
import com.app.ticker.pubsub.model.{Prices, Ticker, TickerName}
import com.app.ticker.util.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

object MainApp extends App with Logging {

  logger.info("Init Akka")
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val mat: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val http: HttpExt = Http()

  logger.info("Init App Components")
  val config = AppConfig()
  val tickerApi = new SymbolTickerClient(config.tickerApi)
  val producer = new TickerDataPublisher(config.tickerProducer)(system.dispatchers.lookup("blocking-io-dispatcher"))
  val tickerAlertStream = new TickerAlertingStream(config.tickerAlertStream)

  val tickerDataFlow = Flow[(String, TickerData)]
    .map { case (subject, data) => (TickerName(subject), Ticker(Prices(data.bestBid, data.bestAsk), data.size)) }
    .map { (producer.publishTicker _).tupled }

  logger.info("Starting main flow")
  val res = for {
    publicDetails <- tickerApi.obtainBulletPublicDetails
    connectId      = "myTickerClient-" + Random.nextInt(1000)
    wsConnection   = WsConnectionDetails(
                       publicDetails.data.token,
                       publicDetails.data.instanceServers.head.endpoint,
                       publicDetails.data.instanceServers.head.pingInterval.millis
                     )
    _             <- tickerApi.connectToWs(wsConnection, connectId, tickerDataFlow)
    _             <- tickerAlertStream.start()
  } yield ()

  logger.info("Waiting for terminating")

  // TODO https://doc.akka.io/docs/akka/2.6/coordinated-shutdown.html
}
