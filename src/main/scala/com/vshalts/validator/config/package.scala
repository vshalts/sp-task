package com.vshalts.validator

import com.comcast.ip4s.{Hostname, Port}
import pureconfig.ConfigReader
import pureconfig.generic.semiauto._
import pureconfig.module.ip4s._

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
      bucket: String
  )

  object AwsConfig {
    implicit val configReader: ConfigReader[AwsConfig] = deriveReader
  }

  case class Config(api: ApiConfig, aws: AwsConfig)

  object Config {
    implicit val configReader: ConfigReader[Config] = deriveReader
  }
}
