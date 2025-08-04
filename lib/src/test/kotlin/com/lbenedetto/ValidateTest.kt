package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.Compatibility.FORBIDDEN
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

internal class ValidateTest : BehaviorSpec({
  Given("A schema") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    When("Schema is the same") {
      val newSchema = oldSchema.deepCopy()

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema) shouldBe ValidationResult()
      }
    }

    When("A field type is changed") {
      val newSchema = oldSchema.withPatches(
        replace("/properties/someIntegerField/type", jsonString("string"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema) shouldBe ValidationResult(
          forbidden = mutableListOf("Changed field at /properties/someIntegerField/type from: \"integer\" to: \"string\"")
        )
      }
    }

    When("A field is made optional") {
      val newSchema = oldSchema.withPatches(
        remove("/required/0")
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(removingRequired = ALLOWED)) shouldBe ValidationResult(
          allowed = mutableListOf("Removed non-null requirement for \"fieldWhichReferencesRequiredTypeDef\" from /required"),
        )
      }
    }

    When("A field is made required") {
      val newSchema = oldSchema.withPatches(
        add("/required/-", jsonString("name"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequired = FORBIDDEN)) shouldBe ValidationResult(
          forbidden = mutableListOf("Added non-null requirement for \"name\" to /required"),
        )
      }
    }

    When("An optional field is added") {
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingOptionalFields = ALLOWED)) shouldBe ValidationResult(
          allowed = mutableListOf("Added new optional field newField at /properties/newField"),
        )
      }
    }

    When("A required field is added") {
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
        add("/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequiredFields = FORBIDDEN)) shouldBe ValidationResult(
          forbidden = mutableListOf(
            "Added non-null requirement for \"newField\" to /required",
            "Added new required field newField at /properties/newField"
          ),
        )
      }
    }

    When("A required field is added to a subnode") {
      val newSchema = oldSchema.withPatches(
        add("/\$defs/SomePojo/properties/newField", """{ "type": "string" }"""),
        add("/\$defs/SomePojo/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequiredFields = FORBIDDEN)) shouldBe ValidationResult(
          forbidden = mutableListOf(
            "Added non-null requirement for \"newField\" to /\$defs/SomePojo/required",
            "Added new required field newField at /\$defs/SomePojo/properties/newField"
          ),
        )
      }
    }

    When("A required field is added to a subnode in an anyOf") {
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
        add("/properties/fieldWithAnyOf/anyOf/1/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequiredFields = FORBIDDEN)) shouldBe ValidationResult(
          forbidden = mutableListOf(
            "Added non-null requirement for \"newField\" to /properties/fieldWithAnyOf/anyOf/1/required",
            "Added new required field newField at /properties/fieldWithAnyOf/anyOf/1/properties/newField"
          ),
        )
      }
    }

    When("A optional field is added to a subnode in an anyOf") {
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingOptionalFields = ALLOWED)) shouldBe ValidationResult(
          allowed = mutableListOf("Added new optional field newField at /properties/fieldWithAnyOf/anyOf/1/properties/newField"),
        )
      }
    }
  }
})
