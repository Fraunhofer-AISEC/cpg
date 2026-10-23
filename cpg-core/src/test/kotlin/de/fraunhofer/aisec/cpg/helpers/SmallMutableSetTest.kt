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
package de.fraunhofer.aisec.cpg.helpers

import kotlin.test.*

/**
 * Exercises [SmallMutableSet] across its inline-storage (0/1/2 elements) and overflow (3+ elements)
 * regimes, including the transitions between them and iterator-based removal at each stage.
 */
class SmallMutableSetTest {
    @Test
    fun testEmpty() {
        val set = smallMutableSetOf<String>()
        assertEquals(0, set.size)
        assertTrue(set.isEmpty())
        assertFalse(set.contains("a"))
        assertTrue(set.containsAll(emptyList()))
        assertFalse(set.iterator().hasNext())
    }

    @Test
    fun testAddInlineAndDeduplicate() {
        val set = smallMutableSetOf<String>()
        assertTrue(set.add("a"))
        assertTrue(set.add("b"))
        assertFalse(set.add("a")) // duplicate in inline slot 0
        assertFalse(set.add("b")) // duplicate in inline slot 1
        assertEquals(2, set.size)
        assertTrue(set.containsAll(listOf("a", "b")))
        assertEquals(setOf("a", "b"), set.toSet())
    }

    @Test
    fun testOverflow() {
        val set = smallMutableSetOf<Int>()
        set.addAll(listOf(1, 2, 3, 4, 5))
        assertEquals(5, set.size)
        for (i in 1..5) assertTrue(i in set)
        assertFalse(set.add(3)) // duplicate in overflow
        assertEquals(setOf(1, 2, 3, 4, 5), set.toSet())
    }

    @Test
    fun testRemoveFromInlineAndOverflow() {
        val set = smallMutableSetOf<Int>()
        set.addAll(listOf(1, 2, 3, 4))

        // Remove from an inline slot.
        assertTrue(set.remove(1))
        assertFalse(set.contains(1))
        assertEquals(3, set.size)

        // Remove from the overflow.
        assertTrue(set.remove(4))
        assertEquals(2, set.size)

        // Removing something absent.
        assertFalse(set.remove(42))

        // Draining the overflow entirely still leaves the set consistent.
        assertTrue(set.remove(3))
        assertEquals(setOf(2), set.toSet())
    }

    @Test
    fun testRemoveAllRetainAllClear() {
        val set = smallMutableSetOf<Int>()
        set.addAll(listOf(1, 2, 3, 4))

        assertTrue(set.removeAll(listOf(2, 4, 99)))
        assertEquals(setOf(1, 3), set.toSet())

        set.addAll(listOf(5, 6))
        assertTrue(set.retainAll(listOf(3, 5)))
        assertEquals(setOf(3, 5), set.toSet())

        set.clear()
        assertTrue(set.isEmpty())
        assertEquals(0, set.size)
    }

    @Test
    fun testIteratorRemoveAtEveryStage() {
        // Remove the first inline element via the iterator.
        val a = smallMutableSetOf<Int>().apply { addAll(listOf(1, 2, 3)) }
        var it = a.iterator()
        it.next()
        it.remove()
        assertEquals(2, a.size)

        // Remove all elements (spanning inline + overflow) via the iterator.
        val b = smallMutableSetOf<Int>().apply { addAll(listOf(1, 2, 3, 4)) }
        it = b.iterator()
        while (it.hasNext()) {
            it.next()
            it.remove()
        }
        assertTrue(b.isEmpty())

        // remove() before next() must fail.
        assertFailsWith<IllegalStateException> { smallMutableSetOf<Int>().iterator().remove() }
    }

    @Test
    fun testEqualsAndHashCode() {
        val set = smallMutableSetOf<Int>().apply { addAll(listOf(1, 2, 3)) }
        val reference = hashSetOf(1, 2, 3)
        assertEquals(reference, set)
        assertEquals(set, reference)
        assertEquals(reference.hashCode(), set.hashCode())
        assertNotEquals(smallMutableSetOf<Int>().apply { addAll(listOf(1, 2)) }, set)
    }
}
