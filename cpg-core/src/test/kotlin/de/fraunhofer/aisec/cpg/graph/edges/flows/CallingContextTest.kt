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
package de.fraunhofer.aisec.cpg.graph.edges.flows

import de.fraunhofer.aisec.cpg.graph.expressions.Call
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CallingContextTest {
    private val a = Call()
    private val b = Call()
    private val c = Call()

    @Test
    fun testEqualContextsHaveEqualStableHashes() {
        val first = CallingContextIn(mutableListOf(a, b))
        val second = CallingContextIn(mutableListOf(a, b))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        // Asking again returns the same hash
        assertEquals(first.hashCode(), first.hashCode())

        assertNotEquals(first, CallingContextIn(mutableListOf(b, a)))
        // The direction is part of the context
        assertNotEquals<CallingContext>(first, CallingContextOut(mutableListOf(a, b)))
    }

    @Test
    fun testHashFollowsModificationOfCalls() {
        val context = CallingContextOut(mutableListOf(a))
        // Make sure a hash is cached before we modify the list
        val before = context.hashCode()

        context.calls.add(b)
        val other = CallingContextOut(mutableListOf(a, b))
        assertEquals(other, context)
        assertEquals(other.hashCode(), context.hashCode())
        assertNotEquals(before, context.hashCode())

        context.calls[1] = c
        assertEquals(CallingContextOut(mutableListOf(a, c)).hashCode(), context.hashCode())

        context.calls.removeAt(1)
        assertEquals(CallingContextOut(mutableListOf(a)).hashCode(), context.hashCode())
        assertEquals(before, context.hashCode())
    }

    @Test
    fun testContextKeepsItsOwnCopy() {
        val list = mutableListOf(a, b)
        val context = CallingContextIn(list)
        val hash = context.hashCode()

        list.add(c)
        assertEquals(listOf(a, b), context.calls)
        assertEquals(hash, context.hashCode())
        assertTrue(context in setOf(CallingContextIn(mutableListOf(a, b))))
    }
}
