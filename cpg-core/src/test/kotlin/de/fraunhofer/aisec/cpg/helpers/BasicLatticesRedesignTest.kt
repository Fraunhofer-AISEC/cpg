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

import de.fraunhofer.aisec.cpg.helpers.functional.MapLatticeElementT
import de.fraunhofer.aisec.cpg.helpers.functional.Order
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLatticeElement
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLatticeElementT
import de.fraunhofer.aisec.cpg.helpers.functional.TripleLatticeElementT
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLatticeElementT
import de.fraunhofer.aisec.cpg.helpers.functional.emptyPowersetLattice
import java.util.IdentityHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BasicLatticesRedesignTest {
    @Test
    fun testPowersetLattice() {
        val powersetLattice = PowersetLattice<String>()

        val emptyLattice1 = powersetLattice.bottom
        val emptyLattice2 = powersetLattice.bottom
        assertEquals(Order.EQUAL, powersetLattice.compare(emptyLattice1, emptyLattice2))
        assertEquals(emptyLattice1, emptyLattice2)

        val blaLattice1 = identitySetOf("bla")
        val blaLattice2 = identitySetOf("bla")
        assertEquals(Order.EQUAL, powersetLattice.compare(blaLattice1, blaLattice2))
        assertEquals(blaLattice1, blaLattice2)

        val blaFooLattice = identitySetOf("bla", "foo")
        assertEquals(Order.GREATER, powersetLattice.compare(blaFooLattice, blaLattice1))
        assertNotEquals(blaFooLattice, blaLattice1)

        assertEquals(Order.LESSER, powersetLattice.compare(blaLattice1, blaFooLattice))
        assertNotEquals(blaLattice1, blaFooLattice)

        val blaBlubLattice = identitySetOf("bla", "blub")
        assertEquals(Order.UNEQUAL, powersetLattice.compare(blaFooLattice, blaBlubLattice))
        assertNotEquals(blaFooLattice, blaBlubLattice)
        assertEquals(Order.UNEQUAL, powersetLattice.compare(blaBlubLattice, blaFooLattice))

        assertNotSame(emptyLattice1, powersetLattice.duplicate(emptyLattice1))
        assertNotSame(emptyLattice1, powersetLattice.duplicate(emptyLattice1))
        assertNotSame(blaLattice1, powersetLattice.duplicate(blaLattice1))
        assertNotSame(blaLattice1, powersetLattice.duplicate(blaLattice1))
        assertNotSame(blaFooLattice, powersetLattice.duplicate(blaFooLattice))
        assertNotSame(blaFooLattice, powersetLattice.duplicate(blaFooLattice))
        assertNotSame(blaBlubLattice, powersetLattice.duplicate(blaBlubLattice))
        assertNotSame(blaBlubLattice, powersetLattice.duplicate(blaBlubLattice))

        val emptyLubEmpty = powersetLattice.lub(emptyLattice1, emptyLattice1)
        assertIs<IdentitySet<String>>(emptyLubEmpty)
        assertNotSame(emptyLattice1, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(Order.EQUAL, powersetLattice.compare(emptyLattice1, emptyLubEmpty))
        assertNotSame(emptyLattice2, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(Order.EQUAL, powersetLattice.compare(emptyLattice2, emptyLubEmpty))

        val emptyLattice3 = identitySetOf<String>()
        val emptyLubBla = powersetLattice.lub(emptyLattice3, blaLattice1)
        assertIs<IdentitySet<String>>(emptyLubBla)
        assertNotSame(emptyLattice3, emptyLubBla)
        assertNotEquals(emptyLattice3, emptyLubBla)
        assertEquals(Order.LESSER, powersetLattice.compare(emptyLattice3, emptyLubBla))
        assertNotSame(blaLattice1, emptyLubBla)
        assertEquals(blaLattice1, emptyLubBla)
        assertEquals(Order.EQUAL, powersetLattice.compare(blaLattice1, emptyLubBla))

        val blaFooBlub = powersetLattice.lub(blaFooLattice, blaBlubLattice)
        assertIs<IdentitySet<String>>(blaFooBlub)
        assertNotSame(emptyLattice3, blaFooBlub)
        assertNotEquals(emptyLattice3, blaFooBlub)
        assertEquals(Order.LESSER, powersetLattice.compare(emptyLattice3, blaFooBlub))
        assertNotSame(blaLattice1, blaFooBlub)
        assertNotEquals(blaLattice1, blaFooBlub)
        assertEquals(Order.LESSER, powersetLattice.compare(blaLattice1, blaFooBlub))
        assertNotSame(blaFooLattice, blaFooBlub)
        assertNotEquals(blaFooLattice, blaFooBlub)
        assertEquals(Order.LESSER, powersetLattice.compare(blaFooLattice, blaFooBlub))
        assertNotSame(blaBlubLattice, blaFooBlub)
        assertNotEquals(blaBlubLattice, blaFooBlub)
        assertEquals(Order.LESSER, powersetLattice.compare(blaBlubLattice, blaFooBlub))
        assertEquals(identitySetOf("bla", "blub", "foo"), blaFooBlub)
    }

    @Test
    fun testMapLattice() {
        val emptyLattice1 = MapLatticeElementT<String, IdentitySet<String>>(IdentityHashMap())
        val emptyLattice2 = MapLatticeElementT<String, IdentitySet<String>>(IdentityHashMap())
        assertEquals(0, emptyLattice1.compareTo(emptyLattice2))
        assertEquals(emptyLattice1, emptyLattice2)
        assertNotSame(emptyLattice1.hashCode(), emptyLattice1.hashCode())

        val aBlaLattice1 =
            MapLatticeElementT<String, IdentitySet<String>>(
                IdentityHashMap(mapOf("a" to PowersetLatticeElementT(identitySetOf("bla"))))
            )
        val aBlaLattice2 =
            MapLatticeElementT<String, IdentitySet<String>>(
                IdentityHashMap(mapOf("a" to PowersetLatticeElementT(identitySetOf("bla"))))
            )
        assertEquals(0, aBlaLattice1.compareTo(aBlaLattice2))
        assertEquals(aBlaLattice1, aBlaLattice2)
        assertNotSame(aBlaLattice1, aBlaLattice2)

        val aBlaFooLattice =
            MapLatticeElementT<String, IdentitySet<String>>(
                IdentityHashMap(mapOf("a" to PowersetLatticeElementT(identitySetOf("bla", "foo"))))
            )
        assertEquals(1, aBlaFooLattice.compareTo(aBlaLattice1))
        assertNotEquals(aBlaFooLattice, aBlaLattice1)
        assertEquals(-1, aBlaLattice1.compareTo(aBlaFooLattice))
        assertNotEquals(aBlaLattice1, aBlaFooLattice)

        val aBlaBFooLattice =
            MapLatticeElementT<String, IdentitySet<String>>(
                IdentityHashMap(
                    mapOf(
                        "a" to PowersetLatticeElement(identitySetOf("bla")),
                        "b" to PowersetLatticeElementT(identitySetOf("foo")),
                    )
                )
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
        assertNotSame(aBlaLattice1.value, aBlaLatticeDuplicate.value)
        assertEquals(aBlaLattice1, aBlaLatticeDuplicate)
        val aBlaFooLatticeDuplicate = aBlaFooLattice.duplicate()
        assertNotSame(aBlaFooLattice, aBlaFooLatticeDuplicate)
        assertNotSame(aBlaBFooLattice.value, aBlaFooLatticeDuplicate.value)
        assertEquals(aBlaFooLattice, aBlaFooLatticeDuplicate)
        val aBlaBFooLatticeDuplicate = aBlaBFooLattice.duplicate()
        assertNotSame(aBlaFooLattice, aBlaBFooLatticeDuplicate)
        assertNotSame(aBlaBFooLattice.value, aBlaBFooLatticeDuplicate.value)
        assertEquals(aBlaBFooLattice, aBlaBFooLatticeDuplicate)

        val emptyLubEmpty = emptyLattice1.lub(emptyLattice1)
        assertIs<MapLatticeElementT<String, IdentitySet<String>>>(emptyLubEmpty)
        assertNotSame(emptyLattice1, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(0, emptyLattice1.compareTo(emptyLubEmpty))
        assertNotSame(emptyLattice2, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(0, emptyLattice2.compareTo(emptyLubEmpty))

        val emptyLubABla = emptyLattice1.lub(aBlaLattice1)
        assertIs<MapLatticeElementT<String, IdentitySet<String>>>(emptyLubABla)
        assertNotSame(emptyLattice1, emptyLubABla)
        assertNotEquals(emptyLattice1, emptyLubABla)
        assertEquals(-1, emptyLattice1.compareTo(emptyLubABla))
        assertNotSame(aBlaLattice1, emptyLubABla)
        assertEquals(aBlaLattice1, emptyLubABla)
        assertEquals(0, aBlaLattice1.compareTo(emptyLubABla))

        val aFooBBlaLattice =
            MapLatticeElementT<String, IdentitySet<String>>(
                IdentityHashMap(
                    mapOf(
                        "a" to PowersetLatticeElement(identitySetOf("foo")),
                        "b" to PowersetLatticeElementT(identitySetOf("bla")),
                    )
                )
            )
        val aBlaFooBBla = aBlaFooLattice.lub(aFooBBlaLattice) // a to {"foo", "bla"}, b to {"bla"}
        assertIs<MapLatticeElementT<String, IdentitySet<String>>>(aBlaFooBBla)
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
        assertEquals(setOf("a", "b"), aBlaFooBBla.value.keys)
        assertEquals(identitySetOf("bla", "foo"), aBlaFooBBla.value["a"]?.value)
        assertEquals(identitySetOf("bla"), aBlaFooBBla.value["b"]?.value)

        assertFalse(aBlaFooBBla == emptyLattice1) // Wrong elements
        assertTrue(emptyLattice1 == emptyLattice2) // This is equal
        assertTrue(aBlaLattice1 == aBlaLattice2) // This is equal
        assertFalse(aBlaFooBBla == aBlaFooBBla.value["a"]) // Wrong types
        assertFalse(aBlaFooBBla.value["a"] == aBlaFooBBla) // Wrong types
    }

    @Test
    fun testPairLattice() {
        val emptyEmpty =
            TupleLatticeElementT<IdentitySet<String>, IdentitySet<String>>(
                Pair(emptyPowersetLattice<String>(), emptyPowersetLattice<String>())
            )
        val emptyBla =
            TupleLatticeElementT<IdentitySet<String>, IdentitySet<String>>(
                Pair(
                    emptyPowersetLattice<String>(),
                    PowersetLatticeElementT<String>(identitySetOf("bla")),
                )
            )
        val blaEmpty =
            TupleLatticeElementT<IdentitySet<String>, IdentitySet<String>>(
                Pair(
                    PowersetLatticeElementT<String>(identitySetOf("bla")),
                    emptyPowersetLattice<String>(),
                )
            )
        val emptyBla2 = emptyBla.duplicate()
        assertEquals(0, emptyBla.compareTo(emptyBla2))
        assertEquals(emptyBla, emptyBla2)
        assertNotSame(emptyBla, emptyBla2)
        assertNotSame(emptyBla.hashCode(), emptyBla2.hashCode())
        val (emptyBlaFirst, emptyBlaSecond) = emptyBla
        assertSame(emptyBlaFirst, emptyBla.value.first)
        assertSame(emptyBlaSecond, emptyBla.value.second)
        assertNotSame(emptyBlaFirst, emptyBla2.value.first)
        assertEquals(emptyBlaFirst, emptyBla2.value.first)
        assertNotSame(emptyBlaSecond, emptyBla2.value.second)
        assertEquals(emptyBlaSecond, emptyBla2.value.second)

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
            TripleLatticeElementT<IdentitySet<String>, IdentitySet<String>, IdentitySet<String>>(
                Triple(
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                )
            )
        val emptyEmptyBla =
            TripleLatticeElementT<IdentitySet<String>, IdentitySet<String>, IdentitySet<String>>(
                Triple(
                    emptyPowersetLattice<String>(),
                    emptyPowersetLattice<String>(),
                    PowersetLatticeElementT<String>(identitySetOf("bla")),
                )
            )
        val emptyBlaEmpty =
            TripleLatticeElementT<IdentitySet<String>, IdentitySet<String>, IdentitySet<String>>(
                Triple(
                    emptyPowersetLattice<String>(),
                    PowersetLatticeElementT<String>(identitySetOf("bla")),
                    emptyPowersetLattice<String>(),
                )
            )
        val blaEmptyEmpty =
            TripleLatticeElementT<IdentitySet<String>, IdentitySet<String>, IdentitySet<String>>(
                Triple(
                    PowersetLatticeElementT<String>(identitySetOf("bla")),
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
        assertSame(emptyBlaFirst, emptyEmptyBla.value.first)
        assertSame(emptyBlaSecond, emptyEmptyBla.value.second)
        assertSame(emptyBlaThird, emptyEmptyBla.value.third)
        assertNotSame(emptyBlaFirst, emptyEmptyBla2.value.first)
        assertEquals(emptyBlaFirst, emptyEmptyBla2.value.first)
        assertNotSame(emptyBlaSecond, emptyEmptyBla2.value.second)
        assertEquals(emptyBlaSecond, emptyEmptyBla2.value.second)
        assertNotSame(emptyBlaThird, emptyEmptyBla2.value.third)
        assertEquals(emptyBlaThird, emptyEmptyBla2.value.third)

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
