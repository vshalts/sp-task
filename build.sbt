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
      log4cats ++ http4s ++ pureconfig ++ betterMonadicFor ++ circe ++ minio,
    // test dependencies
    libraryDependencies ++= (scalatest ++ catsEffectScalatest ++ testcontainers)
      .map(_ % Test),
    run / fork := true,
    Test / fork := true
  )

addCompilerPlugin(kindProjector)
