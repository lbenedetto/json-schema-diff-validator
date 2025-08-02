package com.lbenedetto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.flipkart.zjsonpatch.JsonDiff
import com.flipkart.zjsonpatch.Operation
import java.nio.file.Paths

object Validator {
  val objectMapper = ObjectMapper()

  // Path constants
  private const val REQUIRED = "required"
  private const val PROPERTIES = "properties"
  private const val DEFINITIONS = "definitions"

  /**
   * Validates that the new schema is backward compatible with the old schema.
   * Throws an IllegalStateException if the schemas are not compatible.
   */
  fun validate(
    oldSchemaPath: String,
    newSchemaPath: String,
    config: Config = Config()
  ) : ValidationResult {
    val oldSchema = objectMapper.readTree(Paths.get(oldSchemaPath).toFile())
    val newSchema = objectMapper.readTree(Paths.get(newSchemaPath).toFile())

    return validate(oldSchema, newSchema, config)
  }

  /**
   * Validates that the new schema is backward compatible with the old schema.
   * Throws an IllegalStateException if the schemas are not compatible.
   */
  fun validate(
    oldSchema: JsonNode,
    newSchema: JsonNode,
    config: Config = Config()
  ): ValidationResult {
    val diff = JsonDiff.asJson(oldSchema, newSchema)

    val validationResult = ValidationResult()
    // For tracking replaced and inserted items when allowReorder is true
    val removed = mutableListOf<Pair<String, JsonNode>>()
    val inserted = mutableListOf<String>()

    diff.forEach { node ->
      val operation = Operation.fromRfcName(node["op"].asText())
      val path = node["path"].asText()

      val isMinItems = path.endsWith("minItems")

      when (operation) {
        Operation.MOVE, Operation.REMOVE -> {
          if (getSecondLastSubPath(path) == REQUIRED || isMinItems) {
            // Skip required field removals and minItems changes
            return@forEach
          }

          if (!isFieldRequired(oldSchema, path)) {
            validationResult[config.removingOptionalFields].add(node)
            return@forEach
          }

          validationResult[Compatibility.FORBIDDEN].add(node)
        }

        Operation.REPLACE -> {
          val oldValue = getJsonNodeAtPath(oldSchema, path)
          if (isMinItems && oldValue.isInt && oldValue.asInt() > node["value"].asInt()) {
            // Skip decreasing minItems
            return@forEach
          } else if (config.anyOfReordering == Compatibility.ALLOWED) {
            // Track for reordering check
            val oldValueText = if (oldValue.isTextual) oldValue.asText() else oldValue.toString()
            removed.add(Pair(oldValueText, node))
            val newValueText = if (node["value"].isTextual) node["value"].asText() else node["value"].toString()
            inserted.add(newValueText)
          } else {
            validationResult[config.anyOfReordering].add(node)
          }
        }

        Operation.ADD -> {
          val isNewAnyOfItem = path.matches(Regex(".*/anyOf/\\d+$"))
          val isNewEnumValue = path.matches(Regex(".*/enum/\\d+$"))
          val pathTwoLastLevels = getSecondLastSubPath(path)
          val lastSubPath = getLastSubPath(path)

          // Allow adding minimum field to properties when allowReorder is true
          if (lastSubPath == "minimum") {
            validationResult[config.anyOfReordering].add(node)
            return@forEach
          }

          if (pathTwoLastLevels != PROPERTIES && pathTwoLastLevels != DEFINITIONS) {
            if (isNewAnyOfItem && config.anyOfReordering == Compatibility.ALLOWED) {
              val refValue = node["value"]["\$ref"].asText() ?: node["value"].toString()
              inserted.add(refValue)
            } else if (isNewAnyOfItem) {
              validationResult[config.newOneOf].add(node)
              return@forEach
            } else if(isNewEnumValue) {
              validationResult[config.newEnumValue].add(node)
              return@forEach
            } else {
              validationResult[Compatibility.FORBIDDEN].add(node)
              return@forEach
            }
          }

          if (pathTwoLastLevels == REQUIRED) {
            validationResult[Compatibility.FORBIDDEN].add(node)
            return@forEach
          }
        }
        Operation.COPY, Operation.TEST -> println("Unsupported operation: $node")
      }
    }

    // When reordering is allowed, check that any removed item is also inserted somewhere else
    if (config.anyOfReordering != Compatibility.FORBIDDEN) {
      removed.filter { !inserted.contains(it.first) }
        .forEach { validationResult[Compatibility.FORBIDDEN].add(it.second) }
    }

    return validationResult
  }

  /**
   * Gets the second last segment of a JSON path.
   */
  private fun getSecondLastSubPath(path: String): String {
    val parts = path.split("/")
    return if (parts.size >= 2) parts[parts.size - 2] else ""
  }

  /**
   * Gets the last segment of a JSON path.
   */
  private fun getLastSubPath(path: String): String {
    val parts = path.split("/")
    return if (parts.isNotEmpty()) parts.last() else ""
  }

  /**
   * Gets the JSON node at the specified path.
   */
  private fun getJsonNodeAtPath(root: JsonNode, path: String): JsonNode {
    val parts = path.split("/").filter { it.isNotEmpty() }
    var current = root

    for (part in parts) {
      current = if (part.toIntOrNull() != null) {
        current[part.toInt()]
      } else {
        current[part]
      }
    }

    return current
  }

  private fun isFieldRequired(schema: JsonNode, fieldPath: String): Boolean {
    if (getJsonNodeAtPath(schema, fieldPath.back()).isArray) {
      return true
    }
    val fieldName = getLastSubPath(fieldPath)
    val requiredArray = getJsonNodeAtPath(schema, fieldPath.back().back())[REQUIRED]
    return requiredArray.any { it.asText() == fieldName }
  }

  private fun String.back(): String {
    return substringBeforeLast("/")
  }
}
