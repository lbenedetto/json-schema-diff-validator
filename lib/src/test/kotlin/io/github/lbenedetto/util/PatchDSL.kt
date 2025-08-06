package io.github.lbenedetto.util

import com.fasterxml.jackson.databind.JsonNode
import io.github.lbenedetto.inspector.Inspector
import org.intellij.lang.annotations.Language

object PatchDSL {
  @Language("JSON")
  fun add(path: String, @Language("JSON") value: String): String {
    return operation("add", path, value)
  }

  @Language("JSON")
  fun replace(path: String, @Language("JSON") value: String): String {
    return operation("replace", path, value)
  }

  @Language("JSON")
  fun remove(path: String): String {
    return """
    {
      "op": "remove",
      "path": "$path"
    }
  """.trimIndent()
  }

  @Language("JSON")
  fun operation(op: String, path: String, value: String): String {
    return """
    {
      "op": "$op",
      "path": "$path",
      "value": $value
    }
  """.trimIndent()
  }

  fun jsonString(value: String): String {
    return "\"$value\""
  }

  fun jsonObject(vararg fields: Pair<String, String>): String {
    val jsonFields = fields.joinToString(", ") { "\"${it.first}\": ${it.second}" }
    return "{ $jsonFields }"
  }

  fun node(@Language("JSON") node: String) : JsonNode {
    return Inspector.objectMapper.readTree(node)
  }
}
