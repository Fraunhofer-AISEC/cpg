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
package de.fraunhofer.aisec.cpg.analysis.string.jvm

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
import de.fraunhofer.aisec.cpg.analysis.string.union
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Models a subset of the JVM/Java standard library's string-relevant operations (Phase 3 of the
 * design doc, second-priority language after Python): `StringBuilder`/`StringBuffer.append` chains,
 * `String.format`, `String.join`, `String.concat`, `String.substring`, `String.replace`.
 *
 * **Call matching, dependency-free.** Like
 * [de.fraunhofer.aisec.cpg.analysis.string.python .PythonStringOperationHandler], this does *not*
 * need `cpg-language-java`/`cpg-language-jvm` on the classpath (`cpg-analysis` only has
 * `api(projects.cpgCore)` and `api(projects.cpgConcepts)`, see `cpg-analysis/build.gradle.kts` -
 * unchanged by this handler). Everything used here - [MemberCall.isStatic], [MemberCall.base],
 * [Construction], `call.name.localName`, and [Node.type] - is defined in `cpg-core` and is already
 * populated by the time a JVM/Java frontend has run, regardless of whether that frontend is on this
 * module's classpath. Static calls (`String.format(...)`, `String.join(...)`) are still
 * [MemberCall]s in this codebase (see `cpg-language-java`'s
 * `ExpressionHandler.handleMethodCallExpression`), just with [MemberCall.isStatic] `== true` and a
 * base that refers to the class rather than an instance - this is what lets us tell a static
 * `String.format` apart from an unrelated instance method also named `format`. Instance methods
 * (`.append`, `.concat`, `.substring`, `.replace`, `.toString`) are matched on
 * `call.name.localName` alone, best-effort like the Python handler, refined by checking the *type*
 * of the receiver ([isBuilderType]) where that distinguishes an unrelated user-defined method of
 * the same name (`append`) from the `StringBuilder`/`StringBuffer` one.
 */
