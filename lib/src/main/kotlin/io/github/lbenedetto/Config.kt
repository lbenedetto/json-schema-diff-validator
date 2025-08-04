package io.github.lbenedetto

data class Config(
  /**
   * If a field can be anyOf a set of types, is it safe to add a new type to the set?
   * If deserializing from a cache, this should be safe.
   */
  var addingAnyOf: Compatibility = Compatibility.ALLOWED,
  /**
   * If a field can be anyOf a set of types, is it safe to add a new type to the set?
   * If deserializing from a cache, this might not be safe if that type still exists in the cache.
   */
  var removingAnyOf: Compatibility = Compatibility.FORBIDDEN,

  /**
   * Is it safe to add a new value to an enum?
   * If deserializing from a cache, this should be safe.
   */
  var addingEnumValue: Compatibility = Compatibility.ALLOWED,

  /**
   * Is it safe to remove a value from an enum?
   * If deserializing from a cache, this might not be safe if that value still exists in the cache.
   */
  var removingEnumValue: Compatibility = Compatibility.FORBIDDEN,

  /**
   * Is it safe to add a new optional field?
   * If deserializing from a cache, this should be safe if the missing field deserializes to null.
   */
  var addingOptionalFields: Compatibility = Compatibility.ALLOWED,
  /**
   * Is it safe to remove an optional field?
   * If deserializing from a cache, this should be safe
   */
  var removingOptionalFields: Compatibility = Compatibility.ALLOWED,

  /**
   * Is it safe to add a new required field?
   * If deserializing from a cache, this is definitely dangerous, as the cache does not have the new field.
   */
  var addingRequiredFields: Compatibility = Compatibility.FORBIDDEN,
  /**
   * Is it safe to remove a required field?
   * The new code would be able to read the old cache values, but the old code would not be able to read the new cache values.
   * Could pose challenges for a rollback.
   */
  var removingRequiredFields: Compatibility = Compatibility.FORBIDDEN,

  /**
   * Is it safe to make an existing field required?
   * If deserializing from a cache, this is probably dangerous, as the cache might have null values for the field.
   */
  var addingRequired: Compatibility = Compatibility.FORBIDDEN,
  /**
   * Is it safe to make an existing field optional?
   * This should be safe for the new code, but could pose challenges for a rollback.
   */
  var removingRequired: Compatibility = Compatibility.ALLOWED
) {
  fun addingAnyOf(value: Compatibility) = apply { this.addingAnyOf = value }
  fun removingAnyOf(value: Compatibility) = apply { this.removingAnyOf = value }

  fun addingEnumValue(value: Compatibility) = apply { this.addingEnumValue = value }
  fun removingEnumValue(value: Compatibility) = apply { this.removingEnumValue = value }

  fun addingOptionalFields(value: Compatibility) = apply { this.addingOptionalFields = value }
  fun removingOptionalFields(value: Compatibility) = apply { this.removingOptionalFields = value }

  fun addingRequiredFields(value: Compatibility) = apply { this.addingRequiredFields = value }
  fun removingRequiredFields(value: Compatibility) = apply { this.removingRequiredFields = value }

  fun addingRequired(value: Compatibility) = apply { this.addingRequired = value }
  fun removingRequired(value: Compatibility) = apply { this.removingRequired = value }

  companion object {
    @JvmStatic
    fun defaultConfig() = Config()
  }
}
