/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.Order
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.functional.TripleLattice
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLattice
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
        val mapLattice = MapLattice<String, Set<String>>(PowersetLattice<String>())
        val emptyLattice1 = IdentityHashMap<String, Set<String>>()
        val emptyLattice2 = mapLattice.bottom
        assertEquals(Order.EQUAL, mapLattice.compare(emptyLattice1, emptyLattice2))
        assertEquals(emptyLattice1, emptyLattice2)

        val aBlaLattice1 = IdentityHashMap(mapOf("a" to identitySetOf("bla")))
        val aBlaLattice2 = IdentityHashMap(mapOf("a" to identitySetOf("bla")))
        assertEquals(Order.EQUAL, mapLattice.compare(aBlaLattice1, aBlaLattice2))
        assertEquals(aBlaLattice1, aBlaLattice2)
        assertNotSame(aBlaLattice1, aBlaLattice2)

        val aBlaFooLattice = IdentityHashMap(mapOf("a" to identitySetOf("bla", "foo")))
        assertEquals(Order.GREATER, mapLattice.compare(aBlaFooLattice, aBlaLattice1))
        assertNotEquals(aBlaFooLattice, aBlaLattice1)
        assertEquals(Order.LESSER, mapLattice.compare(aBlaLattice1, aBlaFooLattice))
        assertNotEquals(aBlaLattice1, aBlaFooLattice)

        val aBlaBFooLattice =
            IdentityHashMap(mapOf("a" to identitySetOf("bla"), "b" to identitySetOf("foo")))
        assertEquals(Order.GREATER, mapLattice.compare(aBlaBFooLattice, aBlaLattice1))
        assertNotEquals(aBlaBFooLattice, aBlaLattice1)
        assertEquals(Order.LESSER, mapLattice.compare(aBlaLattice1, aBlaBFooLattice))
        assertNotEquals(aBlaLattice1, aBlaBFooLattice)

        // Duplicates are equal but not identical. Same for the elements.
        val emptyDuplicate = mapLattice.duplicate(emptyLattice1)
        assertNotSame(emptyLattice1, emptyDuplicate)
        // assertNotSame(emptyLattice1.elements, emptyDuplicate.elements) // Somehow, the empty set
        // is the same
        assertEquals(emptyLattice1, emptyDuplicate)
        val aBlaLatticeDuplicate = mapLattice.duplicate(aBlaLattice1)
        assertNotSame(aBlaLattice1, aBlaLatticeDuplicate)
        assertNotSame(aBlaLattice1, aBlaLatticeDuplicate)
        assertEquals(aBlaLattice1, aBlaLatticeDuplicate)
        val aBlaFooLatticeDuplicate = mapLattice.duplicate(aBlaFooLattice)
        assertNotSame(aBlaFooLattice, aBlaFooLatticeDuplicate)
        assertNotSame(aBlaBFooLattice, aBlaFooLatticeDuplicate)
        assertEquals(aBlaFooLattice, aBlaFooLatticeDuplicate)
        val aBlaBFooLatticeDuplicate = mapLattice.duplicate(aBlaBFooLattice)
        assertNotSame(aBlaFooLattice, aBlaBFooLatticeDuplicate)
        assertNotSame(aBlaBFooLattice, aBlaBFooLatticeDuplicate)
        assertEquals(aBlaBFooLattice, aBlaBFooLatticeDuplicate)

        val emptyLubEmpty = mapLattice.lub(emptyLattice1, emptyLattice1)
        assertIs<IdentityHashMap<String, IdentitySet<String>>>(emptyLubEmpty)
        assertIs<IdentityHashMap<String, IdentitySet<String>>>(emptyLattice1)
        assertNotSame(emptyLattice1, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(Order.EQUAL, mapLattice.compare(emptyLattice1, emptyLubEmpty))
        assertNotSame(emptyLattice2, emptyLubEmpty)
        assertEquals(emptyLattice1, emptyLubEmpty)
        assertEquals(Order.EQUAL, mapLattice.compare(emptyLattice2, emptyLubEmpty))

        val emptyLubABla = mapLattice.lub(emptyLattice1, aBlaLattice1)
        assertIs<IdentityHashMap<String, IdentitySet<String>>>(emptyLubABla)
        assertIs<IdentityHashMap<String, IdentitySet<String>>>(mapLattice)
        assertIs<IdentityHashMap<String, IdentitySet<String>>>(emptyLattice1)
        assertNotSame<IdentityHashMap<String, IdentitySet<String>>>(emptyLattice1, mapLattice)
        assertNotEquals<Map<String, Set<String>>>(emptyLattice1, emptyLubABla)
        assertEquals(Order.LESSER, mapLattice.compare(emptyLattice1, emptyLubABla))
        assertNotSame(aBlaLattice1, emptyLubABla)
        assertEquals(aBlaLattice1, emptyLubABla)
        assertEquals(Order.EQUAL, mapLattice.compare(aBlaLattice1, emptyLubABla))

        val aFooBBlaLattice =
            IdentityHashMap(mapOf("a" to identitySetOf("foo"), "b" to identitySetOf("bla")))
        val aBlaFooBBla =
            mapLattice.lub(aBlaFooLattice, aFooBBlaLattice) // a to {"foo", "bla"}, b to {"bla"}
        assertIs<IdentityHashMap<String, IdentitySet<String>>>(aBlaFooBBla)
        assertNotSame(emptyLattice1, aBlaFooBBla)
        assertNotEquals(emptyLattice1, aBlaFooBBla)
        assertEquals(Order.LESSER, mapLattice.compare(emptyLattice1, aBlaFooBBla))
        assertEquals(Order.GREATER, mapLattice.compare(aBlaFooBBla, emptyLattice1))
        assertNotSame(aBlaLattice1, aBlaFooBBla)
        assertNotEquals(aBlaLattice1, aBlaFooBBla)
        assertEquals(Order.LESSER, mapLattice.compare(aBlaLattice1, aBlaFooBBla))
        assertEquals(Order.GREATER, mapLattice.compare(aBlaFooBBla, aBlaLattice1))
        assertNotSame(aBlaFooLattice, aBlaFooBBla)
        assertNotEquals(aBlaFooLattice, aBlaFooBBla)
        assertEquals(Order.LESSER, mapLattice.compare(aBlaFooLattice, aBlaFooBBla))
        assertEquals(Order.GREATER, mapLattice.compare(aBlaFooBBla, aBlaFooLattice))
        assertNotSame(aBlaBFooLattice, aBlaFooBBla)
        assertNotEquals(aBlaBFooLattice, aBlaFooBBla)
        assertEquals(Order.LESSER, mapLattice.compare(aBlaBFooLattice, aBlaFooBBla))
        assertEquals(Order.LESSER, mapLattice.compare(aBlaFooBBla, aBlaBFooLattice))
        assertNotSame(aFooBBlaLattice, aBlaFooBBla)
        assertNotEquals(aFooBBlaLattice, aBlaFooBBla)
        assertEquals(Order.LESSER, mapLattice.compare(aFooBBlaLattice, aBlaFooBBla))
        assertEquals(Order.GREATER, mapLattice.compare(aBlaFooBBla, aFooBBlaLattice))
        assertEquals(setOf("a", "b"), aBlaFooBBla.keys)
        assertEquals(identitySetOf("bla", "foo"), aBlaFooBBla["a"])
        assertEquals(identitySetOf("bla"), aBlaFooBBla["b"])

        assertFalse(aBlaFooBBla == emptyLattice1) // Wrong elements
        assertTrue(emptyLattice1 == emptyLattice2) // This is equal
        assertTrue(aBlaLattice1 == aBlaLattice2) // This is equal
        assertFalse(aBlaFooBBla == aBlaFooBBla["a"]) // Wrong types
        assertFalse(aBlaFooBBla["a"] == aBlaFooBBla) // Wrong types
    }

    @Test
    fun testPairLattice() {
        val tupleLattice = TupleLattice(PowersetLattice<String>(), PowersetLattice<String>())

        val emptyEmpty = tupleLattice.bottom
        val emptyBla = Pair<Set<String>, Set<String>>(identitySetOf(), identitySetOf("bla"))

        val blaEmpty = Pair<Set<String>, Set<String>>(identitySetOf("bla"), identitySetOf())
        val emptyBla2 = tupleLattice.duplicate(emptyBla)
        assertEquals(Order.EQUAL, tupleLattice.compare(emptyBla, emptyBla2))
        assertEquals(emptyBla, emptyBla2)
        assertNotSame(emptyBla, emptyBla2)
        assertNotSame(emptyBla.hashCode(), emptyBla2.hashCode())
        val (emptyBlaFirst, emptyBlaSecond) = emptyBla
        assertSame(emptyBlaFirst, emptyBla.first)
        assertSame(emptyBlaSecond, emptyBla.second)
        assertNotSame(emptyBlaFirst, emptyBla2.first)
        assertEquals(emptyBlaFirst, emptyBla2.first)
        assertNotSame(emptyBlaSecond, emptyBla2.second)
        assertEquals(emptyBlaSecond, emptyBla2.second)

        assertEquals(Order.LESSER, tupleLattice.compare(emptyEmpty, emptyBla))
        assertEquals(Order.LESSER, tupleLattice.compare(emptyEmpty, blaEmpty))
        assertEquals(Order.GREATER, tupleLattice.compare(emptyBla, emptyEmpty))
        assertEquals(Order.GREATER, tupleLattice.compare(blaEmpty, emptyEmpty))
        assertEquals(Order.UNEQUAL, tupleLattice.compare(blaEmpty, emptyBla))
        assertEquals(Order.UNEQUAL, tupleLattice.compare(emptyBla, blaEmpty))

        val blaBla = tupleLattice.lub(emptyBla, blaEmpty)
        assertEquals(Order.LESSER, tupleLattice.compare(emptyEmpty, blaBla))
        assertEquals(Order.LESSER, tupleLattice.compare(emptyBla, blaBla))
        assertEquals(Order.LESSER, tupleLattice.compare(blaEmpty, blaBla))
        assertEquals(Order.GREATER, tupleLattice.compare(blaBla, emptyEmpty))
        assertEquals(Order.GREATER, tupleLattice.compare(blaBla, emptyBla))
        assertEquals(Order.GREATER, tupleLattice.compare(blaBla, blaEmpty))

        // We explicitly want to call equals here
        assertFalse(blaBla == emptyBla) // Wrong elements
        assertFalse(blaBla == blaEmpty) // Wrong elements
        assertTrue(emptyBla2 == emptyBla) // Wrong elements
        assertFalse(blaBla == emptyBlaFirst) // Wrong types
        assertFalse(emptyBlaFirst == blaBla) // Wrong types
    }

    @Test
    fun testTripleLattice() {
        val tripleLattice =
            TripleLattice<Set<String>, Set<String>, Set<String>>(
                PowersetLattice<String>(),
                PowersetLattice<String>(),
                PowersetLattice<String>(),
            )

        val emptyEmptyEmpty =
            Triple(identitySetOf<String>(), identitySetOf<String>(), identitySetOf<String>())
        val emptyEmptyBla =
            Triple(identitySetOf<String>(), identitySetOf<String>(), identitySetOf("bla"))
        val emptyBlaEmpty =
            Triple(identitySetOf<String>(), identitySetOf("bla"), identitySetOf<String>())
        val blaEmptyEmpty =
            Triple(identitySetOf("bla"), identitySetOf<String>(), identitySetOf<String>())

        val emptyEmptyBla2 = tripleLattice.duplicate(emptyEmptyBla)
        assertEquals(Order.EQUAL, tripleLattice.compare(emptyEmptyBla, emptyEmptyBla2))
        assertEquals(emptyEmptyBla, emptyEmptyBla2)
        assertNotSame(emptyEmptyBla, emptyEmptyBla2)
        assertNotSame(emptyEmptyBla.hashCode(), emptyEmptyBla2.hashCode())
        val (emptyBlaFirst, emptyBlaSecond, emptyBlaThird) = emptyEmptyBla
        assertSame(emptyBlaFirst, emptyEmptyBla.first)
        assertSame(emptyBlaSecond, emptyEmptyBla.second)
        assertSame(emptyBlaThird, emptyEmptyBla.third)
        assertNotSame(emptyBlaFirst, emptyEmptyBla2.first)
        assertEquals(emptyBlaFirst, emptyEmptyBla2.first)
        assertNotSame(emptyBlaSecond, emptyEmptyBla2.second)
        assertEquals(emptyBlaSecond, emptyEmptyBla2.second)
        assertNotSame(emptyBlaThird, emptyEmptyBla2.third)
        assertEquals(emptyBlaThird, emptyEmptyBla2.third)

        assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, emptyEmptyBla))
        assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, emptyBlaEmpty))
        assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, blaEmptyEmpty))
        assertEquals(Order.GREATER, tripleLattice.compare(emptyEmptyBla, emptyEmptyEmpty))
        assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyEmpty, emptyEmptyEmpty))
        assertEquals(Order.GREATER, tripleLattice.compare(emptyBlaEmpty, emptyEmptyEmpty))
        assertEquals(Order.UNEQUAL, tripleLattice.compare(blaEmptyEmpty, emptyEmptyBla))
        assertEquals(Order.UNEQUAL, tripleLattice.compare(emptyEmptyBla, blaEmptyEmpty))
        assertEquals(Order.UNEQUAL, tripleLattice.compare(emptyBlaEmpty, blaEmptyEmpty))

        val blaEmptyBla = tripleLattice.lub(emptyEmptyBla, blaEmptyEmpty)
        assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, blaEmptyBla))
        assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyBla, blaEmptyBla))
        assertEquals(Order.LESSER, tripleLattice.compare(blaEmptyEmpty, blaEmptyBla))
        assertEquals(Order.UNEQUAL, tripleLattice.compare(emptyBlaEmpty, blaEmptyBla))
        assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyBla, emptyEmptyEmpty))
        assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyBla, emptyEmptyBla))
        assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyBla, blaEmptyEmpty))

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