class JvmStringOperationHandler : StringOperationHandler {
    override fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
        return when {
            call is Construction && isBuilderType(call.type) ->
                handleBuilderConstruction(call, evaluate)
            call is MemberCall &&
                call.name.localName == "append" &&
                isBuilderType(call.base?.type) -> handleAppend(call, evaluate)
            call is MemberCall &&
                call.name.localName == "toString" &&
                isBuilderType(call.base?.type) -> call.base?.let { evaluate(it) }
            call is MemberCall && !call.isStatic && call.name.localName == "concat" ->
                handleConcat(call, evaluate)
            call is MemberCall && call.isStatic && call.name.localName == "format" ->
                handleFormat(call, evaluate)
            call is MemberCall && call.isStatic && call.name.localName == "join" ->
                handleJoin(call, evaluate)
            call is MemberCall && !call.isStatic && call.name.localName == "substring" ->
                handleSubstring(call, evaluate)
            call is MemberCall && !call.isStatic && call.name.localName == "replace" ->
                handleReplace(call, evaluate)
            else -> null
        }
    }

    private fun isBuilderType(type: Type?): Boolean =
        type != null && type.name.localName in BUILDER_TYPES

    /**
     * `new StringBuilder(...)` / `new StringBuffer(...)`. The no-arg constructor seeds an empty
     * string. The single-arg constructor is overloaded in the JDK between `StringBuilder(String)`
     * (seeds the given content) and `StringBuilder(int capacity)` (still empty) - distinguished
     * here by the argument's static type where that is resolvable; if it is neither clearly numeric
     * nor clearly a string/char-sequence type (e.g. the type could not be inferred), we soundly
     * admit both possibilities via [union] rather than guessing.
     */
    private fun handleBuilderConstruction(
        call: Construction,
        evaluate: (Node) -> StringPattern,
    ): StringPattern {
        val arg = call.arguments.singleOrNull() ?: return const("")
        val typeName = arg.type.name.localName
        return when {
            typeName in NUMERIC_TYPES -> const("")
            typeName.equals("string", ignoreCase = true) ||
                typeName.equals("CharSequence", ignoreCase = true) -> evaluate(arg)
            else -> union(evaluate(arg), const(""))
        }
    }

    /**
     * `sb.append(x)`. [MemberCall.base] is either the original `StringBuilder`/`StringBuffer`
     * (construction, variable, ...) for the first `append` in a chain, or the *previous* `append`
     * call's return value for a fluent chain (`sb.append("a").append("b")`) - either way,
     * [evaluate] on it recurses through this same evaluator/handler, so the chain is resolved one
     * link at a time without any special-casing here for which case it is.
     */
    private fun handleAppend(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val arg = call.arguments.singleOrNull() ?: return null
        return concat(evaluate(base), evaluate(arg))
    }

    /** `s.concat(other)`. */
    private fun handleConcat(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val arg = call.arguments.singleOrNull() ?: return null
        return concat(evaluate(base), evaluate(arg))
    }

    /**
     * `String.format(fmt, args...)`. Only handled when the format string itself is a resolvable
     * constant (mirrors the Python `str.format`/`%`-formatting handlers' conservatism). Supports
     * Java's C-printf-style `%s`/`%d`/`%i` conversions (substituted with the corresponding
     * argument, stringified - `evaluate` on a numeric literal already yields its `toString()`, see
     * `StringEvaluator.handleLiteral`) and `%%` (a literal `%`), tokenized by a single regex so
     * that `%%` adjacent to a real specifier cannot be misparsed by two independent passes (the
     * same reasoning as the Python handlers' brace-/percent-escaping fixes). Every other conversion
     * (`%f`, `%x`, ...) becomes an `Unknown` segment but still consumes the next positional
     * argument, so that later `%s`/`%d` placeholders stay aligned. An explicit argument index
     * (`%1$s`) is honoured; otherwise arguments are consumed in order. `%n` (the platform line
     * separator, modelled here as a literal `"\n"` for simplicity rather than
     * `System.lineSeparator()`) is a Java `Formatter` conversion that takes **no** argument at
     * all - unlike every other conversion, it must not advance the auto-index counter nor consume a
     * value from `values`, or every subsequent positional placeholder would be shifted by one and
     * the result would unsoundly exclude the real output.
     */
    private fun handleFormat(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        val formatArg = args.firstOrNull() ?: return null
        val formatString = evaluate(formatArg).asConstantOrNull() ?: return null
        val values = args.drop(1)

        val parts = mutableListOf<StringPattern>()
        var autoIndex = 0
        var lastEnd = 0
        for (m in JAVA_FORMAT_TOKEN.findAll(formatString)) {
            if (m.range.first > lastEnd) {
                parts.add(const(formatString.substring(lastEnd, m.range.first)))
            }
            val token = m.value
            val conversion = token.last().lowercaseChar()
            parts.add(
                if (token == "%%") {
                    const("%")
                } else if (conversion == 'n') {
                    const("\n")
                } else {
                    val explicitIndex =
                        EXPLICIT_INDEX.find(token)?.groupValues?.get(1)?.toIntOrNull()
                    val index = explicitIndex?.let { it - 1 } ?: autoIndex++
                    val value = values.getOrNull(index)
                    if (conversion == 's' || conversion == 'd' || conversion == 'i') {
                        value?.let { evaluate(it) }
                            ?: StringPattern.Unknown(
                                origin = call,
                                reason = StringPattern.Reason.UNSUPPORTED,
                            )
                    } else {
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
        return concat(parts)
    }

    /**
     * `String.join(delimiter, elements...)` / `String.join(delimiter, iterable)`. Best-effort,
     * mirroring the Python `str.join` handler: statically enumerable elements (either the varargs
     * form, or a single [InitializerList] argument) are joined exactly; anything else (a variable
     * holding a collection built up elsewhere, ...) becomes `Unknown`.
     */
    private fun handleJoin(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val args = call.arguments
        val delimiterArg = args.firstOrNull() ?: return null
        val elements =
            if (args.size == 2 && args[1] is InitializerList) {
                (args[1] as InitializerList).initializers
            } else if (args.size >= 2) {
                args.drop(1)
            } else {
                return StringPattern.Unknown(
                    origin = call,
                    reason = StringPattern.Reason.UNSUPPORTED,
                )
            }

        val separator = evaluate(delimiterArg)
        val parts = mutableListOf<StringPattern>()
        elements.forEachIndexed { index, element ->
            if (index > 0) parts.add(separator)
            parts.add(evaluate(element))
        }
        return concat(parts)
    }

    /**
     * `s.substring(begin[, end])`. Exact when the receiver and the argument(s) are all constant and
     * the indices are in bounds. Otherwise `Unknown`, whose length is bounded above by
     * `min(receiverLength.upper, end)` when `end` is a known constant, else by
     * `receiverLength.upper` alone - the result of `substring` can never be longer than the
     * receiver itself, and never longer than `end` (Java's `substring` throws rather than clamping
     * a too-large `end`, so a resolvable `end` is a genuine upper bound whenever the call does not
     * throw).
     */
    private fun handleSubstring(
        call: MemberCall,
        evaluate: (Node) -> StringPattern,
    ): StringPattern? {
        val base = call.base ?: return null
        val args = call.arguments
        if (args.isEmpty() || args.size > 2) return null

        val receiver = evaluate(base)
        val beginConst = evaluate(args[0]).asConstantOrNull()?.toIntOrNull()
        val endArg = args.getOrNull(1)
        val endConst = endArg?.let { evaluate(it).asConstantOrNull()?.toIntOrNull() }
        val receiverConst = receiver.asConstantOrNull()

        if (receiverConst != null && beginConst != null && (endArg == null || endConst != null)) {
            val end = endConst ?: receiverConst.length
            if (beginConst in 0..receiverConst.length && end in beginConst..receiverConst.length) {
                return const(receiverConst.substring(beginConst, end))
            }
        }

        val receiverUpper =
            (lengthOf(receiver) as? LatticeInterval.Bounded)?.upper
                ?: LatticeInterval.Bound.INFINITE
        val upperBound =
            if (endConst != null) {
                val endBound = LatticeInterval.Bound.Value(endConst.toLong())
                if (endBound < receiverUpper) endBound else receiverUpper
            } else {
                receiverUpper
            }
        return StringPattern.Unknown(
            origin = call,
            reason = StringPattern.Reason.UNSUPPORTED,
            charSet = charSetOf(receiver),
            length = LatticeInterval.Bounded(LatticeInterval.Bound.Value(0), upperBound),
        )
    }

    /**
     * `s.replace(old, new)`. Unlike Python's `str.replace`, Java's 2-arg `String.replace` has no
     * `count` parameter and treats `old`/`new` as `CharSequence` or `char` - both are handled as
     * plain strings here. Reuses the exact same soundness-fixed reasoning as
     * `PythonStringOperationHandler.handleReplace` (both call the shared
     * [de.fraunhofer.aisec.cpg.analysis.string.cannotOccurWithinPrefix]): the over-approximation
     * `Concat(prefix, Unknown)` is only used when `old` provably [cannotOccurWithinPrefix] the
     * receiver's known constant prefix (i.e. cannot start a match within it, including a match
     * straddling its end) - otherwise a match of `old` could rewrite part of the claimed-fixed
     * prefix, so the sound fallback is a coarser `Unknown` whose `charSet` is the union of the
     * receiver's and `new`'s.
     */
    private fun handleReplace(call: MemberCall, evaluate: (Node) -> StringPattern): StringPattern? {
        val base = call.base ?: return null
        val args = call.arguments
        if (args.size != 2) return null

        val receiver = evaluate(base)
        val old = evaluate(args[0])
        val new = evaluate(args[1])

        val receiverConst = receiver.asConstantOrNull()
        val oldConst = old.asConstantOrNull()
        val newConst = new.asConstantOrNull()

        if (
            receiverConst != null && oldConst != null && oldConst.isNotEmpty() && newConst != null
        ) {
            return const(receiverConst.replace(oldConst, newConst))
        }

        call.assume(
            AssumptionType.SoundnessAssumption,
            "We assume that the result of the call to `replace` at `$call` is over-approximated, " +
                "because the receiver, the `old`, or the `new` argument is not a fully known " +
                "constant, or `old` is the empty string. To verify this assumption, we need to " +
                "check whether narrowing these values (e.g. by increasing the evaluator's budget) " +
                "changes the result.",
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

    companion object {
        private val BUILDER_TYPES = setOf("StringBuilder", "StringBuffer")
        private val NUMERIC_TYPES =
            setOf("int", "Integer", "long", "Long", "short", "Short", "byte", "Byte")

        /**
         * Tokenizes Java's `String.format` conversion syntax: `%%` (an escaped literal `%`), or a
         * `%[argument_index$][flags][width][.precision]conversion` specifier. One regex, not two
         * independent passes, for the same reason as the Python handlers' escaping fixes - see
         * [handleFormat]'s KDoc.
         */
        private val JAVA_FORMAT_TOKEN = Regex("%%|%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z]")
        private val EXPLICIT_INDEX = Regex("^%(\\d+)\\$")
    }
}

/**
 * Convenience entry point mirroring [de.fraunhofer.aisec.cpg.analysis.string.evaluateString], but
 * with [JvmStringOperationHandler] registered.
 */
fun Node.evaluateJvmString(config: StringEvaluatorConfig = StringEvaluatorConfig()): StringPattern =
    StringEvaluator(config, operationHandlers = listOf(JvmStringOperationHandler())).evaluate(this)
