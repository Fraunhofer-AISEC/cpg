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
     *   if one is Unknown: return Unknown(..., charSet = one.charSet union charSet(two),
     *                                     length = one.length.widen(length(two)))
     *   j = lub(one, two)
     *   if size(j) <= maxTermSize && depth(j) <= maxTermDepth: return j
     *   if two matches Concat(one, x) structurally: return Concat(one, Star(x))
     *   if two matches Concat(x, one) structurally: return Concat(Star(x), one)
     *   return Unknown(origin = commonOrigin(one, two), reason = WIDENED,
     *                  charSet = charSet(one) union charSet(two), length = length(one).widen(length(two)))
     * ```
     *
     * The `one is Unknown` branch is not in the original design-doc pseudocode but is required for
     * termination: `two` is itself typically produced by a plain (non-widening) [lub]/[normalize]
     * call, which has its *own* independent size-based auto-collapse (`normalize`'s step 8). That
     * collapse computes a fresh, exact `charSet`/`length` from whatever term it happens to see,
     * with no memory of earlier collapses - so on its own it is only non-decreasing, never jumps to
     * a fixpoint. If `one` is already an `Unknown` (the sentinel that we have already given up on
     * exact structure once), the *only* sound way to guarantee this converges is to always widen
     * its `length`/`charSet` against `two`'s, unconditionally - `size(j) <= maxTermSize` would
     * otherwise almost always hold trivially in this state (an `Unknown` union anything is tiny),
     * and we would never reach [LatticeInterval.widen]'s infinity-jump. See
     * [WideningTerminationTest] for the ascending chain this specifically guards against.
     *
     * **Termination sketch.** An ascending chain `t0 <= t1 <= t2 <= ...` is produced by repeated
     * calls `t(i+1) = widen(t(i), s(i))` for some sequence of "one more step" values `s(i)`.
     * Consider what happens once a call reaches the size/depth bound (every unbounded chain must,
     * since [lub] alone strictly grows a term that has not converged):
     * 1. **Star-introduction case.** `t(i+1) = Concat(t(i), Star(x))` (or the symmetric prefix
     *    case) is only reached when `two` is structurally `Concat(one, x)`, i.e. it re-parses as
     *    `t(i)` followed by an extra `x`. `t(i+1)` replaces the *growing* part `x` (which increases
     *    in size every step of an unbounded chain, or the chain would already have converged under
     *    plain `lub`) with `Star(x)`, whose language covers zero-or-more repeats of `x` and
     *    therefore already covers any future `Concat(x, x, ..., x)` suffix without needing to grow
     *    again. So a later call `widen(t(i+1), s(i+1))` where `s(i+1)` again grows the same tail
     *    either (a) is already subsumed by `t(i+1)` and `lub` alone returns something of bounded
     *    size (the `x` part does not need to change), or (b) hits the size/depth guard on the
     *    *candidate* `Concat(one, Star(x))` itself - which we check explicitly before returning
     *    it - and falls through to the `Unknown` case below instead of ever producing an
     *    ever-growing `Star(Star(...))` nesting. Either way, the term stops growing after at most
     *    one more widening step.
     * 2. **Unknown fallback.** Once we reach `Unknown`, every later call takes the `one is Unknown`
     *    branch above, which only touches its two components: [CharSet] has finite height (`Empty
     *    <= Chars <= Any`, and `Chars` can only grow by adding characters up to
     *    [CharSet.MAX_EXPLICIT] before collapsing to `Any`), so repeated `union`s stabilise in at
     *    most `MAX_EXPLICIT + 2` steps.
     *    [de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.widen] is already proven to
     *    terminate (it only ever pushes a bound to `NEGATIVE_INFINITE`/`INFINITE`, each bound can
     *    do that at most once). Once both components stop changing, `Unknown(...)` is structurally
     *    equal to the previous result - and the `one already subsumes two` check at the top
     *    short-circuits to `one` even before recomputing it, i.e. a fixpoint of `widen`.
     *
     * Both branches therefore turn an unbounded ascending chain into one that stabilises after a
     * bounded number of steps, which is exactly what [WideningTerminationTest] checks empirically.
     */
    override fun widen(one: StringPattern, two: StringPattern): StringPattern {
        if (subsumes(one, two)) {
            return one
        }

        if (one is StringPattern.Unknown) {
            val sharedOrigins = unknownOriginsOf(one) intersect unknownOriginsOf(two)
            return StringPattern.Unknown(
                origin = one.origin ?: sharedOrigins.firstOrNull(),
                reason = StringPattern.Reason.WIDENED,
                charSet = one.charSet union charSetOf(two),
                length = one.length.widen(lengthOf(two)),
            )
        }

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

        val sharedOrigins = unknownOriginsOf(one) intersect unknownOriginsOf(two)
        return StringPattern.Unknown(
            origin = sharedOrigins.firstOrNull(),
            reason = StringPattern.Reason.WIDENED,
            charSet = charSetOf(one) union charSetOf(two),
            length = lengthOf(one).widen(lengthOf(two)),
        )
    }
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
