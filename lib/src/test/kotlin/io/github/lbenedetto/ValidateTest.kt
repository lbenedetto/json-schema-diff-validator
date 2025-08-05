package io.github.lbenedetto

import io.github.lbenedetto.Compatibility.*
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.shouldAllow
import io.github.lbenedetto.util.Util.shouldDiscourage
import io.github.lbenedetto.util.Util.shouldForbid
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

internal class ValidateTest : BehaviorSpec({
  Given("A schema") {
    val schemaPath = "ExampleObject.schema"

    When("Schema is the same") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.deepCopy()

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema) shouldBe ValidationResult()
      }
    }

    When("A field type is changed") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        replace("/properties/someIntegerField/type", jsonString("string"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema)
          .shouldForbid("Changed field type from 'INTEGER' to 'STRING' at /properties/someIntegerField/type")
      }
    }

    When("A basic field is made optional") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        remove("/required/1"),
        replace("/properties/someIntegerField/type", """["integer", "null"]""")
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(removingRequired = ALLOWED))
          .shouldAllow(
            "Removed non-null requirement for \"someIntegerField\" from /required",
            "Changed field type from 'INTEGER' to 'INTEGER|NULL' at /properties/someIntegerField/type"
          )
      }
    }

    When("A basic field is made required") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/required/-", jsonString("someNullableField")),
        replace("/properties/someNullableField/type", jsonString("integer"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequired = FORBIDDEN))
          .shouldForbid(
            "Added non-null requirement for \"someNullableField\" to /required",
            "Changed field type from 'INTEGER|NULL' to 'INTEGER' at /properties/someNullableField/type"
          )
      }
    }

    When("An anyOf field is made required") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/required/-", jsonString("fieldWithAnyOf")),
        remove("/properties/fieldWithAnyOf/anyOf/0"),
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequired = DISCOURAGED))
          .shouldDiscourage(
            "Removed null option from anyOf at /properties/fieldWithAnyOf/anyOf",
            "Added non-null requirement for \"fieldWithAnyOf\" to /required"
          )
      }
    }

    When("An anyOf field is made optional") {
      val newSchema = Util.readSchema("ExampleObject.schema")
      val oldSchema = newSchema.withPatches(
        add("/required/-", jsonString("fieldWithAnyOf")),
        remove("/properties/fieldWithAnyOf/anyOf/0"),
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(removingRequired = DISCOURAGED))
          .shouldDiscourage(
            "Added null option to anyOf at /properties/fieldWithAnyOf/anyOf",
            "Removed non-null requirement for \"fieldWithAnyOf\" from /required"
          )
      }
    }

    When("An optional field is added") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingOptionalFields = ALLOWED))
          .shouldAllow("Added new optional field newField at /properties/newField")
      }
    }

    When("A required field is added") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
        add("/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequiredFields = FORBIDDEN))
          .shouldForbid(
            "Added non-null requirement for \"newField\" to /required",
            "Added new required field newField at /properties/newField"
          )
      }
    }

    When("A required field is added to a subnode") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/\$defs/SomePojo/properties/newField", """{ "type": "string" }"""),
        add("/\$defs/SomePojo/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequiredFields = FORBIDDEN))
          .shouldForbid(
            "Added non-null requirement for \"newField\" to /\$defs/SomePojo/required",
            "Added new required field newField at /\$defs/SomePojo/properties/newField"
          )
      }
    }

    When("A required field is added to a subnode in an anyOf") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
        add("/properties/fieldWithAnyOf/anyOf/1/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingRequiredFields = FORBIDDEN))
          .shouldForbid(
            "Added non-null requirement for \"newField\" to /properties/fieldWithAnyOf/anyOf/1/required",
            "Added new required field newField at /properties/fieldWithAnyOf/anyOf/1/properties/newField"
          )
      }
    }

    When("A optional field is added to a subnode in an anyOf") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(addingOptionalFields = ALLOWED))
          .shouldAllow("Added new optional field newField at /properties/fieldWithAnyOf/anyOf/1/properties/newField")
      }
    }
  }
})
