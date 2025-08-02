package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.Compatibility.FORBIDDEN
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveErrors
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec

internal class AllowNewEnumValueTest : BehaviorSpec({
  Given("A schema with an enum") {
    val oldSchema = Util.readSchema("data_options_allowNewEnumValue.schema")

    When("There are new enum values") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/root/properties/fruit/type/enum/-", jsonString("banana")),
        add("/definitions/anotherItem/properties/tshirt/size/enum/-", jsonString("large"))
      )

      Then("Should not have errors if allowNewEnumValue is ALLOWED") {
        Validator.validate(oldSchema, newSchema, Config(newEnumValue = ALLOWED)).shouldNotHaveErrors()
      }

      Then("Should have errors if allowNewEnumValue is FORBIDDEN") {
        Validator.validate(oldSchema, newSchema, Config(newEnumValue = FORBIDDEN)).shouldHaveErrors()
      }
    }
  }
})
