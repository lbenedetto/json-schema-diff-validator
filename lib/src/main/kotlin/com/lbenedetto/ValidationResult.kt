package com.lbenedetto

import com.fasterxml.jackson.databind.JsonNode

data class ValidationResult(
  /**
   * These changes were allowed by the configuration.
   */
  val info: MutableList<JsonNode> = mutableListOf(),

  /**
   * These changes are discouraged but not forbidden by the configuration.
   */
  val warnings: MutableList<JsonNode> = mutableListOf(),

  /**
   * These changes are not allowed by the configuration.
   */
  val errors: MutableList<JsonNode> = mutableListOf(),
) {
  operator fun get(compatibility: Compatibility): MutableList<JsonNode> {
    return when (compatibility) {
      Compatibility.ALLOWED -> info
      Compatibility.DISCOURAGED -> warnings
      Compatibility.FORBIDDEN -> errors
    }
  }
}
