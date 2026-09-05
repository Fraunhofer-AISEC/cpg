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
package de.fraunhofer.aisec.cpg.analysis.string.golang

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluator
import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluatorConfig
import de.fraunhofer.aisec.cpg.analysis.string.StringOperationHandler
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.asConstantOrNull
import de.fraunhofer.aisec.cpg.analysis.string.cannotOccurWithinPrefix
import de.fraunhofer.aisec.cpg.analysis.string.charSetOf
import de.fraunhofer.aisec.cpg.analysis.string.concat
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.analysis.string.constantPrefix
import de.fraunhofer.aisec.cpg.analysis.string.lengthOf
import de.fraunhofer.aisec.cpg.analysis.string.mapConstLeaves
import de.fraunhofer.aisec.cpg.analysis.string.union
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName

/**
 * Models a subset of Go's string-relevant standard library operations (Phase 3 of the design doc):
 * `strings.Join`, `fmt.Sprintf`, `strings.Replace`/`ReplaceAll`, `strings.ToUpper`/`ToLower`,
 * `strings.TrimSpace`/`Trim`/`TrimLeft`/`TrimRight`, `path.Join`/`filepath.Join`.
 *
 * **Call matching, dependency-free.** Like
 * [de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler] and
 * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler], this does *not* need
 * `cpg-language-go` on the classpath (`cpg-analysis` only has `api(projects.cpgCore)` and
 * `api(projects.cpgConcepts)`, unchanged by this handler). Unlike Python's instance-bound string
 * methods, every operation modelled here is a plain, package-qualified function call
 * (`strings.Join(...)`, `fmt.Sprintf(...)`, ...), never a method call on a receiver - Go's standard
 * library does not expose these as methods on `string`. By the time these calls run through
 * `SymbolResolver`, `ResolveMemberAmbiguityPass` (a language-agnostic pass in `cpg-core`, not
 * needed here as a *dependency*, only as the thing that produces this shape at runtime) has already
 * rewritten the `strings.Join` selector-expression chain into a single [Call] whose callee is a
 * plain, fully-qualified [de.fraunhofer.aisec.cpg.graph.expressions.Reference] - at that point
 * `Call` is not itself `HasBase`, so `call.reconstructedImportName` is simply `call.name`, which
 * already *is* `"strings.Join"`. This mirrors exactly how
 * [de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler] matches
 * `os.path.join`.
 *
 * **Not implemented in this pass**: `strings.Contains`/`HasPrefix`/`HasSuffix` return a `Boolean`,
 * not a string, so they are out of scope for [StringOperationHandler], which only handles calls
 * whose result is itself a string. `+`/`+=` string concatenation needs no handler at all: it is
 * already covered generically by `StringEvaluator.handleBinaryOperator`, which is
 * language-agnostic.
 *
 * **`fmt.Sprintf`'s verb-consumption rules.** Unlike Java's `String.format`, which has a `%n`
 * conversion that (uniquely) consumes *no* argument - the bug fixed for
 * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler] - Go's `fmt` package has
 * **no such verb**: a literal newline in a Go format string is always written as the escape `\n`
 * directly in the string, never produced by a dedicated conversion verb. Every verb recognised by
 * `fmt` (`%v`, `%s`, `%d`, `%q`, `%t`, `%x`, ...) consumes exactly one positional argument (except
 * `%%`, which consumes none, same as everywhere else). There is consequently no
 * argument-index-shift trap analogous to `%n` to guard against here - but the argument consumption
 * for every recognised verb (including the conservatively-modelled ones) is still carefully kept
 * 1-for-1 with the actual `fmt` semantics, so that later placeholders stay aligned with the correct
 * argument.
 */
