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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.graph.objectType
import kotlin.test.*
import kotlinx.coroutines.runBlocking

/**
 * Single-step unit tests for [StringLattice.widen]'s internal dispatch (`alignAndWiden`,
 * `alignUnions`, `growWiden`, `widenLeaves`), as opposed to [WideningTerminationTest], which checks
 * the emergent *termination* property across whole ascending chains. These are the branches flagged
 * as untested by coverage: the only call sites exercised by the end-to-end evaluator tests are
 * simple append/prepend-in-a-loop shapes, which never reach e.g. [StringLattice]'s
 * `Union`-alignment or the plain within-budget join path of `growWiden`.
 */
class StringLatticeWideningTest {
    private val lattice = StringLattice()

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun origin(): Node {
        lateinit var n: Node
        testFrontend(config()).build {
            singleTranslationUnit("test.cpp") { tu ->
                n = newLiteral("x", objectType("string"))
                tu.statements += listOf()
            }
        }
        return n
    }

    @Test
    fun testWidenShortCircuitsWhenOneAlreadySubsumesTwo() {
        val one =
            StringPattern.Unknown(charSet = CharSet.Any, reason = StringPattern.Reason.UNSUPPORTED)
        val two = const("anything")
        assertEquals(one, lattice.widen(one, two))
    }

    @Test
    fun testWidenOfIdenticalTermsIsNoOp() {
        val term = concat(const("a"), const("b"))
        assertEquals(term, lattice.widen(term, term))
    }

    @Test
    fun testWidenOfUnknownVsConstJoinsCharSetAndWidensLength() {
        val one =
            StringPattern.Unknown(
                charSet = charsOf('a'),
                length = LatticeInterval.Bounded(1, 1),
                reason = StringPattern.Reason.PARAMETER,
            )
        val two = const("bb")

        val widened = lattice.widen(one, two)

        assertIs<StringPattern.Unknown>(widened)
        assertTrue(charSetContains(widened.charSet, charsOf('a')))
        assertTrue(charSetContains(widened.charSet, charsOf('b')))
        assertTrue(subsumes(widened, one), "widen must over-approximate one: $widened")
        assertTrue(subsumes(widened, two), "widen must over-approximate two: $widened")
    }

    @Test
    fun testWidenOfUnknownPreservesItsOrigin() {
        val node = origin()
        val one = StringPattern.Unknown(origin = node, reason = StringPattern.Reason.PARAMETER)
        val two = const("z")

        val widened = lattice.widen(one, two)

        assertIs<StringPattern.Unknown>(widened)
        assertEquals(node, widened.origin)
    }

    @Test
    fun testWidenOfConcatSameArityRecursesPairwise() {
        val one =
            concat(
                const("a"),
                StringPattern.Unknown(
                    charSet = charsOf('x'),
                    reason = StringPattern.Reason.UNSUPPORTED,
                ),
            )
        val two =
            concat(
                const("a"),
                StringPattern.Unknown(
                    charSet = charsOf('y'),
                    reason = StringPattern.Reason.UNSUPPORTED,
                ),
            )

        val widened = lattice.widen(one, two)

        assertIs<StringPattern.Concat>(widened)
        assertEquals(
            const("a"),
            widened.parts[0],
            "the identical first part must be preserved as-is",
        )
        val second = widened.parts[1]
        assertIs<StringPattern.Unknown>(second)
        assertTrue(charSetContains(second.charSet, charsOf('x')))
        assertTrue(charSetContains(second.charSet, charsOf('y')))
    }

    @Test
    fun testWidenOfStarSameShapeRecursesIntoInner() {
        val one =
            star(
                StringPattern.Unknown(
                    charSet = charsOf('x'),
                    reason = StringPattern.Reason.UNSUPPORTED,
                ),
                min = 0,
                max = 3,
            )
        val two =
            star(
                StringPattern.Unknown(
                    charSet = charsOf('y'),
                    reason = StringPattern.Reason.UNSUPPORTED,
                ),
                min = 0,
                max = 3,
            )

        val widened = lattice.widen(one, two)

        assertIs<StringPattern.Star>(widened)
        assertEquals(0, widened.min)
        assertEquals(3, widened.max)
        assertTrue(charSetContains(charSetOf(widened.inner), charsOf('x')))
        assertTrue(charSetContains(charSetOf(widened.inner), charsOf('y')))
    }

