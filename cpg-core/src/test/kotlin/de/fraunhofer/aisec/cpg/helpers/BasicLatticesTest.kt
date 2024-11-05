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
import de.fraunhofer.aisec.cpg.helpers.functional.emptyMapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.emptyPowersetLattice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class BasicLatticesTest {
    @Test
    fun testPowersetLattice() {
        val emptyLattice1: PowersetLatticeT<Node> = emptyPowersetLattice<Node>()
        val emptyLattice2 = emptyPowersetLattice<Node>()
        assertEquals(0, emptyLattice1.compareTo(emptyLattice2))
        assertEquals(emptyLattice1, emptyLattice2)

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

        val aBlaLattice1 =
            MapLattice<String, Set<String>>(mapOf("a" to PowersetLattice(setOf("bla"))))
        val bBlaLattice2 =
            MapLattice<String, Set<String>>(mapOf("a" to PowersetLattice(setOf("bla"))))
        assertEquals(0, aBlaLattice1.compareTo(bBlaLattice2))
        assertEquals(aBlaLattice1, bBlaLattice2)
        assertNotSame(aBlaLattice1, bBlaLattice2)

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
        // assertNotSame(emptyLattice1.elements, emptyDuplicate.elements)
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
    }
}
