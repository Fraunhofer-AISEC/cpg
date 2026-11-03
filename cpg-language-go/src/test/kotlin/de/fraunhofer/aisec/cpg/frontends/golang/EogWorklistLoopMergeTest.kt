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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayState
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.IdentityHashMap
import java.util.Objects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that the EOG worklist processes a loop before the code after it, and a merge after all of
 * its predecessors.
 *
 * Otherwise, the node after a loop or merge is transferred more than once, and [TagOverlaysPass]
 * attaches an equal overlay each time. For each function of `loopvariants.go`, this re-runs the
 * fixpoint with a transformation that records each processed edge and asserts:
 * - the node after the loop or merge (the last `log.Printf` call) is transferred exactly once;
 * - every `log.Printf` call has exactly one [Tagged] overlay;
 * - the total number of transfers is not higher than on `main` (see [mainTransfers]).
 */
class EogWorklistLoopMergeTest {

    class Tagged(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean = other is Tagged && super.equals(other)

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    /**
     * The total number of transfers per function on `main` (ea05ac4993b), counted like in the test.
     * On `main` the order of the worklist, and with it some of these numbers, varies between JVM
     * runs (e.g. 37 or 41 for `rangeBreakCond`, 33 or 39 for `switchThreeCases`); these are the
     * smallest numbers seen in several runs.
     */
    private val mainTransfers =
        mapOf(
            "rangePlain" to 29,
            "forIndex" to 33,
            "rangeContinue" to 41,
            "rangeBreak" to 15,
            "ifElse" to 18,
            "rangeBreakCond" to 37,
            "rangeBreakCondTagged" to 49,
            "rangeReturn" to 47,
            "nestedLoops" to 61,
            "switchThreeCases" to 33,
            "ifNoElse" to 14,
            "ifElseInLoop" to 49,
            "nestedIfElse" to 33,
        )

    @Test
    fun testNoDuplicateTransfersOrOverlays() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("loopvariants.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
                it.registerPass<TagOverlaysPass>()
                it.configurePass<TagOverlaysPass>(
                    TagOverlaysPass.Configuration(
                        tag = tag { each<Call>("log.Printf").with { Tagged() } }
                    )
                )
            }
        val tu = result.translationUnits.first()
        val ctx = result.finalCtx
        val pass = TagOverlaysPass(ctx)

        for ((name, transfersOnMain) in mainTransfers) {
            val fn = tu.functions[name]
            assertNotNull(fn, name)
            val lattice = NodeToOverlayState(PowersetLattice())
            val startState = pass.getInitialState(lattice, fn)
            val edgeOrder = mutableListOf<EvaluationOrder>()
            lattice.iterateEOG(
                fn.nextEOGEdges,
                startState,
                { l, edge, state ->
                    edgeOrder += edge
                    pass.transfer(l, edge, state)
                },
            )

            // Transfers per node (edge.end == the node the edge points to).
            val counts: MutableMap<Node, Int> = IdentityHashMap()
            for (e in edgeOrder) counts.merge(e.end, 1) { a, b -> a + b }

            // The "after" node is the last log.Printf call in the function — the node after the
            // loop/merge. It must be transferred exactly once.
            val afterCall =
                fn.allChildren<Call>()
                    .filter { it.name.toString().contains("Printf") }
                    .maxByOrNull { it.location?.region?.startLine ?: -1 }
            assertNotNull(afterCall, "[$name] has no log.Printf call")
            val afterTransfers = counts[afterCall] ?: 0
            assertEquals(
                1,
                afterTransfers,
                "[$name] after-node ${afterCall.name} transferred $afterTransfers times, expected 1",
            )

            // Every tagged call must have exactly one overlay.
            for (call in fn.allChildren<Call>().filter { it.name.toString().contains("Printf") }) {
                val overlays = call.overlays.filterIsInstance<Tagged>().size
                assertEquals(
                    1,
                    overlays,
                    "[$name] call ${call.name}@${call.location} has $overlays overlays, expected 1",
                )
            }

            assertTrue(
                edgeOrder.size <= transfersOnMain,
                "[$name] ${edgeOrder.size} transfers, more than on main ($transfersOnMain)",
            )
        }
    }
}
