package io.github.lbenedetto

import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.util.PatchDSL.add
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

internal class UniqueItemsTest : BehaviorSpec({
    Given("A schema without uniqueItems and a schema with uniqueItems") {
        val oldSchemaPath = "withoutUniqueItems.schema"
        val newSchemaPath = "withUniqueItems.schema"

        When("The schemas are compared") {
            val oldSchema = Util.readSchema(oldSchemaPath)
            val newSchema = Util.readSchema(newSchemaPath)

            Then("Only property changes should be detected") {
                Inspector.inspect(oldSchema, newSchema).all().shouldContainExactlyInAnyOrder(
                    FieldChange($$"/$defs/Foo/properties", "uniqueItems", ChangeType.ADDED)
                )
            }
        }
    }
})
