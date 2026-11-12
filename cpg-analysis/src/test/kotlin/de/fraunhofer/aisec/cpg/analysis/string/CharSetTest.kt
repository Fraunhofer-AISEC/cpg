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

import kotlin.test.*

/**
 * Unit tests for the small, finite-height [CharSet] lattice (`Empty <= Chars <= Any`) used to
 * over-approximate the possible characters of a [StringPattern]. Exercised only indirectly (and
 * rarely at the boundary) by the end-to-end handler tests, so the collapsing behaviour around
 * [CharSet.MAX_EXPLICIT] and the [CharSet.Any] identity cases are covered here directly.
 */
class CharSetTest {

    // --- charsOf ---

    @Test
    fun testCharsOfEmptySetIsEmpty() {
        assertEquals(CharSet.Empty, charsOf(emptySet()))
    }

    @Test
    fun testCharsOfSmallSetIsChars() {
        assertEquals(CharSet.Chars(setOf('a', 'b')), charsOf(setOf('a', 'b')))
        assertEquals(CharSet.Chars(setOf('a', 'b')), charsOf('a', 'b'))
    }

    @Test
    fun testCharsOfCollapsesToAnyAboveMaxExplicit() {
        val many = (0 until CharSet.MAX_EXPLICIT + 1).map { ('a' + it) }.toSet()
        assertEquals(CharSet.Any, charsOf(many))
    }

    @Test
    fun testCharsOfAtExactlyMaxExplicitStaysChars() {
        val exact = (0 until CharSet.MAX_EXPLICIT).map { ('a' + it) }.toSet()
        assertEquals(CharSet.Chars(exact), charsOf(exact))
    }

    // --- contains ---

    @Test
    fun testEmptyContainsNoCharacter() {
        assertFalse('a' in CharSet.Empty)
    }

    @Test
    fun testCharsContainsOnlyItsMembers() {
        val cs = charsOf('a', 'b')
        assertTrue('a' in cs)
        assertFalse('c' in cs)
    }

    @Test
    fun testAnyContainsEveryCharacter() {
        assertTrue('x' in CharSet.Any)
    }

    // --- union ---

    @Test
    fun testUnionWithAnyIsAbsorbing() {
        assertEquals(CharSet.Any, CharSet.Any union charsOf('a'))
        assertEquals(CharSet.Any, charsOf('a') union CharSet.Any)
    }

    @Test
    fun testUnionWithEmptyIsIdentity() {
        val cs = charsOf('a', 'b')
        assertEquals(cs, CharSet.Empty union cs)
        assertEquals(cs, cs union CharSet.Empty)
    }

    @Test
    fun testUnionOfCharsMergesSets() {
        assertEquals(charsOf('a', 'b', 'c'), charsOf('a', 'b') union charsOf('b', 'c'))
    }

    @Test
    fun testUnionOfCharsCollapsesToAnyWhenResultExceedsMax() {
        val first = (0 until CharSet.MAX_EXPLICIT).map { ('a' + it) }.toSet()
        val second = setOf('\u0000')
        assertEquals(CharSet.Any, charsOf(first) union charsOf(second))
    }

    // --- intersect ---

    @Test
    fun testIntersectWithEmptyIsAbsorbing() {
        assertEquals(CharSet.Empty, CharSet.Empty intersect charsOf('a'))
        assertEquals(CharSet.Empty, charsOf('a') intersect CharSet.Empty)
    }

    @Test
    fun testIntersectWithAnyIsIdentity() {
        val cs = charsOf('a', 'b')
        assertEquals(cs, CharSet.Any intersect cs)
        assertEquals(cs, cs intersect CharSet.Any)
    }

    @Test
    fun testIntersectOfCharsKeepsCommonElements() {
        assertEquals(charsOf('b'), charsOf('a', 'b') intersect charsOf('b', 'c'))
    }

    @Test
    fun testIntersectOfDisjointCharsIsEmpty() {
        assertEquals(CharSet.Empty, charsOf('a') intersect charsOf('b'))
    }

    // --- lattice properties ---

    @Test
    fun testUnionAndIntersectAreCommutative() {
        val sets = listOf(CharSet.Empty, CharSet.Any, charsOf('a', 'b'), charsOf('b', 'c'))
        for (a in sets) {
            for (b in sets) {
                assertEquals(a union b, b union a, "union($a, $b) should be commutative")
                assertEquals(
                    a intersect b,
                    b intersect a,
                    "intersect($a, $b) should be commutative",
                )
            }
        }
    }

    @Test
    fun testUnionAndIntersectAreIdempotent() {
        val sets = listOf(CharSet.Empty, CharSet.Any, charsOf('a', 'b'))
        for (a in sets) {
            assertEquals(a, a union a)
            assertEquals(a, a intersect a)
        }
    }
}
