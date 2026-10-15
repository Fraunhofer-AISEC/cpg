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

/**
 * Tests for the internal delegation of the node-set MAY wrappers ([followDFGEdgesUntilHitNodes] /
 * [followEOGEdgesUntilHitNodes]) to the recursion-complete [ifdsReachingSources] solver for the
 * exact field-insensitive, context-sensitive, interprocedural regime. Also checks that every other
 * configuration falls through to the legacy engine unchanged.
 */
class IfdsDelegationTest {

    /** Builds the recursive interprocedural DFG on which the legacy engine under-reports. */
    private fun TestLanguageFrontend.recursionGraph(): Pair<Node, Node> {
        val q = newLiteral(1)
        val h = newLiteral(2)
        val r1 = newLiteral(3)
        val r2 = newLiteral(4)
        val s = newLiteral(5)

        val cG = Call()
        val cF = Call()

        q.prevDFGEdges.addContextSensitive(h, callingContext = CallingContextOut(mutableListOf(cG)))
        h.prevDFGEdges.addContextSensitive(h, callingContext = CallingContextOut(mutableListOf(cF)))
        h.prevDFGEdges.addContextSensitive(r1, callingContext = CallingContextIn(mutableListOf(cF)))
        r1.prevDFGEdges.addContextSensitive(
            r2,
            callingContext = CallingContextIn(mutableListOf(cF)),
        )
        r2.prevDFGEdges.addContextSensitive(s, callingContext = CallingContextIn(mutableListOf(cG)))
        return q to s
    }

    /**
     * Delegation closes the recursion gap through the wrapper: a field-insensitive,
     * context-sensitive interprocedural [followDFGEdgesUntilHitNodes] now returns the COMPLETE set
     * {s} (matching [ifdsReachingSources]) rather than the legacy engine's under-report.
     */
    @Test
    fun dfgDelegationClosesRecursionGap() {
        with(TestLanguageFrontend()) {
            val (q, s) = recursionGraph()
            val predicate: (Node) -> Boolean = { it === s }

            val viaWrapper =
                q.followDFGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.DFG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            val viaIfds =
                q.ifdsReachingSourcesDFG(
                    direction = Backward(GraphToFollow.DFG),
                    predicate = predicate,
                )
            assertEquals(
                setOf<Node>(s),
                viaWrapper,
                "wrapper must delegate and return complete {s}",
            )
            assertEquals(viaIfds, viaWrapper, "wrapper result must equal the IFDS solver")
        }
    }

