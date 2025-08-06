package io.github.lbenedetto.inspector

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.flipkart.zjsonpatch.DiffFlags
import com.flipkart.zjsonpatch.JsonDiff
import com.flipkart.zjsonpatch.Operation
import io.github.lbenedetto.jsonschema.CompoundType
import java.nio.file.Paths
import java.util.EnumSet
import kotlin.collections.forEach

object Inspector {
  val objectMapper = ObjectMapper()

  fun inspect(
    oldSchemaPath: String,
    newSchemaPath: String
  ): DetectedChanges {
    val oldSchema = objectMapper.readTree(Paths.get(oldSchemaPath).toFile())
    val newSchema = objectMapper.readTree(Paths.get(newSchemaPath).toFile())

    return inspect(oldSchema, newSchema)
  }

  fun inspect(
    oldSchema: JsonNode,
    newSchema: JsonNode
  ): DetectedChanges {
    // Presort the arrays so that JsonDiff doesn't get confused by re-ordered lists
    sortAllArrays(oldSchema)
    sortAllArrays(newSchema)

    // Simplify the diff configuration to only emit ADD, REMOVE, and REPLACE operations
    val diff = JsonDiff.asJson(
      oldSchema, newSchema, EnumSet.of(
        DiffFlags.OMIT_COPY_OPERATION,
        DiffFlags.OMIT_MOVE_OPERATION,
        DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE
      )
    )

    val changes = DetectedChanges()
    // Json diff for re-ordered lists is complex, so we'll just keep track of every modified list
    // And then do our own comparison between the old and new version of the list
    val modifiedAnyOfPaths = mutableSetOf<String>()
    val modifiedEnumPaths = mutableSetOf<String>()
    val modifiedRequiredPaths = mutableSetOf<String>()
    val removedFieldPaths = mutableSetOf<String>()
    val addedFieldPaths = mutableSetOf<String>()
    val changedFieldPaths = mutableSetOf<String>()

    val modifiedAnyOfRegex = Regex(".*/anyOf/[\\d-]+$")
    val modifiedEnumRegex = Regex(".*/enum/[\\d-]+$")
    val modifiedRequiredRegex = Regex(".*/required/[\\d-]+$")
    val plainFieldTypeRegex = Regex(".*/properties/.*?/type$")

    diff.forEach { node ->
      val operation = Operation.fromRfcName(node["op"].asText())
      val path = node["path"].asText()

      if (path.matches(modifiedAnyOfRegex)) {
        modifiedAnyOfPaths.add(path.back())
      } else if (path.matches(modifiedEnumRegex)) {
        modifiedEnumPaths.add(path.back())
      } else if (path.matches(modifiedRequiredRegex)) {
        modifiedRequiredPaths.add(path.back())
      } else {
        when (operation) {
          Operation.REMOVE -> removedFieldPaths.add(path)
          Operation.ADD -> addedFieldPaths.add(path)
          Operation.REPLACE -> changedFieldPaths.add(path)
          Operation.MOVE, Operation.COPY, Operation.TEST -> throw IllegalStateException("Unsupported operation: $node")
        }
      }
    }

    modifiedAnyOfPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      val fieldPath = path.back().back()
      val fieldName = getLastSubPath(path.back())
      arrayDiff.added.forEach { addedValue ->
        if (addedValue.isNullType()) {
          changes.nonNullRequirement.add(NonNullRequirementChange(fieldPath, fieldName, ChangeType.REMOVED))
        } else {
          changes.anyOf.add(AnyOfChange(path, addedValue, ChangeType.ADDED))
        }
      }
      arrayDiff.removed.forEach { removedValue ->
        if (removedValue.isNullType()) {
          changes.nonNullRequirement.add(NonNullRequirementChange(fieldPath, fieldName, ChangeType.ADDED))
        } else {
          changes.anyOf.add(AnyOfChange(path, removedValue, ChangeType.REMOVED))
        }
      }
    }
    modifiedEnumPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      arrayDiff.added.forEach { addedValue ->
        changes.enumValue.add(EnumValueChange(path, addedValue, ChangeType.ADDED))
      }
      arrayDiff.removed.forEach { removedValue ->
        changes.enumValue.add(EnumValueChange(path, removedValue, ChangeType.REMOVED))
      }
    }
    modifiedRequiredPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      val fieldLocation = "${path.back()}/properties"
      arrayDiff.added.forEach { addedValue ->
        changes.nonNullRequirement.add(
          NonNullRequirementChange(
            fieldLocation,
            addedValue.textValue(),
            ChangeType.ADDED
          )
        )
      }
      arrayDiff.removed.forEach { removedValue ->
        changes.nonNullRequirement.add(
          NonNullRequirementChange(
            fieldLocation,
            removedValue.textValue(),
            ChangeType.REMOVED
          )
        )
      }
    }

    addedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val value = newSchema.at(path).asInt()
        changes.minItems.add(MinItemsChange(path, null, value, ChangeType.ADDED))
        return@forEach
      }
      val fieldName = getLastSubPath(path)
      val isFieldRequired = newSchema.isFieldRequired(path, fieldName)
      changes.fields.add(FieldChange(path.back(), fieldName, ChangeType.ADDED, isFieldRequired))
    }

    removedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val value = oldSchema.at(path).asInt()
        changes.minItems.add(MinItemsChange(path, value, null, ChangeType.REMOVED))
        return@forEach
      }
      val fieldName = getLastSubPath(path)
      val wasFieldRequired = oldSchema.isFieldRequired(path, fieldName)
      changes.fields.add(FieldChange(path.back(), fieldName, ChangeType.REMOVED, wasFieldRequired))
    }

    changedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val oldValue = oldSchema.at(path).asInt()
        val newValue = newSchema.at(path).asInt()
        val changeType = if (newValue > oldValue) {
          ChangeType.INCREASED
        } else {
          ChangeType.DECREASED
        }
        changes.minItems.add(MinItemsChange(path, oldValue, newValue, changeType))
      } else if (path.matches(plainFieldTypeRegex)) {
        val oldType = oldSchema.resolveSimpleType(path)
        val newType = newSchema.resolveSimpleType(path)
        val oldTypeIsNullable = oldType.isNullable()
        val newTypeIsNullable = newType.isNullable()
        val fieldName = getLastSubPath(path.back())
        val fieldLocation = path.back().back()
        if (newTypeIsNullable && !oldTypeIsNullable) {
          changes.nonNullRequirement.add(NonNullRequirementChange(fieldLocation, fieldName, ChangeType.REMOVED))
        } else if (!newTypeIsNullable && oldTypeIsNullable) {
          changes.nonNullRequirement.add(NonNullRequirementChange(fieldLocation, fieldName, ChangeType.ADDED))
        }

        val newTypeIgnoringNull = newType.ignoringNull()
        val oldTypeIgnoringNull = oldType.ignoringNull()
        if (newTypeIgnoringNull != oldTypeIgnoringNull) {
          changes.fieldTypes.add(FieldTypeChange(path, oldTypeIgnoringNull, newTypeIgnoringNull))
        }
      } else {
        val oldValue = oldSchema.at(path).toString()
        val newValue = newSchema.at(path).toString()
        throw IllegalStateException("Unexpected field change: $path. Old value: $oldValue. New value: $newValue")
      }
    }

    return changes
  }

  private fun sortAllArrays(node: JsonNode) {
    node.filter { it is ArrayNode }
      .map { it as ArrayNode }
      .forEach {
        val sorted = it.toList().sortedBy { child -> child.toString() }
        it.removeAll()
        it.addAll(sorted)
      }
  }

  private fun JsonNode.resolveSimpleType(path: String): CompoundType {
    return when (val typeNode = at(path)) {
      is ArrayNode -> CompoundType(typeNode.map { it.textValue() })
      is TextNode -> CompoundType(listOf(typeNode.textValue()))
      else -> throw IllegalStateException("Unexpected type node: $typeNode")
    }
  }

  /**
   * Gets the last segment of a JSON path.
   */
  private fun getLastSubPath(path: String): String {
    return path.substringAfterLast("/")
  }

  private fun String.back(): String {
    return substringBeforeLast("/")
  }

  private fun JsonNode.isFieldRequired(path: String, fieldName: String): Boolean {
    return at(path.back().back()).withArray<ArrayNode>("required")
      .any { it.asText() == fieldName }
  }

  private fun JsonNode.isMinItemsPath(path: String): Boolean {
    if (!path.endsWith("minItems")) {
      return false
    }
    val parentNode = at(path.back())
    if (!parentNode.has("type")) {
      return false
    }

    val type = parentNode.get("type").asText()
    return type == "array"
  }

  private fun JsonNode.isNullType() = has("type") && get("type").asText() == "null"

  data class ArrayDiff(val added: Set<JsonNode>, val removed: Set<JsonNode>)

  private fun computeArrayDiff(oldSchema: JsonNode, newSchema: JsonNode, path: String): ArrayDiff {
    val oldList = oldSchema.withArray<ArrayNode>(path).toSet()
    val newList = newSchema.withArray<ArrayNode>(path).toSet()
    return ArrayDiff(
      added = newList - oldList,
      removed = oldList - newList
    )
  }
}
