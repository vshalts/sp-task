package com.vshalts.validator
package domain

sealed abstract class BusinessError(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)

object BusinessError {
  final case class InvalidJsonError(details: String)
      extends BusinessError(details)

  final case class SchemaNotFoundError() extends BusinessError()

  final case class KeyNotFoundError() extends BusinessError()
}
