package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.MinItemsChange
import io.github.lbenedetto.util.PatchDSL
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class MinItemsTest : BehaviorSpec({
  Given("A schema with minItems") {
    val oldSchema = Util.readSchema("data_minItems.schema")

    When("minItems are removed") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/doc/properties/content/minItems")
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          MinItemsChange("/definitions/doc/properties/content/minItems", 1, null, ChangeType.REMOVED)
        )
      }
    }

    When("minItems are added") {
      val olderSchema = oldSchema.withPatches(
        remove("/definitions/doc/properties/content/minItems")
      )

      Then("Change should be detected") {
        Inspector.inspect(olderSchema, oldSchema).all().shouldContainExactlyInAnyOrder(
          MinItemsChange("/definitions/doc/properties/content/minItems", null, 1, ChangeType.ADDED)
        )
      }
    }

    When("A new field named minItems is added") {
      val newSchema = oldSchema.withPatches(
        PatchDSL.add("/definitions/doc/properties/minItems", """{ "type": ["integer","null"] }"""),
      )

      Then("Validator should detect it as a new field, not as a new minItems requirement") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/definitions/doc/properties", "minItems", ChangeType.ADDED)
        )
      }
    }

    When("minItems is replaced with a smaller value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "0")
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          MinItemsChange("/definitions/doc/properties/content/minItems", 1, 0, ChangeType.REMOVED)
        )
      }
    }

    When("minItems is replaced with a greater value") {
      val newSchema = oldSchema.withPatches(
        replace("/definitions/doc/properties/content/minItems", "10")
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          MinItemsChange("/definitions/doc/properties/content/minItems", 1, 10, ChangeType.ADDED)
        )
      }
    }
  }
})
