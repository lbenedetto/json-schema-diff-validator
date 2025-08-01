package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AllowRemovingOptionalFields : BehaviorSpec({

  Given("A schema") {
    val oldSchema = Util.readSchema("ConsumerItem-redis-v1.schema")

    When("A field is made optional") {
      val newSchema = oldSchema.withPatches(
        remove("/required/0")
      )

      Then("Should not throw an exception") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("A field is made required") {
      val newSchema = oldSchema.withPatches(
        add("/required", jsonString("name"))
      )
      Then("An exception should be thrown") {
        assertThrows<IllegalStateException> { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("An optional field is removed") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/name")
      )

      Then("Should not throw an exception if allowRemovingOptionalFields is true") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }

      Then("Should throw an exception if allowRemovingOptionalFields is false") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema, allowRemovingOptionalFields = false)
        }
      }
    }

    When("A required field is removed") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/debtor")
      )

      Then("An exception should be thrown") {
        assertThrows<IllegalStateException> { Validator.validate(oldSchema, newSchema) }
      }
    }
  }
})
