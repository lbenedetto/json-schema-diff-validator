package io.github.lbenedetto.jsonschema

data class CompoundType(val types: Set<SimpleType>) {
  fun isNullable() = types.contains(SimpleType.NULL)
  fun ignoringNull() = CompoundType(types.filter { it != SimpleType.NULL }.toSet())

  constructor(types: List<String>) : this(types.map { SimpleType.valueOf(it.uppercase()) }.toSet())

  override fun toString(): String {
    return types.sorted().joinToString(separator = "|") { it.name }
  }

  operator fun CompoundType.minus(other: CompoundType): CompoundType {
    return CompoundType(this.types - other.types)
  }
}
