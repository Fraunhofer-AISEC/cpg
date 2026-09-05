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

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Phase 6 property-based soundness testing for [StringLattice].
 *
 * No property-based testing library (kotest-property, jqwik, QuickCheck, ...) is on the classpath
 * anywhere in this project (checked build.gradle.kts / version catalogs), and the design doc's D4
 * ("no new third-party dependencies") rules out adding one just for this. This test hand-rolls
 * random generation instead, in the same style as the ad hoc 300-random-pair `widen` soundness
 * check used during adversarial review of [StringLattice.widen] - formalised here as a permanent,
 * named, fixed-seed test.
 *
 * The seed is fixed (not `Random()`) so a failure is deterministic and reproducible.
 *
 * [StringLatticeTest] already covers the lattice laws (`lub` commutative/idempotent/absorbs bottom,
 * `glb` likewise, `compare` consistent with `lub`/`glb`, `duplicate` identity) on a small, fixed,
 * hand-picked set of terms. This class extends that coverage to a much larger, randomly generated
 * population of terms, and adds the two properties the design doc calls out specifically: `lub`
 * never shrinks the language (checked both via concrete enumeration and via `subsumes`), and
 * idempotence of `lub(a, a)`.
 */
class StringLatticePropertyTest {
    private val lattice = StringLattice()
    private val seed = 424242L
    private val pairCount = 250
    private val enumerationLimit = 200

    /** Bounded-depth/size random [StringPattern] generator mixing every case of the domain. */
    private class PatternGenerator(private val random: Random) {
        private val alphabet = listOf("a", "b", "c", "")

        fun next(maxDepth: Int = 3): StringPattern = generate(maxDepth)

        private fun generate(depth: Int): StringPattern {
            // At depth 0, only leaves (Const/Unknown) - keeps terms finite and normalisation cheap.
            val choice = if (depth <= 0) random.nextInt(2) else random.nextInt(5)
            return when (choice) {
                0 -> const(alphabet[random.nextInt(alphabet.size)])
                1 -> randomUnknown()
                2 ->
                    concat(
                        List(1 + random.nextInt(2)) { generate(depth - 1) },
                        maxTermSize = 64,
                        maxTermDepth = 16,
                        maxUnionSize = 16,
                    )
                3 ->
                    union(
                        List(2 + random.nextInt(2)) { generate(depth - 1) },
                        maxTermSize = 64,
                        maxTermDepth = 16,
                        maxUnionSize = 16,
                    )
                else -> {
                    val min = random.nextInt(2)
                    val max = if (random.nextBoolean()) null else min + random.nextInt(3)
                    star(
                        generate(depth - 1),
                        min = min,
                        max = max,
                        maxTermSize = 64,
                        maxTermDepth = 16,
                        maxUnionSize = 16,
                    )
                }
            }
        }

