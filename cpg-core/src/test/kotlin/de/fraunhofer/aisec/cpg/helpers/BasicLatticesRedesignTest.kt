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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows

class BasicLatticesRedesignTest {
    @Test
    fun testPowersetLattice() {
        runBlocking {
            val powersetLattice = PowersetLattice<String>()

            val emptyLattice1 = powersetLattice.bottom
            val emptyLattice2 = powersetLattice.bottom
            assertEquals(Order.EQUAL, powersetLattice.compare(emptyLattice1, emptyLattice2))
            assertEquals(emptyLattice1, emptyLattice2)

            val blaLattice1 = PowersetLattice.Element("bla")
            val blaLattice2 = PowersetLattice.Element("bla")
            assertEquals(Order.EQUAL, powersetLattice.compare(blaLattice1, blaLattice2))
            assertEquals(blaLattice1, blaLattice2)

            val blaFooLattice = PowersetLattice.Element("bla", "foo")
            assertEquals(Order.GREATER, powersetLattice.compare(blaFooLattice, blaLattice1))
            assertNotEquals(blaFooLattice, blaLattice1)

            val bluLattice = PowersetLattice.Element("blu")
            assertEquals(Order.UNEQUAL, powersetLattice.compare(blaFooLattice, bluLattice))
            assertEquals(Order.UNEQUAL, powersetLattice.compare(bluLattice, blaFooLattice))
            assertNotEquals(blaFooLattice, bluLattice)

            assertEquals(Order.LESSER, powersetLattice.compare(blaLattice1, blaFooLattice))
            assertNotEquals(blaLattice1, blaFooLattice)

            val blaBlubLattice = PowersetLattice.Element("bla", "blub")
            assertEquals(Order.UNEQUAL, powersetLattice.compare(blaFooLattice, blaBlubLattice))
            assertNotEquals(blaFooLattice, blaBlubLattice)
            assertEquals(Order.UNEQUAL, powersetLattice.compare(blaBlubLattice, blaFooLattice))
            assertEquals(Order.UNEQUAL, powersetLattice.compare(blaBlubLattice, bluLattice))
            assertEquals(Order.UNEQUAL, powersetLattice.compare(bluLattice, blaBlubLattice))
            assertNotEquals(blaBlubLattice, bluLattice)

            assertNotSame(emptyLattice1, powersetLattice.duplicate(emptyLattice1))
            assertNotSame(emptyLattice1, powersetLattice.duplicate(emptyLattice1))
            assertNotSame(blaLattice1, powersetLattice.duplicate(blaLattice1))
            assertNotSame(blaLattice1, powersetLattice.duplicate(blaLattice1))
            assertNotSame(blaFooLattice, powersetLattice.duplicate(blaFooLattice))
            assertNotSame(blaFooLattice, powersetLattice.duplicate(blaFooLattice))
            assertNotSame(blaBlubLattice, powersetLattice.duplicate(blaBlubLattice))
            assertNotSame(blaBlubLattice, powersetLattice.duplicate(blaBlubLattice))

            val emptyLubEmpty = runBlocking { powersetLattice.lub(emptyLattice1, emptyLattice1) }
            assertNotSame(emptyLattice1, emptyLubEmpty)
            assertEquals(emptyLattice1, emptyLubEmpty)
            assertEquals(Order.EQUAL, powersetLattice.compare(emptyLattice1, emptyLubEmpty))
            assertNotSame(emptyLattice2, emptyLubEmpty)
            assertEquals(emptyLattice1, emptyLubEmpty)
            assertEquals(Order.EQUAL, powersetLattice.compare(emptyLattice2, emptyLubEmpty))

            val empty1LubBla1 = runBlocking { powersetLattice.lub(emptyLattice1, blaLattice1) }
            assertNotSame(emptyLattice1, emptyLubEmpty)
            assertEquals(emptyLattice1, emptyLubEmpty)
            assertNotSame(emptyLattice1, empty1LubBla1)
            assertNotEquals(emptyLattice1, empty1LubBla1)
            assertNotSame(blaLattice1, empty1LubBla1)
            assertEquals(blaLattice1, empty1LubBla1)
            assertEquals(Order.LESSER, powersetLattice.compare(emptyLattice1, empty1LubBla1))
            assertEquals(Order.EQUAL, powersetLattice.compare(blaLattice1, empty1LubBla1))

            val emptyLattice3 = PowersetLattice.Element<String>()
            val emptyLubBla = runBlocking { powersetLattice.lub(emptyLattice3, blaLattice1) }
            assertNotSame(emptyLattice3, emptyLubBla)
            assertNotEquals(emptyLattice3, emptyLubBla)
            assertEquals(Order.LESSER, powersetLattice.compare(emptyLattice3, emptyLubBla))
            assertNotSame(blaLattice1, emptyLubBla)
            assertEquals(blaLattice1, emptyLubBla)
            assertEquals(Order.EQUAL, powersetLattice.compare(blaLattice1, emptyLubBla))

            val blaFooBlub = runBlocking { powersetLattice.lub(blaFooLattice, blaBlubLattice) }
            val blaGlb = powersetLattice.glb(blaFooLattice, blaBlubLattice)
            assertNotSame(blaLattice1, blaGlb)
            assertNotSame(blaLattice2, blaGlb)
            assertEquals(blaLattice2, blaGlb)
            assertEquals(blaLattice1, blaGlb)
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
            assertEquals(PowersetLattice.Element("bla", "blub", "foo"), blaFooBlub)
        }
    }

