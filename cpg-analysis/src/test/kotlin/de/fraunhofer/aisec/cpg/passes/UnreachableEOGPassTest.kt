/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.functional.Order
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.testcases.Passes
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@Ignore
// TODO Mathias
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnreachableEOGPassTest {
    private lateinit var tu: TranslationUnitDeclaration

    @BeforeAll
    fun beforeAll() {
        tu = Passes.getUnreachability().components.first().translationUnits.first()
    }

    @Test
    fun testIfBothPossible() {
        val method = tu.functions["ifBothPossible"]
        assertNotNull(method)

        val ifStatement = method.ifs.firstOrNull()
        assertNotNull(ifStatement)

        for (edge in ifStatement.nextEOGEdges) {
            assertFalse(edge.unreachable)
        }
    }

    @Test
    fun testIfTrue() {
        val method = tu.functions["ifTrue"]
        assertNotNull(method)

        val ifStatement = method.ifs.firstOrNull()
        assertNotNull(ifStatement)

        // Check if the then-branch is set as reachable including all the edges until reaching the
        // print
        val thenDecl = ifStatement.nextEOGEdges[0]
        assertFalse(thenDecl.unreachable)
        assertEquals(1, thenDecl.end.nextEOGEdges.size)
        // The "++"
        val incOp = thenDecl.end.nextEOGEdges[0]
        assertFalse(incOp.unreachable)
        assertEquals(1, incOp.end.nextEOGEdges.size)
        // The block
        val thenCompound = incOp.end.nextEOGEdges[0]
        assertFalse(thenCompound.unreachable)
        assertEquals(1, thenCompound.end.nextEOGEdges.size)
        // There's the outgoing EOG edge to the statement after the branching
        val thenExit = thenCompound.end.nextEOGEdges[0]
        assertFalse(thenExit.unreachable)

        // Check if the else-branch is set as unreachable including all the edges until reaching the
        // print
        val elseDecl = ifStatement.nextEOGEdges[1]
        assertTrue(elseDecl.unreachable)
        assertEquals(1, elseDecl.end.nextEOGEdges.size)
        // The "--"
        val decOp = elseDecl.end.nextEOGEdges[0]
        assertTrue(decOp.unreachable)
        assertEquals(1, decOp.end.nextEOGEdges.size)
        // The block
        val elseCompound = decOp.end.nextEOGEdges[0]
        assertTrue(elseCompound.unreachable)
        assertEquals(1, elseCompound.end.nextEOGEdges.size)
        // There's the outgoing EOG edge to the statement after the branching
        val elseExit = elseCompound.end.nextEOGEdges[0]
        assertTrue(elseExit.unreachable)

        // After the branching, it's reachable again. Check that we found the merge node and that we
        // continue with reachable edges.
        assertEquals(thenExit.end, elseExit.end)
        val mergeNode = thenExit.end
        assertEquals(1, mergeNode.nextEOGEdges.size)
        assertFalse(mergeNode.nextEOGEdges[0].unreachable)
    }

    @Test
    fun testIfFalse() {
        val method = tu.functions["ifFalse"]
        assertNotNull(method)

        val ifStatement = method.ifs.firstOrNull()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[1].unreachable)
        assertTrue(ifStatement.nextEOGEdges[0].unreachable)
    }

    @Test
    fun testIfTrueComputed() {
        val method = tu.functions["ifTrueComputed"]
        assertNotNull(method)

        val ifStatement = method.ifs.firstOrNull()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[0].unreachable)
        assertTrue(ifStatement.nextEOGEdges[1].unreachable)
    }

    @Test
    fun testIfFalseComputed() {
        val method = tu.functions["ifFalseComputed"]
        assertNotNull(method)

        val ifStatement = method.ifs.firstOrNull()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[1].unreachable)
        assertTrue(ifStatement.nextEOGEdges[0].unreachable)
    }

    @Test
    fun testWhileTrueEndless() {
        val method = tu.functions["whileTrueEndless"]
        assertNotNull(method)

        val whileStatement = method.whileLoops.firstOrNull()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[0].unreachable)
        assertTrue(whileStatement.nextEOGEdges[1].unreachable)
    }

    @Test
    fun testWhileTrue() {
        val method = tu.functions["whileTrue"]
        assertNotNull(method)

        val whileStatement = method.whileLoops.firstOrNull()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[0].unreachable)
        assertFalse(whileStatement.nextEOGEdges[1].unreachable)
    }

    @Test
    fun testWhileComputedTrue() {
        val method = tu.functions["whileComputedTrue"]
        assertNotNull(method)

        val whileStatement = method.whileLoops.firstOrNull()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[0].unreachable)
        assertTrue(whileStatement.nextEOGEdges[1].unreachable)
    }

    @Test
    fun testWhileFalse() {
        val method = tu.functions["whileFalse"]
        assertNotNull(method)

        val whileStatement = method.whileLoops.firstOrNull()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[1].unreachable)
        assertTrue(whileStatement.nextEOGEdges[0].unreachable)
    }

    @Test
    fun testWhileComputedFalse() {
        val method = tu.functions["whileComputedFalse"]
        assertNotNull(method)

        val whileStatement = method.whileLoops.firstOrNull()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[1].unreachable)
        assertTrue(whileStatement.nextEOGEdges[0].unreachable)
    }

    @Test
    fun testWhileUnknown() {
        val method = tu.functions["whileUnknown"]
        assertNotNull(method)

        val whileStatement = method.whileLoops.firstOrNull()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[1].unreachable)
        assertFalse(whileStatement.nextEOGEdges[0].unreachable)
    }

    @Test
    fun testReachabilityLattice() {
        runBlocking {
            val lattice = ReachabilityLattice()
            val bottom = lattice.bottom
            assertEquals(Reachability.BOTTOM, bottom.reachability)
            val unreachable = ReachabilityLattice.Element(Reachability.UNREACHABLE)
            val reachable = ReachabilityLattice.Element(Reachability.REACHABLE)
            val reachable2 = lattice.duplicate(reachable)
            assertNotSame(reachable, reachable2)
            assertEquals(reachable, reachable2)
            assertEquals(setOf(bottom, unreachable, reachable), lattice.elements)

            assertEquals(bottom, lattice.glb(bottom, unreachable))
            assertEquals(bottom, lattice.glb(bottom, reachable))
            assertEquals(unreachable, lattice.glb(unreachable, reachable))
            assertEquals(reachable, lattice.glb(reachable, reachable2))
            assertEquals(bottom, lattice.glb(unreachable, bottom))
            assertEquals(bottom, lattice.glb(reachable, bottom))
            assertEquals(unreachable, lattice.glb(reachable, unreachable))
            assertEquals(reachable, lattice.glb(reachable, reachable2))

            assertEquals(unreachable, runBlocking { lattice.lub(bottom, unreachable) })
            assertEquals(reachable, runBlocking { lattice.lub(bottom, reachable) })
            assertEquals(reachable, runBlocking { lattice.lub(unreachable, reachable) })
            assertEquals(reachable, runBlocking { lattice.lub(reachable, reachable2) })
            assertEquals(unreachable, runBlocking { lattice.lub(unreachable, bottom) })
            assertEquals(reachable, runBlocking { lattice.lub(reachable, bottom) })
            assertEquals(reachable, runBlocking { lattice.lub(reachable, unreachable) })
            assertEquals(reachable, runBlocking { lattice.lub(reachable, reachable2) })

            assertEquals(Order.LESSER, lattice.compare(bottom, unreachable))
            assertEquals(Order.LESSER, lattice.compare(bottom, reachable))
            assertEquals(Order.LESSER, lattice.compare(unreachable, reachable))
            assertEquals(Order.EQUAL, lattice.compare(bottom, bottom.duplicate()))
            assertEquals(Order.EQUAL, lattice.compare(unreachable, unreachable.duplicate()))
            assertEquals(Order.EQUAL, lattice.compare(reachable, reachable2))
            assertEquals(Order.GREATER, lattice.compare(unreachable, bottom))
            assertEquals(Order.GREATER, lattice.compare(reachable, bottom))
            assertEquals(Order.GREATER, lattice.compare(reachable, unreachable))

            assertThrows<IllegalArgumentException> {
                bottom.compare(PowersetLattice.Element<String>())
            }
        }
    }
}
