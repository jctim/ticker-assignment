package com.app.ticker.pubsub

import com.app.ticker.core.TickerAlertStreamConfig
import com.app.ticker.pubsub.model.{Ticker, TickerName, TickerNamed, TickerTime}
import com.app.ticker.util.Logging
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{Branched, Consumed, Grouped, Materialized, Produced}
import org.apache.kafka.streams.{KafkaStreams, Topology}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class TickerAlertingStream(config: TickerAlertStreamConfig)(implicit ec: ExecutionContext) extends CirceSupport with Logging {
  import com.app.ticker.pubsub.model.Codecs._

  private lazy val configAsProps = KafkaUtil.configAsProps(config.kafka)

  KafkaUtil.createTopicIfNotExists(config.dstTopicName, configAsProps)
  KafkaUtil.createTopicIfNotExists("tickers_timed", configAsProps)

  val tickersByTimeBuilder: StreamsBuilder = new StreamsBuilder

  tickersByTimeBuilder
    .stream[TickerName, Ticker](config.srcTopicName)(Consumed.`with`)
    .map((key, value) => (TickerTime(minutesNow()), TickerNamed(key.value, value)))
    .groupByKey(Grouped.`with`)
    .aggregate(Map.empty[String, List[Ticker]])((_, t, acc) => aggregateTickers(t, acc))(Materialized.as("tickers_timed_store"))
    .toStream
    .to("tickers_timed")(Produced.`with`)

  val topology1: Topology = tickersByTimeBuilder.build()

  private def minutesNow() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

  private def aggregateTickers(t: TickerNamed, acc: Map[String, List[Ticker]]): Map[String, List[Ticker]] = {
    acc.get(t.name) match {
      case None           => acc + (t.name -> List(t.ticker))
      case Some(existing) => acc.updated(t.name, existing :+ t.ticker)
    }
  }

  private val tickersByTimeStreams: KafkaStreams = new KafkaStreams(topology1, configAsProps)
  logger.info(s"Topology 1: ${topology1.describe()}")

  def start(): Future[Unit] = Future {
    tickersByTimeStreams.start()
  }

  sys.addShutdownHook {
    tickersByTimeStreams.close()
  }

}
