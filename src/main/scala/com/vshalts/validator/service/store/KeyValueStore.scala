package com.vshalts.validator
package service.store

trait KeyValueStore[F[_]] {
  def put(id: String, value: String): F[Unit]
  def get(id: String): F[String]
}
