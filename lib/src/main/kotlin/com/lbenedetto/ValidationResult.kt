package com.lbenedetto

data class ValidationResult(
  /**
   * These changes were allowed by the configuration.
   */
  val allowed: MutableList<String> = mutableListOf(),

  /**
   * These changes are discouraged but not forbidden by the configuration.
   */
  val discouraged: MutableList<String> = mutableListOf(),

  /**
   * These changes are not allowed by the configuration.
   */
  val forbidden: MutableList<String> = mutableListOf(),
) {
  operator fun get(compatibility: Compatibility): MutableList<String> {
    return when (compatibility) {
      Compatibility.ALLOWED -> allowed
      Compatibility.DISCOURAGED -> discouraged
      Compatibility.FORBIDDEN -> forbidden
    }
  }
}
