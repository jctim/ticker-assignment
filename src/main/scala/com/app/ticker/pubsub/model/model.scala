package com.app.ticker.pubsub.model

import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

import java.time.LocalDateTime

case class TickerName(value: String)

case class Ticker(prices: Prices, size: BigDecimal)
case class Prices(bid: BigDecimal, ask: BigDecimal)

case class TickerTime(value: LocalDateTime)
case class TickerNamed(name: String, ticker: Ticker)
//case class TickerNamedAggregated(name: String, tickers: List[Ticker])

object Codecs {
  implicit lazy val tickerNameEncoder: Encoder[TickerName] = deriveUnwrappedEncoder
  implicit lazy val tickerEncoder: Encoder[Ticker] = deriveEncoder
  implicit lazy val pricesEncoder: Encoder[Prices] = deriveEncoder

  implicit lazy val tickerNameDecoder: Decoder[TickerName] = deriveUnwrappedDecoder
  implicit lazy val tickerDecoder: Decoder[Ticker] = deriveDecoder
  implicit lazy val pricesDecoder: Decoder[Prices] = deriveDecoder

  implicit lazy val tickerTimeEncoder: Encoder[TickerTime] = deriveUnwrappedEncoder
  implicit lazy val tickerTimeDecoder: Decoder[TickerTime] = deriveUnwrappedDecoder
  implicit lazy val tickerNamedEncoder: Encoder[TickerNamed] = deriveEncoder
  implicit lazy val tickerNamedDecoder: Decoder[TickerNamed] = deriveDecoder
//  implicit lazy val tickerNamedAggregatedEncoder: Encoder[TickerNamedAggregated] = deriveEncoder
//  implicit lazy val tickerNamedAggregatedDecoder: Decoder[TickerNamedAggregated] = deriveDecoder
}
