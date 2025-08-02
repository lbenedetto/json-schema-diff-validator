package com.lbenedetto

import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveErrors
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

internal class MinItemsTest : BehaviorSpec({
  Given("A schema with minItems") {
    val oldSchema = Util.readSchema("data_minItems.schema")

    When("minItems are removed") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/doc/properties/content/minItems")
      )

      Then("should not have errors") {
        Validator.validate(oldSchema, newSchema).shouldNotHaveErrors()
      }
    }

    When("minItems is replaced with a smaller value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "0")
      )

      Then("should not have errors") {
        Validator.validate(oldSchema, newSchema).shouldNotHaveErrors()
      }
    }

    When("minItems is replaced with a greater value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "10")
      )

      Then("should have errors") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }
  }
})
