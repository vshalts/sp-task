package com.vshalts.validator
package service.store

import org.typelevel.log4cats.Logger
import cats.implicits._
import cats.effect.{Async, Resource}
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import domain.BusinessError.KeyNotFoundError
import config.AwsConfig
import resource.AwsClient.AwsClient
import io.minio._
import io.minio.errors.ErrorResponseException
import retry.RetryPolicies.{constantDelay, limitRetries}
import retry._

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.util.Base64

class AwsS3KeyValueStore[F[_]: Logger: Sleep: Async] private (
    config: AwsConfig,
    awsClient: AwsClient
) extends KeyValueStore[F] {

  override def put(id: String, value: String): F[Unit] = {
    val logic =
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

    for {
      _ <- Logger[F].debug(s"AWS: Storing id: $id")
      _ <- retryLogic(logic, s"Error storing $id")
    } yield ()
  }

  override def get(id: String): F[String] = {
    val logic = Async[F]
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

    for {
      _ <- Logger[F].debug(s"AWS: Getting value for '$id'")
      value <- retryLogic(logic, s"Error getting '$id'")
    } yield value
  }

  private def retryLogic[A](logic: F[A], errorMessage: => String) = {
    retryingOnAllErrors(
      policy = limitRetries[F](config.retryCount) join constantDelay[F](
        config.retryDelay
      ),
      onError = noop[F, Throwable]
    )(logic).onError {
      case KeyNotFoundError(id) =>
        Logger[F].debug(s"AWS: Can't find '$id'")
      case e: Throwable =>
        Logger[F].error(s"AWS: $errorMessage due to error: ${e.getMessage}")
    }
  }

  private def idToObjectName(id: String) =
    Base64.getUrlEncoder.encodeToString(id.getBytes("UTF-8"))
}

object AwsS3KeyValueStore {
  def make[F[_]: Logger: Sleep: Async](
      config: AwsConfig,
      awsClient: AwsClient
  ) = {
    Resource.pure[F, KeyValueStore[F]](
      new AwsS3KeyValueStore[F](config, awsClient)
    )
  }
}
