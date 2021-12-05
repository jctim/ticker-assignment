package com.app.ticker.client

import akka.actor.ActorRef
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.{Done, NotUsed}
import com.app.ticker.client.model.Request.{PingMessage, WsConnectionDetails, WsSubscribeToTopic}
import com.app.ticker.client.model.Response.WsResponse
import com.app.ticker.client.model.{Request, Response}
import com.app.ticker.core.TickerApiConfig
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.jawn.decode
import io.circe.syntax._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SymbolTickerClient(ticketApiConfig: TickerApiConfig)(implicit
    http: HttpExt,
    ec: ExecutionContext,
    mat: Materializer
) extends FailFastCirceSupport {

  import Request.Codecs._
  import Response.Codecs._

  private val logger = LoggerFactory.getLogger(getClass.getName)

  private val publicTokenReq = Post(ticketApiConfig.baseUrl.withPath(ticketApiConfig.publicTokenPath))

  def obtainBulletPublicDetails: Future[Response.BulletPublic] = {
    http
      .singleRequest(publicTokenReq)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          Unmarshal(entity)
            .to[Response.BulletPublic] // TODO handle decoding errors
            .map(x => { logger.info("BulletPublic Response: " + x); x }) // TODO logIt function
        case _                                          =>
          sys.error("something wrong") // TODO handle error
      }
  }

  def connectToWs(wsDetails: WsConnectionDetails, connectId: String): Future[Done] = {
    val WsConnectionDetails(token, wsEndpoint, pingInterval) = wsDetails

    val req = WebSocketRequest(wsEndpoint.withQuery(Query("token" -> token, "connectId" -> connectId)))
    val webSocketFlow = http.webSocketClientFlow(req)

    val subscribeMsg = WsSubscribeToTopic(
      connectId,
      "/market/ticker:all", // "/market/ticker:BTC-USDT,ETH-USDT",
      response = true
    )

    val pingMsg = PingMessage(connectId, "ping")

    val messageSource: Source[Message, ActorRef] = Source.actorRef[TextMessage.Strict](
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = 50,
      overflowStrategy = OverflowStrategy.fail
    )

    val messageSink: Sink[Message, NotUsed] =
      Flow[Message]
        .mapAsync(1) {
          case TextMessage.Strict(text)         =>
            logger.debug("strict   message: " + text)
            Future.successful(text)
          case TextMessage.Streamed(textStream) =>
            textStream
              .runReduce(_ ++ _)
              .map(msg => { logger.debug("streamed message: " + msg); msg })
        }
        .map { message =>
          decode[WsResponse](message)
            .map(resp => { logger.info("decoded response: " + resp); resp })
            .toTry
        }
        .to(Sink.ignore)

    val ((ws, upgradeResponse), closed) =
      messageSource
        .viaMat(webSocketFlow)(Keep.both)
        .toMat(messageSink)(Keep.both)
        .run()

    val connected = upgradeResponse.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        logger.info("!CONNECTED!")
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    ws ! TextMessage.Strict(subscribeMsg.asJson.noSpaces)

    // TODO or
    // system.scheduler.scheduleWithFixedDelay(10.seconds, 30.seconds)(
    // () => {
    //     println("Action")
    //   }
    // )
    Source.tick(pingInterval / 2, pingInterval, Done).runForeach { _ =>
      logger.debug(s"Pinging msg : $pingInterval.")
      ws ! TextMessage.Strict(pingMsg.asJson.noSpaces)
    }

    // in a real application you would not side effect here
    // and handle errors more carefully
    // connected.onComplete(println)
    // closed.foreach(_ => println("closed"))
    connected
  }

//  def obtainWsFeedFlow(wsDetails: WsConnectionDetails, connectId: String): Future[Done] = {
//    val WsConnectionDetails(wsEndpoint, wsToken) = wsDetails
//    val webSocketFlow = http
//      .webSocketClientFlow(wsEndpoint.withQuery(Query("token" -> wsToken, "connectId" -> connectId)))
//
//    val messageSink: Sink[Message, Future[Done]] =
//      Sink.foreach[Message] {
//        case message: TextMessage.Strict =>
//          println("resp: " + message.text)
//        case t                           =>
//          // ignore other message types
//          println("respt: " + t)
//      }
//
//    val subscribeMsg = WsSubscribeToTopic(
//      connectId,
//      "subscribe",
//      "/market/ticker:all",
//      privateChannel = false,
//      response = true
//    )
//
//    import Request.Codecs._
//    import io.circe.syntax._
//    val messageSource = Source.single(
//      TextMessage.Strict(subscribeMsg.asJson.noSpaces)
//    )
////    val (f1, f2) = messageSource
////      .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
////      .toMat(messageSink)(Keep.both)     // also keep the Future[Done]
////      .run()
//    val ((upgradeResponse), closed) =
//      messageSource
//        .viaMat(webSocketFlow)(Keep.right)
//        .toMat(messageSink)(Keep.both)
//        .run()
//
////    f1.onComplete {
////      case Success(value) => println("s1: " + value)
////      case Failure(ex)    => println("f1: " + ex)
////    }
////    f2.onComplete {
////      case Success(value) => println("s2: " + value)
////      case Failure(ex)    => println("f2: " + ex)
////    }
//    val connected = upgradeResponse.flatMap { upgrade =>
//      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
//        Future.successful(Done)
//      } else {
//        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
//      }
//    }
//
//    connected
//  }
//
//  def obtainWsFeedFlow2(wsDetails: WsConnectionDetails, connectId: String): Future[Done] = {
//    import Request.Codecs._
//    import io.circe.syntax._
//
//    val WsConnectionDetails(wsEndpoint, wsToken) = wsDetails
//    val req = WebSocketRequest(wsEndpoint.withQuery(Query("token" -> wsToken, "connectId" -> connectId)))
//
//    val messageSink: Sink[Message, Future[Done]] =
//      Sink.foreach[Message] {
//        case message: TextMessage.Strict => println("resp: " + message.text)
//        case t                           => println("respt: " + t) // ignore other message types
//      }
//
//    val subscribeMsg = WsSubscribeToTopic(
//      connectId,
//      "subscribe",
//      "/market/ticker:all",
//      privateChannel = false,
//      response = true
//    )
//
//    val messageSource = Source.single(TextMessage.Strict(subscribeMsg.asJson.noSpaces))
//
//    val flow: Flow[Message, Message, NotUsed] =
//      Flow.fromSinkAndSource(Sink.foreach(println), messageSource)
//
//    // val webSocketFlow = http.webSocketClientFlow(req)
//
//    val (upgradeResponse, closed) = http.singleWebSocketRequest(req, flow)
//    val connected = upgradeResponse.map { upgrade =>
//      // just like a regular http request we can access response status which is available via upgrade.response.status
//      // status code 101 (Switching Protocols) indicates that server support WebSockets
//      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
//        println("connected")
//        Done
//      } else {
//        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
//      }
//    }
//    connected.onComplete(println)
//    // closed.foreach(_ => println("closed"))
//    connected
//  }
}
