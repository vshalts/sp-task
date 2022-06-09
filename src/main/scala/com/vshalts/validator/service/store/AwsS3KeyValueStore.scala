package com.vshalts.validator
package service.store

import org.typelevel.log4cats.Logger
import cats.implicits._
import cats.effect.{Async, Resource}
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.vshalts.validator.domain.BusinessError.KeyNotFoundError
import config.AwsConfig
import resource.AwsClient.AwsClient
import io.minio._
import io.minio.errors.ErrorResponseException
import retry.RetryPolicies.{exponentialBackoff, limitRetries}
import retry._

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.util.Base64
import scala.concurrent.duration._

class AwsS3KeyValueStore[F[_]: Logger: Sleep: Async] private (
    config: AwsConfig,
    awsClient: AwsClient
) extends KeyValueStore[F] {

  def init(): F[Unit] = {
    val logic = Async[F]
      .blocking {
        val bucketExistsArgs = BucketExistsArgs
          .builder()
          .bucket(config.bucket)
          .region(config.region)
          .build()

        if (!awsClient.bucketExists(bucketExistsArgs)) {
          val makeBucketArgs = MakeBucketArgs
            .builder()
            .bucket(config.bucket)
            .region(config.region)
            .build()

          awsClient.makeBucket(makeBucketArgs)
        }
      }

    def logging: (Throwable, RetryDetails) => F[Unit] =
      (_, _) => Logger[F].info("Trying to connect to s3")

    retryingOnAllErrors(
      policy = limitRetries[F](8) join exponentialBackoff[F](200.milliseconds),
      onError = logging
    )(logic)
  }

  override def put(id: String, value: String): F[Unit] =
    Async[F].blocking {
      val args = PutObjectArgs
        .builder()
        .bucket(config.bucket)
        .region(config.region)
        .contentType("application/json")
        .`object`(idToObjectName(id))
        .stream(
          new ByteArrayInputStream(value.getBytes("UTF-8")),
          -1,
          10 * 1024 * 1024
        )
        .build()

      val _ = awsClient.putObject(args)
    }

  override def get(id: String): F[String] =
    Async[F]
      .blocking {
        val args = GetObjectArgs
          .builder()
          .bucket(config.bucket)
          .region(config.region)
          .`object`(idToObjectName(id))
          .build()

        val response = awsClient.getObject(args)
        CharStreams.toString(new InputStreamReader(response, Charsets.UTF_8));
      }
      .adaptError {
        case e: ErrorResponseException
            if e.errorResponse().code() == "NoSuchKey" =>
          KeyNotFoundError(id)
      }

  private def idToObjectName(id: String) =
    Base64.getUrlEncoder.encodeToString(id.getBytes("UTF-8"))
}

object AwsS3KeyValueStore {
  def make[F[_]: Logger: Sleep: Async](
      config: AwsConfig,
      awsClient: AwsClient
  ) = {
    for {
      store <- Resource.pure[F, AwsS3KeyValueStore[F]](
        new AwsS3KeyValueStore[F](config, awsClient)
      )
      _ <- Resource.eval(store.init())
    } yield store: KeyValueStore[F]
  }
}
