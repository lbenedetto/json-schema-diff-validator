package io.github.lbenedetto.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.zjsonpatch.JsonPatch
import io.github.lbenedetto.ValidationResult
import io.github.lbenedetto.Validator
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.should
import org.intellij.lang.annotations.Language
import java.nio.file.Paths

object Util {
  fun toDiffNode(@Language("JSON") vararg patches: String): JsonNode {
    return Validator.objectMapper.readTree(
      """
      [
        ${patches.joinToString(separator = ",") { it.trimIndent() }}
      ]
    """.trimIndent()
    )
  }

  fun ObjectNode.withPatches(@Language("JSON") vararg patches: String): ObjectNode {
    val patch = toDiffNode(*patches)
    return JsonPatch.apply(patch, this.deepCopy()) as ObjectNode
  }

  fun readSchema(schemaPath: String): ObjectNode {
    return Validator.objectMapper.readTree(Paths.get("src/test/resources/$schemaPath").toFile()) as ObjectNode
  }

  fun ValidationResult.shouldAllow(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      discouraged should beEmpty()
      forbidden should beEmpty()
    }
    allowed should containExactlyInAnyOrder(*message)
  }

  fun ValidationResult.shouldDiscourage(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      allowed should beEmpty()
      forbidden should beEmpty()
    }
    return discouraged should containExactlyInAnyOrder(*message)
  }

  fun ValidationResult.shouldForbid(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      allowed should beEmpty()
      discouraged should beEmpty()
    }
    return forbidden should containExactlyInAnyOrder(*message)
  }
}
