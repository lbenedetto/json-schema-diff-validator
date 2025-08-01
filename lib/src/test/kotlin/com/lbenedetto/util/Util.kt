package com.lbenedetto.util

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.zjsonpatch.JsonPatch
import com.lbenedetto.Validator
import org.intellij.lang.annotations.Language
import java.nio.file.Paths

object Util {
  fun ObjectNode.withPatches(@Language("JSON") vararg patches: String): ObjectNode {
    val patch = Validator.objectMapper.readTree(
      """
      [
        ${patches.joinToString(separator = ",") { it.trimIndent() }}
      ]
    """.trimIndent()
    )
    return JsonPatch.apply(patch, this.deepCopy()) as ObjectNode
  }

  fun readSchema(schemaPath: String): ObjectNode {
    return Validator.objectMapper.readTree(Paths.get("src/test/resources/$schemaPath").toFile()) as ObjectNode
  }
}
