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
package de.fraunhofer.aisec.cpg.analysis.string.python

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.string.CharSet
import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluator
import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluatorConfig
import de.fraunhofer.aisec.cpg.analysis.string.StringOperationHandler
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.asConstantOrNull
import de.fraunhofer.aisec.cpg.analysis.string.charSetOf
import de.fraunhofer.aisec.cpg.analysis.string.charsOf
import de.fraunhofer.aisec.cpg.analysis.string.concat
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.analysis.string.constantPrefix
import de.fraunhofer.aisec.cpg.analysis.string.lengthOf
import de.fraunhofer.aisec.cpg.analysis.string.star
import de.fraunhofer.aisec.cpg.analysis.string.union
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.argumentByNameOrPosition
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName

/**
 * Models a subset of Python's string-relevant standard library operations (Phase 3 of the design
 * doc). Registered by callers who know they are analyzing Python code, e.g.
 * `StringEvaluator(operationHandlers = listOf(PythonStringOperationHandler()))`.
 *
 * **Call matching.** This handler deliberately does *not* need the `cpg-language-python` frontend
 * on the classpath: `cpg-analysis` (`api(projects.cpgCore)`) can already see
 * [de.fraunhofer.aisec.cpg.passes.reconstructedImportName], a language-agnostic computation defined
 * in `cpg-core`. Plain, module-qualified calls like `os.path.join(...)` are matched via
 * `call.reconstructedImportName`: by the time these run through `SymbolResolver`,
 * `ResolveMemberAmbiguityPass` (a Python-specific pass that lives in `cpg-language-python`, not
 * needed here as a *dependency*, only as the thing that produces this shape at runtime) has already
 * rewritten the `os.path.join` member-access chain into a single [Call] whose callee is a plain,
 * fully-qualified [de.fraunhofer.aisec.cpg.graph.expressions.Reference] - at that point `Call` is
 * not itself [de.fraunhofer.aisec.cpg.graph.HasBase], so `reconstructedImportName` is simply
 * `call.name`, which already *is* `"os.path.join"`. Instance-bound string methods
 * (`"...".format(...)`, `sep.join(...)`, `s.replace(...)`, ...) are never rewritten this way (there
 * is no import to resolve): they stay [MemberCall]s whose base is the receiver expression (often a
 * [de.fraunhofer.aisec.cpg.graph.expressions.Literal] or some other non-`HasBase` expression, whose
 * own name carries no useful information) - those are instead matched on `call.name.localName`,
 * i.e. the method name alone. This is best-effort (it would also match a user-defined
 * `.format()`/`.join()` on an unrelated type), which is acceptable for an opt-in, Python-specific
 * handler.
 *
 * **Not implemented in this pass** (see the design doc's Phase 3 list): `encode`/`decode`, base64,
 * and slicing (lower priority / language-agnostic per the design doc).
 */
