package io.github.lbenedetto.validator

data class Config(
  var anyOf: AnyOf = AnyOf(),
  var enumValue: EnumValue = EnumValue(),
  var nullableField: NullableField = NullableField(),
  var nonNullField: NonNullField = NonNullField(),
  var optionalField: OptionalField = OptionalField(),
  var requiredField: RequiredField = RequiredField(),
  var nonNullRequirement: NonNullRequirement = NonNullRequirement(),
  var notAbsentRequirement: NotAbsentRequirement = NotAbsentRequirement(),
  var minValueRequirement: MinValueRequirement = MinValueRequirement()
) {
  interface AddingOrRemovingConfig {
    var adding: Risk
    var removing: Risk
  }
  data class AnyOf(override var adding: Risk = Risk.SAFE, override var removing: Risk = Risk.FATAL) : AddingOrRemovingConfig
  data class EnumValue(override var adding: Risk = Risk.SAFE, override var removing: Risk = Risk.FATAL) : AddingOrRemovingConfig
  data class NullableField(override var adding: Risk = Risk.SAFE, override var removing: Risk = Risk.SAFE) : AddingOrRemovingConfig
  data class NonNullField(override var adding: Risk = Risk.FATAL, override var removing: Risk = Risk.FATAL) : AddingOrRemovingConfig
  data class OptionalField(override var adding: Risk = Risk.SAFE, override var removing: Risk = Risk.SAFE) : AddingOrRemovingConfig
  data class RequiredField(override var adding: Risk = Risk.FATAL, override var removing: Risk = Risk.FATAL) : AddingOrRemovingConfig
  data class NonNullRequirement(override var adding: Risk = Risk.FATAL, override var removing: Risk = Risk.SAFE) : AddingOrRemovingConfig
  data class NotAbsentRequirement(override var adding: Risk = Risk.FATAL, override var removing: Risk = Risk.SAFE) : AddingOrRemovingConfig
  data class MinValueRequirement(override var adding: Risk = Risk.FATAL, override var removing: Risk = Risk.SAFE) : AddingOrRemovingConfig

  companion object {
    @JvmStatic
    fun defaultConfig() = Config()
  }
}
