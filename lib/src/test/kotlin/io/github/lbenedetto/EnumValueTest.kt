package io.github.lbenedetto

import io.github.lbenedetto.Compatibility.ALLOWED
import io.github.lbenedetto.Compatibility.FORBIDDEN
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.shouldAllow
import io.github.lbenedetto.util.Util.shouldForbid
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

internal class EnumValueTest : BehaviorSpec({
  Given("A schema with an enum") {
    val oldSchema = Util.readSchema("ExampleObject.schema")
    val config = Config(addingEnumValue = ALLOWED, removingEnumValue = FORBIDDEN)
    When("New enum value is added to an enum field") {
      val newSchema = oldSchema.withPatches(
        add("/properties/someEnumValue/enum/-", jsonString("VALUE_C"))
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config)
          .shouldAllow("Added new enum value \"VALUE_C\" to /properties/someEnumValue/enum")
      }
    }

    When("New enum value is added to an enum array field") {
      val newSchema = oldSchema.withPatches(
        add("/properties/listOfEnumValues/items/enum/-", jsonString("VALUE_C"))
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config)
          .shouldAllow("Added new enum value \"VALUE_C\" to /properties/listOfEnumValues/items/enum")
      }
    }

    When("Enum value is removed from an enum field") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/someEnumValue/enum/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config)
          .shouldForbid("Removed enum value \"VALUE_A\" from /properties/someEnumValue/enum")
      }
    }

    When("Enum value is removed from an enum array field") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/listOfEnumValues/items/enum/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config)
          .shouldForbid("Removed enum value \"VALUE_A\" from /properties/listOfEnumValues/items/enum")
      }
    }
  }
})
