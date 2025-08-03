package com.lbenedetto.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.flipkart.zjsonpatch.JsonPatch
import com.lbenedetto.Compatibility
import com.lbenedetto.ValidationResult
import com.lbenedetto.Validator
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.property.Exhaustive
import io.kotest.property.exhaustive.exhaustive
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

  infix fun ValidationResult.shouldHaveDiffCorrectlySortedTo(target: Compatibility): ValidationResult {
    this should haveDiffCorrectlySortedTo(target)
    return this
  }

  infix fun haveDiffCorrectlySortedTo(target: Compatibility) = Matcher<ValidationResult> { value ->
    // create a matcher which ensures that value[target] is not empty and value[anythingElse] is empty
    val othersAreEmpty = Compatibility.entries.filter { it != target }.all { value[it].isEmpty() }
    val targetIsEmpty = value[target].isEmpty()
    return@Matcher MatcherResult(
      othersAreEmpty && !targetIsEmpty,
      {
        val messageBuilder = StringBuilder()
        if (targetIsEmpty) {
          messageBuilder.appendLine("$target should not be empty, but was. ")
        }
        if (!othersAreEmpty) {
          messageBuilder.appendLine("Others should be empty, but were not. ")
        }
        messageBuilder.appendLine("Full result: $value")
        messageBuilder.toString()
      },
      { throw IllegalStateException("Inverting this check does not make sense") },
    )
  }


  /**
   * Copied from [io.kotest.property.exhaustive.times], but without the requirement that B extends from A.
   */
  operator fun <A, B> Exhaustive<A>.times(other: Exhaustive<B>): Exhaustive<Pair<A, B>> {
    val values = this.values.flatMap { a ->
      other.values.map { b ->
        Pair(a, b)
      }
    }
    return values.exhaustive()
  }
}