    /**
     * Non-delegation preserved: adding [FieldSensitive] to the sensitivities must keep the LEGACY
     * engine (IFDS is field-insensitive and must not be used), which still under-reports (empty) on
     * this recursive graph.
     */
    @Test
    fun fieldSensitiveDoesNotDelegate() {
        with(TestLanguageFrontend()) {
            val (q, s) = recursionGraph()
            val predicate: (Node) -> Boolean = { it === s }

            val result =
                q.followDFGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.DFG),
                    sensitivities = FieldSensitive + ContextSensitive,
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            assertEquals(
                emptySet(),
                result,
                "FieldSensitive must fall through to the legacy engine",
            )
        }
    }

    /**
     * A custom [earlyTermination] must fall through to the legacy engine (IFDS cannot honour it).
     * We prove the legacy path is taken by giving an earlyTermination that actually prunes the only
     * path to the source, so the result is empty where delegation would have returned {s}.
     */
    @Test
    fun customEarlyTerminationDoesNotDelegate() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val a = newLiteral(2)
            val s = newLiteral(3)
            q.prevDFGEdges += a
            a.prevDFGEdges += s

            val predicate: (Node) -> Boolean = { it === s }

            // Sanity: without earlyTermination, the wrapper delegates and finds {s}.
            val delegated =
                q.followDFGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.DFG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s), delegated, "no earlyTermination -> delegate -> {s}")

            // With a pruning earlyTermination, the legacy engine is used and prunes at `a`.
            val pruned =
                q.followDFGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.DFG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    earlyTermination = { n, _ -> n === a },
                    predicate = predicate,
                )
            assertEquals(emptySet(), pruned, "custom earlyTermination must fall through and prune")
        }
    }

    /**
     * An [Intraprocedural] scope must fall through to the legacy engine (IFDS always follows
     * call/return edges). On the recursive interprocedural graph the intraprocedural engine cannot
     * cross calls and returns empty, distinguishing it from the interprocedural IFDS result {s}.
     */
    @Test
    fun intraproceduralScopeDoesNotDelegate() {
        with(TestLanguageFrontend()) {
            val (q, s) = recursionGraph()
            val predicate: (Node) -> Boolean = { it === s }

            val result =
                q.followDFGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.DFG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Intraprocedural(),
                    predicate = predicate,
                )
            assertEquals(
                emptySet(),
                result,
                "Intraprocedural must fall through to the legacy engine",
            )
        }
    }

    /**
     * EOG delegation: a context-sensitive-only [followEOGEdgesUntilHitNodes] (no
     * [FilterUnreachableEOG]) delegates to IFDS and matches it. Two call sites invoke the same
     * function; context sensitivity yields only c1's source {s1}.
     */
    @Test
    fun eogDelegationMatchesIfds() {
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

            val viaWrapper =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            val viaIfds =
                q.ifdsReachingSourcesEOG(
                    direction = Backward(GraphToFollow.EOG),
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s1), viaWrapper, "EOG wrapper must delegate -> {s1}")
            assertEquals(viaIfds, viaWrapper, "EOG wrapper result must equal the IFDS solver")
        }
    }

    /**
     * Adding [FilterUnreachableEOG] back (the default EOG sensitivity) must keep the LEGACY engine
     * for [followEOGEdgesUntilHitNodes] (mirrors [fieldSensitiveDoesNotDelegate] for DFG).
     */
    @Test
    fun eogFilterUnreachableDoesNotDelegate() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val s1 = newLiteral(2)
            val c1 = newCall(fqn = "f")
            val f = newFunction("f")
            c1.invokes += f
            s1.nextEOG += c1
            c1.nextEOG += q

            val result =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = FilterUnreachableEOG + ContextSensitive,
                    scope = Interprocedural(),
                    predicate = { it === s1 },
                )
            assertEquals(setOf<Node>(s1), result, "legacy engine must still find {s1} here")
        }
    }

    /**
     * An [Intraprocedural] scope must fall through to the legacy engine for
     * [followEOGEdgesUntilHitNodes] as well (mirrors [intraproceduralScopeDoesNotDelegate] for
     * DFG). `s1` is a plain intraprocedural predecessor of the call site here (not something only
     * reachable by crossing into the callee), so both engines agree on the result; the point of
     * this test is to exercise the non-delegating fallback branch itself.
     */
    @Test
    fun eogIntraproceduralScopeDoesNotDelegate() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val s1 = newLiteral(2)
            val c1 = newCall(fqn = "f")
            val f = newFunction("f")
            c1.invokes += f
            s1.nextEOG += c1
            c1.nextEOG += q

            val result =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Intraprocedural(),
                    predicate = { it === s1 },
                )
            assertEquals(
                setOf<Node>(s1),
                result,
                "Intraprocedural must fall through to the legacy engine",
            )
        }
    }

    /**
     * A custom [earlyTermination] must fall through to the legacy engine for
     * [followEOGEdgesUntilHitNodes] (mirrors [customEarlyTerminationDoesNotDelegate] for DFG).
     */
    @Test
    fun eogCustomEarlyTerminationDoesNotDelegate() {
        with(TestLanguageFrontend()) {
            val q = newLiteral(1)
            val a = newLiteral(2)
            val s = newLiteral(3)
            a.nextEOG += q
            s.nextEOG += a

            val predicate: (Node) -> Boolean = { it === s }

            val delegated =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    predicate = predicate,
                )
            assertEquals(setOf<Node>(s), delegated, "no earlyTermination -> delegate -> {s}")

            val pruned =
                q.followEOGEdgesUntilHitNodes(
                    direction = Backward(GraphToFollow.EOG),
                    sensitivities = arrayOf(ContextSensitive),
                    scope = Interprocedural(),
                    earlyTermination = { n, _ -> n === a },
                    predicate = predicate,
                )
            assertEquals(emptySet(), pruned, "custom earlyTermination must fall through and prune")
        }
    }

    /**
     * [followDFGEdgesUntilHitNodes] and [followEOGEdgesUntilHitNodes] called with every parameter
     * defaulted always fall through to the legacy engine: the default sensitivities
     * ([FieldSensitive] / [FilterUnreachableEOG], both combined with [ContextSensitive]) never
     * equal the singleton `{ContextSensitive}` the delegation guard requires. This exercises the
     * default argument values themselves.
     */
    @Test
    fun defaultArgumentsAlwaysFallThroughToLegacyEngine() {
        with(TestLanguageFrontend()) {
            val a = newLiteral(1)
            val b = newLiteral(2)
            a.nextDFGEdges += b
            assertEquals(setOf<Node>(b), a.followDFGEdgesUntilHitNodes { it === b })

            val c = newLiteral(3)
            val d = newLiteral(4)
            c.nextEOG += d
            assertEquals(setOf<Node>(d), c.followEOGEdgesUntilHitNodes { it === d })
        }
    }
}
