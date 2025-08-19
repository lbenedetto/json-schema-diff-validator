package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.jsonString
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class DefsTest : BehaviorSpec({
    Given("A schema") {
        val schemaPath = "withDefs.schema"

        When("All defs and corresponding props are deleted") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                remove($$"/$defs"),
                remove("/properties/foo"),
                remove("/properties/something"),
            )

            Then("Only property changes should be detected") {
                val result = Inspector.inspect(oldSchema, newSchema).all()
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldChange("/properties", "foo", ChangeType.REMOVED),
                    FieldChange("/properties", "something", ChangeType.REMOVED)
                )
            }
        }

        When("A def and corresponding prop are removed but one def remains") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                remove($$"/$defs/Foo"),
                remove("/properties/foo"),
            )

            Then("Only property changes should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldChange("/properties", "foo", ChangeType.REMOVED),
                )
            }
        }

        When("A def and corresponding prop are added") {
            val oldSchema = Util.readSchema(schemaPath)
            val newDef = """
                {
                  "type": "object",
                  "properties": {
                    "someProp": {
                      "type": "number"
                    }
                  },
                  "required": [
                    "someProp"
                  ]
                }""".trimIndent()
            @Suppress("JsonStandardCompliance")
            val newField = $$"""
                {
                  "anyOf": [
                    {
                      "type": "null"
                    },
                    {
                      "$ref": "#/$defs/NewDef"
                    }
                  ]
                }""".trimIndent()
            val newSchema = oldSchema.withPatches(
                add($$"/$defs/NewDef", newDef),
                add("/properties/newField", newField)
            )

            Then("Only property changes should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldChange("/properties", "newField", ChangeType.ADDED)
                )
            }
        }

        When("A property in a def is added") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                add(
                    $$"/$defs/Foo/properties/newField",
                    """{ "type": ["number", "null"] }"""
                ),
            )

            Then("Change should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldChange($$"/$defs/Foo/properties", "newField", ChangeType.ADDED),
                )
            }
        }

        When("A property in a def is removed") {
            val oldSchema = Util.readSchema(schemaPath).withPatches(
                add(
                    $$"/$defs/Foo/properties/newField",
                    """{ "type": ["number", "null"] }"""
                ),
            )
            val newSchema = oldSchema.withPatches(
                remove($$"/$defs/Foo/properties/newField")
            )

            Then("Change should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldChange($$"/$defs/Foo/properties", "newField", ChangeType.REMOVED),
                )
            }
        }
    }
})
