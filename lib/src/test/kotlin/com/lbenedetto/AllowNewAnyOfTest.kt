package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonObject
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveDiffCorrectlySortedTo
import com.lbenedetto.util.Util.times
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.exhaustive

internal class AllowNewAnyOfTest : BehaviorSpec({
  Given("A new anyOf element is added to the schema") {
    val oldSchema = Util.readSchema("data_options_allowAnyOf.schema")

    val patches = exhaustive(
      listOf(
        add("/definitions/root/items/anyOf/-", """{"type": "number"}"""),
        add("/definitions/anotherItem/content/items/0/anyOf/-", """{"fruit": "pear"}"""),
        add("/definitions/inline_node/anyOf/-", jsonObject("\$ref" to "\"#/definitions/hardBreak_node\""))
      )
    )

    checkAll(patches * Exhaustive.enum<Compatibility>()) { (patch, compatibility) ->
      When("newAnyOf: $compatibility and patch: $patch") {
        val newSchema = oldSchema.withPatches(patch)

        Then("Patch node should be sorted to $compatibility") {
          val config = Config(newAnyOf = compatibility)
          Validator.validate(oldSchema, newSchema, config) shouldHaveDiffCorrectlySortedTo compatibility
        }
      }
    }
  }
})
