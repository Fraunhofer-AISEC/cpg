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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.graph.Node

/** The exact value of this pattern, if it denotes exactly one string. */
fun StringPattern.asConstantOrNull(): String? = enumerate(1)?.singleOrNull()

/**
 * The full, finite language of this pattern, if it has one and it has at most [limit] elements.
 * Returns `null` when the language is infinite (an unbounded [StringPattern.Star]), contains an
 * [StringPattern.Unknown] segment we cannot enumerate, or has more than [limit] elements.
 */
fun StringPattern.enumerate(limit: Int): Set<String>? = enumerateRec(this, limit)

private fun enumerateRec(p: StringPattern, limit: Int): Set<String>? {
    return when (p) {
        is StringPattern.Bottom -> emptySet()
        is StringPattern.Const -> setOf(p.value)
        is StringPattern.Unknown ->
            when {
                p.charSet is CharSet.Empty -> emptySet()
                p.length == LatticeIntervalZero -> setOf("")
                else -> null
            }
        is StringPattern.Concat -> {
            var acc = setOf("")
            for (part in p.parts) {
                val partSet = enumerateRec(part, limit) ?: return null
                val next = mutableSetOf<String>()
                for (a in acc) {
                    for (b in partSet) {
                        next.add(a + b)
                        if (next.size > limit) return null
                    }
                }
                acc = next
            }
            acc
        }
        is StringPattern.Union -> {
            val acc = mutableSetOf<String>()
            for (alt in p.alternatives) {
                val altSet = enumerateRec(alt, limit) ?: return null
                acc.addAll(altSet)
                if (acc.size > limit) return null
            }
            acc
        }
        is StringPattern.Star -> {
            val max = p.max ?: return null
            val innerSet = enumerateRec(p.inner, limit) ?: return null
            val result = mutableSetOf<String>()
            var repeats = setOf("")
            for (k in 0..max) {
                if (k >= p.min) {
                    result.addAll(repeats)
                    if (result.size > limit) return null
                }
                if (k == max) break
                val next = mutableSetOf<String>()
                for (a in repeats) {
                    for (b in innerSet) {
                        next.add(a + b)
                        if (next.size > limit) return null
                    }
                }
                repeats = next
            }
            result
        }
    }
}

private val LatticeIntervalZero = LatticeInterval.Bounded(0, 0)

/**
 * Whether the language of this pattern may contain a string matched by [regex]. Best-effort: if the
 * language can be [enumerate]d within a reasonable budget, checks the enumeration; otherwise
 * conservatively returns `true` - "may" queries are unsound in the `false` direction, so an unknown
 * language is treated as "may match anything".
 */
fun StringPattern.mayMatch(regex: Regex): Boolean {
    val enumerated = enumerate(MAY_MATCH_ENUMERATION_LIMIT) ?: return true
    return enumerated.any { regex.matches(it) }
}

/**
 * Whether every string in the language of this pattern is matched by [regex]. This is the operation
 * genuinely limited by the term-only representation (D3 in the design doc): without complementation
 * we can only decide it when the language can be [enumerate]d within a reasonable budget. It
 * returns a conservative `false` otherwise.
 *
 * This pure, [Node]-free function cannot itself record an `Assumption` for that give-up case (a
 * `StringPattern` is a plain value type, not a [de.fraunhofer.aisec.cpg.assumptions.HasAssumptions]
 * - see the design doc's discussion of this trade-off). The query-API wrapper
 *   `Node.stringMustMatch` (`cpg-analysis/query/StringQueries.kt`), which does have a node context,
 *   records a `SoundnessAssumption` on its `QueryTree` whenever this returns `false` because
 *   [enumerate] gave up, as opposed to a genuine, proven non-match.
 */
fun StringPattern.mustMatch(regex: Regex): Boolean {
    val enumerated = enumerate(MUST_MATCH_ENUMERATION_LIMIT) ?: return false
    return enumerated.isNotEmpty() && enumerated.all { regex.matches(it) }
}

