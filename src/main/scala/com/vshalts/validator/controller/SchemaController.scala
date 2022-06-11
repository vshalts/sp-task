package com.vshalts.validator
package controller

import cats.effect._
import cats.implicits._
import service.schema.SchemaService
import domain._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._

class SchemaController[F[_]: Async](
    schemaService: SchemaService[F]
) extends BaseController {
  override val controllerPrefix = "schema"

  val uploadAction = "uploadSchema"
  val downloadAction = "downloadSchema"

  val uploadEndpoint =
    baseEndpoint.post
      .name(uploadAction)
      .description("Upload schema")
      .in(schemaIdPath)
      .in(customCodecJsonBody[SchemaBody].description("Source json schema"))
      .out(
        oneOf[ValidResponse](
          oneOfVariant(
            StatusCode.Created,
            jsonValidResponse[ValidResponse]("Schema should be uploaded")
          )
        )
      )
      .errorOut(
        oneOfInvalidResponse(
          oneOfVariant(
            StatusCode.BadRequest,
            jsonInvalidResponse[InvalidJsonResponse]("Invalid json for schema")
          )
        )
      )
      .serverLogic { case (schemaId, schemaBody) =>
        schemaService
          .uploadSchema(schemaId, schemaBody)
          .redeem(
            t => Left(handleErrors(uploadAction, schemaId.id, t)),
            _ => Right(ValidResponse(uploadAction, schemaId.id))
          )
      }

  val downloadEndpoint =
    baseEndpoint.get
      .name(downloadAction)
      .description("Download schema")
      .in(schemaIdPath)
      .out(customCodecJsonBody[SchemaBody].description("Json schema body"))
      .errorOut(
        oneOfInvalidResponse(
          oneOfVariant(
            StatusCode.NotFound,
            jsonInvalidResponse[SchemaNotFoundResponse]("Schema not found")
          )
        )
      )
      .serverLogic { schemaId =>
        schemaService
          .downloadSchema(schemaId)
          .redeem(
            t => Left(handleErrors(downloadAction, schemaId.id, t)),
            f => Right(f)
          )
      }

  val descriptions = List(uploadEndpoint, downloadEndpoint)
}
