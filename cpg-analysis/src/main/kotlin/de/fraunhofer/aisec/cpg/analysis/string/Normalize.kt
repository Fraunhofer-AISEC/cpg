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

/** Default bound on total term size (see [normalize]). */
const val DEFAULT_MAX_TERM_SIZE: Int = 64

/** Default bound on term nesting depth (see [normalize]). */
const val DEFAULT_MAX_TERM_DEPTH: Int = 16

/** Default bound on the number of alternatives a [StringPattern.Union] may hold. */
const val DEFAULT_MAX_UNION_SIZE: Int = 16

/**
 * Brings [p] into normal form:
 * 1. Flatten nested [StringPattern.Concat] and nested [StringPattern.Union].
 * 2. Drop `Const("")` from [StringPattern.Concat]; merge adjacent [StringPattern.Const]s.
 * 3. A [StringPattern.Concat] containing [StringPattern.Bottom] becomes [StringPattern.Bottom]; a
 *    [StringPattern.Union] drops [StringPattern.Bottom] alternatives.
 * 4. Collapse singleton [StringPattern.Concat]/[StringPattern.Union] to their element.
 * 5. Sort [StringPattern.Union] alternatives by a stable total order, so that the steps below are
 *    deterministic (equality of the resulting [Set] does not depend on the order, but the dedup and
 *    subsumption steps that iterate over it do, and a stable order keeps rendering reproducible).
 * 6. Deduplicate [StringPattern.Union] alternatives; if an alternative subsumes another, drop the
 *    subsumed one.
 * 7. Factor out a common prefix or suffix of a [StringPattern.Union] where cheap, e.g. `{"ab",
 *    "ac"} -> Concat(Const("a"), Union{Const("b"), Const("c")})`. **Limitation:** only a single
 *    leading/trailing [StringPattern.Const] is factored, and only when *every* alternative is a
 *    [StringPattern.Const] - this is not a fully general factoring algorithm, just enough to keep
 *    the common "shared literal prefix/suffix" case readable.
 * 8. If the [StringPattern.Union] has more than [maxUnionSize] alternatives, or the whole term
 *    exceeds [maxTermSize] leaves or [maxTermDepth] nesting, collapse to [StringPattern.Unknown]
 *    with the joined [CharSet]/[LatticeInterval] of the collapsed leaves.
 *
 * **Invariant:** two normalised terms denote the same language *if* they are structurally equal.
 * The converse does not hold (we may fail to notice that two different terms are equivalent) - that
 * costs precision, never soundness.
 */
fun normalize(
    p: StringPattern,
    maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
): StringPattern {
    val normalized = structuralNormalize(p, maxUnionSize)
    return if (size(normalized) > maxTermSize || depth(normalized) > maxTermDepth) {
        collapseToUnknown(normalized)
    } else {
        normalized
    }
}

/**
 * Steps 1-7 of [normalize] (flattening, merging, dedup/subsumption, affix factoring), without the
 * final whole-term size/depth collapse (step 8). Exposed separately so that [StringLattice.widen]
 * can inspect the *true* size of a join before any auto-collapsing hides it - see the note on
 * [StringLattice.widen] for why this distinction matters for termination.
 */
internal fun structuralNormalize(p: StringPattern, maxUnionSize: Int): StringPattern =
    when (p) {
        is StringPattern.Bottom,
        is StringPattern.Const,
        is StringPattern.Unknown -> p
        is StringPattern.Concat ->
            normalizeConcat(p.parts.map { structuralNormalize(it, maxUnionSize) })
        is StringPattern.Union ->
            normalizeUnion(
                p.alternatives.map { structuralNormalize(it, maxUnionSize) },
                maxUnionSize,
            )
        is StringPattern.Star ->
            normalizeStar(structuralNormalize(p.inner, maxUnionSize), p.min, p.max)
    }

private fun normalizeConcat(parts: List<StringPattern>): StringPattern {
    val flat = mutableListOf<StringPattern>()
    for (part in parts) {
        if (part is StringPattern.Concat) flat.addAll(part.parts) else flat.add(part)
    }
    if (flat.any { it is StringPattern.Bottom }) return StringPattern.Bottom

    val merged = mutableListOf<StringPattern>()
    for (part in flat) {
        if (part is StringPattern.Const && part.value.isEmpty()) continue
        val last = merged.lastOrNull()
        if (last is StringPattern.Const && part is StringPattern.Const) {
            merged[merged.size - 1] = StringPattern.Const(last.value + part.value)
        } else {
            merged.add(part)
        }
    }
    return when {
        merged.isEmpty() -> StringPattern.Const("")
        merged.size == 1 -> merged[0]
        else -> StringPattern.Concat(merged)
    }
}

