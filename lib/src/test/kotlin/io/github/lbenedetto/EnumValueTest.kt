package io.github.lbenedetto

import com.fasterxml.jackson.databind.node.TextNode
import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.EnumValueChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class EnumValueTest : BehaviorSpec({
  Given("A schema with an enum") {
    val oldSchema = Util.readSchema("ExampleObject.schema")
    When("New enum value is added to an enum field") {
      val newSchema = oldSchema.withPatches(
        add("/properties/someEnumValue/enum/-", jsonString("VALUE_C"))
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          EnumValueChange("/properties/someEnumValue/enum", TextNode("VALUE_C"), ChangeType.ADDED)
        )
      }
    }

    When("New enum value is added to an enum array field") {
      val newSchema = oldSchema.withPatches(
        add("/properties/listOfEnumValues/items/enum/-", jsonString("VALUE_C"))
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          EnumValueChange("/properties/listOfEnumValues/items/enum", TextNode("VALUE_C"), ChangeType.ADDED)
        )
      }
    }

    When("Enum value is removed from an enum field") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/someEnumValue/enum/0")
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          EnumValueChange("/properties/someEnumValue/enum", TextNode("VALUE_A"), ChangeType.REMOVED)
        )
      }
    }

    When("Enum value is removed from an enum array field") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/listOfEnumValues/items/enum/0")
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          EnumValueChange("/properties/listOfEnumValues/items/enum", TextNode("VALUE_A"), ChangeType.REMOVED)
        )
      }
    }
  }
})
