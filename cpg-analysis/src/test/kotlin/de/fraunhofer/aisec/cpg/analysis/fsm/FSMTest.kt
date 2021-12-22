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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

    @Test
    fun testFSMDotFile() {
        val fsm = DFA()
        val q1 = fsm.addState(isStart = true)
        val q2 = fsm.addState(isAcceptingState = true)
        val q3 = fsm.addState(isAcceptingState = true)
        val q4 = fsm.addState()
        val q5 = fsm.addState(isAcceptingState = true)
        fsm.addEdge(q1, q2, "create()", "v")
        fsm.addEdge(q2, q3, "check_whole_msg()", "v")
        fsm.addEdge(q2, q4, "update()", "v")
        fsm.addEdge(q2, q5, "check_after_update()", "v")
        fsm.addEdge(q3, q3, "check_whole_msg()", "v")
        fsm.addEdge(q4, q4, "update()", "v")
        fsm.addEdge(q4, q5, "check_after_update()", "v")
        fsm.addEdge(q5, q5, "check_after_update()", "v")
        fsm.addEdge(q5, q4, "update()", "v")

        assertEquals(simpleStringRepresentation, fsm.toDotString())
    }
}
