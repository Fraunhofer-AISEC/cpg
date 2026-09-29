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

class RenderingTest {
    @Test
    fun testConstRoundTrip() {
        assertTrue(const("foo").toRegex().matches("foo"))
        assertFalse(const("foo").toRegex().matches("foobar"))
        assertFalse(const("foo").toRegex().matches("fo"))
    }

    @Test
    fun testConstEscaping() {
        val pattern = const("a.b*c")
        assertTrue(pattern.toRegex().matches("a.b*c"))
        assertFalse(pattern.toRegex().matches("aXbbbbc"))
    }

    @Test
    fun testUnionMatchesEitherAlternative() {
        val pattern = union(const("a"), const("b"))
        assertTrue(pattern.toRegex().matches("a"))
        assertTrue(pattern.toRegex().matches("b"))
        assertFalse(pattern.toRegex().matches("c"))
    }

    @Test
    fun testConcatJuxtaposition() {
        val pattern = concat(const("foo"), const("bar"))
        assertTrue(pattern.toRegex().matches("foobar"))
        assertFalse(pattern.toRegex().matches("foo"))
    }

    @Test
    fun testStarMatchesRepetition() {
        val pattern = star(const("ab"))
        assertTrue(pattern.toRegex().matches(""))
        assertTrue(pattern.toRegex().matches("ab"))
        assertTrue(pattern.toRegex().matches("ababab"))
        assertFalse(pattern.toRegex().matches("aba"))
    }

    @Test
    fun testStarWithMinimum() {
        val pattern = star(const("a"), min = 1)
        assertFalse(pattern.toRegex().matches(""))
        assertTrue(pattern.toRegex().matches("a"))
        assertTrue(pattern.toRegex().matches("aaa"))
    }

    @Test
    fun testUnknownAnyMatchesArbitraryStrings() {
        val pattern =
            StringPattern.Unknown(charSet = CharSet.Any, reason = StringPattern.Reason.UNSUPPORTED)
        assertTrue(pattern.toRegex().matches(""))
        assertTrue(pattern.toRegex().matches("anything at all 123"))
    }

    @Test
    fun testUnknownExplicitCharSet() {
        val pattern =
            StringPattern.Unknown(
                charSet = charsOf('a', 'b'),
                reason = StringPattern.Reason.UNSUPPORTED,
            )
        assertTrue(pattern.toRegex().matches("ababba"))
        assertFalse(pattern.toRegex().matches("abc"))
    }

    @Test
    fun testToStringDelegatesToRegexString() {
        val pattern = const("foo")
        assertEquals(pattern.toRegexString(), pattern.toString())
    }
}
