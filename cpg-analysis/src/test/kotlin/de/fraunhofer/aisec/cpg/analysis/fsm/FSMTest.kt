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

import de.fraunhofer.aisec.cpg.graph.statements.EmptyStatement
import kotlin.test.*

class FSMTest {
    private val simpleStringRepresentation =
        "digraph fsm {\n" +
            "\t\"\" [shape=point];\n" +
            "\tq1 [shape=circle];\n" +
            "\tq2 [shape=doublecircle];\n" +
            "\tq3 [shape=doublecircle];\n" +
            "\tq4 [shape=circle];\n" +
            "\tq5 [shape=doublecircle];\n" +
            "\t\"\" -> q1;\n" +
            "\tq1 -> q2 [label=\"v.create()\"];\n" +
            "\tq2 -> q3 [label=\"v.check_whole_msg()\"];\n" +
            "\tq2 -> q4 [label=\"v.update()\"];\n" +
            "\tq2 -> q5 [label=\"v.check_after_update()\"];\n" +
            "\tq3 -> q3 [label=\"v.check_whole_msg()\"];\n" +
            "\tq4 -> q4 [label=\"v.update()\"];\n" +
            "\tq4 -> q5 [label=\"v.check_after_update()\"];\n" +
            "\tq5 -> q5 [label=\"v.check_after_update()\"];\n" +
            "\tq5 -> q4 [label=\"v.update()\"];\n" +
            "}"

    /** Tests the correct generation of a .dot string from a [NFA]. */
    @Test
    fun testDotStringCreation() {
        val nfa = NFA()
        val q1 = nfa.addState(isStart = true)
        val q2 = nfa.addState(isAcceptingState = true)
        val q3 = nfa.addState(isAcceptingState = true)
        val q4 = nfa.addState()
        val q5 = nfa.addState(isAcceptingState = true)
        nfa.addEdge(q1, Edge("create()", "v", q2))
        nfa.addEdge(q2, Edge("check_whole_msg()", "v", q3))
        nfa.addEdge(q2, Edge("update()", "v", q4))
        nfa.addEdge(q2, Edge("check_after_update()", "v", q5))
        nfa.addEdge(q3, Edge("check_whole_msg()", "v", q3))
        nfa.addEdge(q4, Edge("update()", "v", q4))
        nfa.addEdge(q4, Edge("check_after_update()", "v", q5))
        nfa.addEdge(q5, Edge("check_after_update()", "v", q5))
        nfa.addEdge(q5, Edge("update()", "v", q4))

        assertEquals(simpleStringRepresentation, nfa.toDotString())
    }

    /** Tests the [NFA.deepCopy] method. */
    @Test
    fun testDeepcopy() {
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

        val dfaCopy = dfa.deepCopy()

        assertEquals(dfa, dfaCopy)
        assertFalse { dfa.currentState!! === dfaCopy.currentState!! }

        val createEdge =
            dfa.states.single { it.name == 1 }.outgoingEdges.single { it.op == "create()" }
        val createEdgeCopy =
            dfaCopy.states.single { it.name == 1 }.outgoingEdges.single { it.op == "create()" }
        assertFalse { createEdge === createEdgeCopy }
        assertFalse { createEdge.nextState === createEdgeCopy.nextState }

        val emptyNode = EmptyStatement()
        dfa.initializeOrderEvaluation(emptyNode)
        dfa.makeTransitionWithOp(setOf("create()"), emptyNode)

        assertNotEquals(dfa, dfaCopy)
        assertEquals(dfa.executionTrace, dfa.deepCopy().executionTrace)
    }
}
