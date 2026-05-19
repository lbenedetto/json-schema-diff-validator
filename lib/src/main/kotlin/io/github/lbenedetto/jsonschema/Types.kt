package io.github.lbenedetto.jsonschema

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.StringNode

interface Type {
  fun isNullable(): Boolean
}

enum class PrimitiveType : Type {
  ARRAY, BOOLEAN, INTEGER, NULL, NUMBER, OBJECT, STRING;

  override fun isNullable() = this == NULL
}

data class ReferenceType(val reference: String, val type: Type) : Type {
  override fun isNullable() = type.isNullable()
  override fun toString() = "$reference($type)"
}

data class AnyOfType(val types: Set<Type>) : Type {
  override fun isNullable() = types.any { it.isNullable() }
  override fun toString() = types.toString()
  fun ignoringNull() = AnyOfType(types.filter { !it.isNullable() }.toSet())

  companion object {
    fun from(types: Set<Type>): AnyOfType {
      return AnyOfType(types.flatMap { if (it is AnyOfType) it.types else setOf(it) }.toSet())
    }
  }
}

fun JsonNode.resolveType(rootNode: JsonNode): AnyOfType {
  if (has("type")) {
    return resolveFromTypeNode(this["type"])
  }
  if (has("anyOf")) {
    return resolveFromAnyOfNode(this["anyOf"], rootNode)
  }
  if (has($$"$ref")) {
    return resolveFromRefNode(this[$$"$ref"], rootNode)
  }
  throw IllegalStateException("Unable to resolve type for node: $this")
}

private fun resolveFromAnyOfNode(node: JsonNode, rootNode: JsonNode): AnyOfType {
  return AnyOfType.from(node.values().map { it.resolveType(rootNode) }.toSet())
}

private fun resolveFromTypeNode(node: JsonNode): AnyOfType {
  return when (node) {
    is ArrayNode -> AnyOfType(node.values().map { it.asPrimitiveType() }.toSet())
    is StringNode -> AnyOfType(setOf(node.asPrimitiveType()))
    else -> throw IllegalStateException("Unexpected type node: $node")
  }
}

private fun resolveFromRefNode(refNode: JsonNode, rootNode: JsonNode): AnyOfType {
  val ref = refNode.asString().substringAfter("#")
  return AnyOfType(setOf(ReferenceType(ref, rootNode.at(ref).resolveType(rootNode))))
}

private fun JsonNode.asPrimitiveType(): PrimitiveType = PrimitiveType.valueOf(stringValue().uppercase())