private fun normalizeUnion(alts: List<StringPattern>, maxUnionSize: Int): StringPattern {
    val flat = mutableListOf<StringPattern>()
    for (a in alts) {
        if (a is StringPattern.Union) flat.addAll(a.alternatives) else flat.add(a)
    }
    val noBottom = flat.filter { it !is StringPattern.Bottom }
    if (noBottom.isEmpty()) return StringPattern.Bottom

    val sorted = noBottom.distinct().sortedWith(patternOrder)

    // Drop alternatives that are already subsumed by a kept one, and drop previously kept
    // alternatives that the new, more general candidate subsumes.
    val kept = mutableListOf<StringPattern>()
    for (candidate in sorted) {
        if (kept.any { subsumes(it, candidate) }) continue
        kept.removeAll { it != candidate && subsumes(candidate, it) }
        kept.add(candidate)
    }

    if (kept.size == 1) return kept[0]

    factorCommonAffix(kept, maxUnionSize)?.let {
        return it
    }

    return if (kept.size > maxUnionSize) {
        collapseToUnknown(StringPattern.Union(kept.toSet()))
    } else {
        StringPattern.Union(kept.toSet())
    }
}

private fun normalizeStar(inner: StringPattern, min: Int, max: Int?): StringPattern =
    when {
        max == 0 -> StringPattern.Const("")
        inner is StringPattern.Bottom ->
            if (min <= 0) StringPattern.Const("") else StringPattern.Bottom
        inner is StringPattern.Const && inner.value.isEmpty() -> StringPattern.Const("")
        else -> StringPattern.Star(inner, min, max)
    }

/** A stable total order over [StringPattern]s used to make [normalize] deterministic. */
private val patternOrder: Comparator<StringPattern> = compareBy { it.toRegexString() }

/**
 * Factors a single common leading or trailing [StringPattern.Const] out of [alts], e.g. `{"ab",
 * "ac"} -> Concat(Const("a"), Union{Const("b"), Const("c")})`. Only applies when every alternative
 * is a [StringPattern.Const] - see the limitation documented on [normalize].
 */
private fun factorCommonAffix(alts: List<StringPattern>, maxUnionSize: Int): StringPattern? {
    if (alts.size < 2) return null
    val consts = alts.map { it as? StringPattern.Const ?: return null }
    val strs = consts.map { it.value }
    if (strs.any { it.isEmpty() }) return null

    val prefix = strs.reduce { a, b -> a.commonPrefixWith(b) }
    if (prefix.isNotEmpty()) {
        val rests = strs.map { it.removePrefix(prefix) }
        val restPattern =
            if (rests.all { it.isEmpty() }) StringPattern.Const("")
            else normalizeUnion(rests.map { StringPattern.Const(it) }, maxUnionSize)
        return normalizeConcat(listOf(StringPattern.Const(prefix), restPattern))
    }

    val suffix = strs.reduce { a, b -> a.commonSuffixWith(b) }
    if (suffix.isNotEmpty()) {
        val rests = strs.map { it.removeSuffix(suffix) }
        val restPattern =
            if (rests.all { it.isEmpty() }) StringPattern.Const("")
            else normalizeUnion(rests.map { StringPattern.Const(it) }, maxUnionSize)
        return normalizeConcat(listOf(restPattern, StringPattern.Const(suffix)))
    }

    return null
}

private fun collapseToUnknown(p: StringPattern): StringPattern =
    StringPattern.Unknown(
        origin = null,
        reason = StringPattern.Reason.UNSUPPORTED,
        charSet = charSetOf(p),
        length = lengthOf(p),
    )

/** Number of nodes (leaves and inner nodes) in [p]. Used to bound term growth in [normalize]. */
fun size(p: StringPattern): Int =
    when (p) {
        is StringPattern.Bottom,
        is StringPattern.Const,
        is StringPattern.Unknown -> 1
        is StringPattern.Concat -> 1 + p.parts.sumOf { size(it) }
        is StringPattern.Union -> 1 + p.alternatives.sumOf { size(it) }
        is StringPattern.Star -> 1 + size(p.inner)
    }

/** Nesting depth of [p]. Used to bound term growth in [normalize]. */
fun depth(p: StringPattern): Int =
    when (p) {
        is StringPattern.Bottom,
        is StringPattern.Const,
        is StringPattern.Unknown -> 1
        is StringPattern.Concat -> 1 + (p.parts.maxOfOrNull { depth(it) } ?: 0)
        is StringPattern.Union -> 1 + (p.alternatives.maxOfOrNull { depth(it) } ?: 0)
        is StringPattern.Star -> 1 + depth(p.inner)
    }

/**
 * A best-effort, over-approximating character set of every string [p] may denote: the union of the
 * [CharSet]s of all [StringPattern.Const] and [StringPattern.Unknown] leaves reachable from [p].
 * Only required to over-approximate, never to under-approximate - callers (in particular
 * [StringLattice.widen]) rely on that direction for soundness.
 */
