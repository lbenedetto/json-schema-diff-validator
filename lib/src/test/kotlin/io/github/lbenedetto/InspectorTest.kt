package io.github.lbenedetto

import io.github.lbenedetto.inspector.DetectedChanges
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.util.Util
import io.kotest.core.spec.style.BehaviorSpec
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
  }
})
