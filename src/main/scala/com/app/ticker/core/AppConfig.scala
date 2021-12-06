package com.app.ticker.core

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config.Config

case class AppConfig(
    tickerApi: TickerApiConfig,
    tickerProducer: TickerProducerConfig,
    tickerConsumer: TickerConsumerConfig,
    tickerAlertStream: TickerAlertStreamConfig
)

case class TickerApiConfig(baseUrl: Uri, publicTokenPath: Path)
case class TickerProducerConfig(kafka: Config, topicName: String)
case class TickerConsumerConfig(kafka: Config, topicName: String)
case class TickerAlertStreamConfig(kafka: Config, srcTopicName: String, dstTopicName: String)

object AppConfig {
  import pureconfig._
  import pureconfig.generic.auto._

  implicit val uriReader: ConfigReader[Uri] = ConfigReader[String].map(uri => Uri(uri))
  implicit val pathReader: ConfigReader[Path] = ConfigReader[String].map(uri => Path(uri))

  def apply(): AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
}
