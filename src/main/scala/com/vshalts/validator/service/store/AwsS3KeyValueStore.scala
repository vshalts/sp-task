package com.vshalts.validator
package service.store

import cats.implicits._
import cats.effect.{Async, Resource}
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.vshalts.validator.domain.BusinessError.KeyNotFoundError
import config.AwsConfig
import resource.AwsClient.AwsClient
import io.minio._
import io.minio.errors.ErrorResponseException

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.util.Base64

class AwsS3KeyValueStore[F[_]: Async] private (
    config: AwsConfig,
    awsClient: AwsClient
) extends KeyValueStore[F] {

  def init(): F[Unit] =
    Async[F]
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
          KeyNotFoundError()
      }

  private def idToObjectName(id: String) =
    Base64.getUrlEncoder.encodeToString(id.getBytes("UTF-8"))
}

object AwsS3KeyValueStore {
  def make[F[_]: Async](config: AwsConfig, awsClient: AwsClient) = {
    for {
      store <- Resource.pure(new AwsS3KeyValueStore(config, awsClient))
      _ <- Resource.eval(store.init())
    } yield store: KeyValueStore[F]
  }
}
