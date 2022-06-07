package com.vshalts.validator
package resource

import cats.effect._
import config.AwsConfig
import io.minio.MinioClient

object AwsClient {

  type AwsClient = MinioClient

  def makeAwsClient[F[_]: Sync](
      config: AwsConfig
  ): Resource[F, AwsClient] = {
    Resource.eval(
      Sync[F].delay {
        MinioClient.builder
          .endpoint(config.host.toString, config.port.value, config.secure)
          .credentials(
            config.accessKey,
            config.secretKey
          )
          .region(config.region)
          .build
      }
    )
  }

}
