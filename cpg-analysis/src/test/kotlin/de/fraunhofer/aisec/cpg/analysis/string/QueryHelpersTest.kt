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

/**
 * Unit tests for the pure, [Node]-free term operations in `QueryHelpers.kt`. These complement the
 * end-to-end [StringEvaluatorTest]/handler tests, which only ever evaluate simple expressions and
 * therefore never exercise the give-up/budget paths of [StringPattern.enumerate] or multi-origin
 * [StringPattern.unknownOrigins] directly at the term level.
 */
class QueryHelpersTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    /** Two distinct, real [Node]s to use as [StringPattern.Unknown.origin]s. */
    private fun twoOrigins(): Pair<Node, Node> {
        lateinit var a: Node
        lateinit var b: Node
        testFrontend(config()).build {
            singleTranslationUnit("test.cpp") { tu ->
                a = newLiteral("a", objectType("string"))
                b = newLiteral("b", objectType("string"))
                tu.statements += listOf()
            }
        }
        return a to b
    }

    // --- asConstantOrNull ---

    @Test
    fun testAsConstantOrNullForConst() {
        assertEquals("foo", const("foo").asConstantOrNull())
    }

    @Test
    fun testAsConstantOrNullNullForMultiValuedPattern() {
        assertNull(union(const("foo"), const("bar")).asConstantOrNull())
    }

    @Test
    fun testAsConstantOrNullNullForUnknown() {
        assertNull(
            StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED).asConstantOrNull()
        )
    }

    // --- enumerate ---

    @Test
    fun testEnumerateBottomIsEmpty() {
        assertEquals(emptySet(), StringPattern.Bottom.enumerate(10))
    }

    @Test
    fun testEnumerateUnknownWithEmptyCharSetIsEmpty() {
        val p =
            StringPattern.Unknown(
                charSet = CharSet.Empty,
                reason = StringPattern.Reason.UNSUPPORTED,
            )
        assertEquals(emptySet(), p.enumerate(10))
    }

    @Test
    fun testEnumerateUnknownWithZeroLengthIsEmptyString() {
        val p =
            StringPattern.Unknown(
                length = LatticeInterval.Bounded(0, 0),
                reason = StringPattern.Reason.UNSUPPORTED,
            )
        assertEquals(setOf(""), p.enumerate(10))
    }

    @Test
    fun testEnumerateGenericUnknownGivesUp() {
        val p = StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED)
        assertNull(p.enumerate(10))
    }

    @Test
    fun testEnumerateUnboundedStarGivesUp() {
        assertNull(star(const("a")).enumerate(1000))
    }

    @Test
    fun testEnumerateConcatGivesUpWhenOverLimit() {
        val p = concat(union(const("a"), const("b")), union(const("c"), const("d")))
        assertEquals(setOf("ac", "ad", "bc", "bd"), p.enumerate(10))
        assertNull(p.enumerate(3))
    }

    @Test
    fun testEnumerateUnionGivesUpWhenOverLimit() {
        val p = union((0 until 5).map { const(it.toString()) })
        assertEquals(setOf("0", "1", "2", "3", "4"), p.enumerate(10))
        assertNull(p.enumerate(3))
    }

    @Test
    fun testEnumerateBoundedStarEnumeratesAllRepetitions() {
        val p = star(const("a"), min = 1, max = 3)
        assertEquals(setOf("a", "aa", "aaa"), p.enumerate(10))
    }

    // --- mayMatch ---

    @Test
    fun testMayMatchTrueWhenSomeEnumeratedValueMatches() {
        assertTrue(union(const("foo"), const("bar")).mayMatch(Regex("foo")))
    }

    @Test
    fun testMayMatchFalseWhenNoEnumeratedValueMatches() {
        assertFalse(union(const("foo"), const("bar")).mayMatch(Regex("baz")))
    }

    @Test
    fun testMayMatchConservativelyTrueWhenGivingUp() {
        val p = StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED)
        assertTrue(p.mayMatch(Regex("anything")))
    }

    // --- mustMatch / mustMatchWithGiveUp ---

    @Test
    fun testMustMatchTrueWhenEveryEnumeratedValueMatches() {
        val (result, gaveUp) =
            union(const("foo"), const("foobar")).mustMatchWithGiveUp(Regex("foo.*"))
        assertTrue(result)
        assertFalse(gaveUp)
    }

    @Test
    fun testMustMatchGenuineFalseWhenSomeEnumeratedValueDoesNotMatch() {
        val (result, gaveUp) = union(const("foo"), const("bar")).mustMatchWithGiveUp(Regex("foo"))
        assertFalse(result)
        assertFalse(gaveUp, "a fully-enumerated non-match must not be a give-up")
    }

    @Test
    fun testMustMatchFalseOnEmptyLanguage() {
        // Bottom's language is empty, so "every string matches" is vacuously true in general, but
        // mustMatch conservatively requires at least one string to call it a match.
        val (result, gaveUp) = StringPattern.Bottom.mustMatchWithGiveUp(Regex(".*"))
        assertFalse(result)
        assertFalse(gaveUp)
    }

    @Test
    fun testMustMatchGivesUpWhenUnenumerable() {
        val (result, gaveUp) =
            StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED)
                .mustMatchWithGiveUp(Regex(".*"))
        assertFalse(result)
        assertTrue(gaveUp)
    }

    @Test
    fun testMustMatchDelegatesToMustMatchWithGiveUp() {
        assertTrue(const("foo").mustMatch(Regex("foo")))
        assertFalse(const("bar").mustMatch(Regex("foo")))
    }

    // --- constantPrefix / constantSuffix ---

    @Test
    fun testConstantPrefixOfConcatStopsAtFirstUnknownPart() {
        val p =
            concat(
                const("foo"),
                StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED),
                const("bar"),
            )
        assertEquals("foo", p.constantPrefix())
    }

    @Test
    fun testConstantSuffixOfConcatStopsAtFirstUnknownPartFromTheEnd() {
        val p =
            concat(
                const("foo"),
                StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED),
                const("bar"),
            )
        assertEquals("bar", p.constantSuffix())
    }

    @Test
    fun testConstantPrefixOfUnionIsCommonPrefix() {
        // Built via the internal Union constructor, bypassing normalize()'s own common-prefix
        // factoring (see StringPatternTest.testPrefixFactoring), so this exercises the Union branch
        // of constantPrefix() itself rather than normalize()'s unrelated factoring logic.
        val p = StringPattern.Union(setOf(const("fooAAA"), const("fooBBB")))
        assertEquals("foo", p.constantPrefix())
    }

    @Test
    fun testConstantSuffixOfUnionIsCommonSuffix() {
        val p = StringPattern.Union(setOf(const("xbar"), const("ybar")))
        assertEquals("bar", p.constantSuffix())
    }

    @Test
    fun testConstantPrefixOfStarRequiresMinAtLeastOne() {
        assertEquals("ab", star(const("ab"), min = 1).constantPrefix())
        assertEquals("", star(const("ab"), min = 0, max = 3).constantPrefix())
    }

    @Test
    fun testConstantPrefixAndSuffixOfUnknownAreEmpty() {
        val p = StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED)
        assertEquals("", p.constantPrefix())
        assertEquals("", p.constantSuffix())
    }

    // --- mustContain ---

    @Test
    fun testMustContainUnionIsIntersection() {
        val p = union(concat(const("a"), const("x")), concat(const("b"), const("x")))
        assertEquals(setOf("x"), p.mustContain())
    }

    @Test
    fun testMustContainConcatIsUnionOfParts() {
        // An Unknown part is interposed so the two Consts aren't merged by normalizeConcat, keeping
        // this a genuine multi-part Concat rather than collapsing to a single Const("ab").
        val p =
            concat(
                const("a"),
                StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED),
                const("b"),
            )
        assertEquals(setOf("a", "b"), p.mustContain())
    }

    @Test
    fun testMustContainStarRequiresMinAtLeastOne() {
        assertEquals(setOf("x"), star(const("x"), min = 1).mustContain())
        assertEquals(emptySet(), star(const("x"), min = 0, max = 3).mustContain())
    }

    @Test
    fun testMustContainIgnoresEmptyConst() {
        assertEquals(emptySet(), const("").mustContain())
    }

    // --- isFullyKnown ---

    @Test
    fun testIsFullyKnownFalseForUnknown() {
        assertFalse(StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED).isFullyKnown)
    }

    @Test
    fun testIsFullyKnownTrueForConstsAndFixedRepeatStar() {
        assertTrue(star(const("a"), min = 3, max = 3).isFullyKnown)
    }

    @Test
    fun testIsFullyKnownFalseForVariableRepeatStarEvenWithKnownInner() {
        assertFalse(star(const("a"), min = 0, max = 3).isFullyKnown)
    }

    @Test
    fun testIsFullyKnownPropagatesThroughConcatAndUnion() {
        assertTrue(concat(const("a"), union(const("b"), const("c"))).isFullyKnown)
        assertFalse(
            concat(const("a"), StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED))
                .isFullyKnown
        )
    }

    // --- unknownOrigins ---

    @Test
    fun testUnknownOriginsEmptyForConstsAndBottom() {
        assertEquals(emptySet(), const("foo").unknownOrigins)
        assertEquals(emptySet(), StringPattern.Bottom.unknownOrigins)
    }

    @Test
    fun testUnknownOriginsCollectsSingleOrigin() {
        val (origin, _) = twoOrigins()
        val p = StringPattern.Unknown(origin = origin, reason = StringPattern.Reason.PARAMETER)
        assertEquals(setOf(origin), p.unknownOrigins)
    }

    @Test
    fun testUnknownOriginsCollectsAcrossConcatUnionAndStar() {
        val (originA, originB) = twoOrigins()
        val p =
            concat(
                star(
                    StringPattern.Unknown(
                        origin = originA,
                        reason = StringPattern.Reason.PARAMETER,
                    ),
                    min = 1,
                ),
                union(
                    const("x"),
                    StringPattern.Unknown(
                        origin = originB,
                        reason = StringPattern.Reason.EXTERNAL_INPUT,
                    ),
                ),
            )
        assertEquals(setOf(originA, originB), p.unknownOrigins)
    }

    @Test
    fun testUnknownOriginsDropsNullOrigins() {
        val p = StringPattern.Unknown(origin = null, reason = StringPattern.Reason.UNSUPPORTED)
        assertEquals(emptySet(), p.unknownOrigins)
    }
}
