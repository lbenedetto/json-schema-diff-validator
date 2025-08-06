package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class RemovingOptionalFieldsTest : BehaviorSpec({

  Given("A schema") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    When("An optional field is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/someNullableField"))
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "someNullableField", ChangeType.REMOVED, false)
        )
      }
    }

    When("A required field is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/fieldWhichReferencesRequiredTypeDef"))
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContain(
          FieldChange("/properties", "fieldWhichReferencesRequiredTypeDef", ChangeType.REMOVED, true)
        )
      }
    }
  }
})
