package com.lbenedetto

import com.lbenedetto.Compatibility.ALLOWED
import com.lbenedetto.Compatibility.FORBIDDEN
import com.lbenedetto.util.PatchDSL.add
import com.lbenedetto.util.PatchDSL.jsonObject
import com.lbenedetto.util.Util
import com.lbenedetto.util.Util.shouldHaveErrors
import com.lbenedetto.util.Util.shouldNotHaveErrors
import com.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec

internal class AllowNewOneOfTest : BehaviorSpec({
  Given("A new oneOf element is added to the schema") {
    val oldSchema = Util.readSchema("data_options_allowOneOf.schema")
    val newSchema = oldSchema.withPatches(
      add("/definitions/root/items/anyOf/-", """{"type": "number"}"""),
      add("/definitions/anotherItem/content/items/0/anyOf/-", """{"fruit": "pear"}"""),
      add("/definitions/inline_node/anyOf/-", jsonObject("\$ref" to "\"#/definitions/hardBreak_node\""))
    )

    When("allowNewOneOf is set to true") {
      Then("No errors") {
        Validator.validate(oldSchema, newSchema, Config(newOneOf = ALLOWED)).shouldNotHaveErrors()
      }
    }

    When("allowNewOneOf is set to false") {
      Then("An exception is thrown") {
        Validator.validate(oldSchema, newSchema, Config(newOneOf = FORBIDDEN)).shouldHaveErrors()
      }
    }
  }
})
