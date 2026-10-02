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

class IdentityStackTest {
    /** Distinct but equal objects, so identity and equals() give different answers. */
    private data class Item(val id: Int)

    @Test
    fun testContainsUsesIdentity() {
        val stack = IdentityStack<Item>()
        val a = Item(1)
        stack.push(a)

        assertTrue(a in stack)
        assertFalse(Item(1) in stack, "an equal but distinct object must not count as present")
    }

    @Test
    fun testPopTop() {
        val stack = IdentityStack<Item>()
        val a = Item(1)
        val b = Item(2)
        stack.push(a)
        stack.push(b)

        assertSame(b, stack.top())
        assertSame(b, stack.popTop())
        assertFalse(b in stack)
        assertSame(a, stack.top())
    }

    @Test
    fun testPopThroughReturnsSuffixInPopOrder() {
        val stack = IdentityStack<Item>()
        val items = List(4) { Item(it) }
        items.forEach { stack.push(it) }

        val popped = stack.popThrough(items[1])

        assertEquals(listOf(items[3], items[2], items[1]), popped)
        assertTrue(popped.all { it !in stack })
        assertTrue(items[0] in stack)
        assertSame(items[0], stack.top())
    }

    @Test
    fun testPopThroughRejectsElementNotOnStack() {
        val stack = IdentityStack<Item>()
        stack.push(Item(1))

        assertFailsWith<IllegalArgumentException> { stack.popThrough(Item(1)) }
    }
}
