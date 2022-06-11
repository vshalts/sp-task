package com.vshalts.validator

import com.comcast.ip4s.{Hostname, Port}
import pureconfig.ConfigReader
import pureconfig.generic.semiauto._
import pureconfig.module.ip4s._
import scala.concurrent.duration.FiniteDuration

package config {

  case class ApiConfig(host: Hostname, port: Port)

  object ApiConfig {
    implicit val configReader: ConfigReader[ApiConfig] = deriveReader
  }

  case class AwsConfig(
      accessKey: String,
      secretKey: String,
      host: Hostname,
      port: Port,
      secure: Boolean,
      region: String,
      bucket: String,
      retryCount: Int,
      retryDelay: FiniteDuration
  )

  object AwsConfig {
    implicit val configReader: ConfigReader[AwsConfig] = deriveReader
  }

  case class CacheConfig(
      maximumSize: Long,
      expireAfter: FiniteDuration
  )

  object CacheConfig {
    implicit val configReader: ConfigReader[CacheConfig] = deriveReader
  }

  case class Config(api: ApiConfig, aws: AwsConfig, cache: CacheConfig)

  object Config {
    implicit val configReader: ConfigReader[Config] = deriveReader
  }
}
