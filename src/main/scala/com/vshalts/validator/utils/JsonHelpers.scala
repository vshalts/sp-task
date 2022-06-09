package com.vshalts.validator
package utils

import cats.implicits._
import cats.effect.kernel.Async
import domain.BusinessError.InvalidJsonError

import io.circe._
import io.circe.parser._

object JsonHelpers {

  def parseJson[F[_]: Async](content: String) =
    for {
      jsonResults <- Async[F].blocking(parse(content))
      json <- Async[F].fromEither(jsonResults).adaptError {
        case ParsingFailure(message, _) => InvalidJsonError(message)
      }
    } yield json

  def sanitizeJsonString[F[_]: Async](json: String) = {
    for {
      json <- parseJson[F](json)
      sanitizedStr <- Async[F].blocking(json.deepDropNullValues.noSpaces)
    } yield sanitizedStr
  }
}
