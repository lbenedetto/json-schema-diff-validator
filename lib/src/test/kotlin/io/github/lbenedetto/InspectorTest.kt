package io.github.lbenedetto

import io.github.lbenedetto.CompoundTypeFactory.integerType
import io.github.lbenedetto.CompoundTypeFactory.stringType
import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.DetectedChanges
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.FieldTypeChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.jsonschema.CompoundType
import io.github.lbenedetto.jsonschema.SimpleType
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive

object CompoundTypeFactory {
  fun integerType() = CompoundType(setOf(SimpleType.INTEGER))
  fun stringType() = CompoundType(setOf(SimpleType.STRING))
}

internal class InspectorTest : BehaviorSpec({
  Given("A schema") {
    val schemaPath = "ExampleObject.schema"

    When("Schema is the same") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.deepCopy()

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema) shouldBe DetectedChanges()
      }
    }

    When("A field type is changed") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        replace("/properties/someIntegerField/type", jsonString("string"))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldTypeChange("/properties/someIntegerField/type", integerType(), stringType())
        )
      }
    }

    // These two patches SHOULD always both be generated together, but we need to test our ability to detect them individually.
    // We do not validate that a schema was generated correctly.
    exhaustive(listOf(
      remove("/required/1"),
      replace("/properties/someIntegerField/type", """["integer", "null"]""")
    )).checkAll { patch ->
      When("A basic field is made optional by $patch") {
        val field = "someIntegerField"
        val oldSchema = Util.readSchema("ExampleObject.schema")
        val newSchema = oldSchema.withPatches(patch)

        Then("Change should be detected") {
          Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
            NonNullRequirementChange("/properties", field, ChangeType.REMOVED)
          )
        }
      }
    }

    // These two patches SHOULD always both be generated together, but we need to test our ability to detect them individually.
    // We do not validate that a schema was generated correctly.
    exhaustive(listOf(
      add("/required/-", jsonString("someNullableField")),
      replace("/properties/someNullableField/type", jsonString("integer"))
    )).checkAll { patch ->
      When("A basic field is made required by $patch") {
        val oldSchema = Util.readSchema("ExampleObject.schema")
        val newSchema = oldSchema.withPatches(patch)

        Then("Change should be detected") {
          Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
            NonNullRequirementChange("/properties", "someNullableField", ChangeType.ADDED)
          )
        }
      }
    }

    When("An anyOf field is made required") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/required/-", jsonString("fieldWithAnyOf")),
        remove("/properties/fieldWithAnyOf/anyOf/0"),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "fieldWithAnyOf", ChangeType.ADDED)
        )
      }
    }

    When("An anyOf field is made optional") {
      val newSchema = Util.readSchema("ExampleObject.schema")
      val oldSchema = newSchema.withPatches(
        add("/required/-", jsonString("fieldWithAnyOf")),
        remove("/properties/fieldWithAnyOf/anyOf/0"),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "fieldWithAnyOf", ChangeType.REMOVED),
        )
      }
    }

    When("An optional field is added") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties", "newField", ChangeType.ADDED, false)
        )
      }
    }

    When("A required field is added") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/newField", """{ "type": "string" }"""),
        add("/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties", "newField", ChangeType.ADDED),
          FieldChange("/properties", "newField", ChangeType.ADDED, true)
        )
      }
    }

    When("A required field is added to a subnode") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/\$defs/SomePojo/properties/newField", """{ "type": "string" }"""),
        add("/\$defs/SomePojo/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/\$defs/SomePojo/properties", "newField", ChangeType.ADDED),
          FieldChange("/\$defs/SomePojo/properties", "newField", ChangeType.ADDED, true)
        )
      }
    }

    When("A required field is added to a subnode in an anyOf") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
        add("/properties/fieldWithAnyOf/anyOf/1/required/-", jsonString("newField"))
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          NonNullRequirementChange("/properties/fieldWithAnyOf/anyOf/1/properties", "newField", ChangeType.ADDED),
          FieldChange("/properties/fieldWithAnyOf/anyOf/1/properties", "newField", ChangeType.ADDED, true)
        )
      }
    }

    When("A optional field is added to a subnode in an anyOf") {
      val oldSchema = Util.readSchema("ExampleObject.schema")
      val newSchema = oldSchema.withPatches(
        add("/properties/fieldWithAnyOf/anyOf/1/properties/newField", """{ "type": "string" }"""),
      )

      Then("Change should be detected") {
        Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
          FieldChange("/properties/fieldWithAnyOf/anyOf/1/properties", "newField", ChangeType.ADDED, false)
        )
      }
    }
  }
})
