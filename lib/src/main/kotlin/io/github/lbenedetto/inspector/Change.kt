package io.github.lbenedetto.inspector

import com.fasterxml.jackson.databind.JsonNode

interface Change {
  val path: String
}

interface ChangeWithType : Change {
  val changeType: ChangeType
}

interface ChangeWithField : ChangeWithType {
  val field: String
}

private fun Change.withContext(toString: String) = "${this::class.simpleName}: $toString at $path"

data class AnyOfChange(override val path: String, val value: JsonNode, override val changeType: ChangeType) : ChangeWithType {
  override fun toString() = withContext("$changeType anyOf $value")
}

data class EnumValueChange(override val path: String, val value: JsonNode, override val changeType: ChangeType) : ChangeWithType {
  override fun toString() = withContext("$changeType enum value $value")
}

data class FieldChange(override val path: String, override val field: String, override val changeType: ChangeType) : ChangeWithField {
  override fun toString() = withContext("$changeType field $field")
}

data class FieldTypeChange(override val path: String, val oldType: String, val newType: String) : Change {
  override fun toString() = withContext("Field type changed from $oldType to $newType")
}

data class NonNullRequirementChange(override val path: String, override val field: String, override val changeType: ChangeType) : ChangeWithField {
  override fun toString() = withContext("$changeType non-null requirement for $field")
}

data class NotAbsentRequirementChange(override val path: String, override val field: String, override val changeType: ChangeType) : ChangeWithField {
  override fun toString() = withContext("$changeType not-absent requirement for $field")
}

data class MinItemsChange(override val path: String, val oldValue: Int?, val newValue: Int?, override val changeType: ChangeType) : ChangeWithType {
  override fun toString() = withContext("$changeType minItems ($oldValue -> $newValue)")
}
