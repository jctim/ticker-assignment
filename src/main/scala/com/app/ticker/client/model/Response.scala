package com.app.ticker.client.model

import akka.http.scaladsl.model.Uri
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder

object Response {
  case class BulletPublic(code: Int, data: Data)
  case class Data(instanceServers: List[InstanceServer], token: String)
  case class InstanceServer(
      endpoint: Uri,
      protocol: String,
      encrypt: Boolean,
      pingInterval: Long, // ms
      pingTimeout: Long   // ms
  )

  sealed trait WsResponse {
    def `type`: String
  }

  case class WelcomeMessage(id: String, `type`: String = "welcome") extends WsResponse
  case class AckMessage(id: String, `type`: String = "ack")         extends WsResponse
  case class PongMessage(id: String, `type`: String = "pong")       extends WsResponse
  case class TickerMessage(
      topic: String,
      subject: String,
      data: TickerData,
      `type`: String = "message"
  ) extends WsResponse

  case class TickerData(
      bestAsk: BigDecimal,
      bestAskSize: BigDecimal,
      bestBid: BigDecimal,
      bestBidSize: BigDecimal,
      price: BigDecimal,
      sequence: Long,
      size: BigDecimal,
      time: Long
  )

  object Codecs {
    implicit lazy val uriDecoder: Decoder[Uri] = Decoder[String].map(Uri(_))

    implicit lazy val bulletPublicDecoder: Decoder[BulletPublic] = deriveDecoder
    implicit lazy val dataDecoder: Decoder[Data] = deriveDecoder
    implicit lazy val instanceServerDecoder: Decoder[InstanceServer] = deriveDecoder

    implicit lazy val tickerDataDecoder: Decoder[TickerData] = deriveDecoder
    implicit lazy val wsResponseDecoder: Decoder[WsResponse] = Decoder.instance { c =>
      c.downField("type").as[String] flatMap {
        case "welcome" => Decoder[WelcomeMessage].tryDecode(c.root)
        case "ack"     => Decoder[AckMessage].tryDecode(c.root)
        case "pong"    => Decoder[PongMessage].tryDecode(c.root)
        case "message" => Decoder[TickerMessage].tryDecode(c.root)
      }
    }
  }

}
