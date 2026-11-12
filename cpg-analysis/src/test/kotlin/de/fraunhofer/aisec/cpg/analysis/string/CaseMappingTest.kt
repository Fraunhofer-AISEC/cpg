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
 * Unit tests for [mapConstLeaves]/[mapCharSet], the shared case-mapping distribution logic used by
 * every language handler's `upper`/`lower` case conversion. The handler tests only ever exercise a
 * [StringPattern.Const] or a simple [StringPattern.Unknown] receiver, so the recursive
 * [StringPattern.Concat]/[StringPattern.Union]/[StringPattern.Star] cases, and [mapCharSet]'s
 * [CharSet.Empty]/[CharSet.Any] passthroughs, are otherwise untested.
 */
class CaseMappingTest {
    private val upper: (String) -> String = String::uppercase

    // --- mapConstLeaves ---

    @Test
    fun testMapConstLeavesOnBottomReturnsBottom() {
        assertEquals(StringPattern.Bottom, mapConstLeaves(StringPattern.Bottom, upper))
    }

    @Test
    fun testMapConstLeavesOnConst() {
        assertEquals(const("FOO"), mapConstLeaves(const("foo"), upper))
    }

    @Test
    fun testMapConstLeavesOnConcatMapsEveryLeaf() {
        val p =
            concat(
                const("foo"),
                StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED),
                const("bar"),
            )
        val result = mapConstLeaves(p, upper)
        assertIs<StringPattern.Concat>(result)
        assertEquals(const("FOO"), result.parts.first())
        assertEquals(const("BAR"), result.parts.last())
    }

    @Test
    fun testMapConstLeavesOnUnionMapsEveryAlternative() {
        val p = union(const("foo"), const("bar"))
        assertEquals(union(const("FOO"), const("BAR")), mapConstLeaves(p, upper))
    }

    @Test
    fun testMapConstLeavesOnStarMapsInnerAndPreservesRepetition() {
        val p = star(const("ab"), min = 1, max = 3)
        val result = mapConstLeaves(p, upper)
        assertIs<StringPattern.Star>(result)
        assertEquals(const("AB"), result.inner)
        assertEquals(1, result.min)
        assertEquals(3, result.max)
    }

    @Test
    fun testMapConstLeavesOnUnknownMapsCharSet() {
        val p =
            StringPattern.Unknown(
                charSet = charsOf('a', 'b'),
                reason = StringPattern.Reason.UNSUPPORTED,
            )
        val result = mapConstLeaves(p, upper)
        assertIs<StringPattern.Unknown>(result)
        assertTrue(charSetContains(result.charSet, charsOf('A')))
        assertTrue(charSetContains(result.charSet, charsOf('B')))
    }

    // --- mapCharSet ---

    @Test
    fun testMapCharSetEmptyStaysEmpty() {
        assertEquals(CharSet.Empty, mapCharSet(CharSet.Empty, upper))
    }

    @Test
    fun testMapCharSetAnyStaysAny() {
        assertEquals(CharSet.Any, mapCharSet(CharSet.Any, upper))
    }

    @Test
    fun testMapCharSetMapsEachCharacter() {
        assertEquals(charsOf('A', 'B'), mapCharSet(charsOf('a', 'b'), upper))
    }

    @Test
    fun testMapCharSetNeverDropsACharacterFromAMultiCharacterMapping() {
        // A mapping where a single character expands to several characters (e.g. German sharp s
        // uppercasing to "SS") must contribute *every* resulting character, not just one of them -
        // dropping any would under-approximate the resulting CharSet, violating this domain's
        // soundness invariant (see mapCharSet's KDoc).
        val expand: (String) -> String = { if (it == "a") "xy" else it }
        assertEquals(charsOf('x', 'y'), mapCharSet(charsOf('a'), expand))
    }
}
