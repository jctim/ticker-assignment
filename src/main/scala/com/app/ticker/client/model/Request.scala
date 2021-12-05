package com.app.ticker.client.model

import akka.http.scaladsl.model.Uri
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import scala.concurrent.duration.FiniteDuration

object Request {
  case class WsConnectionDetails(
      token: String,
      wsEndpointUri: Uri,
      pingInterval: FiniteDuration
  )

  sealed trait WsRequest {
    def `type`: String
  }

  case class WsSubscribeToTopic(
      id: String,
      topic: String,
      response: Boolean,
      privateChannel: Boolean = false,
      `type`: String = "subscribe"
  ) extends WsRequest

  case class PingMessage(
      id: String,
      `type`: String = "ping"
  ) extends WsRequest

  object Codecs {
    implicit lazy val subscribeMsgEncoder: Encoder[WsSubscribeToTopic] = deriveEncoder
    implicit lazy val pingMsgEncoder: Encoder[PingMessage] = deriveEncoder
  }
}
