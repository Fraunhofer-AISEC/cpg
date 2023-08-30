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

import de.fraunhofer.aisec.cpg.graph.statements.EmptyStatement
import kotlin.test.*

/**
 * Tests whether the [DFA.equals] method works as expected and correctly identifies whether two DFAs
 * accept the same language.
 */
class DFAEqualityTest {
    @Test
    /** Tests an empty DFA */
    fun testEmptyDfa() {
        assertFailsWith<IllegalStateException>(
            message = "In order to compare to FSMs, both must have exactly one start state.",
            block = { DFA() == DFA() }
        )
    }

    @Test
    /** Tests a DFA with a single state */
    fun testDfaWithSingleState() {
        val dfa1 = DFA(setOf(State(1, isStart = true)))
        val dfa2 = DFA(setOf(State(2, isStart = true)))

        assertEquals(dfa1, dfa2)
    }

    @Test
    /** Tests a DFA with isomorphic states (exactly the same DFA but with different state names) */
    fun testIsomorphicDfa() {
        val getStates = { offset: Int ->
            val q4 = State(4 + offset, isAcceptingState = true)
            val q3 = State(3 + offset).apply { addEdge(Edge("to4", nextState = q4)) }
            val q2 = State(2 + offset).apply { addEdge(Edge("to4", nextState = q4)) }
            val q1 =
                State(1 + offset, isStart = true).apply {
                    addEdge(Edge("to2", nextState = q2))
                    addEdge(Edge("to3", nextState = q3))
                }
            setOf(q1, q2, q3, q4)
        }
        val dfa1 = DFA(getStates(0))
        val dfa2 = DFA(getStates(5))

        assertEquals(dfa1, dfa2)
    }

