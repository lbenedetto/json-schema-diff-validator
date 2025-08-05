package io.github.lbenedetto.jsonschema

internal data class AnyOfSimpleType(val types: Set<SimpleType>) {
  fun isNullable() = types.contains(SimpleType.NULL)
  fun ignoringNull() = types.filter { it != SimpleType.NULL }.toSet()

  constructor(types: List<String>) : this(types.map { SimpleType.valueOf(it.uppercase()) }.toSet())

  override fun toString(): String {
    return "'${types.sorted().joinToString(separator = "|") { it.name }}'"
  }
}
