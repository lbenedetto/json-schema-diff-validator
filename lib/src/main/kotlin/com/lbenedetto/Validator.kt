package com.lbenedetto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.flipkart.zjsonpatch.DiffFlags
import com.flipkart.zjsonpatch.JsonDiff
import com.flipkart.zjsonpatch.Operation
import java.nio.file.Paths
import java.util.*
import kotlin.collections.any
import kotlin.collections.sortedBy

object Validator {
  val objectMapper = ObjectMapper()

  // Path constants
  private const val REQUIRED = "required"
  private const val PROPERTIES = "properties"
  private const val DEFINITIONS = "definitions"

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

    diff.forEach { node ->
      val operation = Operation.fromRfcName(node["op"].asText())
      val path = node["path"].asText()

      if (path.matches(modifiedAnyOfRegex)) {
        modifiedAnyOfPaths.add(path.back())
        return@forEach
      } else if (path.matches(modifiedEnumRegex)) {
        modifiedEnumPaths.add(path.back())
        return@forEach
      } else if (path.matches(modifiedRequiredRegex)) {
        modifiedRequiredPaths.add(path.back())
        return@forEach
      }

      when (operation) {
        Operation.REMOVE -> removedFieldPaths.add(path)
        Operation.ADD -> addedFieldPaths.add(path)
        Operation.REPLACE -> changedFieldPaths.add(path)
        Operation.MOVE, Operation.COPY, Operation.TEST -> throw IllegalStateException("Unsupported operation: $node")
      }
    }

    modifiedAnyOfPaths.forEach { path ->
      val oldList = oldSchema.withArray<ArrayNode>(path).toList()
      val newList = newSchema.withArray<ArrayNode>(path).toList()
      val addedValues = newList - oldList
      val removedValues = oldList - newList

      if (addedValues.isNotEmpty()) {
        addedValues.forEach { addedValue -> validationResult[config.addingAnyOf].add("Added anyOf $addedValue")}
      }

      if (removedValues.isNotEmpty()) {
        removedValues.forEach { removedValue -> validationResult[config.removingAnyOf].add("Removed anyOf $removedValue") }
      }
    }

    modifiedEnumPaths.forEach { path ->
      val oldList = oldSchema.withArray<ArrayNode>(path).toList()
      val newList = newSchema.withArray<ArrayNode>(path).toList()
      val addedValues = newList - oldList
      val removedValues = oldList - newList

      if (addedValues.isNotEmpty()) {
        addedValues.forEach { addedValue -> validationResult[config.addingEnumValue].add("Added enumValue $addedValue")}
      }

      if (removedValues.isNotEmpty()) {
        removedValues.forEach { removedValue -> validationResult[config.removingEnumValue].add("Removed enumValue $removedValue") }
      }
    }

    modifiedRequiredPaths.forEach { path ->
      val oldList = oldSchema.withArray<ArrayNode>(path).toList()
      val newList = newSchema.withArray<ArrayNode>(path).toList()
      val addedValues = newList - oldList
      val removedValues = oldList - newList

      if (addedValues.isNotEmpty()) {
        addedValues.forEach { addedValue -> validationResult[config.addingRequired].add("Added required $addedValue")}
      }

      if (removedValues.isNotEmpty()) {
        removedValues.forEach { removedValue -> validationResult[config.removingRequired].add("Removed required $removedValue") }
      }
    }

    addedFieldPaths.forEach { path ->
      val fieldName = getLastSubPath(path)
      val newFieldIsRequired = newSchema.at(path.back().back()).withArray<ArrayNode>("required")
        .any { it.asText() == fieldName }
      if (newFieldIsRequired) {
        validationResult[config.addingRequiredFields].add("Added required field: $path")
      } else {
        validationResult[config.addingOptionalFields].add("Added optional field: $path")
      }
    }

    removedFieldPaths.forEach { path ->
      if (path.endsWith("minItems")) {
        return@forEach // Always allow removing minItems
      }
      val fieldName = getLastSubPath(path)
      val newFieldIsRequired = oldSchema.at(path.back().back()).withArray<ArrayNode>("required")
        .any { it.asText() == fieldName }
      if (newFieldIsRequired) {
        validationResult[config.removingRequiredFields].add("Removed required field: $path")
      } else {
        validationResult[config.removingOptionalFields].add("Removed optional field: $path")
      }
    }

    changedFieldPaths.forEach { path ->
      if (path.endsWith("minItems")) {
        val oldValue = oldSchema.at(path).asInt()
        val newValue = newSchema.at(path).asInt()
        if (newValue > oldValue) {
          validationResult[Compatibility.FORBIDDEN].add("Increased minItems: $path from $oldValue to $newValue")
        }
      } else {
        validationResult[Compatibility.FORBIDDEN].add("Changed field: $path")
      }
    }

    return validationResult
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
}
