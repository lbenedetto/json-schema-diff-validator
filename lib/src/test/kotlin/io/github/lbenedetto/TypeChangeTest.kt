package io.github.lbenedetto

import io.github.lbenedetto.inspector.FieldTypeChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.util.PatchDSL.replace
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

@Suppress("JsonStandardCompliance") // Reported false positive as KTIJ-35303
internal class TypeChangeTest : BehaviorSpec({
    Given("A schema") {
        val schemaPath = "typeChange.schema"

        When("The ref type in an anyOf with null is changed") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                replace("/properties/foo/anyOf/1", $$"""{ "$ref": "#/$defs/AnotherFoo" }""")
            )

            Then("The field type change should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldTypeChange($$"/properties/foo/anyOf/1/$ref", $$"[/$defs/Foo([OBJECT])]",
                        $$"[/$defs/AnotherFoo([OBJECT])]"),
                )
            }
        }

        When("The ref type of a non-nullable property is changed") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                replace("/properties/anotherFoo", $$"""{ "$ref": "#/$defs/Foo" }""")
            )

            Then("The field type change should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldTypeChange($$"/properties/anotherFoo/$ref", $$"[/$defs/AnotherFoo([OBJECT])]",
                        $$"[/$defs/Foo([OBJECT])]"),
                )
            }
        }

        When("The type of a nullable property is changed and the property is kept nullable") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                replace("/properties/baz", """{"type": ["boolean", "null"]}""")
            )

            Then("The field type change should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldTypeChange("/properties/baz/type/0", "[NUMBER]",
                        "[BOOLEAN]"),
                )
            }
        }

        When("The type of a non-nullable property is changed and the property is kept non-nullable") {
            val oldSchema = Util.readSchema(schemaPath)
            val newSchema = oldSchema.withPatches(
                replace("/properties/anotherBaz", """{ "type": "number" }""")
            )

            Then("The field type change should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldTypeChange("/properties/anotherBaz/type", "[BOOLEAN]",
                        "[NUMBER]"),
                )
            }
        }
    }
})
