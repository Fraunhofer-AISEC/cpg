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
package de.fraunhofer.aisec.cpg.graph.edges.collections

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises [EdgeSet]'s inline (0/1/2 element) storage and its [HashSet] overflow (3+ elements)
 * through a concrete [de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflows] set.
 */
class EdgeSetTest {
    @Test
    fun testInlineOverflowAndDeduplicate() {
        with(TestLanguageFrontend()) {
            val n = newLiteral(0)
            val t1 = newLiteral(1)
            val t2 = newLiteral(2)
            val t3 = newLiteral(3)
            val t4 = newLiteral(4)

            // Two inline elements.
            n.nextDFG += t1
            n.nextDFG += t2
            assertEquals(2, n.nextDFGEdges.size)

            // Third and fourth spill into the overflow set.
            n.nextDFG += t3
            n.nextDFG += t4
            assertEquals(4, n.nextDFGEdges.size)
            assertEquals(setOf<Node>(t1, t2, t3, t4), n.nextDFG)

            // Adding an already-present target (equal edge) is a no-op.
            n.nextDFG += t3
            assertEquals(4, n.nextDFGEdges.size)
        }
    }

    @Test
    fun testRemoveShrinksInlineAndOverflow() {
        with(TestLanguageFrontend()) {
            val n = newLiteral(0)
            val targets = (1..4).map { newLiteral(it) }
            targets.forEach { n.nextDFG += it }
            assertEquals(4, n.nextDFGEdges.size)

            // Remove an overflow element.
            assertTrue(n.nextDFG.remove(targets[3]))
            assertEquals(3, n.nextDFGEdges.size)

            // Remove an inline element.
            assertTrue(n.nextDFG.remove(targets[0]))
            assertEquals(2, n.nextDFGEdges.size)
            assertFalse(targets[0] in n.nextDFG)

            // Removing an absent target.
            assertFalse(n.nextDFG.remove(newLiteral(99)))
        }
    }

    @Test
    fun testIteratorAndIteratorRemove() {
        with(TestLanguageFrontend()) {
            val n = newLiteral(0)
            (1..4).forEach { n.nextDFG += newLiteral(it) }

            // The edge iterator visits every element (inline + overflow).
            assertEquals(4, n.nextDFGEdges.iterator().asSequence().count())

            // Remove everything via the iterator.
            val it = n.nextDFGEdges.iterator()
            while (it.hasNext()) {
                it.next()
                it.remove()
            }
            assertEquals(0, n.nextDFGEdges.size)
        }
    }

    @Test
    fun testRemoveIfAndClear() {
        with(TestLanguageFrontend()) {
            val n = newLiteral(0)
            val keep = newLiteral(1)
            val drop2 = newLiteral(2)
            val drop3 = newLiteral(3)
            listOf(keep, drop2, drop3).forEach { n.nextDFG += it }

            assertTrue(n.nextDFGEdges.removeIf { it.end == drop2 || it.end == drop3 })
            assertEquals(setOf<Node>(keep), n.nextDFG)

            n.nextDFGEdges.clear()
            assertTrue(n.nextDFGEdges.isEmpty())
        }
    }

    /**
     * [EdgeSet.removeAll] must only fire `onRemove` for edges it actually removed. Notifying an
     * edge that is not a member (or a duplicate in the input) would run the mirror-property
     * bookkeeping ([de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection]) for an
     * edge that still legitimately lives elsewhere, evicting it from the mirror and corrupting the
     * next/prev DFG invariant.
     */
    @Test
    fun testRemoveAllOnlyNotifiesActuallyRemovedEdges() {
        with(TestLanguageFrontend()) {
            val n = newLiteral(0)
            val m = newLiteral(10)
            val t1 = newLiteral(1)
            val t2 = newLiteral(2)

            // n -> t1, n -> t2
            n.nextDFG += t1
            n.nextDFG += t2
            // m -> t1 shares the target t1 but is NOT part of n's edge set. Its mirror lives in
            // t1.prevDFGEdges.
            m.nextDFG += t1

            val foreignEdge = m.nextDFGEdges.single { it.end == t1 }
            assertTrue(foreignEdge in t1.prevDFGEdges)

            // Remove n -> t1 together with the foreign edge that is not a member of n.nextDFGEdges.
            // Only n -> t1 must actually be removed and notified.
            val ownEdge = n.nextDFGEdges.single { it.end == t1 }
            assertTrue(n.nextDFGEdges.removeAll(listOf(ownEdge, foreignEdge)))

            // n -> t1 is gone, n -> t2 remains.
            assertEquals(setOf<Node>(t2), n.nextDFG)

            // The foreign edge m -> t1 must be untouched on both sides of the mirror. With the
            // previous (buggy) behavior, notifying foreignEdge here would have evicted it from
            // t1.prevDFGEdges while leaving it in m.nextDFG.
            assertTrue(t1 in m.nextDFG)
            assertTrue(foreignEdge in t1.prevDFGEdges)
        }
    }
}
