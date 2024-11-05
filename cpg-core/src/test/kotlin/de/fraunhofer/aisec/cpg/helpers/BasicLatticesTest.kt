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
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLatticeT
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
}
