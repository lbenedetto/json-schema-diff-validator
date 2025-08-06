package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class AddFieldTest : BehaviorSpec({
  Given("A schema") {
    val schemaPath = "ExampleObject.schema"

    When("A non-null field is added") {
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "newField", ChangeType.ADDED),
          NonNullRequirementChange("/properties", "newField", ChangeType.ADDED)
        )
      }
    }

    When("A nullable field is added") {
      val oldSchema = Util.readSchema(schemaPath)
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": ["string", "null"] }"""),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "newField", ChangeType.ADDED)
        )
      }
    }
  }
})
