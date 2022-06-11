package com.vshalts.validator
package controller

import cats.effect._
import cats.implicits._
import com.vshalts.validator.service.validation.ValidationService
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import domain._

class ValidationController[F[_]: Async](
    validationService: ValidationService[F]
) extends BaseController {
  override val controllerPrefix = "validate"

  val validateAction = "validateDocument"

  val validateDocumentEndpoint =
    baseEndpoint.post
      .name(validateAction)
      .description("Validate document")
      .in(schemaIdPath)
      .in(customCodecJsonBody[DocumentBody].description("Source json document"))
      .out(jsonValidResponse[ValidResponse]("Json document validated"))
      .errorOut(
        oneOfInvalidResponse(
          oneOfVariant(
            StatusCode.BadRequest,
            jsonInvalidResponse[InvalidJsonResponse](
              "Document is not valid json"
            )
          ),
          oneOfVariant(
            StatusCode.BadRequest,
            jsonInvalidResponse[ValidationFailedResponse](
              "Document is not valid according to schema"
            )
          )
        )
      )
      .serverLogic { case (schemaId, documentBody) =>
        validationService
          .validateDocumentBySchemaId(documentBody, schemaId)
          .redeem(
            t => Left(handleErrors(validateAction, schemaId.id, t)),
            r =>
              r match {
                case Right(_) =>
                  Right(ValidResponse(validateAction, schemaId.id))
                case Left(errorMessage) =>
                  Left(
                    ValidationFailedResponse(
                      validateAction,
                      schemaId.id,
                      errorMessage
                    )
                  )
              }
          )
      }

  val descriptions = List(validateDocumentEndpoint)
}
