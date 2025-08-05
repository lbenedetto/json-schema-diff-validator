package io.github.lbenedetto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.flipkart.zjsonpatch.DiffFlags
import com.flipkart.zjsonpatch.JsonDiff
import com.flipkart.zjsonpatch.Operation
import io.github.lbenedetto.jsonschema.AnyOfSimpleType
import java.nio.file.Paths
import java.util.*

object Validator {
  val objectMapper = ObjectMapper()

  fun validate(
    oldSchemaPath: String,
    newSchemaPath: String,
    config: Config = Config()
  ): ValidationResult {
    val oldSchema = objectMapper.readTree(Paths.get(oldSchemaPath).toFile())
    val newSchema = objectMapper.readTree(Paths.get(newSchemaPath).toFile())

    return validate(oldSchema, newSchema, config)
  }

  fun sortAllArrays(node: JsonNode) {
    node.filter { it is ArrayNode }
      .map { it as ArrayNode }
      .forEach {
        val sorted = it.toList().sortedBy { child -> child.toString() }
        it.removeAll()
        it.addAll(sorted)
      }
  }


  fun validate(
    oldSchema: JsonNode,
    newSchema: JsonNode,
    config: Config = Config()
  ): ValidationResult {
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

    val validationResult = ValidationResult()
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
      arrayDiff.added.forEach { addedValue ->
        if (addedValue.isNullType()) {
          validationResult[config.removingRequired].add("Added null option to anyOf at $path")
        } else {
          validationResult[config.addingAnyOf].add("Added new anyOf $addedValue to $path")
        }
      }
      arrayDiff.removed.forEach { removedValue ->
        if (removedValue.isNullType()) {
          validationResult[config.addingRequired].add("Removed null option from anyOf at $path")
        } else {
          validationResult[config.removingAnyOf].add("Removed anyOf $removedValue from $path")
        }
      }
    }
    modifiedEnumPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      arrayDiff.added.forEach { addedValue ->
        validationResult[config.addingEnumValue].add("Added new enum value $addedValue to $path")
      }
      arrayDiff.removed.forEach { removedValue ->
        validationResult[config.removingEnumValue].add("Removed enum value $removedValue from $path")
      }
    }
    modifiedRequiredPaths.forEach { path ->
      val arrayDiff = computeArrayDiff(oldSchema, newSchema, path)
      arrayDiff.added.forEach { addedValue ->
        validationResult[config.addingRequired].add("Added non-null requirement for $addedValue to $path")
      }
      arrayDiff.removed.forEach { removedValue ->
        validationResult[config.removingRequired].add("Removed non-null requirement for $removedValue from $path")
      }
    }

    addedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val value = newSchema.at(path).asInt()
        validationResult[Compatibility.FORBIDDEN].add("Added minItems requirement of $value at $path")
        return@forEach
      }
      val fieldName = getLastSubPath(path)
      if (newSchema.isFieldRequired(path, fieldName)) {
        validationResult[config.addingRequiredFields].add("Added new required field $fieldName at $path")
      } else {
        validationResult[config.addingOptionalFields].add("Added new optional field $fieldName at $path")
      }
    }

    removedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val value = newSchema.at(path).asInt()
        validationResult[Compatibility.ALLOWED].add("Removed minItems requirement of $value at $path")
        return@forEach
      }
      val fieldName = getLastSubPath(path)
      if (oldSchema.isFieldRequired(path, fieldName)) {
        validationResult[config.removingRequiredFields].add("Removed a field $fieldName at $path which was previously required")
      } else {
        validationResult[config.removingOptionalFields].add("Removed a field $fieldName at $path which was previously optional")
      }
    }

    changedFieldPaths.forEach { path ->
      if (newSchema.isMinItemsPath(path)) {
        val oldValue = oldSchema.at(path).asInt()
        val newValue = newSchema.at(path).asInt()
        if (newValue > oldValue) {
          validationResult[Compatibility.FORBIDDEN].add("Increased minItems from $oldValue to $newValue at $path")
        } else {
          validationResult[Compatibility.ALLOWED].add("Decreased minItems from $oldValue to $newValue at $path")
        }
      } else if (path.matches(plainFieldTypeRegex)) {
        val oldType = oldSchema.resolveSimpleType(path)
        val newType = newSchema.resolveSimpleType(path)
        val changeCompatibility = if (newType.isNullable() && !oldType.isNullable()) {
          config.removingRequired
        } else if (!newType.isNullable() && oldType.isNullable()) {
          config.addingRequired
        } else if (newType.ignoringNull() != oldType.ignoringNull()) {
          Compatibility.FORBIDDEN
        } else {
          Compatibility.ALLOWED
        }

        validationResult[changeCompatibility].add("Changed field type from $oldType to $newType at $path")
      } else {
        val oldValue = oldSchema.at(path).toString()
        val newValue = newSchema.at(path).toString()
        validationResult[Compatibility.FORBIDDEN].add("Changed field at $path from: $oldValue to: $newValue")
      }
    }

    return validationResult
  }

  private fun JsonNode.resolveSimpleType(path: String): AnyOfSimpleType {
    return when (val typeNode = at(path)) {
      is ArrayNode -> AnyOfSimpleType(typeNode.map { it.textValue() })
      is TextNode -> AnyOfSimpleType(listOf(typeNode.textValue()))
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

  private fun JsonNode.isFieldRequired(path: String, fieldName: String) : Boolean {
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
