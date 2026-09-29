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
package de.fraunhofer.aisec.cpg.analysis.string.cxx

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluator
import de.fraunhofer.aisec.cpg.analysis.string.StringEvaluatorConfig
import de.fraunhofer.aisec.cpg.analysis.string.StringOperationHandler
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.asConstantOrNull
import de.fraunhofer.aisec.cpg.analysis.string.charSetOf
import de.fraunhofer.aisec.cpg.analysis.string.concat
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.analysis.string.lengthOf
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall

/**
 * Models a subset of the C standard library's buffer-mutating string functions (Phase 3 of the
 * design doc, third-priority language group after Python and JVM): `strcat`, `strncat`, `strcpy`,
 * `snprintf`.
 *
 * **Call matching, dependency-free.** Like
 * [de.fraunhofer.aisec.cpg.analysis.string.python.PythonStringOperationHandler] and
 * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler], this does *not* need
 * `cpg-language-cxx` on the classpath (`cpg-analysis` only has `api(projects.cpgCore)` and
 * `api(projects.cpgConcepts)`, see `cpg-analysis/build.gradle.kts` - unchanged by this handler).
 * Unlike the JVM handler, C has no methods/receivers, so these are plain, global-function-style
 * [Call]s, matched on `call.name.localName` alone - there is no base/receiver to refine the match
 * with, so, symmetrically to how the JVM handler uses [MemberCall.isStatic] and receiver type to
 * rule out an unrelated user method of the same name, this handler rules out [MemberCall] itself (a
 * C++ object's own method of the same name, e.g. `obj.strcat(...)`, is almost certainly not the
 * libc function) rather than matching indiscriminately on the name alone.
 *
 * **Buffer semantics, not value semantics - the core design point of this handler.** Unlike
 * Python/JVM's immutable-string operations, C's string functions operate on a **mutable, fixed-size
 * buffer passed by pointer**: `strcat(dest, src)` *mutates* `dest` in place (and returns a pointer
 * to it - aliasing, not a fresh value). Modelling `dest`'s value *after* the call therefore means
 * modelling what flows out of the call as a new definition of `dest`, not modelling the call's
 * return value in isolation. Concretely:
 * * `strcat`/`strncat` **depend on `dest`'s old value** - the result is `concat(oldDest, src)`.
 * * `strcpy` **overwrites `dest` entirely** - the result is simply `src`, regardless of what `dest`
 *   held before. Conflating these two (treating `strcpy` like `strcat`) is the one mistake this
 *   handler must not make.
 *
 * **DFG-shape assumption (unverified against a real C frontend).** For any of this to produce a
 * useful result, [StringEvaluator] must be asked to evaluate a *later* read of `dest` whose
 * backward DFG resolves to this `Call` node itself (i.e. the frontend/DFG pass must model the call
 * as a def-site for `dest`, the way an `Assign` is a def-site for its lhs) - only then does
 * [StringEvaluator.handleCall] dispatch into this handler for that call. This handler does not
 * itself verify that assumption; it relies on whatever DFG edges are present. [evaluate] is applied
 * to `call.arguments[0]` (the `dest` expression *at the call site*, i.e. the read of `dest` used as
 * an argument) to obtain `dest`'s value *before* this call: since that expression node is itself
 * only ever the target of a *read*, following its own backward DFG naturally lands on whatever
 * wrote `dest` earlier, not circularly back on this same call. Whether a real C/C++ frontend
 * actually wires a "later read of `dest` flows from this call" edge is a genuine open question -
 * this is flagged here, and in the accompanying test file (`CStringOperationHandlerTest.kt`), for
 * re-verification once a real C fixture can be exercised end-to-end; until then, the tests
 * construct that DFG shape by hand to keep the ground truth unambiguous regardless of the real
 * frontend's behaviour.
 *
 * **`snprintf`'s return value vs. `buf`'s value - a caveat as significant as the DFG-shape
 * assumption above, and not to be confused with it.** Unlike `strcat`/`strcpy`, whose C return
 * value genuinely *is* (a pointer aliasing) `dest`, real `snprintf(buf, size, format, args...)`
 * returns an `int` (the number of characters that *would have been* written, ignoring truncation) -
 * it does *not* return the formatted string. [handleSnprintf] cannot tell these two cases apart: it
 * produces the formatted/truncated string unconditionally whenever [StringEvaluator.handleCall]
 * dispatches this `Call` node into it, which is only the *correct* answer when reached via the same
 * DFG-shape mechanism described above for `strcat`/`strcpy` - i.e. a *later read of `buf`* whose
 * backward DFG resolves to this call as `buf`'s def-site. If, instead, this handler is reached
 * because something evaluated the call expression's *own* value directly (e.g. `int n =
 * snprintf(...)` and then evaluating `n`'s def-site chain back to this call, or evaluating the
 * `Call` node itself), the formatted string is the *wrong* answer - the correct one would be an
 * `int` count, which this handler does not attempt to compute. [StringEvaluator]'s dispatch has no
 * mechanism to distinguish "reached via `buf`'s def-site" from "reached via the call's own
 * expression value" without threading additional context through
 * [handleCall]/[StringOperationHandler.handleCall], which is a larger change than this handler's
 * scope - this is accepted as a known limitation of the general `Call`-node dispatch design (shared
 * with, and no worse than, the pre-existing `strcat`/`strcpy` DFG-shape assumption above), not
 * something redesigned here. See `CStringOperationHandlerTest.testSnprintfExact`'s KDoc for how the
 * test suite reflects this.
 *
 * **`snprintf`'s format-string mini-language.** C's `printf` family conversions
 * (`d`/`i`/`o`/`u`/`x`/`X`/`e`/`E`/`f`/`F`/`g`/`G`/`a`/`A`/`c`/`s`/`p`/`n`) are always consumed
 * positionally, in order - unlike Java's `String.format`, standard C `printf` has no `%1$s`-style
 * explicit argument index. Verified against the C printf format-string grammar (not assumed, given
 * this exact class of bug - an argument-consuming conversion being mis-modelled as non-consuming,
 * or vice versa - was just found and fixed for Java's `%n` in
 * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler]): the *only* C conversion
 * that consumes **no** argument at all is `%%` (a literal `%`). In particular, C's `%n` is *not*
 * analogous to Java's argument-less `%n` (the platform line separator) - C's `%n` writes the number
 * of characters output so far into an `int*` argument, so it *does* consume a (pointer) argument,
 * it just contributes no characters to the formatted output. Both are accounted for here: `%%` does
 * not advance the positional-argument counter, `%n` does advance it (consuming, but not
 * stringifying, the corresponding argument) while contributing the empty string.
 *
 * **Not implemented in this pass**: `strncpy`, `sprintf` (unbounded, arguably more dangerous but
 * not listed in the design doc's Phase 3 scope), and every conversion beyond `%s`/`%d`/`%i`/`%%`/
 * `%n` (e.g. `%f`, `%x`) - these become an `Unknown` segment but still consume their positional
 * argument, exactly mirroring the JVM handler's `handleFormat` conservatism.
 */