    @Test
    fun testWidenOfUnionsAlignsMatchingShapesPairwise() {
        val one =
            union(
                const("foo"),
                StringPattern.Unknown(
                    charSet = charsOf('a'),
                    reason = StringPattern.Reason.UNSUPPORTED,
                ),
            )
        val two =
            union(
                const("foo"),
                StringPattern.Unknown(
                    charSet = charsOf('b'),
                    reason = StringPattern.Reason.UNSUPPORTED,
                ),
            )

        val widened = lattice.widen(one, two)

        assertIs<StringPattern.Union>(widened)
        assertTrue(widened.alternatives.contains(const("foo")))
        val unknownAlt =
            widened.alternatives.filterIsInstance<StringPattern.Unknown>().singleOrNull()
        assertNotNull(
            unknownAlt,
            "expected the two Unknown alternatives to be widened into one: $widened",
        )
        assertTrue(charSetContains(unknownAlt.charSet, charsOf('a')))
        assertTrue(charSetContains(unknownAlt.charSet, charsOf('b')))
    }

    @Test
    fun testWidenOfUnionsCarriesOverUnmatchedAlternativesUnchanged() {
        val q = const("q")
        val one = union(const("a"), star(q, min = 1, max = 3))
        val two = union(const("a"), star(q, min = 2, max = 4), const("extra"))

        val widened = lattice.widen(one, two)

        assertEquals(
            union(const("a"), star(q, min = 1, max = 3), star(q, min = 2, max = 4), const("extra")),
            widened,
        )
    }

    @Test
    fun testWidenWithinBudgetReturnsPlainJoin() {
        // Two unrelated small Consts: no aligned sub-structure (Const vs Const isn't a recursable
        // shape), so this falls into growWiden, and the plain join is small enough to return as-is
        // without needing Star-introduction or a leaf collapse.
        val one = const("foo")
        val two = const("bar")

        val widened = lattice.widen(one, two)

        assertEquals(union(const("foo"), const("bar")), widened)
    }

    /**
     * `concat(one, const("a"))` alone would just merge into a bigger `Const` (adjacent `Const`s
     * merge, see [StringPatternTest.testConcatFlattening]), never reaching a shape `growWiden` can
     * recognise. Mirroring [WideningTerminationTest]'s loop fixtures, we first `lub` that growth
     * step in (as a real fixpoint iteration would) - this factors into `Concat(one, Union(..))` -
     * *then* `widen` against the original `one`, which is the shape `matchConcatPrefix` is meant to
     * detect. Bounds are sized so the plain join (too big) is rejected but the Star-introduced
     * candidate (smaller) is accepted - see the size/depth arithmetic in the task notes.
     */
    @Test
    fun testWidenDetectsAppendShapeAndIntroducesStar() = runBlocking {
        val tight = StringLattice(maxTermSize = 6, maxTermDepth = 4, maxUnionSize = 16)
        val one = const("a")
        val two = tight.lub(one, concat(one, const("a")))

        val widened = tight.widen(one, two)

        assertTrue(
            widened is StringPattern.Concat && widened.parts.any { it is StringPattern.Star },
            "expected an append-shape widen to introduce a Star: $widened",
        )
        assertTrue(
            charSetContains(charSetOf(widened), charsOf('a')),
            "the over-approximation must still admit 'a': $widened",
        )
    }

    @Test
    fun testWidenDetectsPrependShapeAndIntroducesStar() = runBlocking {
        val tight = StringLattice(maxTermSize = 6, maxTermDepth = 4, maxUnionSize = 16)
        val one = const("a")
        val two = tight.lub(one, concat(const("x"), one))

        val widened = tight.widen(one, two)

        assertTrue(
            widened is StringPattern.Concat && widened.parts.any { it is StringPattern.Star },
            "expected a prepend-shape widen to introduce a Star: $widened",
        )
        assertTrue(
            charSetContains(charSetOf(widened), charsOf('a')),
            "the over-approximation must still admit 'a': $widened",
        )
    }

