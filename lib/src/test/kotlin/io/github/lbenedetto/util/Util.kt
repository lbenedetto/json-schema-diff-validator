package io.github.lbenedetto.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.zjsonpatch.JsonPatch
import io.github.lbenedetto.Compatibility
import io.github.lbenedetto.Compatibility.*
import io.github.lbenedetto.ValidationResult
import io.github.lbenedetto.Validator
import io.kotest.assertions.print.print
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
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
      allowed shouldNot beEmpty(ALLOWED)
      discouraged should beEmpty(DISCOURAGED)
      forbidden should beEmpty(FORBIDDEN)
    }
    allowed.shouldHaveNoDiff(ALLOWED, message.toSet())
  }

  fun ValidationResult.shouldDiscourage(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      allowed should beEmpty(ALLOWED)
      discouraged shouldNot beEmpty(DISCOURAGED)
      forbidden should beEmpty(FORBIDDEN)
    }
    return discouraged.shouldHaveNoDiff(DISCOURAGED, message.toSet())
  }

  fun ValidationResult.shouldForbid(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      allowed should beEmpty(ALLOWED)
      discouraged should beEmpty(DISCOURAGED)
      forbidden shouldNot beEmpty(FORBIDDEN)
    }
    forbidden.shouldHaveNoDiff(FORBIDDEN, message.toSet())
  }

  fun <T : Comparable<T>> Collection<T>.shouldHaveNoDiff(compatibility: Compatibility, expected: Set<T>) {
    val thisSet = this.toSet()
    val unexpectedValues = expected - thisSet
    val missingValues = thisSet - expected
    val errors = mutableListOf<String>()

    if (unexpectedValues.isNotEmpty()) {
      errors.add("$compatibility should not contain (but did): ${unexpectedValues.print().value}")
    }

    if (missingValues.isNotEmpty()) {
      errors.add("$compatibility should contain (but did not): ${missingValues.print().value}")
    }

    if (errors.isNotEmpty()) {
      errors.add("expected:<${expected.sortedBy { it }}> but was:<${this.sortedBy { it }}>")
      throw AssertionError(errors.joinToString(separator = "\n"))
    }
  }

  fun <T> beEmpty(compatibility: Compatibility): Matcher<Collection<T>> = object : Matcher<Collection<T>> {
    override fun test(value: Collection<T>): MatcherResult = MatcherResult(
      value.isEmpty(),
      { "$compatibility should be empty but contained $value" },
      { "$compatibility should not be empty" }
    )
  }
}
