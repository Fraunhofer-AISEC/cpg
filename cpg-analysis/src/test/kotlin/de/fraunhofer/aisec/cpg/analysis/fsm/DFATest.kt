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
package de.fraunhofer.aisec.cpg.analysis.fsm

import kotlin.test.*

class DFATest {
    /** Tests whether it is possible to create a valid DFA. */
    @Test
    fun testValidDfaCreation() {
        val q1 = State(name = 1, isStart = true)
        val q2 = State(name = 2, isAcceptingState = true)
        q1.addEdge(Edge(op = "valid", nextState = q2))
        val dfa = DFA(setOf(q1, q2))

        assertTrue(q1 in dfa.states)
        assertTrue(q2 in dfa.states)
    }

    /**
     * Test that a DFA cannot be instantiated with a state containing an edge with an empty string
     * as an op. The empty string is reserved for [DFA.initializeOrderEvaluation]
     */
    @Test
    fun testDfaRejectEdgeWithEmptyOp() {
        val q1 = State(name = 1, isStart = true)
        val q2 = State(name = 2, isAcceptingState = true)
        q1.addEdge(
            Edge(op = "", nextState = q2)
        ) // invalid because the empty String is used in 'initializeOrderEvaluation' and therefore
        // reserved

        assertFailsWith<IllegalArgumentException>(
            message = "The empty String is a reserved op for DFAs.",
            block = { DFA(setOf(q1, q2)) }
        )
    }

    /** Test that a DFA cannot be instantiated with a state containing an epsilon edge. */
    @Test
    fun testDfaRejectEpsilonEdge() {
        val q1 = State(name = 1, isStart = true)
        val q2 = State(name = 2, isAcceptingState = true)
        q1.addEdge(
            Edge(op = NFA.EPSILON, nextState = q2)
        ) // invalid because a DFA must not contain epsilon edges

        assertFailsWith<IllegalArgumentException>(
            message = "A DFA state must not contain EPSILON edges!",
            block = { DFA(setOf(q1, q2)) }
        )
    }

    /** Tests that a DFA cannot be created with states containing non-deterministic connections. */
    @Test
    fun testDfaRejectNonDeterministicEdges() {
        val q1 = State(name = 1, isStart = true)
        val q2 = State(name = 2, isAcceptingState = true)
        q1.addEdge(
            Edge(op = "valid", nextState = q2)
        ) // invalid because a DFA must not contain epsilon edges
        q1.addEdge(
            Edge(op = "valid", nextState = q1)
        ) // invalid because a DFA must not contain epsilon edges

        assertFailsWith<IllegalArgumentException>(
            message =
                "State already has an outgoing edge with the same label but a different target!",
            block = { DFA(setOf(q1, q2)) }
        )
    }

    /**
     * Tests that [State.edgeCheck] is correctly set in [DFA.addState] and used in [DFA.addEdge].
     */
    @Test
    fun testEdgeCheckInState() {
        val q1 = State(name = 1, isStart = true)
        val q2 = State(name = 2, isAcceptingState = true)
        q1.addEdge(
            Edge(op = "valid", nextState = q2)
        ) // invalid because a DFA must not contain epsilon edges
        val dfa = DFA(setOf(q1, q2))

        assertFailsWith<IllegalArgumentException>(
            message =
                "State already has an outgoing edge with the same label but a different target!",
            block = {
                dfa.addEdge(
                    q1,
                    Edge(op = NFA.EPSILON, nextState = q1)
                ) // invalid because a DFA must not contain epsilon edges
            }
        )
    }

    /** Tests that [State.edgeCheck] is actually used in [State.addEdge]. */
    @Test
    fun testStateAddEdge() {
        val q1 = State(name = 1, isStart = true)
        val q2 = State(name = 2, isAcceptingState = true)
        q1.addEdge(
            Edge(op = "valid", nextState = q2)
        ) // invalid because a DFA must not contain epsilon edges
        val dfa = DFA(setOf(q1, q2)) // this adds the necessary checks to [State.addEdge]

        assertFailsWith<IllegalArgumentException>(
            message =
                "State already has an outgoing edge with the same label but a different target!",
            block = {
                q1.addEdge(
                    Edge(op = NFA.EPSILON, nextState = q1)
                ) // invalid because a DFA state must not contain epsilon edges
            }
        )
    }
}
