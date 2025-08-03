package com.lbenedetto

import com.fasterxml.jackson.databind.node.ObjectNode
import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonObject
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveDiffCorrectlySortedTo
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.times
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.exhaustive

internal class NewAnyOfTest : BehaviorSpec({
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
      When("newAnyOf: $compatibility and anyOfReordering FORBIDDEN and patch: $patch") {
        val newSchema = oldSchema.withPatches(patch)

        Then("Patch node should be sorted to $compatibility") {
          val config = Config(addingAnyOf = compatibility)
          Validator.validate(oldSchema, newSchema, config) shouldHaveDiffCorrectlySortedTo compatibility
        }
      }
    }

    checkAll(patches * Exhaustive.enum<Compatibility>()) { (patch, compatibility) ->
      When("newAnyOf: $compatibility and anyOfReordering ALLOWED and patch: $patch") {
        val newSchema = oldSchema.withPatches(patch)

        Then("Patch node should be sorted to $compatibility") {
          val config = Config(addingAnyOf = compatibility)
          Validator.validate(oldSchema, newSchema, config) shouldHaveDiffCorrectlySortedTo compatibility
        }
      }
    }
  }

  Given("A list of random refs") {
    val oldList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    When("New refs are added and the list is shuffled") {
      val newList = (oldList + listOf(10, 20, 30, 40, 50)).shuffled()
      data class TestRef(val `$ref`: Int)
      data class TestData(val anyOf: List<TestRef>)
      val oldSchema = Validator.objectMapper.valueToTree<ObjectNode>(TestData(oldList.map { TestRef(it) }))
      val newSchema = Validator.objectMapper.valueToTree<ObjectNode>(TestData(newList.map { TestRef(it) }))
      Then("There should be no errors") {
        Validator.validate(oldSchema, newSchema, Config(addingAnyOf = ALLOWED)).shouldNotHaveErrors()
      }
    }
  }
})
