package com.app.ticker.core

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

case class AppConfig(kafka: KafkaConfig, tickerApi: TickerApiConfig)

case class KafkaConfig(
    producer: Config,
    consumer: Config,
    serializer: Config,
    deserializer: Config,
    tickerTopicName: String,
    pollingTimeout: FiniteDuration,
    generatorPeriod: FiniteDuration,
    generatorParallelismLevel: Int = 1
)

case class TickerApiConfig(baseUrl: Uri, publicTokenPath: Path)

object AppConfig {
  import pureconfig._
  import pureconfig.generic.auto._

  implicit val uriReader: ConfigReader[Uri] = ConfigReader[String].map(uri => Uri(uri))
  implicit val pathReader: ConfigReader[Path] = ConfigReader[String].map(uri => Path(uri))

  def apply(): AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
}
