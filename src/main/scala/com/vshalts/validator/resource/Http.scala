package com.vshalts.validator
package resource

import cats.effect._
import config.ApiConfig
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.syntax.all._

object Http {

  def makeHttpServer[F[_]: Async](
      routes: HttpRoutes[F],
      config: ApiConfig
  ): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(routes.orNotFound)
      .build

}
