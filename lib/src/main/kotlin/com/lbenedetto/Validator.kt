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
    allowNewOneOf: Boolean = false,
    allowNewEnumValue: Boolean = false,
    allowReorder: Boolean = false,
    allowRemovingOptionalFields: Boolean = true
  ) {
    val oldSchema = objectMapper.readTree(Paths.get(oldSchemaPath).toFile())
    val newSchema = objectMapper.readTree(Paths.get(newSchemaPath).toFile())

    validate(oldSchema, newSchema, allowNewOneOf, allowNewEnumValue, allowReorder, allowRemovingOptionalFields)
  }

  /**
   * Validates that the new schema is backward compatible with the old schema.
   * Throws an IllegalStateException if the schemas are not compatible.
   */
  fun validate(
    oldSchema: JsonNode,
    newSchema: JsonNode,
    allowNewOneOf: Boolean = false,
    allowNewEnumValue: Boolean = false,
    allowReorder: Boolean = false,
    allowRemovingOptionalFields: Boolean = true
  ) {
    val diff = JsonDiff.asJson(oldSchema, newSchema)
    val incompatibleChanges = mutableListOf<JsonNode>()

    // For tracking replaced and inserted items when allowReorder is true
    val removed = mutableListOf<Pair<String, JsonNode>>()
    val inserted = mutableListOf<String>()

    for (i in 0 until diff.size()) {
      val node = diff[i]
      val operation = Operation.fromRfcName(node["op"].asText())
      val path = node["path"].asText()

      val isMinItems = path.endsWith("minItems")

      when (operation) {
        Operation.MOVE, Operation.REMOVE -> {
          if (getSecondLastSubPath(path) == REQUIRED || isMinItems) {
            // Skip required field removals and minItems changes
            continue
          }

          if (allowRemovingOptionalFields && !isFieldRequired(oldSchema, path)) {
            continue
          }

          incompatibleChanges.add(node)
        }

        Operation.REPLACE -> {
          val oldValue = getJsonNodeAtPath(oldSchema, path)
          if (isMinItems && oldValue.isInt && oldValue.asInt() > node["value"].asInt()) {
            // Skip decreasing minItems
            continue
          } else {
            if (!allowReorder) {
              incompatibleChanges.add(node)
            } else {
              // Track for reordering check
              val oldValueText = if (oldValue.isTextual) oldValue.asText() else oldValue.toString()
              removed.add(Pair(oldValueText, node))
              val newValueText = if (node["value"].isTextual) node["value"].asText() else node["value"].toString()
              inserted.add(newValueText)
            }
          }
        }

        Operation.ADD -> {
          val isNewAnyOfItem = path.matches(Regex(".*/anyOf/\\d+$"))
          val isNewEnumValue = path.matches(Regex(".*/enum/\\d+$"))
          val pathTwoLastLevels = getSecondLastSubPath(path)
          val lastSubPath = getLastSubPath(path)

          // Allow adding minimum field to properties when allowReorder is true
          if (lastSubPath == "minimum" && allowReorder) {
            continue
          }

          if (pathTwoLastLevels != PROPERTIES && pathTwoLastLevels != DEFINITIONS) {
            if (isNewAnyOfItem && allowReorder) {
              val refValue = node["value"]["\$ref"].asText() ?: node["value"].toString()
              inserted.add(refValue)
            } else if ((isNewAnyOfItem && allowNewOneOf) || (isNewEnumValue && allowNewEnumValue)) {
              // Skip allowed additions
              continue
            } else {
              incompatibleChanges.add(node)
            }
          }

          if (pathTwoLastLevels == REQUIRED) {
            incompatibleChanges.add(node)
          }
        }
        Operation.COPY, Operation.TEST -> println("Unsupported operation: $node")
      }
    }

    // When reordering is allowed, check that any removed item is also inserted somewhere else
    if (allowReorder) {
      for (pair in removed) {
        if (!inserted.contains(pair.first)) {
          incompatibleChanges.add(pair.second)
        }
      }
    }

    // Assert that there are no incompatible changes
    if (incompatibleChanges.isNotEmpty()) {
      throw IllegalStateException("The schema is not backward compatible. Difference include breaking change = ${incompatibleChanges}")
    }
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
