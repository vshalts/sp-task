import sbt._

object Dependencies {

  val catsCore = Seq("org.typelevel" %% "cats-core" % "2.7.0")
  val catsEffect = Seq("org.typelevel" %% "cats-effect" % "3.3.12")

  val log4cats = Seq(
    "log4cats-core",
    "log4cats-slf4j"
  ) map ("org.typelevel" %% _ % "2.3.1")

  val http4s = Seq(
    "http4s-dsl",
    "http4s-circe",
    "http4s-ember-server"
  ) map ("org.http4s" %% _ % "0.23.12")

  val pureconfig = Seq(
    "pureconfig",
    "pureconfig-cats-effect",
    "pureconfig-ip4s"
  ) map ("com.github.pureconfig" %% _ % "0.17.1")

  val logback = Seq("ch.qos.logback" % "logback-classic" % "1.2.11")

  val circe: Seq[ModuleID] = (Seq(
    "circe-core",
    "circe-generic",
    "circe-generic-extras",
    "circe-parser"
  ) map ("io.circe" %% _ % "0.14.1"))

  val scalacache = Seq(
    "scalacache-caffeine",
    "scalacache-cats-effect"
  ) map ("com.github.cb372" %% _ % "0.28.0")

  val testcontainers = Seq(
    "testcontainers-scala-scalatest",
    "testcontainers-scala-localstack"
  ) map ("com.dimafeng" %% _ % "0.40.7")

  val scalatest = Seq("org.scalatest" %% "scalatest" % "3.2.12")

  val catsEffectScalatest =
    Seq("org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0")

  val tapir = Seq(
    "tapir-http4s-server",
    "tapir-openapi-docs",
    "tapir-json-circe",
    "tapir-redoc-bundle"
  ) map ("com.softwaremill.sttp.tapir" %% _ % "1.0.0-RC3")

  val openapi = Seq(
    "openapi-circe",
    "openapi-circe-yaml"
  ) map ("com.softwaremill.sttp.apispec" %% _ % "0.2.1")

  val awssdk = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.12.221")

  val kindProjector =
    "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full

  val betterMonadicFor = Seq("com.olegpy" %% "better-monadic-for" % "0.3.1")

  val minio = Seq("io.minio" % "minio" % "8.4.1")

  val jsonSchemaValidator = Seq(
    "com.github.java-json-tools" % "json-schema-validator" % "2.2.14"
  )

  val catsRetry = Seq("com.github.cb372" %% "cats-retry" % "3.1.0")

}
