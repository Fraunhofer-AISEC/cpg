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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [ifdsReachingSources]. These build small explicit CPG graphs (no external oracle) and
 * check: node-set equivalence to the legacy [followXUntilHitNodes] engine on non-recursive graphs,
 * the recursion-completeness gap where the engine under-reports, the k-limiting knob on DFG and
 * EOG, and termination on cycles / self-recursion.
 */
class IfdsReachingSourcesTest {

    /**
     * HEADLINE: recursion completeness gap. On a recursive interprocedural DFG the legacy engine
     * under-reports (its same-call-site recursion cut returns the empty set), while IFDS(k=∞)
     * returns the complete set {s}.
     *
     * Graph (prev-DFG, backward): q <-out[cG]- h <-out[cF]- h (self-recursion), h <-in[cF]- r1
     * <-in[cF]- r2 <-in[cG]- s.
     */
    @Test
    fun recursionCompletenessGap() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val h = newLiteral(2)
            val r1 = newLiteral(3)
            val r2 = newLiteral(4)
            val s = newLiteral(5)

            val cG = Call()
            val cF = Call()

            q.prevDFGEdges.addContextSensitive(
                h,
                callingContext = CallingContextOut(mutableListOf(cG)),
            )
            h.prevDFGEdges.addContextSensitive(
                h,
                callingContext = CallingContextOut(mutableListOf(cF)),
            )
            h.prevDFGEdges.addContextSensitive(
                r1,
                callingContext = CallingContextIn(mutableListOf(cF)),
            )
            r1.prevDFGEdges.addContextSensitive(
                r2,
                callingContext = CallingContextIn(mutableListOf(cF)),
            )
            r2.prevDFGEdges.addContextSensitive(
                s,
                callingContext = CallingContextIn(mutableListOf(cG)),
            )

            val predicate: (Node) -> Boolean = { it === s }

            // Call the RAW legacy engine directly (bypassing the now-delegating
            // followDFGEdgesUntilHitNodes wrapper) so this test still demonstrates the OLD,
            // recursion-incomplete behaviour. The wrapper itself is covered by IfdsDelegationTest.
            val direction = Backward(GraphToFollow.DFG)
            val scope = Interprocedural()
            val engine =
                q.followXUntilHitNodes(
                    x = { currentNode, currentCtx, path, loopingPaths ->
                        direction.pickNextStep(
                            currentNode,
                            scope,
                            currentCtx,
                            path,
                            loopingPaths,
                            ContextSensitive,
                        )
                    },
                    predicate = predicate,
                )
            // The legacy engine under-reports on this recursive graph.
            assertEquals(emptySet(), engine, "legacy engine must under-report to empty")

            val ifds =
                q.ifdsReachingSourcesDFG(
                    direction = Backward(GraphToFollow.DFG),
                    predicate = predicate,
                )
            // IFDS with summary edges is complete on recursion.
            assertEquals(setOf<Node>(s), ifds, "IFDS(k=inf) must find the complete set {s}")

