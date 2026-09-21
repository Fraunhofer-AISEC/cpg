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
package de.fraunhofer.aisec.cpg.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NameCacheTest {
    @Test
    fun testInternReturnsSameInstanceForEqualNames() {
        val a = NameCache.intern(Name("x"))
        val b = NameCache.intern(Name("x"))

        assertSame(a, b)
    }

    @Test
    fun testInternDistinguishesDifferentParents() {
        val a = NameCache.intern(Name("x", Name("outer1")))
        val b = NameCache.intern(Name("x", Name("outer2")))

        assertEquals("outer1.x", a.toString())
        assertEquals("outer2.x", b.toString())
    }

    @Test
    fun testInternedValueEqualsUninterned() {
        val interned = NameCache.intern(Name("x"))
        val plain = Name("x")

        assertEquals(interned, plain)
    }
}
