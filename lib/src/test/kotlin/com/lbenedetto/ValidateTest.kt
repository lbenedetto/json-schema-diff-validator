package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class ValidateTest : BehaviorSpec({
  Given("A schema") {
    val oldSchema = Util.readSchema("ConsumerItem-redis-v1.schema")

    When("Schema is the same") {
      val newSchema = oldSchema.deepCopy()

      Then("Should not throw an exception") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("A field type is changed") {
      val newSchema = oldSchema.withPatches(
        replace("/properties/debtor/type", jsonString("string"))
      )

      Then("An exception should be thrown") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema)
        }
      }
    }

    When("An optional field is added") {
      val newSchema = oldSchema.withPatches(
        add("/properties/field", """{ "type": "string" }"""),
      )

      Then("No exception should be thrown") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("A required field is added") {
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
        add("/required", jsonString("newField"))
      )

      Then("An exception should be thrown") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema)
        }
      }
    }

    When("A required field is added to a subnode") {
      val newSchema = oldSchema.withPatches(
        add("/properties/debtor/properties/newField", """{ "type": "string" }"""),
        add("/properties/debtor/required", jsonString("newField"))
      )

      Then("An exception should be thrown") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema)
        }
      }
    }

    When("A required field is added to a subnode in an anyOf") {
      val newSchema = oldSchema.withPatches(
        add("/properties/additionalItemProperties/anyOf/1/properties/newField", """{ "type": "string" }"""),
        add("/properties/additionalItemProperties/anyOf/1", jsonString("newField"))
      )

      Then("An exception should be thrown") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema)
        }
      }
    }
  }
})
