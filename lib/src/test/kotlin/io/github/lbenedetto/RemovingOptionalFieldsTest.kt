package io.github.lbenedetto

import io.github.lbenedetto.Compatibility.ALLOWED
import io.github.lbenedetto.Compatibility.FORBIDDEN
import io.github.lbenedetto.util.PatchDSL.remove
import io.github.lbenedetto.util.Util
import io.github.lbenedetto.util.Util.withPatches
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

internal class RemovingOptionalFieldsTest : BehaviorSpec({

  Given("A schema") {
    val oldSchema = Util.readSchema("ExampleObject.schema")

    When("An optional field is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/someNullableField"))
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(removingOptionalFields = ALLOWED)) shouldBe ValidationResult(
          allowed = mutableListOf("Removed a field someNullableField at /properties/someNullableField which was previously optional")
        )
      }
    }

    When("A required field is removed") {
      val newSchema = oldSchema.withPatches(remove("/properties/fieldWhichReferencesRequiredTypeDef"))
      Then("Change should be detected") {
        Validator.validate(oldSchema, newSchema, Config(removingOptionalFields = FORBIDDEN)) shouldBe ValidationResult(
          forbidden = mutableListOf("Removed a field fieldWhichReferencesRequiredTypeDef at /properties/fieldWhichReferencesRequiredTypeDef which was previously required")
        )
      }
    }
  }
})
