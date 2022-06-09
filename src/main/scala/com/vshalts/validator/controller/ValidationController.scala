package com.vshalts.validator
package controller

import cats.effect._
import cats.implicits._
import com.vshalts.validator.service.validation.ValidationService
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.http4s.Http4sServerInterpreter
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
            jsonInvalidResponse[InvalidJsonResponse]("Some error with document")
          ),
          oneOfVariant(
            StatusCode.BadRequest,
            jsonInvalidResponse[ValidationFailedResponse](
              "Some error with document"
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

  val routes: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(descriptions)
}
