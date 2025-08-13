package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NotAbsentRequirementChange
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class ChangeNotAbsentRequirementTest : BehaviorSpec({
  Given("A schema") {
    val schemaPath = "ExampleObject.schema"

    When("A field is made optional") {
      val field = "someIntegerField"
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        remove("/required/0")
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NotAbsentRequirementChange("/properties", field, ChangeType.REMOVED)
        )
      }
    }

    When("A field is made required") {
      val field = "someNullableField"
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        add("/required/-", jsonString(field))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NotAbsentRequirementChange("/properties", field, ChangeType.ADDED)
        )
      }
    }
  }
})