class GoStringOperationHandler : StringOperationHandler {
    override fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        return when (call.reconstructedImportName.toString()) {
            "strings.Join" -> handleJoin(call, evaluate)
            "fmt.Sprintf" -> handleSprintf(call, evaluate)
            "strings.Replace" -> handleReplace(call, evaluate, unbounded = false)
            "strings.ReplaceAll" -> handleReplace(call, evaluate, unbounded = true)
            "strings.ToUpper" -> handleCase(call, evaluate, upper = true)
            "strings.ToLower" -> handleCase(call, evaluate, upper = false)
            "strings.TrimSpace" -> handleTrim(call, evaluate, hasCutsetArg = false)
            "strings.Trim",
            "strings.TrimLeft",
            "strings.TrimRight" -> handleTrim(call, evaluate, hasCutsetArg = true)
            "path.Join",
            "filepath.Join" -> handlePathJoin(call, evaluate)
            else -> null
        }
    }

    /**
     * `strings.Join(elems, sep)`. Best-effort, mirroring `PythonStringOperationHandler.handleJoin`:
     * only handled when `elems` is an [InitializerList] with statically known elements (e.g. a
     * slice composite literal `[]string{ "a", "b"}`); anything else (a variable holding a slice
     * built up elsewhere, a function result, ...) becomes `Unknown`, since we have no general way
     * to enumerate an arbitrary slice's elements.
     */
    private fun handleJoin(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        if (args.size != 2) return null
        val elemsArg = args[0]
        val sepArg = args[1]
        val elements =
            (elemsArg as? InitializerList)?.initializers
                ?: return StringPattern.Unknown(
                    origin = call,
                    reason = StringPattern.Reason.UNSUPPORTED,
                )

        val separator = evaluate(sepArg)
        val parts = mutableListOf<StringPattern>()
        elements.forEachIndexed { index, element ->
            if (index > 0) parts.add(separator)
            parts.add(evaluate(element))
        }
        return concat(parts)
    }

    /**
     * `fmt.Sprintf(format, args...)`. Only handled when the format string itself is a resolvable
     * constant (mirrors the Python/Java format handlers' conservatism). Supports `%s`, `%d`, and
     * `%v` (Go's "default format" verb - substituted with the corresponding argument, stringified,
     * exactly like `%s`/`%d` - `evaluate` on a literal already yields its `toString()`, see
     * `StringEvaluator.handleLiteral`) and `%%` (a literal `%`), tokenized by a single regex so
     * that `%%` adjacent to a real verb cannot be misparsed by two independent passes (the same
     * reasoning as the Python/Java format handlers' escaping fixes). Every other verb (`%q`, `%t`,
     * `%x`, ...) becomes an `Unknown` segment but still consumes the next positional argument - see
     * this class's KDoc for why Go has no `%n`-style no-argument verb to special-case.
     */
    private fun handleSprintf(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        val formatArg = args.firstOrNull() ?: return null
        val formatString = evaluate(formatArg).asConstantOrNull() ?: return null
        val values = args.drop(1)

        val parts = mutableListOf<StringPattern>()
        var valueIndex = 0
        var lastEnd = 0
        for (m in GO_FORMAT_TOKEN.findAll(formatString)) {
            if (m.range.first > lastEnd) {
                parts.add(const(formatString.substring(lastEnd, m.range.first)))
            }
            val token = m.value
            val conversion = token.last()
            parts.add(
                if (token == "%%") {
                    const("%")
                } else if (conversion == 's' || conversion == 'd' || conversion == 'v') {
                    val value = values.getOrNull(valueIndex++)
                    value?.let { evaluate(it) }
                        ?: StringPattern.Unknown(
                            origin = call,
                            reason = StringPattern.Reason.UNSUPPORTED,
                        )
                } else {
                    valueIndex++
                    StringPattern.Unknown(origin = call, reason = StringPattern.Reason.UNSUPPORTED)
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
     * `strings.Replace(s, old, new, n)` (`n < 0` means "replace all") and `strings.ReplaceAll(s,
     * old, new)` (equivalent to `n == -1`, [unbounded] `== true`). Reuses the exact same
     * soundness-fixed reasoning as `PythonStringOperationHandler.handleReplace`/
     * `JvmStringOperationHandler.handleReplace` (all three call the shared
     * [cannotOccurWithinPrefix]): exact when the receiver, `old`, `new`, and (for the bounded form)
     * `n` are all constants and `old` is non-empty; the over-approximation `Concat(prefix,
     * Unknown)` is only used when `old` provably [cannotOccurWithinPrefix] the receiver's known
     * constant prefix, otherwise the sound fallback is a coarser `Unknown` whose `charSet` is the
     * union of the receiver's and `new`'s.
     */
    private fun handleReplace(
        call: Call,
        evaluate: (Node) -> StringPattern,
        unbounded: Boolean,
    ): StringPattern? {
        val args = call.arguments
        if (unbounded && args.size != 3) return null
        if (!unbounded && args.size != 4) return null

        val receiver = evaluate(args[0])
        val old = evaluate(args[1])
        val new = evaluate(args[2])

        val receiverConst = receiver.asConstantOrNull()
        val oldConst = old.asConstantOrNull()
        val newConst = new.asConstantOrNull()
        val nConst = if (unbounded) -1 else evaluate(args[3]).asConstantOrNull()?.toIntOrNull()

        if (
            receiverConst != null &&
                oldConst != null &&
                oldConst.isNotEmpty() &&
                newConst != null &&
                nConst != null
        ) {
            return const(
                if (nConst < 0) receiverConst.replace(oldConst, newConst)
                else boundedReplace(receiverConst, oldConst, newConst, nConst)
            )
        }

        call.assume(
            AssumptionType.SoundnessAssumption,
            "We assume that the result of the call to `${call.name}` at `$call` is " +
                "over-approximated, because the receiver, the `old`, the `new`, or the `n` " +
                "argument is not a fully known constant, or `old` is the empty string (whose " +
                "replace semantics - insert `new` between every rune - are not modelled exactly). " +
                "To verify this assumption, we need to check whether narrowing these values (e.g. " +
                "by increasing the evaluator's budget) changes the result.",
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
     * [receiver] with [new], matching Go's `strings.Replace(s, old, new, n)` for `n >= 0`. Requires
     * `old` to be non-empty.
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
     * `strings.ToUpper(s)`/`strings.ToLower(s)`. Exact when the argument is constant. Otherwise,
     * distributes the case mapping over every [StringPattern.Const] leaf of the term (see the
     * shared [mapConstLeaves]) - sound because case conversion commutes with concatenation, union
     * and repetition (modulo locale edge cases we do not model).
     */
    private fun handleCase(
        call: Call,
        evaluate: (Node) -> StringPattern,
        upper: Boolean,
    ): StringPattern? {
        val arg = call.arguments.singleOrNull() ?: return null
        val value = evaluate(arg)
        val f: (String) -> String = if (upper) String::uppercase else String::lowercase
        return mapConstLeaves(value, f)
    }

    /**
     * `strings.TrimSpace(s)` (no `cutset` argument, implicitly trims whitespace) or
     * `strings.Trim(s, cutset)`/`strings.TrimLeft(s, cutset)`/`strings.TrimRight(s, cutset)` (an
     * explicit `cutset` argument selects which characters to trim). Exact when `s` is constant and,
     * if present, `cutset` resolves to a constant string. Otherwise, since trimming can only shrink
     * (or keep) the length, never grow it, and can only ever remove characters that were already
     * admitted by `s`'s [de.fraunhofer.aisec.cpg.analysis.string.CharSet] (regardless of which
     * characters `cutset` selects - that only affects *how much* is trimmed, never *which*
     * characters could remain), a sound over-approximation is `Unknown` with `s`'s `CharSet` and a
     * length interval of `[0, sLength.upper]`.
     */
    private fun handleTrim(
        call: Call,
        evaluate: (Node) -> StringPattern,
        hasCutsetArg: Boolean,
    ): StringPattern? {
        val args = call.arguments
        if (hasCutsetArg && args.size != 2) return null
        if (!hasCutsetArg && args.size != 1) return null

        val s = evaluate(args[0])
        val sConst = s.asConstantOrNull()
        val cutsetConst = if (hasCutsetArg) evaluate(args[1]).asConstantOrNull() else null

        if (sConst != null && (!hasCutsetArg || cutsetConst != null)) {
            val trimmed =
                if (cutsetConst != null) {
                    when (call.name.localName) {
                        "TrimLeft" -> sConst.trimStart { it in cutsetConst }
                        "TrimRight" -> sConst.trimEnd { it in cutsetConst }
                        else -> sConst.trim { it in cutsetConst }
                    }
                } else {
                    sConst.trim()
                }
            return const(trimmed)
        }

        val sLength = lengthOf(s)
        val upperBound =
            (sLength as? LatticeInterval.Bounded)?.upper ?: LatticeInterval.Bound.INFINITE
        return StringPattern.Unknown(
            origin = call,
            reason = StringPattern.Reason.UNSUPPORTED,
            charSet = charSetOf(s),
            length = LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), upperBound),
        )
    }

    /**
     * `path.Join(a, b, ...)` / `filepath.Join(a, b, ...)` becomes `Concat(a, "/", b, "/", ...)`,
     * mirroring `PythonStringOperationHandler.handleOsPathJoin`. As there, we deliberately assume
     * `/` as the separator - `filepath.Join` would use `\` on Windows, but modelling the host OS is
     * out of scope, and `/` matches this design's existing `os.path.join` precedent.
     */
    private fun handlePathJoin(call: Call, evaluate: (Node) -> StringPattern): StringPattern {
        val parts = mutableListOf<StringPattern>()
        call.arguments.forEachIndexed { index, arg ->
            if (index > 0) parts.add(const("/"))
            parts.add(evaluate(arg))
        }
        return concat(parts)
    }

    companion object {
        /**
         * Tokenizes Go's `fmt` verb syntax: `%%` (an escaped literal `%`), or a
         * `%[flags][width][.precision]verb` specifier. One regex, not two independent passes, for
         * the same reason as the Python/Java handlers' escaping fixes - see [handleSprintf]'s KDoc.
         */
        private val GO_FORMAT_TOKEN = Regex("%%|%[-+#0 ]*\\d*(\\.\\d+)?[a-zA-Z]")
    }
}

/**
 * Convenience entry point mirroring [de.fraunhofer.aisec.cpg.analysis.string.evaluateString], but
 * with [GoStringOperationHandler] registered.
 */
fun Node.evaluateGoString(config: StringEvaluatorConfig = StringEvaluatorConfig()): StringPattern =
    StringEvaluator(config, operationHandlers = listOf(GoStringOperationHandler())).evaluate(this)
