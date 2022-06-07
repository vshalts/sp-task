package com.vshalts.validator

import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.LocalStackContainer
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import org.scalatest._
import flatspec._
import matchers._
import org.testcontainers.containers.localstack.{
  LocalStackContainer => JavaLocalStackContainer
}

trait IntegrationTest
    extends AsyncFlatSpec
    with AsyncIOSpec
    with should.Matchers
    with TestContainersForAll {

  override type Containers = LocalStackContainer

  override def startContainers(): Containers = {
    val localStackContainer = LocalStackContainer
      .Def(tag = "0.14.3.1", services = Seq(JavaLocalStackContainer.Service.S3))
      .start()
    localStackContainer
  }
}
