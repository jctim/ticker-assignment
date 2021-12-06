package com.app.ticker.pubsub

import com.app.ticker.core.TickerProducerConfig
import com.app.ticker.pubsub.model.{Ticker, TickerName}
import com.app.ticker.util.Logging
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}

import scala.concurrent.{ExecutionContext, Future, blocking}

class TickerDataPublisher(config: TickerProducerConfig)(implicit ec: ExecutionContext) extends CirceSupport with Logging {
  import com.app.ticker.pubsub.model.Codecs._

  private lazy val configAsProps = KafkaUtil.configAsProps(config.kafka)

  KafkaUtil.createTopicIfNotExists(config.topicName, configAsProps)

  private val producer = new KafkaProducer[TickerName, Ticker](
    configAsProps,
    toSerializer[TickerName],
    toSerializer[Ticker]
  )

  def publishTicker(name: TickerName, ticker: Ticker): Future[RecordMetadata] = {
    val record = new ProducerRecord[TickerName, Ticker](config.topicName, name, ticker)
    Future {
      blocking {
        producer.send(record).get()
      }
    }
  }

  sys.addShutdownHook {
    producer.close()
  }
}
