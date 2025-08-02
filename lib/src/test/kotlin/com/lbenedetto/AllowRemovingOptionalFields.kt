package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.Compatibility.FORBIDDEN
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveErrors
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec

internal class AllowRemovingOptionalFields : BehaviorSpec({

  Given("A schema") {
    val oldSchema = Util.readSchema("ConsumerItem-redis-v1.schema")

    When("A field is made optional") {
      val newSchema = oldSchema.withPatches(
        remove("/required/0")
      )

      Then("Should not throw an exception") {
        Validator.validate(oldSchema, newSchema).shouldNotHaveErrors()
      }
    }

    When("A field is made required") {
      val newSchema = oldSchema.withPatches(
        add("/required", jsonString("name"))
      )

      Then("An exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }

    When("An optional field is removed") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/name")
      )

      Then("Should not throw an exception if allowRemovingOptionalFields is ALLOWED") {
        Validator.validate(oldSchema, newSchema, Config(removingOptionalFields = ALLOWED)).shouldNotHaveErrors()
      }

      Then("Should throw an exception if allowRemovingOptionalFields is FORBIDDEN") {
        Validator.validate(oldSchema, newSchema, Config(removingOptionalFields = FORBIDDEN)).shouldHaveErrors()
      }
    }

    When("A required field is removed") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/debtor")
      )

      Then("An exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }
  }
})
