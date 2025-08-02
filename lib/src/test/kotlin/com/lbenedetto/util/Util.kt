package com.lbenedetto.util

import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.zjsonpatch.JsonPatch
import com.lbenedetto.ValidationResult
import com.lbenedetto.Validator
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.haveLength
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

  fun haveErrors() = Matcher<ValidationResult> { value ->
    MatcherResult(
      value.errors.isNotEmpty(),
      { "result should have errors, but did not. Full result: $value" },
      { "result should not have errors, but did. Full result: $value" },
    )
  }

  fun ValidationResult.shouldHaveErrors(): ValidationResult {
    this should haveErrors()
    return this
  }

  fun ValidationResult.shouldNotHaveErrors(): ValidationResult {
    this shouldNot haveErrors()
    return this
  }
}
