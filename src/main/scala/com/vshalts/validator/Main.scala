package com.vshalts.validator

import cats.effect._
import cats.implicits._
import com.vshalts.validator.service.store.AwsS3KeyValueStore
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j._
import resource.{AwsClient, Config, Http}
import controller.SchemaController
import service.schema.SchemaService
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp.Simple {

  type F[A] = IO[A]

  implicit val logger: Logger[F] =
    LoggerFactory[F].getLogger

  override def run: IO[Unit] = {

    val program = for {
      config <- Config.makeConfig[F]
      bindAddress = s"${config.api.host}:${config.api.port.value}"
      _ <- Resource.eval(
        Logger[F].info(s"Starting server bound to address $bindAddress")
      )

      awsClient <- AwsClient.makeAwsClient[F](config.aws)
      keyValueStore <- AwsS3KeyValueStore.make[F](config.aws, awsClient)

      schemaService <- SchemaService.make[F](keyValueStore)
      schemaController = new SchemaController(schemaService)

      routes = schemaController.routes

      docsEndpoints = RedocInterpreter().fromEndpoints[F](
        schemaController.descriptions.map(_.endpoint),
        "Json validator",
        "1.0.0"
      )

      docsRoute =
        Http4sServerInterpreter[F]().toRoutes(docsEndpoints)

      allRoute = routes <+> docsRoute

      _ <- Http.makeHttpServer[F](allRoute, config.api)
    } yield ()

    program.useForever.unsafeRunSync()(runtime)
  }

}