    @Test
    fun testMapLattice() {
        runBlocking {
            val mapLattice =
                MapLattice<String, PowersetLattice.Element<String>>(PowersetLattice<String>())
            val emptyLattice1 = MapLattice.Element<String, PowersetLattice.Element<String>>()
            val emptyLattice2 = mapLattice.bottom
            assertEquals(Order.EQUAL, mapLattice.compare(emptyLattice1, emptyLattice2))
            assertEquals(emptyLattice1, emptyLattice2)

            val blaPowerset = PowersetLattice.Element("bla")

            assertThrows<IllegalArgumentException> { emptyLattice1.compare(blaPowerset) }
            assertThrows<IllegalArgumentException> { blaPowerset.compare(emptyLattice1) }

            val aBlaLattice1 = (MapLattice.Element("a" to blaPowerset))
            val aBlaLattice2 = (MapLattice.Element("a" to PowersetLattice.Element("bla")))
            assertEquals(Order.EQUAL, mapLattice.compare(aBlaLattice1, aBlaLattice2))
            assertEquals(aBlaLattice1, aBlaLattice2)
            assertNotSame(aBlaLattice1, aBlaLattice2)

            val aBlaFooLattice = (MapLattice.Element("a" to PowersetLattice.Element("bla", "foo")))
            assertEquals(Order.GREATER, mapLattice.compare(aBlaFooLattice, aBlaLattice1))
            assertNotEquals(aBlaFooLattice, aBlaLattice1)
            assertEquals(Order.LESSER, mapLattice.compare(aBlaLattice1, aBlaFooLattice))
            assertNotEquals(aBlaLattice1, aBlaFooLattice)

            val aBlaBFooLattice =
                (MapLattice.Element(
                    "a" to PowersetLattice.Element("bla"),
                    "b" to PowersetLattice.Element("foo"),
                ))
            assertEquals(Order.GREATER, mapLattice.compare(aBlaBFooLattice, aBlaLattice1))
            assertNotEquals(aBlaBFooLattice, aBlaLattice1)
            assertEquals(Order.LESSER, mapLattice.compare(aBlaLattice1, aBlaBFooLattice))
            assertNotEquals(aBlaLattice1, aBlaBFooLattice)

            // Duplicates are equal but not identical. Same for the elements.
            val emptyDuplicate = mapLattice.duplicate(emptyLattice1)
            assertNotSame(emptyLattice1, emptyDuplicate)
            assertNotSame(emptyLattice1, emptyDuplicate) // Somehow, the empty set
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

            val emptyLubEmpty = runBlocking { mapLattice.lub(emptyLattice1, emptyLattice1) }
            assertNotSame(emptyLattice1, emptyLubEmpty)
            assertEquals(emptyLattice1, emptyLubEmpty)
            assertEquals(Order.EQUAL, mapLattice.compare(emptyLattice1, emptyLubEmpty))
            assertNotSame(emptyLattice2, emptyLubEmpty)
            assertEquals(emptyLattice1, emptyLubEmpty)
            assertEquals(Order.EQUAL, mapLattice.compare(emptyLattice2, emptyLubEmpty))

            val emptyLubABla = runBlocking { mapLattice.lub(emptyLattice1, aBlaLattice1) }
            assertNotSame(emptyLattice1, mapLattice.bottom)
            assertNotEquals<Map<String, Set<String>>>(emptyLattice1, emptyLubABla)
            assertEquals(Order.LESSER, mapLattice.compare(emptyLattice1, emptyLubABla))
            assertNotSame(aBlaLattice1, emptyLubABla)
            assertEquals(aBlaLattice1, emptyLubABla)
            assertEquals(Order.EQUAL, mapLattice.compare(aBlaLattice1, emptyLubABla))

            val aFooBBlaLattice =
                MapLattice.Element(
                    "a" to PowersetLattice.Element("foo"),
                    "b" to PowersetLattice.Element("bla"),
                )
            val aBlaFooBBla = runBlocking {
                mapLattice.lub(aBlaFooLattice, aFooBBlaLattice)
            } // a to {"foo", "bla"}, b to {"bla"}
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
            assertEquals(Order.UNEQUAL, mapLattice.compare(aBlaBFooLattice, aBlaFooBBla))
            assertEquals(Order.UNEQUAL, mapLattice.compare(aBlaFooBBla, aBlaBFooLattice))
            assertNotSame(aFooBBlaLattice, aBlaFooBBla)
            assertNotEquals(aFooBBlaLattice, aBlaFooBBla)
            assertEquals(Order.LESSER, mapLattice.compare(aFooBBlaLattice, aBlaFooBBla))
            assertEquals(Order.GREATER, mapLattice.compare(aBlaFooBBla, aFooBBlaLattice))
            assertEquals(setOf("a", "b"), aBlaFooBBla.keys)
            assertEquals(PowersetLattice.Element("bla", "foo"), aBlaFooBBla["a"])
            assertEquals(PowersetLattice.Element("bla"), aBlaFooBBla["b"])

            assertFalse(aBlaFooBBla == emptyLattice1) // Wrong elements
            assertTrue(emptyLattice1 == emptyLattice2) // This is equal
            assertTrue(aBlaLattice1 == aBlaLattice2) // This is equal
            assertFalse(aBlaFooBBla == aBlaFooBBla["a"]) // Wrong types
            assertFalse(aBlaFooBBla["a"] == aBlaFooBBla) // Wrong types

            val aEmptyBEmptyGlb = mapLattice.glb(aFooBBlaLattice, aBlaBFooLattice)
            val aEmptyBEmpty =
                MapLattice.Element(
                    "a" to PowersetLattice.Element<String>(),
                    "b" to PowersetLattice.Element<String>(),
                )
            assertNotSame(aEmptyBEmptyGlb, aEmptyBEmpty)
            assertEquals(aEmptyBEmptyGlb, aEmptyBEmpty)
            assertEquals(Order.LESSER, aEmptyBEmptyGlb.compare(aFooBBlaLattice))
            assertEquals(Order.LESSER, aEmptyBEmptyGlb.compare(aBlaBFooLattice))

            val aBlaGlb = mapLattice.glb(aBlaFooLattice, aBlaBFooLattice)
            assertNotSame(aBlaGlb, aBlaLattice1)
            assertNotSame(aBlaGlb, aBlaLattice2)
            assertEquals(aBlaGlb, aBlaLattice1)
            assertEquals(Order.LESSER, aBlaGlb.compare(aBlaFooLattice))
            assertEquals(Order.LESSER, aBlaGlb.compare(aBlaBFooLattice))
        }
    }

