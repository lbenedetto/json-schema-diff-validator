package io.github.lbenedetto

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.lbenedetto.Compatibility.ALLOWED
import io.github.lbenedetto.Compatibility.FORBIDDEN
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonObject
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

internal class AnyOfTest : BehaviorSpec({
  Given("A new anyOf element is added to the schema") {
    val oldSchema = Util.readSchema("data_options_allowAnyOf.schema")
    val config = Config(addingAnyOf = ALLOWED, removingAnyOf = FORBIDDEN)

    When("A new anyOf element is added for a field") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/root/items/anyOf/1", """{"type": "number"}""")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          allowed = mutableListOf("Added new anyOf {\"type\":\"number\"} to /definitions/root/items/anyOf")
        )
      }
    }

    When("A new anyOf element is added for an array") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/anotherItem/content/items/0/anyOf/1", """{"fruit": "pear"}""")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          allowed = mutableListOf("Added new anyOf {\"fruit\":\"pear\"} to /definitions/anotherItem/content/items/0/anyOf")
        )
      }
    }

    When("A new anyOf ref is added for a field") {
      val newSchema = oldSchema.withPatches(
        add("/definitions/inline_node/anyOf/1", jsonObject("\$ref" to "\"#/definitions/hardBreak_node\""))
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          allowed = mutableListOf("Added new anyOf {\"\$ref\":\"#/definitions/hardBreak_node\"} to /definitions/inline_node/anyOf")
        )
      }
    }

    When("An anyOf element is removed from a field") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/root/items/anyOf/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          forbidden = mutableListOf("Removed anyOf {\"type\":\"string\"} from /definitions/root/items/anyOf")
        )
      }
    }

    When("An anyOf element is removed from an array") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/anotherItem/content/items/0/anyOf/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          forbidden = mutableListOf("Removed anyOf {\"fruit\":\"banana\"} from /definitions/anotherItem/content/items/0/anyOf")
        )
      }
    }

    When("An anyOf ref is removed from a field") {
      val newSchema = oldSchema.withPatches(
        remove("/definitions/inline_node/anyOf/0")
      )
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          forbidden = mutableListOf("Removed anyOf {\"\$ref\":\"#/definitions/text_node\"} from /definitions/inline_node/anyOf")
        )
      }
    }
  }

  Given("A list of random refs") {
    val oldList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    When("New refs are added, a ref is removed, and the list is shuffled") {
      val newList = (oldList + listOf(10, 20, 30, 40, 50)).shuffled().filter { it != 9 }

      data class TestRef(val `$ref`: Int)
      data class TestData(val anyOf: List<TestRef>)

      val oldSchema = Validator.objectMapper.valueToTree<ObjectNode>(TestData(oldList.map { TestRef(it) }))
      val newSchema = Validator.objectMapper.valueToTree<ObjectNode>(TestData(newList.map { TestRef(it) }))
      Then("There should be no errors") {
        val config = Config(addingAnyOf = ALLOWED, removingAnyOf = FORBIDDEN)
        Validator.validate(oldSchema, newSchema, config) shouldBe ValidationResult(
          allowed = mutableListOf(
            "Added new anyOf {\"\$ref\":10} to /anyOf",
            "Added new anyOf {\"\$ref\":20} to /anyOf",
            "Added new anyOf {\"\$ref\":30} to /anyOf",
            "Added new anyOf {\"\$ref\":40} to /anyOf",
            "Added new anyOf {\"\$ref\":50} to /anyOf",
          ),
          forbidden = mutableListOf(
            "Removed anyOf {\"\$ref\":9} from /anyOf"
          )
        )
      }
    }
  }
})
