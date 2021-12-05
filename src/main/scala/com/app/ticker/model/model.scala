package com.app.ticker.model

case class TickerName(value: String) extends AnyVal

case class Ticker(prices: Prices, size: Long)
case class Prices(bid: Double, ask: Double)