    @Test
    fun testPairLattice() {
        runBlocking {
            val tupleLattice = TupleLattice(PowersetLattice<String>(), PowersetLattice<String>())

            val emptyEmpty = tupleLattice.bottom
            val emptyBla =
                TupleLattice.Element<
                    PowersetLattice.Element<String>,
                    PowersetLattice.Element<String>,
                >(
                    PowersetLattice.Element(),
                    PowersetLattice.Element("bla"),
                )

            val blaEmpty =
                TupleLattice.Element<
                    PowersetLattice.Element<String>,
                    PowersetLattice.Element<String>,
                >(
                    PowersetLattice.Element("bla"),
                    PowersetLattice.Element(),
                )
            val emptyBla2 = tupleLattice.duplicate(emptyBla)
            assertEquals(Order.EQUAL, tupleLattice.compare(emptyBla, emptyBla2))
            assertEquals(emptyBla, emptyBla2)
            assertNotSame(emptyBla, emptyBla2)
            val (emptyBlaFirst, emptyBlaSecond) = emptyBla
            assertSame(emptyBlaFirst, emptyBla.first)
            assertSame(emptyBlaSecond, emptyBla.second)
            assertNotSame(emptyBlaFirst, emptyBla2.first)
            assertEquals(emptyBlaFirst, emptyBla2.first)
            assertNotSame(emptyBlaSecond, emptyBla2.second)
            assertEquals(emptyBlaSecond, emptyBla2.second)

            assertThrows<IllegalArgumentException> { emptyBlaFirst.compare(emptyBla) }
            assertThrows<IllegalArgumentException> { emptyBla.compare(emptyBlaFirst) }

            assertEquals(Order.LESSER, tupleLattice.compare(emptyEmpty, emptyBla))
            assertEquals(Order.LESSER, tupleLattice.compare(emptyEmpty, blaEmpty))
            assertEquals(Order.GREATER, tupleLattice.compare(emptyBla, emptyEmpty))
            assertEquals(Order.GREATER, tupleLattice.compare(blaEmpty, emptyEmpty))
            assertEquals(Order.UNEQUAL, tupleLattice.compare(blaEmpty, emptyBla))
            assertEquals(Order.UNEQUAL, tupleLattice.compare(emptyBla, blaEmpty))

            val emptyEmptyGlb = tupleLattice.glb(blaEmpty, emptyBla)
            assertNotSame(emptyEmpty, emptyEmptyGlb)
            assertEquals(emptyEmpty, emptyEmptyGlb)
            assertEquals(Order.LESSER, tupleLattice.compare(emptyEmptyGlb, blaEmpty))
            assertEquals(Order.LESSER, tupleLattice.compare(emptyEmptyGlb, emptyBla))

            val emptyEmptyDuplicate = tupleLattice.duplicate(emptyEmpty)
            assertNotSame(emptyEmpty, emptyEmptyDuplicate)
            assertEquals(emptyEmpty, emptyEmptyDuplicate)
            assertEquals(emptyEmpty.hashCode(), emptyEmptyDuplicate.hashCode())

            val blaBla = runBlocking { tupleLattice.lub(emptyBla, blaEmpty) }
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
    }

    @Test
    fun testTripleLattice() {
        runBlocking {
            val tripleLattice =
                TripleLattice<
                    PowersetLattice.Element<String>,
                    PowersetLattice.Element<String>,
                    PowersetLattice.Element<String>,
                >(
                    PowersetLattice<String>(),
                    PowersetLattice<String>(),
                    PowersetLattice<String>(),
                )

            val bottom = tripleLattice.bottom

            val emptyEmptyEmpty =
                TripleLattice.Element(
                    PowersetLattice.Element<String>(),
                    PowersetLattice.Element<String>(),
                    PowersetLattice.Element<String>(),
                )
            assertNotSame(bottom, emptyEmptyEmpty)
            assertEquals(bottom, emptyEmptyEmpty)
            assertEquals(bottom.hashCode(), emptyEmptyEmpty.hashCode())

            val emptyEmptyBla =
                TripleLattice.Element(
                    PowersetLattice.Element<String>(),
                    PowersetLattice.Element<String>(),
                    PowersetLattice.Element("bla"),
                )
            val emptyBlaEmpty =
                TripleLattice.Element(
                    PowersetLattice.Element<String>(),
                    PowersetLattice.Element("bla"),
                    PowersetLattice.Element<String>(),
                )
            val blaEmptyEmpty =
                TripleLattice.Element(
                    PowersetLattice.Element("bla"),
                    PowersetLattice.Element<String>(),
                    PowersetLattice.Element<String>(),
                )

            val emptyEmptyBla2 = tripleLattice.duplicate(emptyEmptyBla)
            assertEquals(Order.EQUAL, tripleLattice.compare(emptyEmptyBla, emptyEmptyBla2))
            assertEquals(emptyEmptyBla, emptyEmptyBla2)
            assertNotSame(emptyEmptyBla, emptyEmptyBla2)
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

            assertThrows<IllegalArgumentException> { emptyBlaFirst.compare(emptyEmptyBla) }
            assertThrows<IllegalArgumentException> { emptyEmptyBla.compare(emptyBlaFirst) }

            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, emptyEmptyBla))
            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, emptyBlaEmpty))
            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, blaEmptyEmpty))
            assertEquals(Order.GREATER, tripleLattice.compare(emptyEmptyBla, emptyEmptyEmpty))
            assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyEmpty, emptyEmptyEmpty))
            assertEquals(Order.GREATER, tripleLattice.compare(emptyBlaEmpty, emptyEmptyEmpty))
            assertEquals(Order.UNEQUAL, tripleLattice.compare(blaEmptyEmpty, emptyEmptyBla))
            assertEquals(Order.UNEQUAL, tripleLattice.compare(emptyEmptyBla, blaEmptyEmpty))
            assertEquals(Order.UNEQUAL, tripleLattice.compare(emptyBlaEmpty, blaEmptyEmpty))

            val blaEmptyBla = runBlocking { tripleLattice.lub(emptyEmptyBla, blaEmptyEmpty) }
            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmpty, blaEmptyBla))
            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyBla, blaEmptyBla))
            assertEquals(Order.LESSER, tripleLattice.compare(blaEmptyEmpty, blaEmptyBla))
            assertEquals(Order.UNEQUAL, tripleLattice.compare(emptyBlaEmpty, blaEmptyBla))
            assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyBla, emptyEmptyEmpty))
            assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyBla, emptyEmptyBla))
            assertEquals(Order.GREATER, tripleLattice.compare(blaEmptyBla, blaEmptyEmpty))

            val emptyEmptyEmptyGlb = tripleLattice.glb(blaEmptyBla, emptyBlaEmpty)
            assertNotSame(emptyEmptyEmptyGlb, emptyEmptyEmpty)
            assertEquals(emptyEmptyEmptyGlb, emptyEmptyEmpty)
            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmptyGlb, blaEmptyBla))
            assertEquals(Order.LESSER, tripleLattice.compare(emptyEmptyEmptyGlb, emptyBlaEmpty))

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
}
