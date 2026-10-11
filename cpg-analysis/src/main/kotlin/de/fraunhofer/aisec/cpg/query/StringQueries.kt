/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluatorConfig
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.evaluateString
import de.fraunhofer.aisec.cpg.analysis.string.mayMatch
import de.fraunhofer.aisec.cpg.analysis.string.mustMatchWithGiveUp
import de.fraunhofer.aisec.cpg.analysis.string.unknownOrigins
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.Node

/**
 * Runs the [de.fraunhofer.aisec.cpg.analysis.string.StringEvaluator] on [this] and wraps the
 * resulting [StringPattern] in a [QueryTree], so that it composes with the rest of the query
 * language (see the design doc's "Public API" section, `docs/docs/CPG/impl/string-analysis.md`).
 *
 * Assumptions recorded during evaluation are surfaced on the returned [QueryTree]: the evaluator
 * records every over-approximation as an [de.fraunhofer.aisec.cpg.assumptions.Assumption] on some
 * [Node] rather than on the (assumption-free) [StringPattern] value itself - either on the node
 * being evaluated at the time (e.g. `PythonStringOperationHandler`'s `replace` over-approximation,
 * recorded on the `call`) or on [this] node (budget exhaustion, recorded on the root node passed to
 * `evaluate()`, see `StringEvaluator.budgetExceeded`). Both cases are reachable from the result
 * pattern: the former because that node becomes a [StringPattern.Unknown.origin] in the result (see
 * [unknownOrigins]), the latter because it is always [this].
 */
fun Node.stringValue(
    config: StringEvaluatorConfig = StringEvaluatorConfig()
): QueryTree<StringPattern> {
    val pattern = evaluateString(config)
    val assumptions = (pattern.unknownOrigins + this).flatMapTo(mutableSetOf()) { it.assumptions }
    return QueryTree(
        value = pattern,
        stringRepresentation = "the string value of `${this.compactToString()}` is `$pattern`",
        node = this,
        assumptions = assumptions,
        operator = GenericQueryOperators.EVALUATE,
    )
}

/**
 * Whether every string [this] may evaluate to is matched by [regex]. Delegates to
 * [StringPattern.mustMatch] on the pattern computed by [stringValue].
 *
 * [StringPattern.mustMatch] is conservative: it returns `false` both when it *proves* a
 * counter-example exists and when it merely gives up because the pattern's language could not be
 * enumerated within budget (see its KDoc, D3 in the design doc). Since this function has a [Node]
 * context, the give-up case is distinguished here and recorded as a
 * [AssumptionType.SoundnessAssumption] on the returned [QueryTree] - this is what resolves the
 * `mustMatch`/`QueryHelpers.kt` "record Assumption when reachable from a node context" TODO from
 * Phase 2.
 */
fun Node.stringMustMatch(
    regex: Regex,
    config: StringEvaluatorConfig = StringEvaluatorConfig(),
): QueryTree<Boolean> {
    val patternTree = stringValue(config)
    val pattern = patternTree.value
    val (result, gaveUp) = pattern.mustMatchWithGiveUp(regex)

    val queryTree =
        QueryTree(
            value = result,
            children = listOf(patternTree),
            stringRepresentation =
                "the string value of `${this.compactToString()}` (`$pattern`) must match `$regex`",
            node = this,
            assumptions = patternTree.assumptions.toMutableSet(),
            operator = GenericQueryOperators.EVALUATE,
        )
    if (gaveUp) {
        queryTree.assume(
            AssumptionType.SoundnessAssumption,
            "We assume that `$this` does NOT always match `$regex`, but this is only a " +
                "conservative default: the computed pattern `$pattern` has a language we could " +
                "not fully enumerate within budget, so mustMatch cannot prove or disprove the " +
                "match. To verify this assumption, we need to check whether the pattern can be " +
                "refined or enumerated with a larger budget.",
            scope = this,
        )
    }
    return queryTree
}

/**
 * Whether the language of [this] may contain a string matched by [regex]. Delegates to
 * [StringPattern.mayMatch] on the pattern computed by [stringValue].
 */
fun Node.stringMayMatch(
    regex: Regex,
    config: StringEvaluatorConfig = StringEvaluatorConfig(),
): QueryTree<Boolean> {
    val patternTree = stringValue(config)
    val pattern = patternTree.value
    return QueryTree(
        value = pattern.mayMatch(regex),
        children = listOf(patternTree),
        stringRepresentation =
            "the string value of `${this.compactToString()}` (`$pattern`) may match `$regex`",
        node = this,
        assumptions = patternTree.assumptions.toMutableSet(),
        operator = GenericQueryOperators.EVALUATE,
    )
}
