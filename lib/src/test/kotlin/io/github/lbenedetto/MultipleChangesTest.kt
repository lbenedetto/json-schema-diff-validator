package io.github.lbenedetto

import io.github.lbenedetto.util.Util
import io.github.lbenedetto.validator.Config
import io.github.lbenedetto.validator.Validator
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class MultipleChangesTest : BehaviorSpec({
    Given("A schema") {
        val oldSchemaPath = "multipleChangesBefore.schema"
        val newSchemaPath = "multipleChangesAfter.schema"

        When("Multiple changes are made to the schema properties") {
            val oldSchema = Util.readSchema(oldSchemaPath)
            val newSchema = Util.readSchema(newSchemaPath)

            Then("Changes should be correctly reported") {
                val validationResult = Validator.validate(oldSchema, newSchema, Config.defaultConfig())
                validationResult.safe.shouldContainExactlyInAnyOrder(
                    "FieldChange: ADDED field optionalProp at /properties as nullable}",
                    "FieldChange: ADDED field optionalProp at /properties as optional}",
                    "NonNullRequirementChange: REMOVED non-null requirement for value at /properties",
                    "NotAbsentRequirementChange: REMOVED non-null requirement for value at /properties"
                )
                validationResult.risky shouldBe emptyList()
                validationResult.fatal.shouldContainExactlyInAnyOrder(
                    "FieldChange: ADDED field reqProp at /properties as non-null}",
                    "FieldChange: ADDED field reqProp at /properties as required}",
                    "NonNullRequirementChange: ADDED non-null requirement for reqProp at /properties",
                    "NotAbsentRequirementChange: ADDED non-null requirement for reqProp at /properties"
                )
            }
        }
    }
})
