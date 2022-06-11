package com.vshalts.validator

import cats.effect.{Async, IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.comcast.ip4s.{Hostname, Port}
import com.dimafeng.testcontainers.LocalStackContainer
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import config.AwsConfig
import resource.AwsClient
import service.store.{AwsS3KeyValueStore, KeyValueStore}
import org.scalatest._
import flatspec._
import io.minio.MakeBucketArgs
import matchers._
import org.testcontainers.containers.localstack.{
  LocalStackContainer => JavaLocalStackContainer
}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

trait IntegrationTest
    extends AsyncFlatSpec
    with AsyncIOSpec
    with should.Matchers
    with TestContainersForAll {

  override type Containers = LocalStackContainer

  override def startContainers(): Containers = {
    val localStackContainer = LocalStackContainer
      .Def(tag = "0.14.3.1", services = Seq(JavaLocalStackContainer.Service.S3))
      .start()
    localStackContainer
  }

  def withAwsStore[A](f: KeyValueStore[IO] => IO[A]) = withContainers {
    localStack =>
      val awsConfig = (for {
        host <- Hostname.fromString(localStack.host)
        port <- Port.fromInt(localStack.mappedPort(4566))
      } yield AwsConfig(
        accessKey = "fake",
        secretKey = "fake",
        host = host,
        port = port,
        secure = false,
        region = "us-east-1",
        bucket = "validator",
        retryCount = 1,
        retryDelay = 100.millis
      )).getOrElse(throw new RuntimeException("Unexpected error"))

      implicit val logger = Slf4jLogger.getLogger[IO]

      val makeBucketArgs = MakeBucketArgs
        .builder()
        .bucket(awsConfig.bucket)
        .region(awsConfig.region)
        .build()

      val awsStore = for {
        client <- AwsClient.makeAwsClient[IO](awsConfig)
        _ <- Resource.eval(
          Async[IO].blocking(client.makeBucket(makeBucketArgs))
        )
        store <- AwsS3KeyValueStore.make[IO](awsConfig, client)
      } yield store

      awsStore.use(f)
  }
}
