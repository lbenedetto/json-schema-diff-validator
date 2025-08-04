package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.util.PatchDSL
import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

internal class MinItemsTest : BehaviorSpec({
  Given("A schema with minItems") {
    val oldSchema = Util.readSchema("data_minItems.schema")

    When("minItems are removed") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/doc/properties/content/minItems")
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema) shouldBe ValidationResult(
          allowed = mutableListOf("Removed minItems requirement of 0 at /definitions/doc/properties/content/minItems")
        )
      }
    }

    When("minItems are added") {
      val olderSchema = oldSchema.withPatches(
        remove("/definitions/doc/properties/content/minItems")
      )

      Then("Change should be detected") {
        Validator.validate(olderSchema, oldSchema) shouldBe ValidationResult(
          forbidden = mutableListOf("Added minItems requirement of 1 at /definitions/doc/properties/content/minItems")
        )
      }
    }

    When("A new field named minItems is added") {
      val newSchema = oldSchema.withPatches(
        PatchDSL.add("/definitions/doc/properties/minItems", """{ "type": "integer" }"""),
      )

      Then("Validator should detect it as a new field, not as a new minItems requirement") {
        Validator.validate(oldSchema, newSchema, Config(addingOptionalFields = ALLOWED)) shouldBe ValidationResult(
          allowed = mutableListOf("Added new optional field minItems at /definitions/doc/properties/minItems"),
        )
      }
    }

    When("minItems is replaced with a smaller value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "0")
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema) shouldBe ValidationResult(
          allowed = mutableListOf("Decreased minItems from 1 to 0 at /definitions/doc/properties/content/minItems")
        )
      }
    }

    When("minItems is replaced with a greater value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "10")
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema) shouldBe ValidationResult(
          forbidden = mutableListOf("Increased minItems from 1 to 10 at /definitions/doc/properties/content/minItems")
        )
      }
    }
  }
})
