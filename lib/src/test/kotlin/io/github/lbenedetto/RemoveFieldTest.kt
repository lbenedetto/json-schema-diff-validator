package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class RemoveFieldTest : BehaviorSpec({

  Given("A schema") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    When("A field nullable by type is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/someNullableField"))
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "someNullableField", ChangeType.REMOVED)
        )
      }
    }

    When("A field non-null by type is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/someIntegerField"))
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContain(
          FieldChange("/properties", "someIntegerField", ChangeType.REMOVED)
        )
      }
    }

    When("A field nullable by ref is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/fieldWhichReferencesNullableTypeDef"))
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "fieldWhichReferencesNullableTypeDef", ChangeType.REMOVED)
        )
      }
    }

    When("A field non-null by ref is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/fieldWhichReferencesNonNullTypeDef"))
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "fieldWhichReferencesNonNullTypeDef", ChangeType.REMOVED),
          NonNullRequirementChange("/properties", "fieldWhichReferencesNonNullTypeDef", ChangeType.REMOVED)
        )
      }
    }
  }
})
