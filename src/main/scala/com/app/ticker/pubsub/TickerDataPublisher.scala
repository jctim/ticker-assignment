package com.app.ticker.pubsub

import com.app.ticker.pubsub.model.{Ticker, TickerName}
import com.app.ticker.util.Logging
import com.typesafe.config.Config
import io.github.azhur.kafkaserdecirce.CirceSupport
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.errors.TopicExistsException

import java.lang.{Integer => JInt, Short => JShort}
import java.util.Properties
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.Try

class TickerDataPublisher(topicName: String, producerConfig: Config)(implicit ec: ExecutionContext) extends CirceSupport with Logging {
  import com.app.ticker.pubsub.model.Codecs._

  createTopic(topicName, configAsMap)

  private val producer = new KafkaProducer[TickerName, Ticker](
    configAsMap.asJava,
    toSerializer[TickerName],
    toSerializer[Ticker]
  )

  def publishTicker(name: TickerName, ticker: Ticker): Future[RecordMetadata] = {
    val record = new ProducerRecord[TickerName, Ticker](topicName, name, ticker)
    Future {
      blocking {
        producer.send(record).get()
      }
    }
  }

  lazy val configAsMap: Map[String, AnyRef] =
    // todo try https://github.com/andr83/scalaconfig
    producerConfig
      .entrySet()
      .asScala
      .map(entry => entry.getKey -> entry.getValue.unwrapped())
      .toMap

  def createTopic(topic: String, clusterConfig: Map[String, AnyRef]): Unit = {
    val newTopic = new NewTopic(topic, Option.empty[JInt].toJava, Option.empty[JShort].toJava);
    val adminClient = AdminClient.create(clusterConfig.asJava)
    Try(adminClient.createTopics(List(newTopic).asJava).all.get)
      .map(_ => logger.debug(s"Topic '$topicName' has been created successfully"))
      .recover { case e: Exception =>
        // Ignore if TopicExistsException, which may be valid if topic exists
        if (!e.getCause.isInstanceOf[TopicExistsException]) throw new RuntimeException(e)
        logger.debug(s"Topic '$topicName' already exists")
      }
    adminClient.close()
  }
}
