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

import de.fraunhofer.aisec.cpg.helpers.ConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.concurrentIdentitySetOf
import de.fraunhofer.aisec.cpg.helpers.functional.HasWidening
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.Order

/**
 * The [Lattice] over [StringPattern]. [bottom] is [StringPattern.Bottom]; [lub] is exact
 * ([normalize] of the [StringPattern.Union] of both operands); [glb] is exact for the easy cases
 * and otherwise the sound fallback described on [glb]; [compare] is derived from [subsumes], which
 * is only sound in one direction (see its KDoc) - this is why [Order.EQUAL] is the only result
 * callers can fully trust, which is exactly what [Lattice.iterateEogInternal] needs for termination
 * detection.
 *
 * We deliberately do not implement `HasNarrowing`: a syntactic narrowing operator here would have
 * to pick between two terms without genuine language intersection (no automaton backing yet, see
 * the design doc's "Future work" section), and doing so unsoundly (e.g. `narrow(a, b) = if
 * subsumes(b, a) b else a`) can shrink the represented language below the true one. [HasWidening]
 * alone is enough to guarantee termination; narrowing is only a precision refinement, which the
 * term-only domain cannot do soundly without more work than a first iteration warrants.
 */
class StringLattice(
    val maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    val maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    val maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
) : Lattice<StringPattern>, HasWidening<StringPattern> {
    override var elements: ConcurrentIdentitySet<StringPattern> = concurrentIdentitySetOf()

    override val bottom: StringPattern = StringPattern.Bottom

    /**
     * `StringPattern`s are immutable, so [allowModify] has no observable effect: there is nothing
     * to mutate in place, and returning a freshly normalised term is exactly as cheap as
     * "modifying" `one` would be. The flag is accepted only to satisfy the [Lattice] interface.
     */
    override suspend fun lub(
        one: StringPattern,
        two: StringPattern,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): StringPattern =
        if (widen) {
            widen(one, two)
        } else {
            normalize(StringPattern.Union(setOf(one, two)), maxTermSize, maxTermDepth, maxUnionSize)
        }

    /**
     * Exact for the easy cases:
     * - either side is [StringPattern.Bottom] -> `Bottom`.
     * - equal terms -> that term.
     * - [StringPattern.Const] vs [StringPattern.Const] -> `Bottom` unless equal.
     * - [StringPattern.Const] vs [StringPattern.Unknown] -> the `Const` if its `charSet`/`length`
     *   are admitted by the `Unknown`, else `Bottom`.
     *
     * Otherwise the sound fallback: if one term [subsumes] the other, the intersection is the more
     * specific (subsumed) one; if neither subsumes the other, an exact intersection would need
     * automaton-based language intersection (see the design doc's "Future work" section), so we
     * return `Bottom` - the safe direction for a meet computed from below, since `Bottom`'s
     * language (the empty set) is trivially a subset of both operands' languages.
     */
    override suspend fun glb(one: StringPattern, two: StringPattern): StringPattern =
        when {
            one is StringPattern.Bottom || two is StringPattern.Bottom -> StringPattern.Bottom
            one == two -> one
            one is StringPattern.Const && two is StringPattern.Const ->
                if (one.value == two.value) one else StringPattern.Bottom
            one is StringPattern.Const && two is StringPattern.Unknown ->
                if (
                    charSetContains(two.charSet, charSetOf(one)) &&
                        intervalContains(two.length, lengthOf(one))
                )
                    one
                else StringPattern.Bottom
            two is StringPattern.Const && one is StringPattern.Unknown ->
                if (
                    charSetContains(one.charSet, charSetOf(two)) &&
                        intervalContains(one.length, lengthOf(two))
                )
                    two
                else StringPattern.Bottom
            subsumes(one, two) -> two
            subsumes(two, one) -> one
            else -> StringPattern.Bottom
        }

    /**
     * Derived from [subsumes]: `a == b` -> [Order.EQUAL]; `subsumes(a, b) && !subsumes(b, a)` ->
     * `a` is more general, i.e. [Order.GREATER]; the mirror case -> [Order.LESSER]; otherwise
     * [Order.UNEQUAL]. Delegates to [StringPattern.compare], which implements exactly this.
     */
    override fun compare(one: StringPattern, two: StringPattern): Order = one.compare(two)

    /** Terms are immutable. */
    override fun duplicate(one: StringPattern): StringPattern = one

    /**
     * Ensures that repeated widening of an ascending chain stabilises after finitely many steps.
     *
     * ```
     * widen(one, two):
     *   if one already subsumes two: return one   // no growth, already a fixpoint
     *   return alignAndWiden(one, two)
     *
     * alignAndWiden(one, two):
     *   if one == two: return one                              // fixpoint, no growth here
     *   if one is Unknown or two is Unknown: return widenLeaves(one, two)
     *   if one, two are same-arity Concat/matching Star/both Union: recurse pairwise, rebuild
     *   otherwise: growWiden(one, two)                          // shape genuinely differs
     *
     * growWiden(one, two):
     *   j = lub(one, two)
     *   if size(j) <= maxTermSize && depth(j) <= maxTermDepth: return j
     *   if two matches Concat(one, x) structurally: return Concat(one, Star(x))
     *   if two matches Concat(x, one) structurally: return Concat(Star(x), one)
     *   return widenLeaves(one, two)
     *
     * widenLeaves(one, two):
     *   return Unknown(origin = commonOrigin(one, two), reason = WIDENED,
     *                  charSet = charSet(one) union charSet(two), length = length(one).widen(length(two)))
     * ```
     *
     * **Why the structural recursion is needed.** `two` is itself typically produced by a plain
     * (non-widening) [lub]/[normalize] call, which has its *own* independent size-based
     * auto-collapse (`normalize`'s step 8). That collapse computes a fresh, exact
     * `charSet`/`length` `Unknown` from whatever term it happens to see, with no memory of earlier
     * collapses. If that fresh `Unknown` only ever got compared against `one` as a *whole* (the
     * original, buggy version of this function), the top-level `Union` dedup in
     * [structuralNormalize] can find that the fresh `Unknown` already subsumes every alternative
     * contributed by `one`'s history and silently drop them - without ever invoking
     * [LatticeInterval.widen] on the leaf that actually needs it. The term's *shape* then repeats
     * identically every iteration (so the outer `size(j) <= maxTermSize` guard trivially holds
     * forever), while a `length`/`charSet` buried at a leaf position keeps growing by a fresh,
     * unwidened recomputation each time - never stabilising. See
     * [WideningTerminationTest.testUnionWrappedGrowingTailStabilizes] for the ascending chain this
     * specifically guards against.
     *
     * `alignAndWiden` fixes this by recursing into corresponding sub-terms of `one` and `two`
     * *before* any collapse can hide the comparison, and widening (rather than exact-recomputing)
     * at every position where either side is already an `Unknown` - not just when the entire term
     * is.
     *
     * **Termination sketch.**
     * 1. **The recursion in `alignAndWiden` itself is finite.** Every recursive call either (a)
     *    hits the `one == two` fixpoint (no further recursion), (b) hits the `Unknown`-vs-anything
     *    case (no further recursion - it computes [widenLeaves] directly), or (c) recurses into
     *    *strictly smaller* children of a matching `Concat`/`Union`/`Star` shape. So the recursion
     *    depth is bounded by the depth of the smaller of `one` and `two`, and it always terminates.
     * 2. **The sequence of widen calls across fixpoint iterations converges.** Consider any
     *    position in the term that would otherwise grow forever. Once a call at that position takes
     *    the `Unknown`-vs-anything branch, every later call at the same position takes it again (an
     *    `Unknown` stays an `Unknown` under this branch), which only touches two finite-height
     *    components: [CharSet] (`Empty <= Chars <= Any`, `Chars` can only grow up to
     *    [CharSet.MAX_EXPLICIT] before collapsing to `Any`) and
     *    [de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.widen] (already proven to
     *    terminate: each bound can jump to `NEGATIVE_INFINITE`/`INFINITE` at most once). Once both
     *    stabilise, `widenLeaves(...)` reproduces the same `Unknown`, and the smart constructors
     *    that rebuild the enclosing `Concat`/`Union`/`Star` pass it through unchanged (they only
     *    re-derive an exact `charSet`/`length` when the *whole rebuilt term* still exceeds
     *    `maxTermSize`/`maxTermDepth`, which the shape-stable positions this function targets do
     *    not), so the position is a fixpoint from then on.
     * 3. **The remaining, genuinely shape-changing case is unchanged from before.** Once every
     *    position that would otherwise grow forever has become `Unknown`-shaped and stable, only
     *    top-level shape growth (e.g. a `Concat` gaining one more part per iteration) is left,
     *    which `growWiden` already handles exactly as the pre-existing size/depth-gated logic did
     *    (Star-introduction for the append/prepend shape, or the `Unknown` fallback otherwise).
     *
     * Together, every position converges after a bounded number of steps, which is exactly what
     * [WideningTerminationTest] checks empirically.
     */
    override fun widen(one: StringPattern, two: StringPattern): StringPattern {
        if (subsumes(one, two)) {
            return one
        }
        return alignAndWiden(one, two)
    }

    /**
     * Recurses into matching sub-structure of [one] and [two], widening (via [widenLeaves]) at
     * every position where either side is a [StringPattern.Unknown], instead of letting a later,
     * whole-term collapse silently replace accumulated history with a fresh exact recomputation.
     * See the termination sketch on [widen].
     */
    private fun alignAndWiden(one: StringPattern, two: StringPattern): StringPattern {
        if (one == two) {
            return one
        }
        if (one is StringPattern.Unknown || two is StringPattern.Unknown) {
            return widenLeaves(one, two)
        }
        val aligned =
            when {
                one is StringPattern.Concat &&
                    two is StringPattern.Concat &&
                    one.parts.size == two.parts.size ->
                    concat(one.parts.zip(two.parts).map { (a, b) -> alignAndWiden(a, b) })
                one is StringPattern.Star &&
                    two is StringPattern.Star &&
                    one.min == two.min &&
                    one.max == two.max ->
                    star(alignAndWiden(one.inner, two.inner), one.min, one.max)
                one is StringPattern.Union && two is StringPattern.Union -> alignUnions(one, two)
                else -> null
            }
        return aligned ?: growWiden(one, two)
    }

    /**
     * Pairs up alternatives of [one] and [two] that have the same top-level shape (see
     * [sameTopShape]) and widens each pair via [alignAndWiden]; alternatives that find no partner
     * are carried over unchanged (still sound: they only add to the represented language). Rebuilt
     * via the normalising [union] smart constructor.
     */
    private fun alignUnions(one: StringPattern.Union, two: StringPattern.Union): StringPattern {
        val remaining = two.alternatives.toMutableList()
        val widenedAlts = mutableListOf<StringPattern>()
        for (a in one.alternatives) {
            val matchIndex = remaining.indexOfFirst { sameTopShape(a, it) }
            if (matchIndex >= 0) {
                widenedAlts.add(alignAndWiden(a, remaining.removeAt(matchIndex)))
            } else {
                widenedAlts.add(a)
            }
        }
        widenedAlts.addAll(remaining)
        return union(widenedAlts)
    }

    /**
     * The genuine "term shape grew" case: [one] and [two] have no aligned sub-structure to widen
     * pairwise (either a top-level shape mismatch, e.g. `Concat` vs `Union`, or `alignAndWiden`'s
     * caller already tried and failed to align them). Falls back to the size/depth-gated join, the
     * Star-introduction detection for append/prepend-in-a-loop shapes, and finally [widenLeaves].
     */
    private fun growWiden(one: StringPattern, two: StringPattern): StringPattern {
        // Deliberately uses structuralNormalize, not normalize/lub: normalize() would silently
        // auto-collapse an oversized join to an ad hoc Unknown by itself, which would make this
        // check always pass trivially and this function would never reach the real widening logic
        // (Star-introduction or the explicit Unknown fallback below), breaking termination for
        // exactly the ascending chains this function exists to handle.
        val j = structuralNormalize(StringPattern.Union(setOf(one, two)), maxUnionSize)
        if (size(j) <= maxTermSize && depth(j) <= maxTermDepth) {
            return j
        }

        matchConcatPrefix(one, two)?.let { x ->
            val candidate = concat(one, star(x))
            if (size(candidate) <= maxTermSize && depth(candidate) <= maxTermDepth) {
                return candidate
            }
        }
        matchConcatSuffix(one, two)?.let { x ->
            val candidate = concat(star(x), one)
            if (size(candidate) <= maxTermSize && depth(candidate) <= maxTermDepth) {
                return candidate
            }
        }

        return widenLeaves(one, two)
    }

    /**
     * The terminating leaf-level widen: over-approximates both [one] and [two] as a single
     * [StringPattern.Unknown], widening `charSet`/`length` (rather than exact-recomputing them) so
     * that repeated calls at the same position are guaranteed to stabilise - see the termination
     * sketch on [widen].
     */
    private fun widenLeaves(one: StringPattern, two: StringPattern): StringPattern {
        val origin =
            (one as? StringPattern.Unknown)?.origin
                ?: (unknownOriginsOf(one) intersect unknownOriginsOf(two)).firstOrNull()
        return StringPattern.Unknown(
            origin = origin,
            reason = StringPattern.Reason.WIDENED,
            charSet = charSetOf(one) union charSetOf(two),
            length = lengthOf(one).widen(lengthOf(two)),
        )
    }
}

