package io.github.lbenedetto.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.zjsonpatch.JsonPatch
import io.github.lbenedetto.validator.Risk
import io.github.lbenedetto.validator.Risk.*
import io.github.lbenedetto.inspector.Inspector
import io.github.lbenedetto.validator.ValidationResult
import io.kotest.assertions.print.print
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.intellij.lang.annotations.Language
import java.nio.file.Paths

object Util {
  fun toDiffNode(@Language("JSON") vararg patches: String): JsonNode {
    return Inspector.objectMapper.readTree(
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
    return Inspector.objectMapper.readTree(Paths.get("src/test/resources/$schemaPath").toFile()) as ObjectNode
  }

  fun ValidationResult.shouldAllow(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      safe shouldNot beEmpty(SAFE)
      risky should beEmpty(RISKY)
      fatal should beEmpty(FATAL)
    }
    safe.shouldHaveNoDiff(SAFE, message.toSet())
  }

  fun ValidationResult.shouldDiscourage(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      safe should beEmpty(SAFE)
      risky shouldNot beEmpty(RISKY)
      fatal should beEmpty(FATAL)
    }
    return risky.shouldHaveNoDiff(RISKY, message.toSet())
  }

  fun ValidationResult.shouldForbid(vararg message: String, enforceOthersEmpty: Boolean = true) {
    if (enforceOthersEmpty) {
      safe should beEmpty(SAFE)
      risky should beEmpty(RISKY)
      fatal shouldNot beEmpty(FATAL)
    }
    fatal.shouldHaveNoDiff(FATAL, message.toSet())
  }

  fun <T : Comparable<T>> Collection<T>.shouldHaveNoDiff(risk: Risk, expected: Set<T>) {
    val thisSet = this.toSet()
    val unexpectedValues = expected - thisSet
    val missingValues = thisSet - expected
    val errors = mutableListOf<String>()

    if (unexpectedValues.isNotEmpty()) {
      errors.add("$risk should not contain (but did): ${unexpectedValues.print().value}")
    }

    if (missingValues.isNotEmpty()) {
      errors.add("$risk should contain (but did not): ${missingValues.print().value}")
    }

    if (errors.isNotEmpty()) {
      errors.add("expected:<${expected.sortedBy { it }}> but was:<${this.sortedBy { it }}>")
      throw AssertionError(errors.joinToString(separator = "\n"))
    }
  }

  fun <T> beEmpty(risk: Risk): Matcher<Collection<T>> = object : Matcher<Collection<T>> {
    override fun test(value: Collection<T>): MatcherResult = MatcherResult(
      value.isEmpty(),
      { "$risk should be empty but contained $value" },
      { "$risk should not be empty" }
    )
  }
}
