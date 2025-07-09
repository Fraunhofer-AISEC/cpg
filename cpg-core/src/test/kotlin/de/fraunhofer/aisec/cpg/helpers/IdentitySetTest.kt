/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import kotlin.test.*

class IdentitySetTest {
    @Test
    fun testDoubleAdd() {
        val set = IdentitySet<Int>()
        assertTrue(set.add(1))
        assertFalse(set.add(1))
        assertTrue(set.add(2))

        assertEquals(2, set.size)

        assertTrue(set.addAll(setOf(1, 2, 3)))
        assertFalse(set.addAll(setOf(1, 2)))
    }

    @Test
    fun testContains() {
        val set = IdentitySet<Int>(2)
        set.add(1)
        set.add(2)

        assertTrue(1 in set)
        assertTrue(2 in set)
        assertFalse(3 in set)

        assertTrue(set.contains(1))
        assertTrue(set.contains(2))
        assertFalse(set.contains(3))
    }

    @Test
    fun testEquals() {
        val set = IdentitySet<Int>()
        set.add(1)
        set.add(2)

        assertEquals(set, identitySetOf(1, 2))
        assertNotEquals(set, identitySetOf(1, 2, 3))
        assertFalse(set == identitySetOf("1", "2", 3))
        assertNotEquals(set, identitySetOf(1))

        assertEquals(identitySetOf(1, 2), set)
        assertNotEquals(identitySetOf(1, 2, 3), set)
        assertNotEquals(identitySetOf(1), set)
        assertFalse(set == identitySetOf("1", "2", 3))
        assertFalse(set == listOf(1, 2))
    }

    @Test
    fun testIsEmpty() {
        val set = IdentitySet<Int>()
        assertTrue(set.isEmpty())
        assertFalse(set.isNotEmpty())
        set.add(1)
        assertFalse(set.isEmpty())
        assertTrue(set.isNotEmpty())
    }

    @Test
    fun testRemove() {
        val set = IdentitySet<Int>()
        set.add(1)
        set.add(2)
        set.add(3)
        set.add(4)
        set.add(5)
        set.add(6)

        assertEquals(setOf(1, 2, 3, 4, 5, 6), set)
        set.remove(2)
        assertEquals(setOf(1, 3, 4, 5, 6), set)
        set.remove(2)
        assertEquals(setOf(1, 3, 4, 5, 6), set)

        set.removeAll(setOf(1, 3))
        assertEquals(setOf(4, 5, 6), set)

        set.removeAll(setOf(3, 4))
        assertEquals(setOf(5, 6), set)

        set.clear()
        assertTrue(set.isEmpty())
    }

    @Test
    fun testToSortedList() {
        val set = IdentitySet<Int>()
        set.add(1)
        set.add(2)
        set.add(3)
        set.add(4)
        set.add(5)
        set.add(6)

        assertEquals(listOf(1, 2, 3, 4, 5, 6), set.toSortedList())
    }
}
