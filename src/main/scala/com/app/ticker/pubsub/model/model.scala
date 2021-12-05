package com.app.ticker.pubsub.model

import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder

case class TickerName(value: String)

case class Ticker(prices: Prices, size: BigDecimal)
case class Prices(bid: BigDecimal, ask: BigDecimal)

object Codecs {
  implicit lazy val tickerNameSerializer: Encoder[TickerName] = deriveUnwrappedEncoder
  implicit lazy val tickerSerializer: Encoder[Ticker] = deriveEncoder
  implicit lazy val pricesSerializer: Encoder[Prices] = deriveEncoder
}
