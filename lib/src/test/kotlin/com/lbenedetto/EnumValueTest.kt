package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.Compatibility.FORBIDDEN
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
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
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          allowed = mutableListOf("Added new enum value \"VALUE_C\" to /properties/someEnumValue/enum")
        )
      }
    }

    When("New enum value is added to an enum array field") {
      val newSchema = oldSchema.withPatches(
        add("/properties/listOfEnumValues/items/enum/-", jsonString("VALUE_C"))
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          allowed = mutableListOf("Added new enum value \"VALUE_C\" to /properties/listOfEnumValues/items/enum")
        )
      }
    }

    When("Enum value is removed from an enum field") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/someEnumValue/enum/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          forbidden = mutableListOf("Removed enum value \"VALUE_A\" from /properties/someEnumValue/enum")
        )
      }
    }

    When("Enum value is removed from an enum array field") {
      val newSchema = oldSchema.withPatches(
        remove("/properties/listOfEnumValues/items/enum/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          forbidden = mutableListOf("Removed enum value \"VALUE_A\" from /properties/listOfEnumValues/items/enum")
        )
      }
    }
  }
})
