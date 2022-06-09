package com.vshalts.validator
package service.store

import com.vshalts.validator.domain.BusinessError.KeyNotFoundError

class TestAwsS3KeyValueStore extends IntegrationTest {
  it should "save and load values for key" in {
    withAwsStore { store =>
      val result = for {
        _ <- store.put("testKey", "testValue")
        res <- store.get("testKey")
      } yield res

      result.asserting(_ shouldBe "testValue")
    }
  }

  it should "overwrite old data for key" in {
    withAwsStore { store =>
      val result = for {
        _ <- store.put("testKey", "firstValue")
        _ <- store.put("testKey", "secondValue")
        res <- store.get("testKey")
      } yield res

      result.asserting(_ shouldBe "secondValue")
    }
  }

  it should "detect not existing key" in {
    withAwsStore { store =>
      store.get("absentKey").assertThrows[KeyNotFoundError]
    }
  }
}
