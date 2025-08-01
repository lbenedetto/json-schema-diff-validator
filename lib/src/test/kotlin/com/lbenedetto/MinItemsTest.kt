package com.lbenedetto

import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class MinItemsTest : BehaviorSpec({
  Given("A schema with minItems") {
    val oldSchema = Util.readSchema("data_minItems.schema")

    When("minItems are removed") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/doc/properties/content/minItems")
      )

      Then("should not throw an exception") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("minItems is replaced with a smaller value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "0")
      )

      Then("should not throw an exception") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("minItems is replaced with a greater value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "10")
      )

      Then("should throw an exception") {
        assertThrows<IllegalStateException> { Validator.validate(oldSchema, newSchema) }
      }
    }
  }
})