/**
 * `true` iff [a] and [b] have the same top-level [StringPattern] constructor (arity-compatible for
 * [StringPattern.Concat], min/max-compatible for [StringPattern.Star]), or either is a
 * [StringPattern.Unknown] - used by [StringLattice.alignUnions] to decide which alternatives to
 * pair up for pairwise widening rather than carrying over unchanged.
 */
private fun sameTopShape(a: StringPattern, b: StringPattern): Boolean =
    when {
        a is StringPattern.Unknown || b is StringPattern.Unknown -> true
        a is StringPattern.Bottom && b is StringPattern.Bottom -> true
        a is StringPattern.Const && b is StringPattern.Const -> true
        a is StringPattern.Concat && b is StringPattern.Concat -> a.parts.size == b.parts.size
        a is StringPattern.Union && b is StringPattern.Union -> true
        a is StringPattern.Star && b is StringPattern.Star -> a.min == b.min && a.max == b.max
        else -> false
    }

/** The top-level parts of a [StringPattern.Concat], or `[p]` itself if it is not one. */
private fun partsOf(p: StringPattern): List<StringPattern> =
    if (p is StringPattern.Concat) p.parts else listOf(p)

/**
 * If [two] structurally equals `Concat(one, x)` for some non-empty `x`, returns `x` as a single
 * [StringPattern] (already [concat]-normalised). Otherwise `null`.
 */
private fun matchConcatPrefix(one: StringPattern, two: StringPattern): StringPattern? {
    val oneParts = partsOf(one)
    val twoParts = partsOf(two)
    if (twoParts.size <= oneParts.size) return null
    if (twoParts.subList(0, oneParts.size) != oneParts) return null
    return concat(twoParts.subList(oneParts.size, twoParts.size))
}

/**
 * If [two] structurally equals `Concat(x, one)` for some non-empty `x`, returns `x` as a single
 * [StringPattern] (already [concat]-normalised). Otherwise `null`.
 */
private fun matchConcatSuffix(one: StringPattern, two: StringPattern): StringPattern? {
    val oneParts = partsOf(one)
    val twoParts = partsOf(two)
    if (twoParts.size <= oneParts.size) return null
    val tailStart = twoParts.size - oneParts.size
    if (twoParts.subList(tailStart, twoParts.size) != oneParts) return null
    return concat(twoParts.subList(0, tailStart))
}
