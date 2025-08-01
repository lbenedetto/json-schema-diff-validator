package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AllowNewEnumValueTest : BehaviorSpec({
  Given("A schema with an enum") {
    val oldSchema = Util.readSchema("data_options_allowNewEnumValue.schema")

    When("There are new enum values") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/root/properties/fruit/type/enum/-", jsonString("banana")),
        add("/definitions/anotherItem/properties/tshirt/size/enum/-", jsonString("large"))
      )

      Then("Should not throw an exception if allowNewEnumValue is true") {
        assertDoesNotThrow {
          Validator.validate(oldSchema, newSchema, allowNewEnumValue = true)
        }
      }

      Then("Should throw an exception if allowNewEnumValue is false") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema, allowNewEnumValue = false)
        }
      }
    }
  }
})
