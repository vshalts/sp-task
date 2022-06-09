package com.vshalts.validator
package controller

import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.auto._
import domain._
import io.circe.{Decoder, Encoder}
import sttp.model.StatusCode
import InvalidResponse._
import domain.BusinessError._
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.EndpointOutput.OneOfVariant

trait BaseController {
  def controllerPrefix: String

  implicit val schemaBodyEncoder: Encoder[SchemaBody] = deriveUnwrappedEncoder
  implicit val schemaBodyDecoder: Decoder[SchemaBody] = deriveUnwrappedDecoder

  implicit lazy val schemaBodyCodec: JsonCodec[SchemaBody] =
    Codec.json(raw => DecodeResult.Value(SchemaBody(raw)))(_.content)

  implicit lazy val documentBodyCodec: JsonCodec[DocumentBody] =
    Codec.json(raw => DecodeResult.Value(DocumentBody(raw)))(_.content)

  val invalidResponseSchema: Schema[InvalidResponseSchema] = Schema.derived
  val validResponseSchema: Schema[ValidResponseSchema] = Schema.derived

  def jsonInvalidResponse[T <: InvalidResponse: Encoder: Decoder: Schema](
      description: String
  ) =
    jsonBody[T].schema(invalidResponseSchema.as[T]).description(description)

  def jsonValidResponse[T <: ValidResponse: Encoder: Decoder: Schema](
      description: String
  ) =
    jsonBody[T].schema(validResponseSchema.as[T]).description(description)

  def oneOfInvalidResponse(
      firstVariant: OneOfVariant[_ <: InvalidResponse],
      otherVariants: OneOfVariant[_ <: InvalidResponse]*
  ) = {
    oneOf[InvalidResponse](
      firstVariant,
      (oneOfVariant(
        StatusCode.InternalServerError,
        jsonInvalidResponse[UnknownResponse]("Internal error")
      ) ::
        otherVariants.toList): _*
    )
  }

  def baseEndpoint =
    endpoint
      .in(controllerPrefix)

  def handleErrors(
      action: String,
      id: String,
      error: Throwable
  ): InvalidResponse = {
    error match {
      case InvalidJsonError(details) => InvalidJsonResponse(action, id, details)
      case SchemaNotFoundError(_)    => SchemaNotFoundResponse(action, id)
      case _                         => UnknownResponse(action, id)
    }
  }

  val schemaIdPath =
    path[SchemaId].name("schemaId").description("Schema id")
}
