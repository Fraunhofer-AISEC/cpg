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
package de.fraunhofer.aisec.cpg.analysis.abstracteval

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.BOTTOM
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.INFINITE
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.NEGATIVE_INFINITE
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bounded
import kotlin.test.*
import kotlin.test.Test
import org.junit.jupiter.api.assertDoesNotThrow

class LatticeIntervalTest {
    @Test
    fun testCreate() {
        assertDoesNotThrow { BOTTOM }
        assertDoesNotThrow { Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) }
        assertDoesNotThrow { Bounded(INFINITE, INFINITE) }
        assertDoesNotThrow { Bounded(NEGATIVE_INFINITE, INFINITE) }
        assertDoesNotThrow { Bounded(0, 0) }
        assertDoesNotThrow { Bounded(-5, 5) }

        // Test whether the arguments are switched if necessary
        assertEquals(Bounded(NEGATIVE_INFINITE, INFINITE), Bounded(INFINITE, NEGATIVE_INFINITE))
        assertEquals(Bounded(-5, 5), Bounded(5, -5))
    }

    @Test
    fun testCompare() {
        // comparison including BOTTOM
        assertEquals(0, BOTTOM.compareTo(BOTTOM))
        assertEquals(-1, BOTTOM.compareTo(Bounded(-1, 1)))
        assertEquals(-1, BOTTOM.compareTo(Bounded(NEGATIVE_INFINITE, INFINITE)))
        assertEquals(1, Bounded(-1, 1).compareTo(BOTTOM))
        assertEquals(1, Bounded(NEGATIVE_INFINITE, INFINITE).compareTo(BOTTOM))

        // comparison with non-overlapping intervals
        assertEquals(-1, Bounded(NEGATIVE_INFINITE, -1).compareTo(Bounded(1, INFINITE)))
        assertEquals(1, Bounded(1, INFINITE).compareTo(Bounded(NEGATIVE_INFINITE, -1)))

        // comparison with overlapping intervals
        assertEquals(0, Bounded(NEGATIVE_INFINITE, 0).compareTo(Bounded(0, INFINITE)))
        assertEquals(0, Bounded(-4, 2).compareTo(Bounded(-2, 4)))
        assertEquals(0, Bounded(-5, 5).compareTo(Bounded(1, 3)))
        assertEquals(0, Bounded(-3, 1).compareTo(Bounded(-5, 5)))
    }

    @Test
    fun testEquals() {
        // comparison with BOTTOM
        assertFalse(BOTTOM.equals(null))
        assertFalse(BOTTOM.equals(Bounded(0, 0)))
        assertFalse(Bounded(0, 0).equals(BOTTOM))
        assertEquals(BOTTOM, BOTTOM)

        // comparison with different Intervals
        assertNotEquals(Bounded(NEGATIVE_INFINITE, 0), Bounded(0, INFINITE))
        assertNotEquals(Bounded(0, 10), Bounded(0, 9))
        assertNotEquals(Bounded(0, 10), Bounded(1, 10))
        assertNotEquals(Bounded(0, 10), Bounded(5, 6))
        assertNotEquals(Bounded(0, 9), Bounded(0, 10))
        assertNotEquals(Bounded(1, 10), Bounded(0, 10))
        assertNotEquals(Bounded(5, 6), Bounded(0, 10))

        // comparison with same Intervals
        assertEquals(Bounded(-5, 5), Bounded(-5, 5))
        assertEquals(Bounded(0, 0), Bounded(0, 0))
        assertEquals(Bounded(NEGATIVE_INFINITE, INFINITE), Bounded(NEGATIVE_INFINITE, INFINITE))
    }

    @Test
    fun testAddition() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM + Bounded(5, 5))
        assertEquals(BOTTOM, Bounded(5, 5) + BOTTOM)
        assertEquals(BOTTOM, BOTTOM + Bounded(NEGATIVE_INFINITE, INFINITE))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE) + BOTTOM)
        assertEquals(BOTTOM, BOTTOM + BOTTOM)

        // Without BOTTOM
        assertEquals(Bounded(INFINITE, INFINITE), Bounded(INFINITE, INFINITE) + Bounded(-5, 5))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) + Bounded(-5, 5),
        )
        assertEquals(Bounded(INFINITE, INFINITE), Bounded(-5, 5) + Bounded(INFINITE, INFINITE))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
            Bounded(-5, 5) + Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
        )
        assertEquals(Bounded(-8, 8), Bounded(-5, 5) + Bounded(-3, 3))

        // Illegal Operations
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) + Bounded(0, INFINITE),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(INFINITE, INFINITE) + Bounded(NEGATIVE_INFINITE, 0),
        )
    }

    @Test
    fun testSubtraction() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM - Bounded(5, 5))
        assertEquals(BOTTOM, Bounded(5, 5) - BOTTOM)
        assertEquals(BOTTOM, BOTTOM - Bounded(NEGATIVE_INFINITE, INFINITE))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE) - BOTTOM)
        assertEquals(BOTTOM, BOTTOM - BOTTOM)

        // Without BOTTOM
        assertEquals(Bounded(INFINITE, INFINITE), Bounded(INFINITE, INFINITE) - Bounded(-5, 5))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) - Bounded(-5, 5),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
            Bounded(-5, 5) - Bounded(INFINITE, INFINITE),
        )
        assertEquals(
            Bounded(INFINITE, INFINITE),
            Bounded(-5, 5) - Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
        )
        assertEquals(Bounded(-8, 8), Bounded(-5, 5) - Bounded(-3, 3))

        // Illegal Operations
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(-10, INFINITE) - Bounded(0, INFINITE),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(NEGATIVE_INFINITE, 10) - Bounded(NEGATIVE_INFINITE, 0),
        )
    }

    @Test
    fun testMultiplication() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM * Bounded(5, 5))
        assertEquals(BOTTOM, Bounded(5, 5) * BOTTOM)
        assertEquals(BOTTOM, BOTTOM * Bounded(NEGATIVE_INFINITE, INFINITE))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE) * BOTTOM)
        assertEquals(BOTTOM, BOTTOM * BOTTOM)

        // Without BOTTOM
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(INFINITE, INFINITE) * Bounded(-5, 5),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) * Bounded(-5, 5),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(-5, 5) * Bounded(INFINITE, INFINITE),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(-5, 5) * Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
        )
        assertEquals(Bounded(15, 15), Bounded(-5, 5) * Bounded(-3, 3))

        // Illegal Operations
        assertEquals(
            Bounded(NEGATIVE_INFINITE, 0),
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) * Bounded(0, INFINITE),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, 0),
            Bounded(INFINITE, INFINITE) * Bounded(NEGATIVE_INFINITE, 0),
        )
    }

    @Test
    fun testDivision() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM / Bounded(5, 5))
        assertEquals(BOTTOM, Bounded(5, 5) / BOTTOM)
        assertEquals(BOTTOM, BOTTOM / Bounded(NEGATIVE_INFINITE, INFINITE))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE) / BOTTOM)
        assertEquals(BOTTOM, BOTTOM / BOTTOM)

        // Without BOTTOM
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(INFINITE, INFINITE) / Bounded(-5, 5),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) / Bounded(-5, 5),
        )
        assertEquals(Bounded(0, 0), Bounded(-5, 5) / Bounded(INFINITE, INFINITE))
        assertEquals(Bounded(0, 0), Bounded(-5, 5) / Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE))
        assertEquals(Bounded(5, 5), Bounded(-15, 15) / Bounded(-3, 3))

        // Illegal Operations
        assertEquals(
            LatticeInterval.TOP,
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) / Bounded(1, INFINITE),
        )

        assertEquals(
            LatticeInterval.TOP,
            Bounded(INFINITE, INFINITE) / Bounded(NEGATIVE_INFINITE, 1),
        )

        assertEquals(
            LatticeInterval.TOP,
            Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) / Bounded(NEGATIVE_INFINITE, 1),
        )

        assertEquals(LatticeInterval.TOP, Bounded(INFINITE, INFINITE) / Bounded(1, INFINITE))

        assertEquals(LatticeInterval.TOP, Bounded(2, 4) / Bounded(-1, 0))
    }

    @Test
    fun testModulo() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM % Bounded(5, 5))
        assertEquals(BOTTOM, Bounded(5, 5) % BOTTOM)
        assertEquals(BOTTOM, BOTTOM % Bounded(NEGATIVE_INFINITE, INFINITE))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE) % BOTTOM)
        assertEquals(BOTTOM, BOTTOM % BOTTOM)

        // Without BOTTOM
        assertEquals(Bounded(0, 10), Bounded(INFINITE, INFINITE) % Bounded(5, 10))
        assertEquals(Bounded(0, 10), Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE) % Bounded(5, 10))
        assertEquals(Bounded(-5, 5), Bounded(-5, 5) % Bounded(INFINITE, INFINITE))
        assertEquals(Bounded(-1, 1), Bounded(-10, 10) % Bounded(-3, 3))

        // Illegal Operations

        assertEquals(LatticeInterval.TOP, Bounded(-5, 5) % Bounded(0, 5))

        assertEquals(LatticeInterval.TOP, Bounded(-5, 5) % Bounded(-5, 0))

        assertEquals(
            LatticeInterval.TOP,
            Bounded(-5, 5) % Bounded(NEGATIVE_INFINITE, NEGATIVE_INFINITE),
        )
    }

    @Test
    fun testJoin() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM.join(Bounded(5, 5)))
        assertEquals(BOTTOM, Bounded(5, 5).join(BOTTOM))
        assertEquals(BOTTOM, BOTTOM.join(Bounded(NEGATIVE_INFINITE, INFINITE)))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE).join(BOTTOM))
        assertEquals(BOTTOM, BOTTOM.join(BOTTOM))

        // Without BOTTOM
        assertEquals(
            Bounded(NEGATIVE_INFINITE, 10),
            Bounded(5, 10).join(Bounded(NEGATIVE_INFINITE, -5)),
        )
        assertEquals(Bounded(-10, INFINITE), Bounded(-10, -5).join(Bounded(5, INFINITE)))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(0, 0).join(Bounded(NEGATIVE_INFINITE, INFINITE)),
        )
        assertEquals(Bounded(-10, 10), Bounded(9, 10).join(Bounded(-10, -9)))
    }

    @Test
    fun testMeet() {
        // With BOTTOM
        assertEquals(Bounded(5, 5), BOTTOM.meet(Bounded(5, 5)))
        assertEquals(Bounded(5, 5), Bounded(5, 5).meet(BOTTOM))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            BOTTOM.meet(Bounded(NEGATIVE_INFINITE, INFINITE)),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(NEGATIVE_INFINITE, INFINITE).meet(BOTTOM),
        )
        assertEquals(BOTTOM, BOTTOM.meet(BOTTOM))

        // Without BOTTOM
        assertEquals(BOTTOM, Bounded(5, 10).meet(Bounded(NEGATIVE_INFINITE, -5)))
        assertEquals(BOTTOM, Bounded(-10, -5).meet(Bounded(5, INFINITE)))
        assertEquals(BOTTOM, Bounded(9, 10).meet(Bounded(-10, -9)))
        assertEquals(Bounded(0, 0), Bounded(0, 0).meet(Bounded(NEGATIVE_INFINITE, INFINITE)))
        assertEquals(Bounded(-9, 9), Bounded(-9, 10).meet(Bounded(-10, 9)))
    }

    @Test
    fun testWiden() {
        // With BOTTOM
        assertEquals(Bounded(5, 5), BOTTOM.widen(Bounded(5, 5)))
        assertEquals(Bounded(5, 5), Bounded(5, 5).widen(BOTTOM))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            BOTTOM.widen(Bounded(NEGATIVE_INFINITE, INFINITE)),
        )
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(NEGATIVE_INFINITE, INFINITE).widen(BOTTOM),
        )
        assertEquals(BOTTOM, BOTTOM.widen(BOTTOM))

        // Without BOTTOM
        assertEquals(
            Bounded(NEGATIVE_INFINITE, 10),
            Bounded(5, 10).widen(Bounded(NEGATIVE_INFINITE, -5)),
        )
        assertEquals(Bounded(-10, INFINITE), Bounded(-10, -5).widen(Bounded(5, INFINITE)))
        assertEquals(
            Bounded(NEGATIVE_INFINITE, INFINITE),
            Bounded(0, 0).widen(Bounded(NEGATIVE_INFINITE, INFINITE)),
        )
        assertEquals(Bounded(NEGATIVE_INFINITE, 10), Bounded(9, 10).widen(Bounded(-10, -9)))
        assertEquals(Bounded(-10, INFINITE), Bounded(-10, -9).widen(Bounded(9, 10)))
    }

    @Test
    fun testNarrow() {
        // With BOTTOM
        assertEquals(BOTTOM, BOTTOM.narrow(Bounded(5, 5)))
        assertEquals(BOTTOM, Bounded(5, 5).narrow(BOTTOM))
        assertEquals(BOTTOM, BOTTOM.narrow(Bounded(NEGATIVE_INFINITE, INFINITE)))
        assertEquals(BOTTOM, Bounded(NEGATIVE_INFINITE, INFINITE).narrow(BOTTOM))
        assertEquals(BOTTOM, BOTTOM.narrow(BOTTOM))

        // Without BOTTOM
        assertEquals(Bounded(5, 10), Bounded(5, 10).narrow(Bounded(NEGATIVE_INFINITE, -5)))
        assertEquals(Bounded(-10, -5), Bounded(-10, -5).narrow(Bounded(5, INFINITE)))
        assertEquals(Bounded(0, 0), Bounded(0, 0).narrow(Bounded(NEGATIVE_INFINITE, INFINITE)))
        assertEquals(Bounded(-5, 5), Bounded(NEGATIVE_INFINITE, -5).narrow(Bounded(5, 10)))
        assertEquals(Bounded(-5, 5), Bounded(5, INFINITE).narrow(Bounded(-10, -5)))
        assertEquals(Bounded(0, 0), Bounded(NEGATIVE_INFINITE, INFINITE).narrow(Bounded(0, 0)))
    }

    @Test
    fun testToString() {
        //        assertEquals("BOTTOM", BOTTOM.toString())
        //        assertEquals(
        //            "[NEGATIVE_INFINITE, INFINITE]",
        //            Bounded(NEGATIVE_INFINITE, INFINITE).toString()
        //        )
        assertEquals("[-5, 5]", Bounded(-5, 5).toString())
    }

    @Test
    fun testWrapper() {
        val bottomWrapper = IntervalLattice(BOTTOM)
        val zeroWrapper = IntervalLattice(Bounded(0, 0))
        val outerWrapper = IntervalLattice(Bounded(-5, 5))
        val infinityWrapper = IntervalLattice(Bounded(NEGATIVE_INFINITE, INFINITE))

        // compare to
        assertEquals(-1, bottomWrapper.compareTo(zeroWrapper))
        assertEquals(0, zeroWrapper.compareTo(zeroWrapper))
        assertEquals(1, zeroWrapper.compareTo(bottomWrapper))

        // contains
        assertFalse(bottomWrapper.contains(zeroWrapper))
        assertFalse(zeroWrapper.contains(bottomWrapper))
        assertFalse(zeroWrapper.contains(outerWrapper))
        assertTrue(outerWrapper.contains(zeroWrapper))

        // widen
        assertEquals(outerWrapper, outerWrapper.widen(zeroWrapper))
        assertEquals(infinityWrapper, zeroWrapper.widen(outerWrapper))

        // narrow
        assertEquals(outerWrapper, infinityWrapper.narrow(outerWrapper))
        assertEquals(outerWrapper, outerWrapper.narrow(zeroWrapper))
        assertEquals(outerWrapper, outerWrapper.narrow(infinityWrapper))
    }
}
