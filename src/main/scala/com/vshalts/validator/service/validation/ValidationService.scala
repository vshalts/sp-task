package com.vshalts.validator
package service.validation

import cats.implicits._
import cats.effect._
import cats.effect.kernel.Async
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.vshalts.validator.utils.JsonHelpers
import service.schema.SchemaService
import domain._

trait ValidationService[F[_]] {

  def validateDocument(
      document: DocumentBody,
      schemaBody: SchemaBody
  ): F[Either[String, Unit]]

  def validateDocumentBySchemaId(
      document: DocumentBody,
      schemaId: SchemaId
  ): F[Either[String, Unit]]

}

object ValidationService {

  def make[F[_]: Async](
      schemaService: SchemaService[F]
  ): Resource[F, ValidationService[F]] =
    Resource.pure(new ValidationServiceImpl[F](schemaService))

  private class ValidationServiceImpl[F[_]: Async](
      schemaService: SchemaService[F]
  ) extends ValidationService[F] {

    def validateDocument(
        document: DocumentBody,
        schemaBody: SchemaBody
    ): F[Either[String, Unit]] =
      for {
        documentJsonStr <- JsonHelpers.sanitizeJsonString(document.content)
        res <- Async[F].blocking {
          val schemaNode = JsonLoader.fromString(schemaBody.content)
          val documentNode = JsonLoader.fromString(documentJsonStr)
          val factory = JsonSchemaFactory.byDefault
          val schema = factory.getJsonSchema(schemaNode)
          val report = schema.validate(documentNode)
          if (report.isSuccess)
            Right(())
          else
            Left(report.iterator().next().getMessage)
        }
      } yield res

    def validateDocumentBySchemaId(
        document: DocumentBody,
        schemaId: SchemaId
    ): F[Either[String, Unit]] =
      for {
        schemaBody <- schemaService.downloadSchema(schemaId)
        result <- validateDocument(document, schemaBody)
      } yield result
  }
}