fun charSetOf(p: StringPattern): CharSet =
    when (p) {
        is StringPattern.Bottom -> CharSet.Empty
        is StringPattern.Const -> charsOf(p.value.toSet())
        is StringPattern.Unknown -> p.charSet
        is StringPattern.Concat ->
            p.parts.fold(CharSet.Empty as CharSet) { acc, part -> acc union charSetOf(part) }
        is StringPattern.Union ->
            p.alternatives.fold(CharSet.Empty as CharSet) { acc, alt -> acc union charSetOf(alt) }
        is StringPattern.Star -> if (p.max == 0) CharSet.Empty else charSetOf(p.inner)
    }

/**
 * A best-effort length interval for [p]: exact for [StringPattern.Const], the sum for
 * [StringPattern.Concat] when computable, the join across [StringPattern.Union] alternatives, and
 * the repeat-count-scaled interval for [StringPattern.Star]. Only required to over-approximate,
 * i.e. the true length must always lie within the returned interval; it may be wider than
 * necessary.
 */
fun lengthOf(p: StringPattern): LatticeInterval =
    when (p) {
        is StringPattern.Bottom -> LatticeInterval.BOTTOM
        is StringPattern.Const -> LatticeInterval.Bounded(p.value.length, p.value.length)
        is StringPattern.Unknown -> p.length
        is StringPattern.Concat ->
            p.parts.fold(LatticeInterval.Bounded(0, 0) as LatticeInterval) { acc, part ->
                acc + lengthOf(part)
            }
        is StringPattern.Union -> p.alternatives.map { lengthOf(it) }.reduce { a, b -> a.join(b) }
        is StringPattern.Star -> {
            val innerLength = lengthOf(p.inner)
            if (innerLength is LatticeInterval.BOTTOM) {
                if (p.min <= 0) LatticeInterval.Bounded(0, 0) else LatticeInterval.BOTTOM
            } else {
                val repeatCount =
                    LatticeInterval.Bounded(
                        LatticeInterval.Bound.Value(p.min.toLong()),
                        p.max?.let { LatticeInterval.Bound.Value(it.toLong()) }
                            ?: LatticeInterval.Bound.INFINITE,
                    )
                innerLength * repeatCount
            }
        }
    }

/** `true` iff [inner] is fully contained within [outer]. The empty interval is contained in all. */
fun intervalContains(outer: LatticeInterval, inner: LatticeInterval): Boolean =
    when {
        inner is LatticeInterval.BOTTOM -> true
        outer is LatticeInterval.BOTTOM -> false
        outer is LatticeInterval.Bounded && inner is LatticeInterval.Bounded ->
            outer.lower <= inner.lower && outer.upper >= inner.upper
        else -> false
    }

/** `true` iff every character admitted by [inner] is also admitted by [outer]. */
fun charSetContains(outer: CharSet, inner: CharSet): Boolean =
    when {
        inner is CharSet.Empty -> true
        outer is CharSet.Any -> true
        outer is CharSet.Empty -> false
        inner is CharSet.Any -> false
        inner is CharSet.Chars && outer is CharSet.Chars -> outer.chars.containsAll(inner.chars)
        else -> false
    }

/** All non-null [StringPattern.Unknown.origin]s reachable from [p]. */
fun unknownOriginsOf(p: StringPattern): Set<Node> =
    when (p) {
        is StringPattern.Bottom,
        is StringPattern.Const -> emptySet()
        is StringPattern.Unknown -> setOfNotNull(p.origin)
        is StringPattern.Concat -> p.parts.flatMapTo(mutableSetOf()) { unknownOriginsOf(it) }
        is StringPattern.Union -> p.alternatives.flatMapTo(mutableSetOf()) { unknownOriginsOf(it) }
        is StringPattern.Star -> unknownOriginsOf(p.inner)
    }

/**
 * Sound-in-one-direction language inclusion: when this returns `true`, `language(b) subset-of-or-
 * equal language(a)` really holds. When it returns `false`, inclusion may still hold - we just
 * failed to notice - so callers must treat `false` as "unknown", never as a proof of non-inclusion.
 */
fun subsumes(a: StringPattern, b: StringPattern): Boolean {
    if (a == b) return true
    if (b is StringPattern.Bottom) return true
    if (a is StringPattern.Bottom) return false

    return when (a) {
        is StringPattern.Const -> false
        is StringPattern.Unknown ->
            charSetContains(a.charSet, charSetOf(b)) && intervalContains(a.length, lengthOf(b))
        is StringPattern.Concat ->
            b is StringPattern.Concat &&
                a.parts.size == b.parts.size &&
                a.parts.zip(b.parts).all { (x, y) -> subsumes(x, y) }
        is StringPattern.Union ->
            when (b) {
                is StringPattern.Union ->
                    b.alternatives.all { bb -> a.alternatives.any { subsumes(it, bb) } }
                else -> a.alternatives.any { subsumes(it, b) }
            }
        is StringPattern.Star ->
            when {
                b is StringPattern.Star &&
                    b.inner == a.inner &&
                    a.min <= b.min &&
                    (a.max == null || (b.max != null && b.max <= a.max)) -> true
                a.min == 0 && b is StringPattern.Const && b.value.isEmpty() -> true
                else -> false
            }
    }
}
