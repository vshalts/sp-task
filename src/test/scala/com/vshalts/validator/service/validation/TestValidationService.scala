package com.vshalts.validator
package service.validation

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.vshalts.validator.domain.BusinessError.InvalidJsonError
import domain.{DocumentBody, SchemaBody}
import service.schema.SchemaService
import service.store.TestKeyValueStore
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should

class TestValidationService
    extends AsyncFlatSpec
    with AsyncIOSpec
    with should.Matchers {

  it should "validate document for valid json" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(DocumentBody("fake"), SchemaBody("{}"))
        .assertThrows[InvalidJsonError]
    }
  }

  it should "validate schema for valid json" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(DocumentBody("{}"), SchemaBody("fake"))
        .assertThrows[InvalidJsonError]
    }
  }

  it should "accept valid document" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(
          DocumentBody("""123"""),
          SchemaBody("""{ "type": "number" }""".stripMargin)
        )
        .asserting(_ shouldBe Right(()))
    }
  }

  it should "generate error on invalid document" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(
          DocumentBody("""123"""),
          SchemaBody("""{ "type": "string" }""".stripMargin)
        )
        .asserting(
          _ shouldBe Left(
            "instance type (integer) does not match any allowed primitive type (allowed: [\"string\"])"
          )
        )
    }
  }

  it should "accept when required fields present" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(
          DocumentBody("""{ "name": "Vadim" }"""),
          SchemaBody(
            """{ "type": "object", "properties": { "name": { "type": "string" } }, "required": ["name"] }""".stripMargin
          )
        )
        .asserting(_ shouldBe Right(()))
    }
  }

  it should "return error when required fields absent" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(
          DocumentBody("""{ "a": "b" }"""),
          SchemaBody(
            """{ "type": "object", "properties": { "name": { "type": "string" } }, "required": ["name"] }""".stripMargin
          )
        )
        .asserting(
          _ shouldBe Left(
            "object has missing required properties ([\"name\"])"
          )
        )

    }
  }

  it should "remove null value" in {
    withValidationService { validationService =>
      validationService
        .validateDocument(
          DocumentBody("""{ "a": "b", "name": null }"""),
          SchemaBody(
            """{ "type": "object", "properties": { "name": { "type": "null" } }, "required": ["name"] }""".stripMargin
          )
        )
        .asserting(
          _ shouldBe Left(
            "object has missing required properties ([\"name\"])"
          )
        )

    }
  }

  def withValidationService[A](f: ValidationService[IO] => IO[A]) = {
    (for {
      testJeyValueStore <- Resource.eval(TestKeyValueStore.make[IO])
      schemaService <- SchemaService.make[IO](testJeyValueStore)
      validationService <- ValidationService.make(schemaService)
    } yield validationService).use(f)
  }
}
