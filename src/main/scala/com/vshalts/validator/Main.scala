package com.vshalts.validator

import cats.effect._
import cats.implicits._
import service.store.AwsS3KeyValueStore
import service.validation.ValidationService
import service.schema.SchemaService
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j._
import resource.{AwsClient, Config, Http}
import controller.{SchemaController, ValidationController}
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
      validationService <- ValidationService.make[F](schemaService)
      schemaController = new SchemaController[F](schemaService)
      validationController = new ValidationController[F](validationService)

      descriptions =
        schemaController.descriptions ++ validationController.descriptions

      routes = Http4sServerInterpreter[F]().toRoutes(descriptions)

      docsEndpoints = RedocInterpreter().fromEndpoints[F](
        descriptions.map(_.endpoint),
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
