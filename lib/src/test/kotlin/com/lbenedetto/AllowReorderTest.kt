package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonObject
import com.lbenedetto.util.PatchDSL.replace
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AllowReorderTest : BehaviorSpec({
  Given("A schema with an inline node") {
    val oldSchema = Util.readSchema("data_options_allowReorder.schema")

    When("Items are reordered") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/status_node", """{"type": "object"}"""),
        add("/definitions/inline_node/anyOf/1", jsonObject("\$ref" to "\"#/definitions/inline_node\""))
      )

      Then("Should throw if allowReorder is false") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema, allowReorder = false)
        }
      }

      Then("Should not throw if allowReorder is true") {
        assertDoesNotThrow {
          Validator.validate(oldSchema, newSchema, allowReorder = true)
        }
      }
    }

    When("Items are removed as a result of reordering") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/status_node", """{"type": "object"}"""),
        replace("/definitions/inline_node/anyOf/0", jsonObject("\$ref" to "\"#/definitions/inline_node\""))
      )

      Then("Should throw if allowReorder is false") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema, allowReorder = false)
        }
      }

      Then("Should throw if allowReorder is true") {
        assertThrows<IllegalStateException> {
          Validator.validate(oldSchema, newSchema, allowReorder = true)
        }
      }
    }
  }
})
