package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonString
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveDiffCorrectlySortedTo
import com.lbenedetto.util.Util.times
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.exhaustive

internal class AllowNewEnumValueTest : BehaviorSpec({
  Given("A schema with an enum") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    val patches = exhaustive(
      listOf(
        add("/properties/someEnumValue/enum/-", jsonString("VALUE_C")),
        add("/properties/listOfEnumValues/items/enum/-", jsonString("VALUE_C"))
      )
    )

    checkAll(patches * Exhaustive.enum<Compatibility>()) { (patch, compatibility) ->
      When("newEnumValue: $compatibility and patch: $patch") {
        val newSchema = oldSchema.withPatches(patch)

        Then("Patch node should be sorted to $compatibility") {
          val config = Config(newEnumValue = compatibility)
          Validator.validate(oldSchema, newSchema, config) shouldHaveDiffCorrectlySortedTo compatibility
        }
      }
    }
  }
})
