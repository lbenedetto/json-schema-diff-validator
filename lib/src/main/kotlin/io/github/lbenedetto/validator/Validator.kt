package io.github.lbenedetto.validator

import com.fasterxml.jackson.databind.JsonNode
import io.github.lbenedetto.inspector.Change
import io.github.lbenedetto.inspector.ChangeType
import io.github.lbenedetto.inspector.ChangeWithField
import io.github.lbenedetto.inspector.ChangeWithType
import io.github.lbenedetto.inspector.DetectedChanges
import io.github.lbenedetto.inspector.FieldChange
import io.github.lbenedetto.inspector.FieldTypeChange
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.inspector.NonNullRequirementChange
import io.github.lbenedetto.inspector.NotAbsentRequirementChange

object Validator {
  fun validate(
    oldSchemaPath: String,
    newSchemaPath: String,
    config: Config
  ): ValidationResult {
    val changes = Inspector.inspect(oldSchemaPath, newSchemaPath)
    return validate(changes, config)
  }

  fun validate(
    oldSchema: JsonNode,
    newSchema: JsonNode,
    config: Config
  ): ValidationResult {
    val changes = Inspector.inspect(oldSchema, newSchema)
    return validate(changes, config)
  }

  fun validate(changes: DetectedChanges, config: Config): ValidationResult {
    val result = ValidationResult()
    val changesPerPath = changes.perPath()

    result.addBasicChanges(changes.anyOf, config.anyOf)
    result.addBasicChanges(changes.enumValue, config.enumValue)
    result.addFieldChanges(changes.fields, changesPerPath, config)
    result.addFieldTypeChanges(changes.fieldTypes)
    result.addBasicChanges(changes.nonNullRequirement, config.nonNullRequirement)
    result.addBasicChanges(changes.notAbsentRequirement, config.notAbsentRequirement)
    result.addBasicChanges(changes.minItems, config.minValueRequirement)

    return result
  }

  fun ValidationResult.addFieldChanges(fieldChanges: Set<FieldChange>, changesPerPath: Map<String, List<Change>>, config: Config) {
    fieldChanges.forEach { change ->
        val changesAtPath = (changesPerPath[change.path] ?: emptyList())
            .filter { it is ChangeWithField && it.field == change.field }
      val hasNonNullRequirement = changesAtPath.any { it is NonNullRequirementChange }
      val hasNotAbsentRequirement = changesAtPath.any { it is NotAbsentRequirementChange }

      addFieldChangeWithRequirements(
        change = change,
        config = if (hasNonNullRequirement) config.nonNullField else config.nullableField,
        label = if (hasNonNullRequirement) "non-null" else "nullable",
      )

      addFieldChangeWithRequirements(
        change = change,
        config = if (hasNotAbsentRequirement) config.requiredField else config.optionalField,
        label = if (hasNotAbsentRequirement) "required" else "optional",
      )
    }
  }

  private fun ValidationResult.addFieldChangeWithRequirements(
    change: FieldChange,
    config: Config.AddingOrRemovingConfig,
    label: String,
  ) {
    val risk = when (change.changeType) {
      ChangeType.ADDED -> config.adding
      ChangeType.REMOVED -> config.removing
    }
    this[risk].add("$change as $label}")
  }

  fun ValidationResult.addFieldTypeChanges(fieldTypeChanges: Set<FieldTypeChange>) {
    this[Risk.FATAL].addAll(fieldTypeChanges.map { it.toString() })
  }

  fun ValidationResult.addBasicChanges(changes: Set<ChangeWithType>, config: Config.AddingOrRemovingConfig) = changes.forEach { change ->
    val risk = when(change.changeType) {
      ChangeType.ADDED -> config.adding
      ChangeType.REMOVED -> config.removing
    }
    this[risk].add(change.toString())
  }
}
