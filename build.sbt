import Dependencies._

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / organization := "com.vshalts.validator"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "json-validator",
    // main dependencies
    libraryDependencies ++= catsCore ++ catsEffect ++ logback ++ tapir ++ openapi ++
      log4cats ++ http4s ++ pureconfig ++ betterMonadicFor ++ circe ++ minio ++
      jsonSchemaValidator ++ catsRetry ++ scaffeine,
    // test dependencies
    libraryDependencies ++= (scalatest ++ catsEffectScalatest ++ testcontainers ++ awssdk)
      .map(_ % Test),
    dockerExposedPorts := Seq(
      sys.env.getOrElse("API_PORT", "9000").toInt
    ),
    dockerBaseImage := "adoptopenjdk/openjdk16:slim",
    run / fork := true,
    Test / fork := true
  )

addCompilerPlugin(kindProjector)
