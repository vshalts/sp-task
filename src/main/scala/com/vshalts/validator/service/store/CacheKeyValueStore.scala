package com.vshalts.validator
package service.store

import cats.implicits._
import cats.effect.{Async, Resource, Sync}
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import domain.BusinessError.KeyNotFoundError
import config.CacheConfig
import org.typelevel.log4cats.Logger

class CacheKeyValueStore[F[_]: Logger: Async] private (
    cache: Cache[String, Option[String]],
    underlyingStore: KeyValueStore[F]
) extends KeyValueStore[F] {

  override def put(id: String, value: String): F[Unit] = {
    for {
      _ <- underlyingStore.put(id, value)
      _ <- Logger[F].debug(s"Cache: store value for $id")
      _ <- Async[F].delay(cache.put(id, Some(value)))
    } yield ()
  }
  override def get(id: String): F[String] = {
    for {
      valueOpt <- Async[F].delay(cache.getIfPresent(id))
      value <- valueOpt match {
        case Some(None) =>
          Logger[F].debug(s"Cache: return key not found for $id") *>
            Async[F].raiseError(KeyNotFoundError(id))
        case Some(Some(v)) =>
          Logger[F].debug(s"Cache: return value for $id") *>
            Sync[F].pure(v)
        case None =>
          for {
            value <- underlyingStore.get(id).adaptError {
              case KeyNotFoundError(e) =>
                cache.put(id, None)
                KeyNotFoundError(e)
            }
            _ <- Logger[F].debug(s"Cache: store value for $id")
            _ <- Async[F].delay(cache.put(id, Some(value)))
          } yield value
      }
    } yield value
  }
}

object CacheKeyValueStore {

  def make[F[_]: Logger: Async](
      config: CacheConfig,
      underlyingStore: KeyValueStore[F]
  ) = {
    val cache = Scaffeine()
      .maximumSize(config.maximumSize)
      .expireAfterAccess(config.expireAfter)
      .build[String, Option[String]]()

    Resource.pure[F, KeyValueStore[F]](
      new CacheKeyValueStore[F](cache, underlyingStore)
    )
  }
}
