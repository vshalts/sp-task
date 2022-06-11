package com.vshalts.validator
package domain

import io.circe.{Encoder, Json}
import sttp.tapir.Schema.annotations.{description, encodedExample}

sealed trait Response {
  def action: String
  def id: String
}

trait InvalidResponse extends Response {
  def message: String
}

final case class InvalidResponseSchema(
    @description("Requested action")
    action: String,
    @description("Schema id")
    id: String,
    @description("Response status")
    status: String,
    @description("Error message")
    message: String
)

object InvalidResponse {
  implicit def encodeInvalidResponse[T <: InvalidResponse]: Encoder[T] =
    (a: T) =>
      Json.obj(
        ("action", Json.fromString(a.action)),
        ("id", Json.fromString(a.id)),
        ("status", Json.fromString("error")),
        ("message", Json.fromString(a.message))
      )
}

final case class ValidResponse(
    action: String,
    id: String
) extends Response

final case class ValidResponseSchema(
    @description("Requested action")
    action: String,
    @description("Schema id")
    id: String,
    @description("Response status")
    @encodedExample("success")
    status: String
)

object ValidResponse {
  implicit val encodeValidResponse: Encoder[ValidResponse] =
    (a: ValidResponse) =>
      Json.obj(
        ("action", Json.fromString(a.action)),
        ("id", Json.fromString(a.id)),
        ("status", Json.fromString("success"))
      )
}

final case class SchemaNotFoundResponse(
    action: String,
    id: String
) extends InvalidResponse {
  override def message: String = s"Schema not found"
}

final case class InvalidJsonResponse(
    action: String,
    id: String,
    details: String
) extends InvalidResponse {
  override def message: String = s"Invalid JSON: $details"
}

final case class UnknownResponse(
    action: String,
    id: String
) extends InvalidResponse {
  override def message: String = s"Internal error"
}

final case class ValidationFailedResponse(
    action: String,
    id: String,
    errorMessage: String
) extends InvalidResponse {
  override def message: String = errorMessage
}
