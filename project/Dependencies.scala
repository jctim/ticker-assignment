import sbt._

object Versions {
  val Akka = "2.6.8"
  val AkkaHttp = "10.2.7"
  val Circe = "0.14.1"

}

object Dependencies {
  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.2.9")

  lazy val kafka = Seq(
    "org.apache.kafka" % "kafka-clients" % "2.6.0"
//    "io.confluent" % "kafka-avro-serializer" % "6.0.0"
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
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % Versions.Circe)

  lazy val akkaHttpCirce = Seq(
    "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"
  )

  lazy val logback = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}
