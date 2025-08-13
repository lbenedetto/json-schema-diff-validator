package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonObject
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class ChangeNonNullRequirementTest : BehaviorSpec({
  Given("A schema") {
    val schemaPath = "ExampleObject.schema"

    When("A field is made nullable by type") {
      val field = "someIntegerField"
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        replace("/properties/someIntegerField/type", """["integer", "null"]""")
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", field, ChangeType.REMOVED)
        )
      }
    }

    When("A field is made nonnull by type") {
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        replace("/properties/someNullableField/type", jsonString("integer"))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "someNullableField", ChangeType.ADDED)
        )
      }
    }

    When("An field is made nonnull by anyOf") {
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        remove("/properties/nullableFieldWithAnyOf/anyOf/0"),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "nullableFieldWithAnyOf", ChangeType.ADDED)
        )
      }
    }

    When("An field is made nullable by anyOf") {
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/-", """{"type": "null"}"""),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "fieldWithAnyOf", ChangeType.REMOVED),
        )
      }
    }

    When("A field is made nullable by changing anyOf") {
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        remove($$"/properties/fieldWhichReferencesNonNullTypeDef/$ref"),
        add("/properties/fieldWhichReferencesNonNullTypeDef/anyOf", "[]"),
        add("/properties/fieldWhichReferencesNonNullTypeDef/anyOf/-", """{"type": "null"}"""),
        add("/properties/fieldWhichReferencesNonNullTypeDef/anyOf/-", jsonObject($$"$ref" to jsonString($$"#/$defs/SomePojo"))),
      )
      println(newSchema.toPrettyString())

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "fieldWhichReferencesNonNullTypeDef", ChangeType.REMOVED),
        )
      }
    }
  }
})
