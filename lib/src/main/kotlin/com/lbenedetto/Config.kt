package com.lbenedetto

data class Config(
  var newAnyOf: Compatibility = Compatibility.FORBIDDEN,
  var newEnumValue: Compatibility = Compatibility.FORBIDDEN,
  var anyOfReordering: Compatibility = Compatibility.FORBIDDEN,
  var removingOptionalFields: Compatibility = Compatibility.ALLOWED,
  var makingFieldsRequired: Compatibility = Compatibility.FORBIDDEN
) {
  fun newAnyOf(value: Compatibility) = apply { this.newAnyOf = value }
  fun newEnumValue(value: Compatibility) = apply { this.newEnumValue = value }
  fun anyOfReordering(value: Compatibility) = apply { this.anyOfReordering = value }
  fun removingOptionalFields(value: Compatibility) = apply { this.removingOptionalFields = value }
  fun makingFieldsOptional(value: Compatibility) = apply { this.makingFieldsRequired = value }

  companion object {
    @JvmStatic
    fun defaultConfig() = Config()
  }
}
