package com.lbenedetto

import com.fasterxml.jackson.databind.JsonNode

data class ValidationResult(
  /**
   * These changes were allowed by the configuration.
   */
  val info: MutableList<String> = mutableListOf(),

  /**
   * These changes are discouraged but not forbidden by the configuration.
   */
  val warnings: MutableList<String> = mutableListOf(),

  /**
   * These changes are not allowed by the configuration.
   */
  val errors: MutableList<String> = mutableListOf(),
) {
  operator fun get(compatibility: Compatibility): MutableList<String> {
    return when (compatibility) {
      Compatibility.ALLOWED -> info
      Compatibility.DISCOURAGED -> warnings
      Compatibility.FORBIDDEN -> errors
    }
  }
}
