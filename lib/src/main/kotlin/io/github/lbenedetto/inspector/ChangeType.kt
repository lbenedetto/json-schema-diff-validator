package io.github.lbenedetto.inspector

enum class ChangeType {
  ADDED,
  REMOVED,

  /**
   * Only applicable for [MinItemsChange]
   **/
  INCREASED,

  /**
   * Only applicable for [MinItemsChange]
   **/
  DECREASED
}
