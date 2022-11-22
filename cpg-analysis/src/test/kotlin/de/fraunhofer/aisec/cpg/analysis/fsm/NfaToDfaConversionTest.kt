/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests whether a NFA is correctly converted to a DFA.
 *
 * If this test fails, make sure that the following tests work first as the functionality these test
 * is needed for this test:
 * - [DFAEqualityTest]
 */
class NfaToDfaConversionTest {
    @Test
    /**
     * Tests a simple sequence order.
     *
     * The NFA can be converted to .dot format using: [NFA.toDotString].
     */
    fun testSequenceOrder() {
        val nfa = NFA()
        val q0 = nfa.addState(isStart = true)
        val q1 = nfa.addState()
        val q2 = nfa.addState()
        val q3 = nfa.addState(isAcceptingState = true)
        nfa.addEdge(q0, Edge(op = "create", base = "cm", nextState = q1))
        nfa.addEdge(q1, Edge(op = NFA.EPSILON, nextState = q2))
        nfa.addEdge(q2, Edge(op = "init", base = "cm", nextState = q3))

        val expectedDfa = DFA()
        val q11 = expectedDfa.addState(isStart = true)
        val q22 = expectedDfa.addState()
        val q33 = expectedDfa.addState(isAcceptingState = true)
        expectedDfa.addEdge(q11, Edge("create", "cm", q22))
        expectedDfa.addEdge(q22, Edge("init", "cm", q33))

        assertEquals(expected = expectedDfa, actual = nfa.toDfa())
    }

    @Test
    /**
     * Tests a simple order with a branch.
     *
     * The NFA can be converted to .dot format using: [NFA.toDotString].
     */
    fun testOrderWithBranch() {
        val nfa = NFA()
        val q0 = nfa.addState(isStart = true)
        val q1 = nfa.addState()
        val q2 = nfa.addState()
        val q3 = nfa.addState(isAcceptingState = true)
        val q4 = nfa.addState(isAcceptingState = true)
        nfa.addEdge(q0, Edge(op = NFA.EPSILON, nextState = q1))
        nfa.addEdge(q0, Edge(op = NFA.EPSILON, nextState = q2))
        nfa.addEdge(q1, Edge(op = "create", base = "cm", nextState = q3))
        nfa.addEdge(q2, Edge(op = "init", base = "cm", nextState = q4))

        val expectedDfa = DFA()
        val q00 = expectedDfa.addState(isStart = true)
        val q11 = expectedDfa.addState(isAcceptingState = true)
        expectedDfa.addEdge(q00, Edge("create", "cm", q11))
        expectedDfa.addEdge(q00, Edge("init", "cm", q11))

        assertEquals(expected = expectedDfa, actual = nfa.toDfa())
    }

    @Test
    /**
     * Tests a simple order with a maybe qualifier.
     *
     * The NFA can be converted to .dot format using: [NFA.toDotString].
     */
    fun testOrderWithMaybeQualifier() {
        val nfa = NFA()
        val q0 = nfa.addState(isStart = true, isAcceptingState = true)
        val q1 = nfa.addState()
        val q2 = nfa.addState(isAcceptingState = true)
        nfa.addEdge(q0, Edge(op = NFA.EPSILON, nextState = q1))
        nfa.addEdge(q1, Edge(op = "create", base = "cm", nextState = q2))
        nfa.addEdge(q2, Edge(op = NFA.EPSILON, nextState = q0))

        val expectedDfa = DFA()
        val q00 = expectedDfa.addState(isAcceptingState = true, isStart = true)
        expectedDfa.addEdge(q00, Edge("create", "cm", q00))

        assertEquals(expected = expectedDfa, actual = nfa.toDfa())
    }

    @Test
    /**
     * Tests a simple order with an option qualifier.
     *
     * The NFA can be converted to .dot format using: [NFA.toDotString].
     */
    fun testOrderWithOptionQualifier() {
        val nfa = NFA()
        val q0 = nfa.addState(isStart = true, isAcceptingState = true)
        val q1 = nfa.addState()
        val q2 = nfa.addState(isAcceptingState = true)
        nfa.addEdge(q0, Edge(op = NFA.EPSILON, nextState = q1))
        nfa.addEdge(q1, Edge(op = "create", base = "cm", nextState = q2))

        val expectedDfa = DFA()
        val q00 = expectedDfa.addState(isStart = true, isAcceptingState = true)
        val q11 = expectedDfa.addState(isAcceptingState = true)
        expectedDfa.addEdge(q00, Edge("create", "cm", q11))

        assertEquals(expected = expectedDfa, actual = nfa.toDfa())
    }
}
