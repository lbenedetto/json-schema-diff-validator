package io.github.lbenedetto.inspector

data class DetectedChanges(
  /**
   * Changes to field types using anyOf
   */
  val anyOf: MutableSet<AnyOfChange> = mutableSetOf(),

  /**
   * Changes to Enums
   */
  val enumValue: MutableSet<EnumValueChange> = mutableSetOf(),

  /**
   * Changes to fields
   */
  val fields: MutableSet<FieldChange> = mutableSetOf(),

  /**
   * Changes to field types
   */
  val fieldTypes: MutableSet<FieldTypeChange> = mutableSetOf(),

  /**
   * Changes to nullability requirements
   */
  val nonNullRequirement: MutableSet<NonNullRequirementChange> = mutableSetOf(),

  /**
   * Changes to the required presence of a field
   */
  val notAbsentRequirement: MutableSet<NotAbsentRequirementChange> = mutableSetOf(),

  /**
   * Changes to minimum items in an array requirement
   */
  val minItems: MutableSet<MinItemsChange> = mutableSetOf()
) {
  fun all(): Set<Change> = anyOf + enumValue + fields + fieldTypes + nonNullRequirement + notAbsentRequirement + minItems
  fun perPath(): Map<String, List<Change>> = all().groupBy { it.path }
}
