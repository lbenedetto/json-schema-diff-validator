package com.lbenedetto

import com.lbenedetto.util.PatchDSL.remove
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveDiffCorrectlySortedTo
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum

internal class RemovingOptionalFieldsTest : BehaviorSpec({

  Given("A schema") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    checkAll(Exhaustive.enum<Compatibility>()) { compatibility ->
      val config = Config(removingOptionalFields = compatibility)
      When("removingOptionalFields is $compatibility and an optional field is removed") {
        val newSchema = oldSchema.withPatches(remove("/properties/someNullableField"))
        Then("Patch node should be sorted to $compatibility") {
          Validator.validate(oldSchema, newSchema, config) shouldHaveDiffCorrectlySortedTo compatibility
        }
      }
    }

    checkAll(Exhaustive.enum<Compatibility>()) { compatibility ->
      val config = Config(removingRequiredFields = compatibility)
      When("removingRequiredFields is $compatibility and a required field is removed") {
        val newSchema = oldSchema.withPatches(remove("/properties/fieldWhichReferencesRequiredTypeDef"))
        Then("Patch node should be sorted to $compatibility") {
          Validator.validate(oldSchema, newSchema, config) shouldHaveDiffCorrectlySortedTo compatibility
        }
      }
    }
  }
})