class PythonStringOperationHandler : StringOperationHandler {
    override fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        return when {
            call.reconstructedImportName.toString() == "os.path.join" ->
                handleOsPathJoin(call, evaluate)
            call is MemberCall && call.name.localName == "format" -> handleFormat(call, evaluate)
            call is MemberCall && call.name.localName == "join" -> handleJoin(call, evaluate)
            call is MemberCall && call.name.localName == "replace" -> handleReplace(call, evaluate)
            call is MemberCall && call.name.localName in STRIP_METHODS ->
                handleStrip(call, evaluate)
            call is MemberCall && call.name.localName in CASE_METHODS -> handleCase(call, evaluate)
            else -> null
        }
    }

    /**
     * `"%s and %s" % (a, b)`, i.e. a [BinaryOperator] with operator code `%` whose [lhs][
     * BinaryOperator.lhs] is the format string and whose [rhs][BinaryOperator.rhs] is either a
     * single substitution value, or an [InitializerList] (the shape the Python frontend uses for a
     * tuple, see `ExpressionHandler.handleTuple`) holding several.
     *
     * Only handled when the format string itself is a resolvable constant (returns `null`
     * otherwise, mirroring [handleFormat]'s conservatism). Supports `%s` (string), `%d`/`%i`
     * (stringified, exactly like [handleFormat]'s placeholders - `evaluate` on a numeric literal
     * already yields its `toString()`, see `StringEvaluator.handleLiteral`) and `%%` (a literal
     * `%`), via a single tokenizing regex, so that `%%` adjacent to a real specifier cannot be
     * misparsed by two independent passes (the same reasoning as [handleFormat]'s `{{`/`}}` fix).
     * Every other conversion specifier (`%f`, `%x`, a `%(name)s` dict-style reference, ...) becomes
     * an `Unknown` segment rather than failing the whole call - conservative, not exact, but still
     * consumes a positional value (except for the dict-style case, which does not consume one) so
     * that subsequent `%s`/`%d` placeholders stay aligned with the correct argument.
     */
    override fun handleBinaryOperator(
        op: BinaryOperator,
        evaluate: (Node) -> StringPattern,
    ): StringPattern? {
        if (op.operatorCode != "%") return null
        val formatString = evaluate(op.lhs).asConstantOrNull() ?: return null
        val values = (op.rhs as? InitializerList)?.initializers ?: listOf(op.rhs)

        val parts = mutableListOf<StringPattern>()
        var valueIndex = 0
        var lastEnd = 0
        for (m in PERCENT_TOKEN.findAll(formatString)) {
            if (m.range.first > lastEnd) {
                parts.add(const(formatString.substring(lastEnd, m.range.first)))
            }
            val token = m.value
            parts.add(
                when {
                    token == "%%" -> const("%")
                    token.contains('(') ->
                        StringPattern.Unknown(
                            origin = op,
                            reason = StringPattern.Reason.UNSUPPORTED,
                        )
                    token.last() == 's' || token.last() == 'd' || token.last() == 'i' -> {
                        val value = values.getOrNull(valueIndex++)
                        value?.let { evaluate(it) }
                            ?: StringPattern.Unknown(
                                origin = op,
                                reason = StringPattern.Reason.UNSUPPORTED,
                            )
                    }
                    else -> {
                        valueIndex++
                        StringPattern.Unknown(
                            origin = op,
                            reason = StringPattern.Reason.UNSUPPORTED,
                        )
                    }
                }
            )
            lastEnd = m.range.last + 1
        }
        if (lastEnd < formatString.length) {
            parts.add(const(formatString.substring(lastEnd)))
        }
        return concat(parts)
    }

    /**
     * `os.path.join(a, b, ...)` becomes `Concat(a, "/", b, "/", ...)`, mirroring
     * `PythonValueEvaluator.handleCall`'s naive string concatenation, but at the [StringPattern]
     * level instead of stringifying already-concrete values.
     */
    private fun handleOsPathJoin(call: Call, evaluate: (Node) -> StringPattern): StringPattern {
        val parts = mutableListOf<StringPattern>()
        call.arguments.forEachIndexed { index, arg ->
            if (index > 0) parts.add(const("/"))
            parts.add(evaluate(arg))
        }
        return concat(parts)
    }

    /**
     * `"...".format(a, b, name=c)`. Only handled when the format string itself is a resolvable
     * constant (returns `null` otherwise, so the generic fallback in [StringEvaluator] applies).
     * Supports positional (`{0}`, `{}`) and named (`{name}`) placeholders, and Python's `{{`/`}}`
     * brace-escaping (`"{{x}}".format()` is the literal string `"{x}"`, not a substitution field
     * named `"x"`) - a single regex distinguishes `{{`, `}}` and `{field}` tokens so that escapes
     * adjacent to a real placeholder (e.g. `"{{{0}}}"`) are handled correctly. A format-spec suffix
     * (`{0:>10}`) is recognised but not honoured (padding/alignment is not modelled, only the
     * placeholder substitution itself). An out-of-range positional index or an unresolvable named
     * argument becomes an `Unknown` segment rather than failing the whole call.
     */
    private fun handleFormat(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val formatString = evaluate(base).asConstantOrNull() ?: return null

        val parts = mutableListOf<StringPattern>()
        var autoIndex = 0
        var lastEnd = 0
        for (m in FORMAT_TOKEN.findAll(formatString)) {
            if (m.range.first > lastEnd) {
                parts.add(const(formatString.substring(lastEnd, m.range.first)))
            }
            when (m.value) {
                "{{" -> parts.add(const("{"))
                "}}" -> parts.add(const("}"))
                else -> {
                    val field = m.value.substring(1, m.value.length - 1).substringBefore(':')
                    val replacement =
                        when {
                            field.isEmpty() -> call.argumentByNameOrPosition(position = autoIndex++)
                            field.toIntOrNull() != null ->
                                call.argumentByNameOrPosition(position = field.toInt())
                            else -> call.argumentByNameOrPosition(name = field)
                        }
                    parts.add(
                        replacement?.let { evaluate(it) }
                            ?: StringPattern.Unknown(
                                origin = call,
                                reason = StringPattern.Reason.UNSUPPORTED,
                            )
                    )
                }
            }
            lastEnd = m.range.last + 1
        }
        if (lastEnd < formatString.length) {
            parts.add(const(formatString.substring(lastEnd)))
        }
        return concat(parts)
    }

    /**
     * `sep.join(iterable)`. Best-effort: only handled when the iterable is an [InitializerList]
     * with statically known elements (e.g. a list/tuple literal); anything else (a variable holding
     * a list built up elsewhere, a generator, ...) becomes `Unknown`, since we have no general way
     * to enumerate an arbitrary iterable's elements.
     */
    private fun handleJoin(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val iterableArg = call.arguments.singleOrNull() ?: return null
        val elements =
            (iterableArg as? InitializerList)?.initializers
                ?: return StringPattern.Unknown(
                    origin = call,
                    reason = StringPattern.Reason.UNSUPPORTED,
                )

        val separator = evaluate(base)
        val parts = mutableListOf<StringPattern>()
        elements.forEachIndexed { index, element ->
            if (index > 0) parts.add(separator)
            parts.add(evaluate(element))
        }
        return concat(parts)
    }

    /**
     * `s.replace(old, new)` / `s.replace(old, new, count)`. Exact when the receiver, `old`, `new`,
     * and (if present) `count` are all constants, and `old` is non-empty - Python's semantics for
     * an empty `old` (inserting `new` between every character) are not modelled exactly and always
     * fall through to the over-approximation below.
     *
     * Otherwise over-approximated, and a [AssumptionType]`.SoundnessAssumption` is recorded on
     * [call] itself (rather than via the root-node mechanism [StringEvaluator] uses for budget
     * exhaustion), since the imprecision here is intrinsic to this one call and does not depend on
     * the evaluator's overall budget - scoping it to the call is more precise.
     *
     * The over-approximation is `Concat(constantPrefix(s), Unknown)` **only** when `old` is a
     * known, non-empty constant that provably cannot start a match within that prefix (see
     * [cannotOccurWithinPrefix]) - otherwise a match of `old` could straddle or lie entirely inside
     * the claimed-fixed prefix and rewrite it, so the sound fallback is a coarser `Unknown` whose
     * `charSet` is the union of the receiver's and `new`'s character sets and whose length is
     * unbounded.
     */
    private fun handleReplace(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val args = call.arguments
        if (args.size < 2) return null

        val receiver = evaluate(base)
        val old = evaluate(args[0])
        val new = evaluate(args[1])

        val receiverConst = receiver.asConstantOrNull()
        val oldConst = old.asConstantOrNull()
        val newConst = new.asConstantOrNull()

        val countArg = call.argumentByNameOrPosition(name = "count", position = 2)
        val countConst = countArg?.let { evaluate(it).asConstantOrNull()?.toIntOrNull() }

        if (
            receiverConst != null &&
                oldConst != null &&
                oldConst.isNotEmpty() &&
                newConst != null &&
                (countArg == null || countConst != null)
        ) {
            return const(
                if (countArg == null) receiverConst.replace(oldConst, newConst)
                else boundedReplace(receiverConst, oldConst, newConst, countConst!!)
            )
        }

        call.assume(
            AssumptionType.SoundnessAssumption,
            "We assume that the result of the call to `replace` at `$call` is over-approximated, " +
                "because the receiver, the `old`, the `new`, or the `count` argument is not a " +
                "fully known constant, or `old` is the empty string (whose replace semantics are " +
                "not modelled exactly). To verify this assumption, we need to check whether " +
                "narrowing these values (e.g. by increasing the evaluator's budget) changes the " +
                "result.",
            scope = call,
        )
        val prefix = receiver.constantPrefix()
        return if (
            oldConst != null && oldConst.isNotEmpty() && cannotOccurWithinPrefix(prefix, oldConst)
        ) {
            concat(
                const(prefix),
                StringPattern.Unknown(origin = call, reason = StringPattern.Reason.UNSUPPORTED),
            )
        } else {
            StringPattern.Unknown(
                origin = call,
                reason = StringPattern.Reason.UNSUPPORTED,
                charSet = charSetOf(receiver) union charSetOf(new),
                length = LatticeInterval.TOP,
            )
        }
    }

    /**
     * Bounded, left-to-right, non-overlapping replacement of up to [count] occurrences of [old] in
     * [receiver] with [new], matching Python's `str.replace(old, new, count)`. Requires `old` to be
     * non-empty.
     */
    private fun boundedReplace(receiver: String, old: String, new: String, count: Int): String {
        if (count <= 0) return receiver
        val sb = StringBuilder()
        var i = 0
        var remaining = count
        while (i < receiver.length) {
            if (remaining > 0 && receiver.startsWith(old, i)) {
                sb.append(new)
                i += old.length
                remaining--
            } else {
                sb.append(receiver[i])
                i++
            }
        }
        return sb.toString()
    }

    /**
     * `true` iff [old] provably cannot start a match within [prefix], including a match that starts
     * inside [prefix] and extends past its end - the condition under which `Concat(prefix,
     * Unknown)` soundly over-approximates `receiver.replace(old, new)` when `receiver`'s known
     * prefix is exactly [prefix]. Conservative: returns `false` (i.e. "cannot rule it out")
     * whenever unsure.
     */
    private fun cannotOccurWithinPrefix(prefix: String, old: String): Boolean {
        if (prefix.contains(old)) return false
        val maxOverlap = minOf(prefix.length, old.length - 1)
        for (k in 1..maxOverlap) {
            if (prefix.endsWith(old.substring(0, k))) return false
        }
        return true
    }

    /**
     * `s.strip()`/`s.lstrip()`/`s.rstrip()`, optionally with a `chars` argument selecting which
     * characters to strip (default: whitespace). Exact when the receiver is constant and, if
     * present, `chars` resolves to a constant string. Otherwise, since stripping can only shrink
     * (or keep) the length, never grow it, and can only ever remove characters that were already
     * admitted by the receiver's [CharSet] (regardless of which characters `chars` selects - that
     * only affects *how much* is stripped, never *which* characters could remain), a sound
     * over-approximation is `Unknown` with the receiver's [CharSet] and a length interval of `[0,
     * receiverLength.upper]`.
     */
    private fun handleStrip(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val receiver = evaluate(base)
        val receiverConst = receiver.asConstantOrNull()
        val charsArg = call.argumentByNameOrPosition(name = "chars", position = 0)
        val charsConst = charsArg?.let { evaluate(it).asConstantOrNull() }

        if (receiverConst != null && (charsArg == null || charsConst != null)) {
            val stripped =
                if (charsConst != null) {
                    when (call.name.localName) {
                        "lstrip" -> receiverConst.trimStart { it in charsConst }
                        "rstrip" -> receiverConst.trimEnd { it in charsConst }
                        else -> receiverConst.trim { it in charsConst }
                    }
                } else {
                    when (call.name.localName) {
                        "lstrip" -> receiverConst.trimStart()
                        "rstrip" -> receiverConst.trimEnd()
                        else -> receiverConst.trim()
                    }
                }
            return const(stripped)
        }

        val receiverLength = lengthOf(receiver)
        val upperBound =
            (receiverLength as? LatticeInterval.Bounded)?.upper ?: LatticeInterval.Bound.INFINITE
        return StringPattern.Unknown(
            origin = call,
            reason = StringPattern.Reason.UNSUPPORTED,
            charSet = charSetOf(receiver),
            length = LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), upperBound),
        )
    }

    /**
     * `s.upper()`/`s.lower()`. Exact when the receiver is constant. Otherwise, distributes the case
     * mapping over every [StringPattern.Const] leaf of the term (see [mapConstLeaves]) - sound
     * because case conversion commutes with concatenation, union and repetition (module locale edge
     * cases we do not model, e.g. Turkish dotless-i).
     */
    private fun handleCase(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val receiver = evaluate(base)
        val upper = call.name.localName == "upper"
        val f: (String) -> String = if (upper) String::uppercase else String::lowercase
        return mapConstLeaves(receiver, f)
    }

    /**
     * Maps every [StringPattern.Const] leaf of [p] through [f], re-normalising the result via the
     * smart constructors. [StringPattern.Unknown] leaves have their [CharSet] mapped via
     * [mapCharSet], which soundly accounts for characters whose full-string case mapping under [f]
     * produces more than one character (e.g. German `ß` uppercasing to `"SS"`).
     *
     * Provably terminating: this is a structural recursion over [p], which is already a finite term
     * (bounded by the evaluator's `maxTermSize`/`maxTermDepth`) - no new nesting is introduced.
     */
    private fun mapConstLeaves(p: StringPattern, f: (String) -> String): StringPattern =
        when (p) {
            is StringPattern.Bottom -> p
            is StringPattern.Const -> const(f(p.value))
            is StringPattern.Concat -> concat(p.parts.map { mapConstLeaves(it, f) })
            is StringPattern.Union -> union(p.alternatives.map { mapConstLeaves(it, f) })
            is StringPattern.Star -> star(mapConstLeaves(p.inner, f), p.min, p.max)
            is StringPattern.Unknown -> p.copy(charSet = mapCharSet(p.charSet, f))
        }

    /**
     * Maps [charSet] through [f], applied to each character's full-string representation (so that a
     * character whose mapping under [f] is itself multiple characters, e.g. German `ß` uppercasing
     * to `"SS"`, contributes *all* of those resulting characters). Never drops a possible output
     * character - dropping would under-approximate the resulting [CharSet], violating this domain's
     * soundness invariant (see the design doc: results must always be supersets of what is actually
     * reachable).
     */
    private fun mapCharSet(charSet: CharSet, f: (String) -> String): CharSet =
        when (charSet) {
            is CharSet.Empty,
            is CharSet.Any -> charSet
            is CharSet.Chars ->
                charsOf(charSet.chars.flatMap { c -> f(c.toString()).toList() }.toSet())
        }

    companion object {
        private val FORMAT_TOKEN = Regex("\\{\\{|\\}\\}|\\{[^{}]*\\}")
        private val STRIP_METHODS = setOf("strip", "lstrip", "rstrip")
        private val CASE_METHODS = setOf("upper", "lower")

        /**
         * Tokenizes Python's `%`-format strings: `%%` (an escaped literal `%`), `%(name)...` (a
         * dict-style reference, format-spec part parsed but not modelled), or a positional
         * specifier (`%s`, `%5.2f`, ...). One regex, not two independent passes, so that `%%`
         * adjacent to a real specifier cannot be misparsed - see [handleBinaryOperator]'s KDoc.
         */
        private val PERCENT_TOKEN = Regex("%%|%(\\([^)]*\\))?[-+0 #]*\\d*(\\.\\d+)?[a-zA-Z]")
    }
}

/**
 * Convenience entry point mirroring [de.fraunhofer.aisec.cpg.analysis.string.evaluateString], but
 * with [PythonStringOperationHandler] registered.
 */
fun Node.evaluatePythonString(
    config: StringEvaluatorConfig = StringEvaluatorConfig()
): StringPattern =
    StringEvaluator(config, operationHandlers = listOf(PythonStringOperationHandler()))
        .evaluate(this)
