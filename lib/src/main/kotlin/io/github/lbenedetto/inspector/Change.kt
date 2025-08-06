package io.github.lbenedetto.inspector

import com.fasterxml.jackson.databind.JsonNode
import io.github.lbenedetto.jsonschema.CompoundType

interface Change {
  val path: String
}

private fun Change.withContext(toString: String) = "${this::class.simpleName}: $toString at $path"

data class AnyOfChange(override val path: String, val value: JsonNode, val change: ChangeType) : Change {
  override fun toString() = withContext("$change anyOf $value")
}

data class EnumValueChange(override val path: String, val value: JsonNode, val change: ChangeType) : Change {
  override fun toString() = withContext("$change enum value $value")
}

data class FieldChange(override val path: String, val fieldName: String, val change: ChangeType, val required: Boolean) : Change {
  override fun toString() = withContext("$change field $fieldName")
}

data class FieldTypeChange(override val path: String, val oldType: CompoundType, val newType: CompoundType) : Change {
  override fun toString() = withContext("Field type changed from $oldType to $newType")
}

data class NonNullRequirementChange(override val path: String, val value: String, val change: ChangeType) : Change {
  override fun toString() = withContext("$change non-null requirement for $value")
}

data class MinItemsChange(override val path: String, val oldValue: Int?, val newValue: Int?, val change: ChangeType) : Change {
  override fun toString() = withContext("$change minItems ($oldValue -> $newValue)")
}