class CStringOperationHandler : StringOperationHandler {
    override fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        if (call is MemberCall) return null
        return when (call.name.localName) {
            "strcat" -> handleStrcat(call, evaluate)
            "strncat" -> handleStrncat(call, evaluate)
            "strcpy" -> handleStrcpy(call, evaluate)
            "snprintf" -> handleSnprintf(call, evaluate)
            else -> null
        }
    }

    /**
     * `strcat(dest, src)`. The value of `dest` after this call is `concat(oldDest, src)` - see this
     * class's KDoc for why `evaluate(call.arguments[0])` yields `dest`'s value *before* this call,
     * not a circular self-reference.
     */
    private fun handleStrcat(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        if (args.size != 2) return null
        return concat(evaluate(args[0]), evaluate(args[1]))
    }

    /**
     * `strncat(dest, src, n)`. Like [handleStrcat], but at most `n` characters of `src` are
     * appended (plus a null terminator that does not contribute to the string value). Exact when
     * both `src` and `n` are constant; otherwise the appended segment is over-approximated as
     * `Unknown`, bounded above by `min(n, src's own upper length bound)` whenever either is known -
     * deliberately not losing the one piece of certain information (the bound) we do have, even
     * when we cannot compute an exact value.
     */
    private fun handleStrncat(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        if (args.size != 3) return null
        val dest = args[0]
        val src = args[1]

        val srcPattern = evaluate(src)
        val srcConst = srcPattern.asConstantOrNull()
        val nConst = evaluate(args[2]).asConstantOrNull()?.toIntOrNull()

        val appended =
            if (srcConst != null && nConst != null) {
                const(srcConst.take(nConst.coerceAtLeast(0)))
            } else {
                val srcUpper =
                    (lengthOf(srcPattern) as? LatticeInterval.Bounded)?.upper
                        ?: LatticeInterval.Bound.INFINITE
                val nUpper =
                    nConst?.let { LatticeInterval.Bound.Value(it.toLong()) }
                        ?: LatticeInterval.Bound.INFINITE
                val upper = if (nUpper < srcUpper) nUpper else srcUpper
                StringPattern.Unknown(
                    origin = call,
                    reason = StringPattern.Reason.UNSUPPORTED,
                    charSet = charSetOf(srcPattern),
                    length = LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), upper),
                )
            }
        return concat(evaluate(dest), appended)
    }

    /**
     * `strcpy(dest, src)`. Unlike [handleStrcat]/[handleStrncat], this *overwrites* `dest`
     * entirely: the value of `dest` after this call is simply `src`'s value, independent of what
     * `dest` held before. This distinction (cat/ncat depend on the old value, cpy does not) is the
     * core semantic point of this handler.
     */
    private fun handleStrcpy(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        if (args.size != 2) return null
        return evaluate(args[1])
    }

    /**
     * `snprintf(buf, size, format, args...)`. Mirrors
     * [de.fraunhofer.aisec.cpg.analysis.string.jvm.JvmStringOperationHandler.handleFormat]'s
     * printf-style tokenisation (see this class's KDoc for the `%n`-consumption verification),
     * adapted for C's always-positional argument order, plus `size`-based truncation: `snprintf`
     * never writes more than `size - 1` characters plus a null terminator. When the formatted
     * result is fully constant and `size` is a known constant, the result is truncated exactly;
     * otherwise, if `size` is known, the result's length bound is tightened to `size - 1` rather
     * than left unbounded, since that bound is real, exploitable information for a
     * buffer-overflow-relevant analysis.
     *
     * **Only correct when reached via `buf`'s def-site, not via the call's own expression value** -
     * see this class's KDoc, "`snprintf`'s return value vs. `buf`'s value", for why: real
     * `snprintf`'s own expression value is an `int` count, not this formatted string, and this
     * handler has no way to tell the two dispatch reasons apart. Callers/tests must exercise this
     * handler via a later read of `buf` whose `prevDFG` resolves to the `snprintf` call (mirroring
     * the `strcat`/`strcpy` tests), not by evaluating the `Call` node directly, to exercise the
     * intended, correct usage.
     */
    private fun handleSnprintf(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        if (args.size < 3) return null
        val sizeConst = evaluate(args[1]).asConstantOrNull()?.toIntOrNull()
        val formatString = evaluate(args[2]).asConstantOrNull() ?: return null
        val values = args.drop(3)

        val parts = mutableListOf<StringPattern>()
        var argIndex = 0
        var lastEnd = 0
        for (m in C_FORMAT_TOKEN.findAll(formatString)) {
            if (m.range.first > lastEnd) {
                parts.add(const(formatString.substring(lastEnd, m.range.first)))
            }
            val token = m.value
            val conversion = token.last()
            parts.add(
                when {
                    token == "%%" -> const("%")
                    conversion == 'n' -> {
                        // Consumes a pointer argument (to store the character count), but
                        // contributes no characters to the formatted output.
                        argIndex++
                        const("")
                    }
                    conversion == 's' || conversion == 'd' || conversion == 'i' -> {
                        val value = values.getOrNull(argIndex++)
                        value?.let { evaluate(it) }
                            ?: StringPattern.Unknown(
                                origin = call,
                                reason = StringPattern.Reason.UNSUPPORTED,
                            )
                    }
                    else -> {
                        argIndex++
                        StringPattern.Unknown(
                            origin = call,
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
        val formatted = concat(parts)

        if (sizeConst == null) return formatted
        val maxLen = (sizeConst - 1).coerceAtLeast(0)
        val exact = formatted.asConstantOrNull()
        return if (exact != null) {
            const(if (exact.length > maxLen) exact.substring(0, maxLen) else exact)
        } else {
            val currentUpper =
                (lengthOf(formatted) as? LatticeInterval.Bounded)?.upper
                    ?: LatticeInterval.Bound.INFINITE
            val maxBound = LatticeInterval.Bound.Value(maxLen.toLong())
            val newUpper = if (maxBound < currentUpper) maxBound else currentUpper
            StringPattern.Unknown(
                origin = call,
                reason = StringPattern.Reason.UNSUPPORTED,
                charSet = charSetOf(formatted),
                length = LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), newUpper),
            )
        }
    }

    companion object {
        /**
         * Tokenizes C's `printf`-family conversion syntax: `%%` (an escaped literal `%`), or a
         * `%[flags][width][.precision][length modifier]conversion` specifier, always positional (no
         * `%1$s`-style explicit index, unlike Java's `String.format`). One regex, not two
         * independent passes, so that `%%` adjacent to a real specifier cannot be misparsed - same
         * reasoning as the Python/JVM handlers' escaping fixes.
         */
        private val C_FORMAT_TOKEN = Regex("%%|%[-+ 0#]*\\d*(\\.\\d+)?(hh|h|ll|l|L|j|z|t)?[a-zA-Z]")
    }
}

/**
 * Convenience entry point mirroring [de.fraunhofer.aisec.cpg.analysis.string.evaluateString], but
 * with [CStringOperationHandler] registered.
 */
fun Node.evaluateCString(config: StringEvaluatorConfig = StringEvaluatorConfig()): StringPattern =
    StringEvaluator(config, operationHandlers = listOf(CStringOperationHandler())).evaluate(this)
