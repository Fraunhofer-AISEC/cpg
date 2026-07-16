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
package de.fraunhofer.aisec.cpg.helpers.functional

import kotlin.test.*
import kotlinx.coroutines.runBlocking

/** Tests [PersistentMapLattice] using a [PowersetLattice] of strings as the inner lattice. */
class PersistentLatticesTest {

    private val lattice =
        PersistentMapLattice<String, PowersetLattice.Element<String>>(PowersetLattice())

    private fun element(vararg entries: Pair<String, PowersetLattice.Element<String>>) =
        PersistentMapLattice.Element(*entries)

    @Test
    fun testElementBasics() {
        val a = element("k1" to PowersetLattice.Element("a"))
        assertEquals(1, a.size)
        assertEquals(setOf("k1"), a.keys)
        assertNotNull(a["k1"])
        assertNull(a["missing"])
        assertEquals(1, a.entries.size)
        assertEquals(1, a.iterator().asSequence().count())
    }

    @Test
    fun testPutIsImmutable() {
        val a = element("k1" to PowersetLattice.Element("a"))
        val b = a.put("k2", PowersetLattice.Element("b"))

        // The original is unchanged (structural sharing / copy-on-write).
        assertEquals(1, a.size)
        assertEquals(setOf("k1"), a.keys)

        assertEquals(2, b.size)
        assertEquals(setOf("k1", "k2"), b.keys)
    }

    @Test
    fun testCompare() {
        val empty = lattice.bottom
        val one = element("k1" to PowersetLattice.Element("a"))
        val oneCopy = element("k1" to PowersetLattice.Element("a"))

        assertEquals(Order.EQUAL, lattice.compare(one, oneCopy))
        assertEquals(Order.LESSER, lattice.compare(empty, one))
        assertEquals(Order.GREATER, lattice.compare(one, empty))

        // A greater key set (superset).
        val two =
            element("k1" to PowersetLattice.Element("a"), "k2" to PowersetLattice.Element("c"))
        assertEquals(Order.GREATER, lattice.compare(two, one))
        assertEquals(Order.LESSER, lattice.compare(one, two))

        // Same key, disjoint values -> incomparable.
        val other = element("k1" to PowersetLattice.Element("b"))
        assertEquals(Order.UNEQUAL, lattice.compare(one, other))

        assertEquals(one, oneCopy)
    }

    @Test
    fun testLub() = runBlocking {
        val one = element("k1" to PowersetLattice.Element("a"))
        val two =
            element("k1" to PowersetLattice.Element("b"), "k2" to PowersetLattice.Element("c"))

        val merged = lattice.lub(one, two)
        assertEquals(setOf("k1", "k2"), merged.keys)
        // k1 = lub({a}, {b}) = {a, b}
        assertEquals(2, merged["k1"]?.size)
        assertEquals(1, merged["k2"]?.size)

        // lub is >= both operands.
        assertEquals(Order.GREATER, lattice.compare(merged, one))
        assertEquals(Order.GREATER, lattice.compare(merged, two))

        // The operands were not mutated.
        assertEquals(1, one.size)
        assertEquals(2, two.size)
    }

    @Test
    fun testGlb() = runBlocking {
        val one =
            element("k1" to PowersetLattice.Element("a", "x"), "k2" to PowersetLattice.Element("c"))
        val two =
            element("k1" to PowersetLattice.Element("a"), "k3" to PowersetLattice.Element("d"))

        val glb = lattice.glb(one, two)
        // Only the shared key survives.
        assertEquals(setOf("k1"), glb.keys)
        // k1 = glb({a, x}, {a}) = {a}
        assertEquals(1, glb["k1"]?.size)
    }

    @Test
    fun testElementConstructorsAndHelpers() {
        val fromMap = PersistentMapLattice.Element(mapOf("k1" to PowersetLattice.Element("a")))
        val fromPairs = PersistentMapLattice.Element(listOf("k1" to PowersetLattice.Element("a")))
        assertEquals(fromMap, fromPairs)
        assertEquals(fromMap.hashCode(), fromMap.hashCode())

        val two =
            element("k1" to PowersetLattice.Element("a"), "k2" to PowersetLattice.Element("b"))
        assertEquals(listOf("k1"), two.filter { (k, _) -> k == "k1" }.map { it.first })
        assertEquals(2, two.entries.size)
    }

    @Test
    fun testCompareIncomparableMixed() {
        // On k1 `left` is smaller, on k2 `left` is larger -> the two are incomparable.
        val left =
            element("k1" to PowersetLattice.Element("a"), "k2" to PowersetLattice.Element("b", "c"))
        val right =
            element("k1" to PowersetLattice.Element("a", "x"), "k2" to PowersetLattice.Element("b"))
        assertEquals(Order.UNEQUAL, lattice.compare(left, right))
        assertEquals(Order.UNEQUAL, lattice.compare(right, left))
    }

    @Test
    fun testDuplicateAndBottom() {
        assertTrue(lattice.bottom.keys.isEmpty())

        val one = element("k1" to PowersetLattice.Element("a"))
        val dup = lattice.duplicate(one)
        assertEquals(Order.EQUAL, lattice.compare(one, dup))
        assertEquals(one, dup)
    }
}
