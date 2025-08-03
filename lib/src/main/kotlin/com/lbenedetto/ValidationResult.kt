package com.lbenedetto

import com.fasterxml.jackson.databind.JsonNode

data class ValidationResult(
  /**
   * These changes were allowed by the configuration.
   */
  val info: MutableList<Any> = mutableListOf(),

  /**
   * These changes are discouraged but not forbidden by the configuration.
   */
  val warnings: MutableList<Any> = mutableListOf(),

  /**
   * These changes are not allowed by the configuration.
   */
  val errors: MutableList<Any> = mutableListOf(),
) {
  operator fun get(compatibility: Compatibility): MutableList<Any> {
    return when (compatibility) {
      Compatibility.ALLOWED -> info
      Compatibility.DISCOURAGED -> warnings
      Compatibility.FORBIDDEN -> errors
    }
  }
}