    @Test
    /**
     * Tests a DFA and its equivalent minimal DFA. Uses the two DFAs depicted
     * [here](https://en.wikipedia.org/wiki/DFA_minimization)
     */
    fun testMinimalDfaEquality() {
        val q6 = State(6) // f
        val q5 = State(5, isAcceptingState = true) // e
        val q4 = State(4, isAcceptingState = true) // d
        val q3 = State(3, isAcceptingState = true) // c
        val q2 = State(2) // b
        val q1 = State(1, isStart = true) // a

        q1.addEdge(Edge("0", nextState = q2))
        q1.addEdge(Edge("1", nextState = q3))
        q2.addEdge(Edge("0", nextState = q1))
        q2.addEdge(Edge("1", nextState = q4))
        q3.addEdge(Edge("0", nextState = q5))
        q3.addEdge(Edge("1", nextState = q6))
        q4.addEdge(Edge("0", nextState = q5))
        q4.addEdge(Edge("1", nextState = q6))
        q5.addEdge(Edge("0", nextState = q5))
        q5.addEdge(Edge("1", nextState = q6))
        q6.addEdge(Edge("0", nextState = q6))
        q6.addEdge(Edge("1", nextState = q6))

        val dfa1 = DFA(setOf(q1, q2, q3, q4, q5, q6))

        // construct the equivalent minimal DFA
        val state33 =
            State(3).apply {
                addEdge(Edge("1", nextState = this))
                addEdge(Edge("0", nextState = this))
            } // a,b
        val state22 =
            State(2, isAcceptingState = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = state33))
            } // c,d,e
        val state11 =
            State(1, isStart = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = state22))
            } // f
        val dfa2 = DFA(setOf(state11, state22, state33))

        assertEquals(dfa1, dfa2)
    }

    @Test
    /** Tests whether we can correctly flag non-equivalent DFAs. */
    fun testNonEquivalentDfa() {
        val q6 = State(6) // f
        val q5 = State(5, isAcceptingState = true) // e
        val q4 = State(4, isAcceptingState = true) // d
        val q3 = State(3, isAcceptingState = true) // c
        val q2 = State(2) // b
        val q1 = State(1, isStart = true) // a

        q1.addEdge(Edge("0", nextState = q2))
        q1.addEdge(Edge("1", nextState = q3))
        q2.addEdge(Edge("0", nextState = q1))
        q2.addEdge(Edge("1", nextState = q4))
        q3.addEdge(Edge("0", nextState = q5))
        q3.addEdge(Edge("1", nextState = q6))
        q4.addEdge(Edge("0", nextState = q5))
        q4.addEdge(Edge("1", nextState = q6))
        q5.addEdge(Edge("0", nextState = q5))
        q5.addEdge(Edge("1", nextState = q6))
        q6.addEdge(Edge("0", nextState = q6))
        q6.addEdge(Edge("1", nextState = q6))

        val dfa1 = DFA(setOf(q1, q2, q3, q4, q5, q6))

        // construct another DFA that does not accept the same language
        val q33 =
            State(3).apply {
                addEdge(
                    Edge("1", nextState = this)
                ) // this is where an equivalent DFA would need an additional Edge("0",
                // nextState=this)
            } // a,b
        val q22 =
            State(2, isAcceptingState = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = q33))
            } // c,d,e
        val q11 =
            State(1, isStart = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = q22))
            } // f
        val dfa2 = DFA(setOf(q11, q22, q33))

        assertNotEquals(dfa1, dfa2)
    }

    @Test
    /** Tests whether we can correctly flag non-equivalent DFAs a second time. */
    fun testDfaWithSingleDifference() {
        // construct another DFA that does not accept the same language

        val q3 = State(3).apply { addEdge(Edge("1", nextState = this)) } // a,b
        val q2 =
            State(2, isAcceptingState = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = q3))
            } // c,d,e
        val q1 =
            State(1, isStart = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = q2))
            } // f
        val dfa1 = DFA(setOf(q1, q2, q3))

        // construct another DFA that does not accept the same language
        val state33 = State(3).apply { addEdge(Edge("0", nextState = this)) } // a,b
        val state22 =
            State(2, isAcceptingState = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = state33))
            } // c,d,e
        val state11 =
            State(1, isStart = true).apply {
                addEdge(Edge("0", nextState = this))
                addEdge(Edge("1", nextState = state22))
            } // f
        val dfa2 = DFA(setOf(state11, state22, state33))

        assertNotEquals(dfa1, dfa2)
    }

    @Test
    /**
     * Tests whether the current state is correctly used to determine whether two DFAs are equal.
     */
    fun testDfaEqualityWithCurrentState() {
        val getDfa = {
            val dfa = DFA()
            val q1 = dfa.addState(isStart = true)
            val q2 = dfa.addState(isAcceptingState = true)
            val q3 = dfa.addState(isAcceptingState = true)
            val q4 = dfa.addState()
            val q5 = dfa.addState(isAcceptingState = true)
            dfa.addEdge(q1, Edge("create()", "v", q2))
            dfa.addEdge(q2, Edge("check_whole_msg()", "v", q3))
            dfa.addEdge(q2, Edge("update()", "v", q4))
            dfa.addEdge(q2, Edge("check_after_update()", "v", q5))
            dfa.addEdge(q3, Edge("check_whole_msg()", "v", q3))
            dfa.addEdge(q4, Edge("update()", "v", q4))
            dfa.addEdge(q4, Edge("check_after_update()", "v", q5))
            dfa.addEdge(q5, Edge("check_after_update()", "v", q5))
            dfa.addEdge(q5, Edge("update()", "v", q4))
            dfa
        }

        val dfa = getDfa()
        val oldDfa = getDfa()

        val emptyNode = EmptyStatement()
        dfa.initializeOrderEvaluation(emptyNode)
        dfa.makeTransitionWithOp(setOf("create()"), emptyNode)

        assertTrue(dfa.isAccepted)
        assertNotEquals(oldDfa, dfa)

        oldDfa.initializeOrderEvaluation(emptyNode)
        oldDfa.makeTransitionWithOp(setOf("create()"), emptyNode)
        assertEquals(oldDfa, dfa)

        oldDfa.addEdge(
            oldDfa.states.single { it.name == 1 },
            Edge("create2()", "v", oldDfa.states.single { it.name == 3 })
        )
        assertNotEquals(oldDfa, dfa)
    }
}
