import Dependencies._

ThisBuild / scalaVersion     := "2.13.7"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "ticker-assignment",
    libraryDependencies ++= scalaTest.map(_ % Test),
    libraryDependencies ++= akkaHttp ++ circe ++ akkaHttpCirce ++ pureConfig ++ kafka ++ kafkaSerdeCirce ++ logback,
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      // "-Xfatal-warnings",
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