            // Sanity: IFDS is a strict superset of the (empty) engine result here.
            assertTrue(ifds.containsAll(engine) && ifds.size > engine.size)
        }
    }

    /**
     * k-knob on DFG. Two call sites c1, c2 flow into the same parameter; the query is c1's result.
     * IFDS(k=∞) correlates call/return and returns only the own-call-site source {arg1}. IFDS(k=0)
     * ignores the pushdown and returns the superset {arg1, arg2}. On this non-recursive graph
     * IFDS(k=∞) also equals the legacy engine.
     */
    @Test
    fun kKnobDfg() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val retBody = newLiteral(2)
            val param = newLiteral(3)
            val arg1 = newLiteral(4)
            val arg2 = newLiteral(5)

            val c1 = Call()
            val c2 = Call()

            q.prevDFGEdges.addContextSensitive(
                retBody,
                callingContext = CallingContextOut(mutableListOf(c1)),
            )
            retBody.prevDFGEdges += param
            param.prevDFGEdges.addContextSensitive(
                arg1,
                callingContext = CallingContextIn(mutableListOf(c1)),
            )
            param.prevDFGEdges.addContextSensitive(
                arg2,
                callingContext = CallingContextIn(mutableListOf(c2)),
            )

            val predicate: (Node) -> Boolean = { it === arg1 || it === arg2 }

            val engine =
                q.followDFGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.DFG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(arg1), engine, "engine: context-sensitive -> {arg1}")

            val ifdsFull =
                q.ifdsReachingSourcesDFG(
                    direction = Backward(GraphToFollow.DFG),
                    k = Int.MAX_VALUE,
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(arg1), ifdsFull, "IFDS(k=inf) -> only own-call-site {arg1}")
            // Non-recursive equivalence to the legacy engine.
            assertEquals(engine, ifdsFull, "IFDS(k=inf) must equal the engine on this DAG")

            val ifdsZero =
                q.ifdsReachingSourcesDFG(
                    direction = Backward(GraphToFollow.DFG),
                    k = 0,
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(arg1, arg2), ifdsZero, "IFDS(k=0) -> superset {arg1, arg2}")
        }
    }

    /**
     * k-knob on EOG (interprocedural context via Invoke edges). Two call sites c1, c2 invoke the
     * same function f; the query is c1's EOG successor. IFDS(k=∞) returns only c1's source {s1};
     * IFDS(k=0) returns the superset {s1, s2}. IFDS(k=∞) equals the legacy engine here.
     *
     * Forward EOG: s1 -> c1 -> q, s2 -> c2; c1, c2 both invoke f.
     */
    @Test
    fun kKnobEog() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val s1 = newLiteral(2)
            val s2 = newLiteral(3)
            val f = newFunction("f")
            val c1 = newCall(fqn = "f")
            val c2 = newCall(fqn = "f")

            c1.invokes += f
            c2.invokes += f

            s1.nextEOG += c1
            c1.nextEOG += q
            s2.nextEOG += c2

            val predicate: (Node) -> Boolean = { it === s1 || it === s2 }

            val engine =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s1), engine, "engine: context-sensitive EOG -> {s1}")

            val ifdsFull =
                q.ifdsReachingSourcesEOG(
                    direction = Backward(GraphToFollow.EOG),
                    k = Int.MAX_VALUE,
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s1), ifdsFull, "IFDS(k=inf) EOG -> only c1's source {s1}")
            assertEquals(engine, ifdsFull, "IFDS(k=inf) must equal the engine on this EOG")

            val ifdsZero =
                q.ifdsReachingSourcesEOG(
                    direction = Backward(GraphToFollow.EOG),
                    k = 0,
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s1, s2), ifdsZero, "IFDS(k=0) EOG -> superset {s1, s2}")
        }
    }

    /**
     * Intraprocedural EOG frontier equivalence: both engine and IFDS must return the frontier {s1,
     * s2}, with the node `behind` (further back than s1) shadowed by the absorbing sink s1.
     */
    @Test
    fun intraproceduralEogEquivalence() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val a = newLiteral(2)
            val s1 = newLiteral(3)
            val s2 = newLiteral(4)
            val behind = newLiteral(5)

            behind.nextEOG += s1
            s1.nextEOG += a
            s2.nextEOG += a
            a.nextEOG += q

            val predicate: (Node) -> Boolean = { it === s1 || it === s2 || it === behind }

            val engine =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            val ifds =
                q.ifdsReachingSourcesEOG(
                    direction = Backward(GraphToFollow.EOG),
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s1, s2), ifds, "IFDS EOG frontier -> {s1, s2}")
            assertEquals(engine, ifds, "IFDS must equal the engine intraprocedurally")
        }
    }

    /**
     * Termination on a plain DFG cycle with no reachable source: the traversal must terminate and
     * return the empty set (rather than loop forever).
     */
    @Test
    fun terminatesOnCycle() {
        with(TestLanguageFrontend()) {
            val a = newLiteral(1)
            val b = newLiteral(2)

            // a <-> b cycle (plain DFG).
            a.prevDFGEdges += b
            b.prevDFGEdges += a

            val ifds =
                a.ifdsReachingSourcesDFG(
                    direction = Backward(GraphToFollow.DFG),
                    predicate = { false },
                )
            assertEquals(emptySet(), ifds, "cycle with no source -> empty, must terminate")
        }
    }

    /**
     * Termination on interprocedural self-recursion: a function that calls itself. With no
     * reachable source the solver must still terminate and return the empty set; summary reuse
     * bounds the recursion.
     */
    @Test
    fun terminatesOnSelfRecursion() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val h = newLiteral(2)

            val cF = Call()

            // q <-out[cF]- h, and h recurses into itself via cF, with no matching return / source.
            q.prevDFGEdges.addContextSensitive(
                h,
                callingContext = CallingContextOut(mutableListOf(cF)),
            )
            h.prevDFGEdges.addContextSensitive(
                h,
                callingContext = CallingContextOut(mutableListOf(cF)),
            )

            val ifds =
                q.ifdsReachingSourcesDFG(
                    direction = Backward(GraphToFollow.DFG),
                    predicate = { false },
                )
            assertEquals(emptySet(), ifds, "self-recursion with no source -> empty, must terminate")
        }
    }
}
