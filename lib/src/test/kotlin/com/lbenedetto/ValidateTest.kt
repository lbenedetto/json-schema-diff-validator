package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveErrors
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.assertDoesNotThrow

internal class ValidateTest : BehaviorSpec({
  Given("A schema") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    When("Schema is the same") {
      val newSchema = oldSchema.deepCopy()

      Then("Should not throw an exception") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema) }
      }
    }

    When("A field type is changed") {
      val newSchema = oldSchema.withPatches(
        replace("/properties/someIntegerField/type", jsonString("string"))
      )

      Then("An exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }

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
        add("/required/-", jsonString("name"))
      )

      Then("An exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }

    When("An optional field is added") {
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
      )

      Then("No exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldNotHaveErrors()
      }
    }

    When("A required field is added") {
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
        add("/required/-", jsonString("newField"))
      )

      Then("An exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }

    When("A required field is added to a subnode") {
      val newSchema = oldSchema.withPatches(
        add("/\$defs/SomePojo/properties/newField", """{ "type": "string" }"""),
        add("/\$defs/SomePojo/required/-", jsonString("newField"))
      )

      Then("An exception should be thrown") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }

    When("A required field is added to a subnode in an anyOf") {
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
        add("/properties/fieldWithAnyOf/anyOf/1/required/-", jsonString("newField"))
      )

      Then("Should have errors") {
        Validator.validate(oldSchema, newSchema).shouldHaveErrors()
      }
    }

    When("A optional field is added to a subnode in an anyOf") {
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
      )

      Then("Should have errors") {
        Validator.validate(oldSchema, newSchema).shouldNotHaveErrors()
      }
    }
  }
})
