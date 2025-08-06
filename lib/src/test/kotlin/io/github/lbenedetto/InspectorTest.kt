package io.github.lbenedetto

import io.github.lbenedetto.inspector.DetectedChanges
import io.github.lbenedetto.inspector.FieldTypeChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

internal class InspectorTest : BehaviorSpec({
  Given("A schema") {
    val schemaPath = "ExampleObject.schema"

    When("Schema is the same") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.deepCopy()

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema) shouldBe DetectedChanges()
      }
    }

    When("A field type is changed") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        replace("/properties/someIntegerField/type", jsonString("string"))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldTypeChange("/properties/someIntegerField/type", "INTEGER", "STRING")
        )
      }
    }
  }
})
