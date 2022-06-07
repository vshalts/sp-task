package com.vshalts.validator
package resource

import cats.effect._
import config._
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._

object Config {

  def makeConfig[F[_]: Sync]: Resource[F, Config] =
    Resource.eval(ConfigSource.default.loadF[F, Config]())
}
