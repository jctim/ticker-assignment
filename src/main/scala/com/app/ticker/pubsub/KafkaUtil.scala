package com.app.ticker.pubsub

import com.app.ticker.util.Logging
import com.typesafe.config.Config
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.common.errors.TopicExistsException

import java.lang.{Integer => JInt, Short => JShort}
import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.util.Try

object KafkaUtil extends Logging {

  def createTopicIfNotExists(topicName: String, clusterConfig: Properties): Unit = {
    val newTopic = new NewTopic(topicName, Option.empty[JInt].toJava, Option.empty[JShort].toJava);
    val adminClient = AdminClient.create(clusterConfig)
    Try(adminClient.createTopics(List(newTopic).asJava).all.get)
      .map(_ => logger.info(s"Topic '$topicName' has been created successfully"))
      .recover { case e: Exception =>
        // Ignore if TopicExistsException, which may be valid if topic exists
        if (!e.getCause.isInstanceOf[TopicExistsException]) throw new RuntimeException(e)
        logger.info(s"Topic '$topicName' already exists")
      }
    adminClient.close()
  }

  def configAsMap(config: Config): Map[String, AnyRef] =
    // todo try https://github.com/andr83/scalaconfig
    config
      .entrySet()
      .asScala
      .map(entry => entry.getKey -> entry.getValue.unwrapped())
      .toMap

  def configAsProps(config: Config): Properties = {
    val props = new Properties
    props.putAll(configAsMap(config).asJava)
    props
  }
}