private const val MAY_MATCH_ENUMERATION_LIMIT = 1000

/**
 * `internal`, not `private`: `Node.stringMustMatch` needs the exact same budget to determine
 * whether a conservative `false` from [mustMatch] was a genuine proof of non-match or a give-up (in
 * which case it must attach a soundness assumption).
 */
internal const val MUST_MATCH_ENUMERATION_LIMIT = 1000

/**
 * The longest prefix that is guaranteed to be present in every string of this pattern's language.
 */
fun StringPattern.constantPrefix(): String =
    when (this) {
        is StringPattern.Bottom -> ""
        is StringPattern.Const -> value
        is StringPattern.Concat -> {
            val sb = StringBuilder()
            for (part in parts) {
                val exact = part.asConstantOrNull()
                if (exact != null) {
                    sb.append(exact)
                    continue
                }
                sb.append(part.constantPrefix())
                break
            }
            sb.toString()
        }
        is StringPattern.Union ->
            alternatives.map { it.constantPrefix() }.reduceOrNull { a, b -> a.commonPrefixWith(b) }
                ?: ""
        is StringPattern.Star -> if (min >= 1) inner.constantPrefix() else ""
        is StringPattern.Unknown -> ""
    }

/**
 * The longest suffix that is guaranteed to be present in every string of this pattern's language.
 */
fun StringPattern.constantSuffix(): String =
    when (this) {
        is StringPattern.Bottom -> ""
        is StringPattern.Const -> value
        is StringPattern.Concat -> {
            val sb = StringBuilder()
            for (part in parts.asReversed()) {
                val exact = part.asConstantOrNull()
                if (exact != null) {
                    sb.insert(0, exact)
                    continue
                }
                sb.insert(0, part.constantSuffix())
                break
            }
            sb.toString()
        }
        is StringPattern.Union ->
            alternatives.map { it.constantSuffix() }.reduceOrNull { a, b -> a.commonSuffixWith(b) }
                ?: ""
        is StringPattern.Star -> if (min >= 1) inner.constantSuffix() else ""
        is StringPattern.Unknown -> ""
    }

/**
 * Substrings that are guaranteed to be present in every string of this pattern's language.
 * Best-effort and conservative - the empty set is always a valid, safe answer. In particular this
 * does not attempt to find substrings that only become guaranteed by combining adjacent parts (e.g.
 * `Concat(Union{"a","b"}, Const("c"))` guarantees `"c"` but this implementation may also miss
 * combinations across that boundary).
 */
fun StringPattern.mustContain(): Set<String> =
    when (this) {
        is StringPattern.Bottom -> emptySet()
        is StringPattern.Const -> if (value.isEmpty()) emptySet() else setOf(value)
        is StringPattern.Concat -> parts.flatMapTo(mutableSetOf()) { it.mustContain() }
        is StringPattern.Union ->
            alternatives.map { it.mustContain() }.reduceOrNull { a, b -> a intersect b }
                ?: emptySet()
        is StringPattern.Star -> if (min >= 1) inner.mustContain() else emptySet()
        is StringPattern.Unknown -> emptySet()
    }

/**
 * `true` iff this pattern contains no [StringPattern.Unknown] and no [StringPattern.Star] with
 * variable repetition count. A [StringPattern.Star] with `min == max` is "fully known in shape"
 * only if its inner pattern is itself fully known.
 */
val StringPattern.isFullyKnown: Boolean
    get() =
        when (this) {
            is StringPattern.Bottom -> true
            is StringPattern.Const -> true
            is StringPattern.Unknown -> false
            is StringPattern.Concat -> parts.all { it.isFullyKnown }
            is StringPattern.Union -> alternatives.all { it.isFullyKnown }
            is StringPattern.Star -> max != null && min == max && inner.isFullyKnown
        }

/** All [StringPattern.Unknown.origin]s reachable from this pattern, for explanations. */
val StringPattern.unknownOrigins: Set<Node>
    get() = unknownOriginsOf(this)
