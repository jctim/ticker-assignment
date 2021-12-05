import sbt._

object Versions {
  val Akka = "2.6.8"
  val AkkaHttp = "10.2.7"
  val Circe = "0.14.1"
  val Kafka = "2.8.0"
  val KafkaSerde = "0.6.3"
}

object Dependencies {
  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.2.9")

  lazy val kafka = Seq(
    "org.apache.kafka"  % "kafka-clients"       % Versions.Kafka,
    "org.apache.kafka" %% "kafka-streams-scala" % Versions.Kafka
  )

  lazy val pureConfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.17.1",
    "com.typesafe"           % "config"     % "1.4.1"
  )

  lazy val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % Versions.Akka,
    "com.typesafe.akka" %% "akka-stream"      % Versions.Akka,
    "com.typesafe.akka" %% "akka-http"        % Versions.AkkaHttp
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core"           % Versions.Circe,
    "io.circe" %% "circe-generic"        % Versions.Circe,
    "io.circe" %% "circe-generic-extras" % Versions.Circe,
    "io.circe" %% "circe-parser"         % Versions.Circe
  )

  lazy val akkaHttpCirce = Seq(
    "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"
  )

  lazy val kafkaSerdeCirce = Seq(
    "io.github.azhur" %% "kafka-serde-circe" % Versions.KafkaSerde
  )

  lazy val logback = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}
