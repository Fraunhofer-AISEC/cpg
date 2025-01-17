/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLatticeT
import de.fraunhofer.aisec.cpg.helpers.functional.TripleLattice
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLattice
import de.fraunhofer.aisec.cpg.helpers.functional.emptyMapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.emptyPowersetLattice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BasicLatticesTest {
    @Test
    fun testPowersetLattice() {
        val emptyLattice1: PowersetLatticeT<Node> = emptyPowersetLattice<Node>()
        val emptyLattice2 = emptyPowersetLattice<Node>()
        assertEquals(0, emptyLattice1.compareTo(emptyLattice2))
        assertEquals(emptyLattice1, emptyLattice2)
        assertNotSame(emptyLattice1.hashCode(), emptyLattice1.hashCode())

        val blaLattice1 = PowersetLattice<String>(setOf("bla"))
        val blaLattice2 = PowersetLattice<String>(setOf("bla"))
        assertEquals(0, blaLattice1.compareTo(blaLattice2))
        assertEquals(blaLattice1, blaLattice2)

        val blaFooLattice = PowersetLattice<String>(setOf("bla", "foo"))
        assertEquals(1, blaFooLattice.compareTo(blaLattice1))
        assertNotEquals(blaFooLattice, blaLattice1)

        assertEquals(-1, blaLattice1.compareTo(blaFooLattice))
        assertNotEquals(blaLattice1, blaFooLattice)

        val blaBlubLattice = PowersetLattice<String>(setOf("bla", "blub"))
        assertEquals(-1, blaFooLattice.compareTo(blaBlubLattice))
        assertNotEquals(blaFooLattice, blaBlubLattice)

        assertEquals(-1, blaBlubLattice.compareTo(blaFooLattice))
        assertNotEquals(blaBlubLattice, blaFooLattice)

        assertNotSame(emptyLattice1, emptyLattice1.duplicate())
        assertNotSame(emptyLattice1.elements, emptyLattice1.duplicate().elements)
        assertNotSame(blaLattice1, blaLattice1.duplicate())
        assertNotSame(blaLattice1.elements, blaLattice1.duplicate().elements)
        assertNotSame(blaFooLattice, blaFooLattice.duplicate())
        assertNotSame(blaFooLattice.elements, blaFooLattice.duplicate().elements)
        assertNotSame(blaBlubLattice, blaBlubLattice.duplicate())
        assertNotSame(blaBlubLattice.elements, blaBlubLattice.duplicate().elements)

        val emptyLubEmpty = emptyLattice1.lub(emptyLattice1)
        assertIs<PowersetLattice<Node>>(emptyLubEmpty)
        assertNotSame(emptyLattice1, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(0, emptyLattice1.compareTo(emptyLubEmpty))
        assertNotSame(emptyLattice2, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(0, emptyLattice2.compareTo(emptyLubEmpty))

        val emptyLattice3 = emptyPowersetLattice<String>()
        val emptyLubBla = emptyLattice3.lub(blaLattice1)
        assertIs<PowersetLattice<String>>(emptyLubBla)
        assertNotSame(emptyLattice3, emptyLubBla)
        assertNotEquals(emptyLattice3, emptyLubBla)
        assertEquals(-1, emptyLattice3.compareTo(emptyLubBla))
        assertNotSame(blaLattice1, emptyLubBla)
        assertEquals(blaLattice1, emptyLubBla)
        assertEquals(0, blaLattice1.compareTo(emptyLubBla))

        val blaFooBlub = blaFooLattice.lub(blaBlubLattice)
        assertIs<PowersetLattice<String>>(blaFooBlub)
        assertNotSame(emptyLattice3, blaFooBlub)
        assertNotEquals(emptyLattice3, blaFooBlub)
        assertEquals(-1, emptyLattice3.compareTo(blaFooBlub))
        assertNotSame(blaLattice1, blaFooBlub)
        assertNotEquals(blaLattice1, blaFooBlub)
        assertEquals(-1, blaLattice1.compareTo(blaFooBlub))
        assertNotSame(blaFooLattice, blaFooBlub)
        assertNotEquals(blaFooLattice, blaFooBlub)
        assertEquals(-1, blaFooLattice.compareTo(blaFooBlub))
        assertNotSame(blaBlubLattice, blaFooBlub)
        assertNotEquals(blaBlubLattice, blaFooBlub)
        assertEquals(-1, blaBlubLattice.compareTo(blaFooBlub))
        assertEquals(setOf("bla", "blub", "foo"), blaFooBlub.elements)
    }

    @Test
    fun testMapLattice() {
        val emptyLattice1 = emptyMapLattice<String, Set<String>>()
        val emptyLattice2 = emptyMapLattice<String, Set<String>>()
        assertEquals(0, emptyLattice1.compareTo(emptyLattice2))
        assertEquals(emptyLattice1, emptyLattice2)
        assertNotSame(emptyLattice1.hashCode(), emptyLattice1.hashCode())

        val aBlaLattice1 =
            MapLattice<String, Set<String>>(mapOf("a" to PowersetLattice(setOf("bla"))))
        val aBlaLattice2 =
            MapLattice<String, Set<String>>(mapOf("a" to PowersetLattice(setOf("bla"))))
        assertEquals(0, aBlaLattice1.compareTo(aBlaLattice2))
        assertEquals(aBlaLattice1, aBlaLattice2)
        assertNotSame(aBlaLattice1, aBlaLattice2)

        val aBlaFooLattice =
            MapLattice<String, Set<String>>(mapOf("a" to PowersetLattice(setOf("bla", "foo"))))
        assertEquals(1, aBlaFooLattice.compareTo(aBlaLattice1))
        assertNotEquals(aBlaFooLattice, aBlaLattice1)
        assertEquals(-1, aBlaLattice1.compareTo(aBlaFooLattice))
        assertNotEquals(aBlaLattice1, aBlaFooLattice)

        val aBlaBFooLattice =
            MapLattice<String, Set<String>>(
                mapOf("a" to PowersetLattice(setOf("bla")), "b" to PowersetLattice(setOf("foo")))
            )
        assertEquals(1, aBlaBFooLattice.compareTo(aBlaLattice1))
        assertNotEquals(aBlaBFooLattice, aBlaLattice1)
        assertEquals(-1, aBlaLattice1.compareTo(aBlaBFooLattice))
        assertNotEquals(aBlaLattice1, aBlaBFooLattice)

        // Duplicates are equal but not identical. Same for the elements.
        val emptyDuplicate = emptyLattice1.duplicate()
        assertNotSame(emptyLattice1, emptyDuplicate)
        // assertNotSame(emptyLattice1.elements, emptyDuplicate.elements) // Somehow, the empty set
        // is the same
        assertEquals(emptyLattice1, emptyDuplicate)
        val aBlaLatticeDuplicate = aBlaLattice1.duplicate()
        assertNotSame(aBlaLattice1, aBlaLatticeDuplicate)
        assertNotSame(aBlaLattice1.elements, aBlaLatticeDuplicate.elements)
        assertEquals(aBlaLattice1, aBlaLatticeDuplicate)
        val aBlaFooLatticeDuplicate = aBlaFooLattice.duplicate()
        assertNotSame(aBlaFooLattice, aBlaFooLatticeDuplicate)
        assertNotSame(aBlaBFooLattice.elements, aBlaFooLatticeDuplicate.elements)
        assertEquals(aBlaFooLattice, aBlaFooLatticeDuplicate)
        val aBlaBFooLatticeDuplicate = aBlaBFooLattice.duplicate()
        assertNotSame(aBlaFooLattice, aBlaBFooLatticeDuplicate)
        assertNotSame(aBlaBFooLattice.elements, aBlaBFooLatticeDuplicate.elements)
        assertEquals(aBlaBFooLattice, aBlaBFooLatticeDuplicate)

        val emptyLubEmpty = emptyLattice1.lub(emptyLattice1)
        assertIs<MapLattice<String, Set<String>>>(emptyLubEmpty)
        assertNotSame(emptyLattice1, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(0, emptyLattice1.compareTo(emptyLubEmpty))
        assertNotSame(emptyLattice2, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(0, emptyLattice2.compareTo(emptyLubEmpty))

        val emptyLubABla = emptyLattice1.lub(aBlaLattice1)
        assertIs<MapLattice<String, Set<String>>>(emptyLubABla)
        assertNotSame(emptyLattice1, emptyLubABla)
        assertNotEquals(emptyLattice1, emptyLubABla)
        assertEquals(-1, emptyLattice1.compareTo(emptyLubABla))
        assertNotSame(aBlaLattice1, emptyLubABla)
        assertEquals(aBlaLattice1, emptyLubABla)
        assertEquals(0, aBlaLattice1.compareTo(emptyLubABla))

        val aFooBBlaLattice =
            MapLattice<String, Set<String>>(
                mapOf("a" to PowersetLattice(setOf("foo")), "b" to PowersetLattice(setOf("bla")))
            )
        val aBlaFooBBla = aBlaFooLattice.lub(aFooBBlaLattice) // a to {"foo", "bla"}, b to {"bla"}
        assertIs<MapLattice<String, Set<String>>>(aBlaFooBBla)
        assertNotSame(emptyLattice1, aBlaFooBBla)
        assertNotEquals(emptyLattice1, aBlaFooBBla)
        assertEquals(-1, emptyLattice1.compareTo(aBlaFooBBla))
        assertEquals(1, aBlaFooBBla.compareTo(emptyLattice1))
        assertNotSame(aBlaLattice1, aBlaFooBBla)
        assertNotEquals(aBlaLattice1, aBlaFooBBla)
        assertEquals(-1, aBlaLattice1.compareTo(aBlaFooBBla))
        assertEquals(1, aBlaFooBBla.compareTo(aBlaLattice1))
        assertNotSame(aBlaFooLattice, aBlaFooBBla)
        assertNotEquals(aBlaFooLattice, aBlaFooBBla)
        assertEquals(-1, aBlaFooLattice.compareTo(aBlaFooBBla))
        assertEquals(1, aBlaFooBBla.compareTo(aBlaFooLattice))
        assertNotSame(aBlaBFooLattice, aBlaFooBBla)
        assertNotEquals(aBlaBFooLattice, aBlaFooBBla)
        assertEquals(-1, aBlaBFooLattice.compareTo(aBlaFooBBla))
        assertEquals(-1, aBlaFooBBla.compareTo(aBlaBFooLattice))
        assertNotSame(aFooBBlaLattice, aBlaFooBBla)
        assertNotEquals(aFooBBlaLattice, aBlaFooBBla)
        assertEquals(-1, aFooBBlaLattice.compareTo(aBlaFooBBla))
        assertEquals(1, aBlaFooBBla.compareTo(aFooBBlaLattice))
        assertEquals(setOf("a", "b"), aBlaFooBBla.elements.keys)
        assertEquals(setOf("bla", "foo"), aBlaFooBBla.elements["a"]?.elements)
        assertEquals(setOf("bla"), aBlaFooBBla.elements["b"]?.elements)

        assertFalse(aBlaFooBBla == emptyLattice1) // Wrong elements
        assertTrue(emptyLattice1 == emptyLattice2) // This is equal
        assertTrue(aBlaLattice1 == aBlaLattice2) // This is equal
        assertFalse(aBlaFooBBla == aBlaFooBBla.elements["a"]) // Wrong types
        assertFalse(aBlaFooBBla.elements["a"] == aBlaFooBBla) // Wrong types
    }

    @Test
    fun testPairLattice() {
        val emptyEmpty =
            TupleLattice<Set<String>, Set<String>>(
                Pair(emptyPowersetLattice<String>(), emptyPowersetLattice<String>())
            )
        val emptyBla =
            TupleLattice<Set<String>, Set<String>>(
                Pair(emptyPowersetLattice<String>(), PowersetLattice<String>(setOf("bla")))
            )
        val blaEmpty =
            TupleLattice<Set<String>, Set<String>>(
                Pair(PowersetLattice<String>(setOf("bla")), emptyPowersetLattice<String>())
            )
        val emptyBla2 = emptyBla.duplicate()
        assertEquals(0, emptyBla.compareTo(emptyBla2))
        assertEquals(emptyBla, emptyBla2)
        assertNotSame(emptyBla, emptyBla2)
        assertNotSame(emptyBla.hashCode(), emptyBla2.hashCode())
        val (emptyBlaFirst, emptyBlaSecond) = emptyBla
        assertSame(emptyBlaFirst, emptyBla.elements.first)
        assertSame(emptyBlaSecond, emptyBla.elements.second)
        assertNotSame(emptyBlaFirst, emptyBla2.elements.first)
        assertEquals(emptyBlaFirst, emptyBla2.elements.first)
        assertNotSame(emptyBlaSecond, emptyBla2.elements.second)
        assertEquals(emptyBlaSecond, emptyBla2.elements.second)

        assertEquals(-1, emptyEmpty.compareTo(emptyBla))
        assertEquals(-1, emptyEmpty.compareTo(blaEmpty))
        assertEquals(1, emptyBla.compareTo(emptyEmpty))
        assertEquals(1, blaEmpty.compareTo(emptyEmpty))
        assertEquals(-1, blaEmpty.compareTo(emptyBla))
        assertEquals(-1, emptyBla.compareTo(blaEmpty))

        val blaBla = emptyBla.lub(blaEmpty)
        assertEquals(-1, emptyEmpty.compareTo(blaBla))
        assertEquals(-1, emptyBla.compareTo(blaBla))
        assertEquals(-1, blaEmpty.compareTo(blaBla))
        assertEquals(1, blaBla.compareTo(emptyEmpty))
        assertEquals(1, blaBla.compareTo(emptyBla))
        assertEquals(1, blaBla.compareTo(blaEmpty))

        // We explicitly want to call equals here
        assertFalse(blaBla == emptyBla) // Wrong elements
        assertFalse(blaBla == blaEmpty) // Wrong elements
        assertTrue(emptyBla2 == emptyBla) // Wrong elements
        assertFalse(blaBla == emptyBlaFirst) // Wrong types
        assertFalse(emptyBlaFirst == blaBla) // Wrong types
    }

    @Test
    fun testTripleLattice() {
        val emptyEmptyEmpty =
            TripleLattice<Set<String>, Set<String>, Set<String>>(
                Triple(
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                )
            )
        val emptyEmptyBla =
            TripleLattice<Set<String>, Set<String>, Set<String>>(
                Triple(
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                    PowersetLattice<String>(setOf("bla")),
                )
            )
        val emptyBlaEmpty =
            TripleLattice<Set<String>, Set<String>, Set<String>>(
                Triple(
                    emptyPowersetLattice<String>(),
                    PowersetLattice<String>(setOf("bla")),
                    emptyPowersetLattice<String>(),
                )
            )
        val blaEmptyEmpty =
            TripleLattice<Set<String>, Set<String>, Set<String>>(
                Triple(
                    PowersetLattice<String>(setOf("bla")),
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                )
            )
        val emptyEmptyBla2 = emptyEmptyBla.duplicate()
        assertEquals(0, emptyEmptyBla.compareTo(emptyEmptyBla2))
        assertEquals(emptyEmptyBla, emptyEmptyBla2)
        assertNotSame(emptyEmptyBla, emptyEmptyBla2)
        assertNotSame(emptyEmptyBla.hashCode(), emptyEmptyBla2.hashCode())
        val (emptyBlaFirst, emptyBlaSecond, emptyBlaThird) = emptyEmptyBla
        assertSame(emptyBlaFirst, emptyEmptyBla.elements.first)
        assertSame(emptyBlaSecond, emptyEmptyBla.elements.second)
        assertSame(emptyBlaThird, emptyEmptyBla.elements.third)
        assertNotSame(emptyBlaFirst, emptyEmptyBla2.elements.first)
        assertEquals(emptyBlaFirst, emptyEmptyBla2.elements.first)
        assertNotSame(emptyBlaSecond, emptyEmptyBla2.elements.second)
        assertEquals(emptyBlaSecond, emptyEmptyBla2.elements.second)
        assertNotSame(emptyBlaThird, emptyEmptyBla2.elements.third)
        assertEquals(emptyBlaThird, emptyEmptyBla2.elements.third)

        assertEquals(-1, emptyEmptyEmpty.compareTo(emptyEmptyBla))
        assertEquals(-1, emptyEmptyEmpty.compareTo(emptyBlaEmpty))
        assertEquals(-1, emptyEmptyEmpty.compareTo(blaEmptyEmpty))
        assertEquals(1, emptyEmptyBla.compareTo(emptyEmptyEmpty))
        assertEquals(1, blaEmptyEmpty.compareTo(emptyEmptyEmpty))
        assertEquals(1, emptyBlaEmpty.compareTo(emptyEmptyEmpty))
        assertEquals(-1, blaEmptyEmpty.compareTo(emptyEmptyBla))
        assertEquals(-1, emptyEmptyBla.compareTo(blaEmptyEmpty))
        assertEquals(-1, emptyBlaEmpty.compareTo(blaEmptyEmpty))

        val blaEmptyBla = emptyEmptyBla.lub(blaEmptyEmpty)
        assertEquals(-1, emptyEmptyEmpty.compareTo(blaEmptyBla))
        assertEquals(-1, emptyEmptyBla.compareTo(blaEmptyBla))
        assertEquals(-1, blaEmptyEmpty.compareTo(blaEmptyBla))
        assertEquals(-1, emptyBlaEmpty.compareTo(blaEmptyBla))
        assertEquals(1, blaEmptyBla.compareTo(emptyEmptyEmpty))
        assertEquals(1, blaEmptyBla.compareTo(emptyEmptyBla))
        assertEquals(1, blaEmptyBla.compareTo(blaEmptyEmpty))

        // We explicitly want to call equals here
        assertFalse(blaEmptyBla == emptyEmptyBla) // Wrong elements
        assertFalse(emptyEmptyEmpty == emptyEmptyBla) // Wrong elements
        assertFalse(emptyEmptyEmpty == emptyBlaEmpty) // Wrong elements
        assertFalse(emptyEmptyEmpty == blaEmptyEmpty) // Wrong elements
        assertTrue(emptyEmptyBla2 == emptyEmptyBla) // Same type and same elements
        assertFalse(blaEmptyBla == emptyBlaFirst) // Wrong types
        assertFalse(emptyBlaFirst == blaEmptyBla) // Wrong types
    }
}
