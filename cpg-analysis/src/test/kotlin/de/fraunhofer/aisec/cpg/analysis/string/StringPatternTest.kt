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

class StringPatternTest {
    @Test
    fun testNormalizationOrderIndependence() {
        // Same set of alternatives built in different construction order normalises identically.
        val a = union(const("foo"), const("bar"), const("baz"))
        val b = union(const("baz"), const("foo"), const("bar"))
        val c = union(const("bar"), const("baz"), const("foo"))
        assertEquals(a, b)
        assertEquals(b, c)
    }

    @Test
    fun testConcatFlattening() {
        val nested = concat(concat(const("a"), const("b")), const("c"))
        // Adjacent consts must merge, and no nested Concat may survive.
        assertEquals(const("abc"), nested)
        if (nested is StringPattern.Concat) {
            fail("Expected a fully merged Const, but got a Concat: $nested")
        }
    }

    @Test
    fun testConcatNoNestedConcatSurvives() {
        val inner =
            concat(StringPattern.Unknown(reason = StringPattern.Reason.UNSUPPORTED), const("x"))
        val outer = concat(const("a"), inner, const("b"))
        assertTrue(outer is StringPattern.Concat)
        val parts = (outer as StringPattern.Concat).parts
        assertTrue(
            parts.none { it is StringPattern.Concat },
            "No part should itself be a Concat: $parts",
        )
    }

    @Test
    fun testUnionNoNestedUnionSurvives() {
        val inner = union(const("a"), const("b"))
        val outer = union(inner, const("c"))
        assertTrue(outer is StringPattern.Union)
        val alts = (outer as StringPattern.Union).alternatives
        assertTrue(
            alts.none { it is StringPattern.Union },
            "No alternative should itself be a Union: $alts",
        )
        assertEquals(setOf(const("a"), const("b"), const("c")), alts)
    }

    @Test
    fun testNoBottomLeaksExceptAsWholeResult() {
        assertEquals(StringPattern.Bottom, concat(const("a"), StringPattern.Bottom, const("b")))
        assertEquals(StringPattern.Bottom, union())
        assertEquals(const("a"), union(const("a"), StringPattern.Bottom))
    }

    @Test
    fun testSingletonCollapse() {
        assertEquals(const("a"), concat(const("a")))
        assertEquals(const("a"), union(const("a")))
        assertEquals(const("a"), union(const("a"), const("a")))
    }

    @Test
    fun testEmptyConstDroppedFromConcat() {
        assertEquals(const("ab"), concat(const(""), const("a"), const(""), const("b"), const("")))
    }

    @Test
    fun testUnionSubsumptionDrop() {
        val specific = const("ab")
        val general =
            StringPattern.Unknown(
                charSet = charsOf('a', 'b'),
                reason = StringPattern.Reason.UNSUPPORTED,
            )
        // The Unknown already admits any string over {a,b} of any length, so it subsumes "ab".
        val result = union(specific, general)
        assertEquals(general, result)
    }

    @Test
    fun testPrefixFactoring() {
        val result = union(const("ab"), const("ac"))
        assertEquals(concat(const("a"), union(const("b"), const("c"))), result)
    }

    @Test
    fun testStarOverEmptyCollapses() {
        assertEquals(const(""), star(const("")))
        assertEquals(const(""), star(StringPattern.Bottom, min = 0))
        assertEquals(StringPattern.Bottom, star(StringPattern.Bottom, min = 1))
    }

    @Test
    fun testCollapseOnOversizedTerm() {
        // Single-character alternatives never share a common prefix/suffix, so this cannot be
        // factored down - it must collapse via the maxUnionSize bound instead.
        val many = (0 until 40).map { const(('A' + it).toString()) }
        val result = union(many)
        assertTrue(
            result is StringPattern.Unknown,
            "Expected an oversized union to collapse: $result",
        )
    }
}
