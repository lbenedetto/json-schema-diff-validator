package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.Compatibility.FORBIDDEN
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonObject
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveErrors
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec

internal class AllowReorderTest : BehaviorSpec({
  Given("A schema with an inline node") {
    val oldSchema = Util.readSchema("data_options_allowReorder.schema")

    When("Items are reordered") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/status_node", """{"type": "object"}"""),
        add("/definitions/inline_node/anyOf/1", jsonObject("\$ref" to "\"#/definitions/inline_node\""))
      )

      Then("Should have errors if allowReorder is false") {
        Validator.validate(oldSchema, newSchema, Config(anyOfReordering = FORBIDDEN)).shouldHaveErrors()
      }

      Then("Should not have errors if allowReorder is true") {
        Validator.validate(oldSchema, newSchema, Config(anyOfReordering = ALLOWED)).shouldNotHaveErrors()
      }
    }

    When("Items are removed as a result of reordering") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/status_node", """{"type": "object"}"""),
        replace("/definitions/inline_node/anyOf/0", jsonObject("\$ref" to "\"#/definitions/inline_node\""))
      )

      Then("Should have errors if allowReorder is false") {
        Validator.validate(oldSchema, newSchema, Config(anyOfReordering = FORBIDDEN)).shouldHaveErrors()
      }

      Then("Should have errors if allowReorder is true") {
        Validator.validate(oldSchema, newSchema, Config(anyOfReordering = ALLOWED)).shouldHaveErrors()
      }
    }
  }
})
