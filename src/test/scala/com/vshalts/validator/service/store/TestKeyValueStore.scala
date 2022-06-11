package com.vshalts.validator.service.store

import cats.effect._
import cats.implicits._
import com.vshalts.validator.domain.BusinessError.KeyNotFoundError

class TestKeyValueStore[F[_]: Sync](ref: Ref[F, Map[String, String]])
    extends KeyValueStore[F] {

  override def put(id: String, value: String): F[Unit] =
    ref.update(_ + (id -> value))

  override def get(id: String): F[String] =
    for {
      m <- ref.get
      value <- m.get(id) match {
        case Some(v) => Sync[F].pure(v)
        case None    => Sync[F].raiseError(KeyNotFoundError(id))
      }
    } yield value
}

object TestKeyValueStore {
  def make[F[_]: Sync] = {
    for {
      ref <- Ref.of[F, Map[String, String]](Map.empty)
    } yield new TestKeyValueStore[F](ref)
  }
}
