package com.vshalts.validator
package domain

sealed abstract class BusinessError(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)

object BusinessError {
  final case class InvalidJsonError(details: String)
      extends BusinessError(details)

  final case class SchemaNotFoundError(id: SchemaId) extends BusinessError()

  final case class KeyNotFoundError(id: String) extends BusinessError()
}