        private fun randomUnknown(): StringPattern =
            when (random.nextInt(3)) {
                0 -> StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED)
                1 ->
                    StringPattern.Unknown(
                        charSet = charsOf('a', 'b', 'c'),
                        reason = StringPattern.Reason.PARAMETER,
                    )
                else -> StringPattern.Unknown(reason = StringPattern.Reason.WIDENED)
            }
    }

    private fun randomPairs(): List<Pair<StringPattern, StringPattern>> {
        val generator = PatternGenerator(Random(seed))
        return List(pairCount) { generator.next() to generator.next() }
    }

    /**
     * `lub` never shrinks the language: every concretely enumerable string of [a] or [b] must be
     * matched by `lub(a, b)`. When a side cannot be enumerated (an unbounded `Star` or an
     * `Unknown`), it is skipped for this concrete check - covered instead by the `subsumes`-based
     * check below, which does not need enumeration.
     */
    @Test
    fun testLubNeverShrinksLanguageConcretely() = runBlocking {
        for ((a, b) in randomPairs()) {
            val joined = lattice.lub(a, b)
            val regex = joined.toRegex()
            a.enumerate(enumerationLimit)?.forEach { s ->
                assertTrue(
                    regex.matches(s),
                    "lub($a, $b) = $joined does not match \"$s\", which is in the language of $a",
                )
            }
            b.enumerate(enumerationLimit)?.forEach { s ->
                assertTrue(
                    regex.matches(s),
                    "lub($a, $b) = $joined does not match \"$s\", which is in the language of $b",
                )
            }
        }
    }

    /**
     * The same soundness property stated via the `subsumes` relation instead of concrete
     * enumeration - exercises a different code path (no enumeration budget involved) and covers the
     * cases (unbounded `Star`, `Unknown`) the concrete check above must skip.
     */
    @Test
    fun testLubSubsumesBothOperands() = runBlocking {
        for ((a, b) in randomPairs()) {
            val joined = lattice.lub(a, b)
            assertTrue(subsumes(joined, a), "lub($a, $b) = $joined must subsume $a")
            assertTrue(subsumes(joined, b), "lub($a, $b) = $joined must subsume $b")
        }
    }

    /** `lub(a, a)` is idempotent: it structurally equals `a` after normalisation. */
    @Test
    fun testLubIdempotentOnRandomTerms() = runBlocking {
        for ((a, _) in randomPairs()) {
            assertEquals(a, lattice.lub(a, a), "lub($a, $a) should be idempotent")
        }
    }

    /** `lub` is commutative on randomly generated terms, not just the small hand-picked set. */
    @Test
    fun testLubCommutativeOnRandomTerms() = runBlocking {
        for ((a, b) in randomPairs()) {
            assertEquals(lattice.lub(a, b), lattice.lub(b, a), "lub($a, $b) should be commutative")
        }
    }

    /**
     * Regression test for a real bug this class's random generation found: two
     * [StringPattern.Unknown]s with the same [CharSet]/length (both default to
     * [CharSet.Any]/[de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.TOP]) but
     * different [StringPattern.Reason] render identically (both `.*`) while being structurally
     * unequal. Before the fix to `patternOrder` in `Normalize.kt`, [StringLattice.lub]'s
     * `Union`-alternative dedup broke this rendering tie using the iteration order of the
     * underlying `Set<StringPattern>`, which differs between `setOf(one, two)` and `setOf(two,
     * one)` - making `lub(a, b)` and `lub(b, a)` keep a different one of the two tied alternatives.
     * Not a soundness bug (either kept alternative is a sound over-approximation, since they
     * mutually [subsumes] each other), but a genuine violation of the lub-commutativity lattice
     * law. `patternOrder` now breaks such ties deterministically (by `reason` first, then a
     * same-object-reference-based fallback), so this must hold structurally, not just "both sound".
     */
    @Test
    fun testLubCommutativeForTiedUnknownReasons() = runBlocking {
        val a = StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED)
        val b = StringPattern.Unknown(reason = StringPattern.Reason.WIDENED)
        assertEquals(
            lattice.lub(a, b),
            lattice.lub(b, a),
            "lub(a, b) and lub(b, a) must be structurally identical for two Unknowns that render " +
                "identically but differ only in `reason`",
        )
    }

    /**
     * `compare`/`lub`/`glb` internal consistency, restated from [StringLatticeTest] on the random
     * population: whenever `subsumes(lub(a,b), a)` holds and the reverse does not, `compare` must
     * report `a` as `LESSER` than the join (`compare` is derived from `subsumes`, see
     * [StringPattern.compare]).
     */
    @Test
    fun testCompareConsistentWithSubsumesOnRandomTerms() = runBlocking {
        for ((a, b) in randomPairs()) {
            val joined = lattice.lub(a, b)
            val cmp = lattice.compare(a, joined)
            val aSubsumesJoined = subsumes(a, joined)
            val joinedSubsumesA = subsumes(joined, a)
            when {
                a == joined ->
                    assertEquals(
                        de.fraunhofer.aisec.cpg.helpers.functional.Order.EQUAL,
                        cmp,
                        "compare($a, $joined) should be EQUAL for structurally equal terms",
                    )
                joinedSubsumesA && !aSubsumesJoined ->
                    assertEquals(
                        de.fraunhofer.aisec.cpg.helpers.functional.Order.LESSER,
                        cmp,
                        "compare($a, $joined): $a is strictly more specific than its join, " +
                            "expected LESSER",
                    )
                else -> {
                    // Both-subsume (structurally different but same-language terms the
                    // normalisation invariant cannot tell apart) or neither-subsumes: compare must
                    // not claim a direction it cannot prove.
                    assertTrue(
                        cmp != de.fraunhofer.aisec.cpg.helpers.functional.Order.GREATER,
                        "compare($a, $joined) must not claim $a is GREATER than its own join",
                    )
                }
            }
        }
    }
}
