package io.github.lbenedetto

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.lbenedetto.inspector.AnyOfChange
import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonObject
import io.github.lbenedetto.util.PatchDSL.node
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class AnyOfTest : BehaviorSpec({
  Given("A schema with an anyOf element") {


    When("A new anyOf element is added") {
      val node = """{"type": "number"}"""
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/-", node)
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          AnyOfChange("/properties/fieldWithAnyOf/anyOf", node(node), ChangeType.ADDED)
        )
      }
    }

    When("An anyOf element is removed") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        remove("/properties/fieldWithAnyOf/anyOf/0")
      )
      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          AnyOfChange("/properties/fieldWithAnyOf/anyOf", node("""{"type":"boolean"}"""), ChangeType.REMOVED)
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

      val oldSchema = Inspector.objectMapper.valueToTree<ObjectNode>(TestData(oldList.map { TestRef(it) }))
      val newSchema = Inspector.objectMapper.valueToTree<ObjectNode>(TestData(newList.map { TestRef(it) }))
      Then("There should be no errors") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          AnyOfChange("/anyOf", node(jsonObject("\$ref" to "10")), ChangeType.ADDED),
          AnyOfChange("/anyOf", node(jsonObject("\$ref" to "20")), ChangeType.ADDED),
          AnyOfChange("/anyOf", node(jsonObject("\$ref" to "30")), ChangeType.ADDED),
          AnyOfChange("/anyOf", node(jsonObject("\$ref" to "40")), ChangeType.ADDED),
          AnyOfChange("/anyOf", node(jsonObject("\$ref" to "50")), ChangeType.ADDED),
          AnyOfChange("/anyOf", node(jsonObject("\$ref" to "9")), ChangeType.REMOVED),
        )
      }
    }
  }
})
