package io.github.lbenedetto.validator

data class ValidationResult(
  /**
   * These changes were allowed by the configuration.
   */
  val safe: MutableList<String> = mutableListOf(),

  /**
   * These changes are discouraged but not forbidden by the configuration.
   */
  val risky: MutableList<String> = mutableListOf(),

  /**
   * These changes are not allowed by the configuration.
   */
  val fatal: MutableList<String> = mutableListOf(),
) {
  operator fun get(compatibility: Risk): MutableList<String> {
    return when (compatibility) {
      Risk.SAFE -> safe
      Risk.RISKY -> risky
      Risk.FATAL -> fatal
    }
  }
}
