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
          changes.nonNullRequirement += NonNullRequirementChange(fieldPath, fieldName, ChangeType.REMOVED)
        } else {
          changes.anyOf += AnyOfChange(path, addedValue, ChangeType.ADDED)
        }
      }
      arrayDiff.removed.forEach { removedValue ->
        if (removedValue.isNullType()) {
          changes.nonNullRequirement += NonNullRequirementChange(fieldPath, fieldName, ChangeType.ADDED)
        } else {
          changes.anyOf += AnyOfChange(path, removedValue, ChangeType.REMOVED)
        }
      }
    }
    modifiedEnumPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      changes.enumValue += arrayDiff.added.map { EnumValueChange(path, it, ChangeType.ADDED) }
      changes.enumValue += arrayDiff.removed.map { EnumValueChange(path, it, ChangeType.REMOVED) }
    }
    modifiedRequiredPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      val fieldLocation = "${path.back()}/properties"
      changes.notAbsentRequirement += arrayDiff.added
        .map { NotAbsentRequirementChange(fieldLocation, it.textValue(), ChangeType.ADDED) }
      changes.notAbsentRequirement += arrayDiff.removed
        .map { NotAbsentRequirementChange(fieldLocation, it.textValue(), ChangeType.REMOVED) }
    }

    addedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val value = newSchema.at(path).asInt()
        changes.minItems += MinItemsChange(path, null, value, ChangeType.ADDED)
        return@forEach
      }
      val fieldName = getLastSubPath(path)
      val isFieldRequired = newSchema.isFieldRequired(path, fieldName)
      if (isFieldRequired) {
        changes.notAbsentRequirement += NotAbsentRequirementChange(path.back(), fieldName, ChangeType.ADDED)
      }
      val isFieldNullable = newSchema.isFieldNullable(path)
      if (!isFieldNullable) {
        changes.nonNullRequirement += NonNullRequirementChange(path.back(), fieldName, ChangeType.ADDED)
      }
      changes.fields += FieldChange(path.back(), fieldName, ChangeType.ADDED)
    }

    removedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val value = oldSchema.at(path).asInt()
        changes.minItems += MinItemsChange(path, value, null, ChangeType.REMOVED)
        return@forEach
      }
      val fieldName = getLastSubPath(path)
      val wasFieldRequired = oldSchema.isFieldRequired(path, fieldName)
      if (wasFieldRequired) {
        changes.notAbsentRequirement += NotAbsentRequirementChange(path.back(), fieldName, ChangeType.REMOVED)
      }
      val wasFieldNullable = oldSchema.isFieldNullable(path)
      if (!wasFieldNullable) {
        changes.nonNullRequirement += NonNullRequirementChange(path.back(), fieldName, ChangeType.REMOVED)
      }
      changes.fields += FieldChange(path.back(), fieldName, ChangeType.REMOVED)
    }

    changedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val oldValue = oldSchema.at(path).asInt()
        val newValue = newSchema.at(path).asInt()
        val changeType = if (newValue > oldValue) ChangeType.ADDED else ChangeType.REMOVED
        changes.minItems += MinItemsChange(path, oldValue, newValue, changeType)
      } else if (path.matches(plainFieldTypeRegex)) {
        val oldType = oldSchema.at(path).resolveType()
        val newType = newSchema.at(path).resolveType()
        val fieldName = getLastSubPath(path.back())
        val fieldLocation = path.back().back()

        val oldTypeIsNullable = oldType.isNullable()
        val newTypeIsNullable = newType.isNullable()
        if (newTypeIsNullable && !oldTypeIsNullable) {
          changes.nonNullRequirement += NonNullRequirementChange(fieldLocation, fieldName, ChangeType.REMOVED)
        } else if (!newTypeIsNullable && oldTypeIsNullable) {
          changes.nonNullRequirement += NonNullRequirementChange(fieldLocation, fieldName, ChangeType.ADDED)
        }

        val newTypeIgnoringNull = newType.ignoringNull()
        val oldTypeIgnoringNull = oldType.ignoringNull()
        if (newTypeIgnoringNull != oldTypeIgnoringNull) {
          changes.fieldTypes += FieldTypeChange(path, oldTypeIgnoringNull.toString(), newTypeIgnoringNull.toString())
        }
      } else if (path.endsWith("\$ref")) {
        val oldValue = oldSchema.at(oldSchema.at(path).textValue().substringAfter("#"))
        val newValue = newSchema.at(newSchema.at(path).textValue().substringAfter("#"))
        changes.fieldTypes += FieldTypeChange(path, oldValue.toString(), newValue.toString())
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

  private fun JsonNode.resolveType(): CompoundType {
    return when (this) {
      is ArrayNode -> CompoundType(this.map { it.textValue() })
      is TextNode -> CompoundType(listOf(this.textValue()))
      else -> throw IllegalStateException("Unexpected type node: $this")
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

  private fun JsonNode.isFieldNullable(path: String): Boolean {
    val nodeAtPath = at(path)
    if (nodeAtPath.has("type")) {
      return nodeAtPath["type"].resolveType().isNullable()
    }
    if (nodeAtPath.has("anyOf")) {
      return nodeAtPath.withArray<JsonNode>("anyOf")
        .mapIndexed { index, _ -> this.isFieldNullable("$path/anyOf/$index")}
        .any()
    }
    if (nodeAtPath.has("\$ref")) {
      return isFieldNullable(nodeAtPath["\$ref"].asText().substringAfter("#"))
    }

    throw IllegalStateException("Unable to determine field nullability at $path")
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
