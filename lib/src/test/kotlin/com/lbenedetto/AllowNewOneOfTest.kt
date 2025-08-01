package com.lbenedetto

import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonObject
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AllowNewOneOfTest : BehaviorSpec({
  Given("A new oneOf element is added to the schema") {
    val oldSchema = Util.readSchema("data_options_allowOneOf.schema")
    val newSchema = oldSchema.withPatches(
      add("/definitions/root/items/anyOf/-", """{"type": "number"}"""),
      add("/definitions/anotherItem/content/items/0/anyOf/-", """{"fruit": "pear"}"""),
      add("/definitions/inline_node/anyOf/-", jsonObject("\$ref" to "\"#/definitions/hardBreak_node\""))
    )

    When("allowNewOneOf is set to true") {
      Then("No exception is thrown") {
        assertDoesNotThrow { Validator.validate(oldSchema, newSchema, allowNewOneOf = true) }
      }
    }

    When("allowNewOneOf is set to false") {
      Then("An exception is thrown") {
        assertThrows<IllegalStateException> { Validator.validate(oldSchema, newSchema, allowNewOneOf = false) }
      }
    }
  }
})
