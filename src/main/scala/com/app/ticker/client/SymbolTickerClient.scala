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
import com.app.ticker.client.model.Response.{TickerData, TickerMessage, WsResponse}
import com.app.ticker.client.model.{Request, Response}
import com.app.ticker.core.TickerApiConfig
import com.app.ticker.util.Logging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.jawn.decode
import io.circe.syntax._
import org.slf4j.event.Level.DEBUG

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class SymbolTickerClient(ticketApiConfig: TickerApiConfig)(implicit
    http: HttpExt,
    ec: ExecutionContext,
    mat: Materializer
) extends FailFastCirceSupport
    with Logging {

  import Request.Codecs._
  import Response.Codecs._

  private val publicTokenReq = Post(ticketApiConfig.baseUrl.withPath(ticketApiConfig.publicTokenPath))

  def obtainBulletPublicDetails: Future[Response.BulletPublic] = {
    http
      .singleRequest(publicTokenReq)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          Unmarshal(entity)
            .to[Response.BulletPublic]
            .map(logIt("BulletPublic Response"))
        case _                                          =>
          sys.error("something wrong")
      }
  }

  def connectToWs(wsDetails: WsConnectionDetails, connectId: String, tickerDataFlow: Flow[(String, TickerData), Future[_], NotUsed]): Future[Done] = {
    val WsConnectionDetails(token, wsEndpoint, pingInterval) = wsDetails

    val req = WebSocketRequest(wsEndpoint.withQuery(Query("token" -> token, "connectId" -> connectId)))
    val webSocketFlow = http.webSocketClientFlow(req)

    val subscribeMsg = WsSubscribeToTopic(
      connectId,
      "/market/ticker:all",
      response = true
    )

    val pingMsg = PingMessage(connectId)

    val messageSource: Source[Message, ActorRef] = Source.actorRef[TextMessage.Strict](
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = 50,
      overflowStrategy = OverflowStrategy.fail
    )

    val flow: Flow[Message, (String, TickerData), NotUsed] =
      Flow[Message]
        .collect { case textMsg: TextMessage => textMsg }
        .mapAsync(1) {
          case TextMessage.Strict(text)         =>
            Future
              .successful(text)
              .map(logIt(s"strict   message", DEBUG))
          case TextMessage.Streamed(textStream) =>
            textStream
              .runReduce(_ ++ _)
              .map(logIt(s"streamed message", DEBUG))
        }
        .map { message =>
          decode[WsResponse](message)
            .map(logIt("decoded response", DEBUG))
            .toTry
        }
        .collect { case Success(TickerMessage(_, subject, data, _)) => (subject, data) }

    val ((ws, upgradeResponse), _) =
      messageSource
        .viaMat(webSocketFlow)(Keep.both)
        .via(flow)
        .via(tickerDataFlow)
        .toMat(Sink.ignore)(Keep.both)
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

    connected
  }
}