    @Test
    fun testWidenFallsBackToWidenLeavesWhenShapesIncompatibleAndOversized() {
        // Tight bounds and genuinely incompatible shapes (Const vs. a Union of unrelated Concats)
        // leave growWiden no aligned structure and no append/prepend shape to detect, so it must
        // collapse to a single Unknown via widenLeaves.
        val tight = StringLattice(maxTermSize = 1, maxTermDepth = 1, maxUnionSize = 1)
        val one = const("a")
        val two = union(concat(const("x"), const("y")), concat(const("p"), const("q")))

        val widened = tight.widen(one, two)

        assertIs<StringPattern.Unknown>(widened)
        assertTrue(subsumes(widened, one), "widen must over-approximate one: $widened")
        assertTrue(subsumes(widened, two), "widen must over-approximate two: $widened")
    }

    /**
     * `growWiden` tries [de.fraunhofer.aisec.cpg.analysis.string.matchConcatPrefix] *before*
     * checking whether the resulting Star-introduced candidate actually fits the size/depth
     * budget - a match that is found but still too big must fall through to
     * [de.fraunhofer.aisec.cpg.analysis.string.matchConcatSuffix] (which fails here, since [two]
     * was built by appending, not prepending) and finally to `widenLeaves`, rather than returning
     * the oversized candidate anyway. [one]/[two] are built with a generous lattice so [two] keeps
     * its full `Concat(Const, Union)` shape (see [testWidenDetectsAppendShapeAndIntroducesStar]);
     * only the `widen` call itself uses a tight lattice, so the plain join `j` *and* the Star
     * candidate are both rejected by size.
     */
    @Test
    fun testGrowWidenPrefixCandidateStillOversizedFallsThroughToWidenLeaves() = runBlocking {
        val generous = StringLattice()
        val one = const("a")
        val two = generous.lub(one, concat(one, const("a")))

        val tight = StringLattice(maxTermSize = 5, maxTermDepth = 4, maxUnionSize = 16)
        val widened = tight.widen(one, two)

        assertIs<StringPattern.Unknown>(widened)
        assertTrue(charSetContains(widened.charSet, charsOf('a')))
    }

    /** The mirror image of the prefix case above, for the prepend shape. */
    @Test
    fun testGrowWidenSuffixCandidateStillOversizedFallsThroughToWidenLeaves() = runBlocking {
        val generous = StringLattice()
        val one = const("a")
        val two = generous.lub(one, concat(const("x"), one))

        val tight = StringLattice(maxTermSize = 5, maxTermDepth = 4, maxUnionSize = 16)
        val widened = tight.widen(one, two)

        assertIs<StringPattern.Unknown>(widened)
        assertTrue(charSetContains(widened.charSet, charsOf('a')))
    }

    /**
     * `alignUnions`' pairing (via `sameTopShape`) must also match same-arity [StringPattern.Concat]
     * alternatives, not just `Const`/`Unknown`/`Star` ones - otherwise two `Concat`-shaped
     * alternatives that should be widened pairwise would instead be carried over unchanged (or
     * mismatched against an unrelated alternative), permanently diverging on every further widen
     * round of a real ascending chain.
     */
    @Test
    fun testWidenOfUnionsMatchesSameArityConcatAlternatives() {
        val one =
            union(
                concat(
                    const("a"),
                    StringPattern.Unknown(
                        charSet = charsOf('x'),
                        reason = StringPattern.Reason.UNSUPPORTED,
                    ),
                ),
                const("z"),
            )
        val two =
            union(
                concat(
                    const("a"),
                    StringPattern.Unknown(
                        charSet = charsOf('y'),
                        reason = StringPattern.Reason.UNSUPPORTED,
                    ),
                ),
                const("w"),
            )

        val widened = lattice.widen(one, two)

        assertIs<StringPattern.Union>(widened)
        val concatAlt = widened.alternatives.filterIsInstance<StringPattern.Concat>().singleOrNull()
        assertNotNull(
            concatAlt,
            "expected the two same-arity Concats to be widened into one: $widened",
        )
        assertEquals(const("a"), concatAlt.parts[0])
        val widenedUnknown = concatAlt.parts[1]
        assertIs<StringPattern.Unknown>(widenedUnknown)
        assertTrue(charSetContains(widenedUnknown.charSet, charsOf('x')))
        assertTrue(charSetContains(widenedUnknown.charSet, charsOf('y')))
    }
}
