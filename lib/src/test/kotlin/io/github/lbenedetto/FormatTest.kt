package io.github.lbenedetto

import io.github.lbenedetto.inspector.*
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.github.lbenedetto.validator.Config
import io.github.lbenedetto.validator.Validator
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class FormatTest : BehaviorSpec({
  Given("A schema field without a format") {

    When("A format is added to the field") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add($$"/$defs/SomePojo/properties/someField/format", jsonString("date-time"))
      )
      Then("Change should be detected") {
        val changes = Inspector.inspect(oldSchema, newSchema)
        changes.all().shouldContainExactlyInAnyOrder(
          FieldFormatChange($$"/$defs/SomePojo/properties/someField", ChangeType.ADDED, null, "date-time")
        )
        Validator.validate(changes, Config())
      }
    }
  }

  Given("A schema with a field literally named 'format'") {

    When("That field is added") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/format", """{"type": "string"}""")
      )
      Then("It should be treated as a normal field, not a format change") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "format", ChangeType.ADDED),
          NonNullRequirementChange("/properties", "format", ChangeType.ADDED)
        )
      }
    }
  }

  Given("A schema field with a format") {

    When("The format is removed") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        remove("/properties/listOfObjects/items/properties/someRequiredField/format")
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldFormatChange(
            "/properties/listOfObjects/items/properties/someRequiredField",
            ChangeType.REMOVED,
            "date-time",
            null
          )
        )
      }
    }

    When("The format is changed") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        replace("/properties/listOfObjects/items/properties/someRequiredField/format", jsonString("date"))
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldFormatChange(
            "/properties/listOfObjects/items/properties/someRequiredField",
            ChangeType.ADDED,
            "date-time",
            "date"
          )
        )
      }
    }
  }
})
