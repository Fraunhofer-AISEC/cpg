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
 * **Not implemented in this pass** (see the design doc's Phase 3 list): `%`-style formatting (this
 * is a [de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator] with operator code `%`, not a
 * [Call], so it needs a different extension point than [StringOperationHandler.handleCall];
 * retrofitting that hook is left as follow-up work rather than restructuring [StringEvaluator]'s
 * dispatch for this one case), `encode`/`decode`, base64, and slicing (lower priority /
 * language-agnostic per the design doc).
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
     * Supports positional (`{0}`, `{}`) and named (`{name}`) placeholders; a format-spec suffix
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
        for (m in FORMAT_PLACEHOLDER.findAll(formatString)) {
            if (m.range.first > lastEnd) {
                parts.add(const(formatString.substring(lastEnd, m.range.first)))
            }
            val field = m.groupValues[1].substringBefore(':')
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
     * `s.replace(old, new)`. Exact when the receiver and both arguments are constants; otherwise
     * over-approximated as `Concat(constantPrefix(s), Unknown)`, and a
     * [AssumptionType]`.SoundnessAssumption` is recorded on [call] itself (rather than via the
     * root-node mechanism [StringEvaluator] uses for budget exhaustion), since the imprecision here
     * is intrinsic to this one call and does not depend on the evaluator's overall budget - scoping
     * it to the call is more precise.
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
        if (receiverConst != null && oldConst != null && newConst != null) {
            return const(receiverConst.replace(oldConst, newConst))
        }

        call.assume(
            AssumptionType.SoundnessAssumption,
            "We assume that the result of the call to `replace` at `$call` is over-approximated " +
                "by its known constant prefix followed by `.*`, because the receiver, the `old`, " +
                "or the `new` argument is not a fully known constant. To verify this assumption, " +
                "we need to check whether narrowing these values (e.g. by increasing the " +
                "evaluator's budget) changes the result.",
            scope = call,
        )
        val prefix = receiver.constantPrefix()
        return concat(
            const(prefix),
            StringPattern.Unknown(origin = call, reason = StringPattern.Reason.UNSUPPORTED),
        )
    }

    /**
     * `s.strip()`/`s.lstrip()`/`s.rstrip()`. Exact when the receiver is constant. Otherwise, since
     * stripping can only shrink (or keep) the length, never grow it, and can only ever remove
     * characters that were already admitted by the receiver's [CharSet], a sound over-approximation
     * is `Unknown` with the receiver's [CharSet] and a length interval of `[0,
     * receiverLength.upper]`.
     */
    private fun handleStrip(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val receiver = evaluate(base)
        val receiverConst = receiver.asConstantOrNull()
        if (receiverConst != null) {
            val stripped =
                when (call.name.localName) {
                    "lstrip" -> receiverConst.trimStart()
                    "rstrip" -> receiverConst.trimEnd()
                    else -> receiverConst.trim()
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
     * smart constructors. [StringPattern.Unknown] leaves have their [CharSet] mapped character-by-
     * character (best-effort: a character that maps to more than one character under [f], e.g.
     * German `ß` uppercasing to `SS`, is left unmapped, which only affects [CharSet.Chars]
     * precision, never soundness of the overall [StringPattern.length] bound).
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

    private fun mapCharSet(charSet: CharSet, f: (String) -> String): CharSet =
        when (charSet) {
            is CharSet.Empty,
            is CharSet.Any -> charSet
            is CharSet.Chars ->
                CharSet.Chars(
                    charSet.chars.mapNotNull { c -> f(c.toString()).singleOrNull() }.toSet()
                )
        }

    companion object {
        private val FORMAT_PLACEHOLDER = Regex("\\{([^{}]*)\\}")
        private val STRIP_METHODS = setOf("strip", "lstrip", "rstrip")
        private val CASE_METHODS = setOf("upper", "lower")
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
