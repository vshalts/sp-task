package com.vshalts.validator
package service.schema

import cats.implicits._
import cats.effect.Resource
import cats.effect.kernel.Async
import com.vshalts.validator.domain.BusinessError.{
  KeyNotFoundError,
  SchemaNotFoundError
}
import com.vshalts.validator.service.store.KeyValueStore
import com.vshalts.validator.utils.JsonHelpers
import domain._

trait SchemaService[F[_]] {
  def uploadSchema(schemaId: SchemaId, schemaBody: SchemaBody): F[Unit]
  def downloadSchema(schemaId: SchemaId): F[SchemaBody]
}

object SchemaService {

  def make[F[_]: Async](
      keyValueStore: KeyValueStore[F]
  ): Resource[F, SchemaService[F]] = {
    Resource.pure(new SchemaServiceImpl[F](keyValueStore))
  }

  private class SchemaServiceImpl[F[_]: Async](keyValueStore: KeyValueStore[F])
      extends SchemaService[F] {

    override def uploadSchema(
        schemaId: SchemaId,
        schemaBody: SchemaBody
    ): F[Unit] = {
      for {
        _ <- JsonHelpers.parseJson(schemaBody.content) // to validate json
        _ <- keyValueStore.put(schemaId.id, schemaBody.content)
      } yield ()
    }

    override def downloadSchema(schemaId: SchemaId): F[SchemaBody] = {
      keyValueStore.get(schemaId.id).map(SchemaBody).adaptError {
        case KeyNotFoundError() => SchemaNotFoundError()
      }
    }
  }
}
