/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

class NfaToRegexTest {
    @Test
    fun testSimpleRegex() {
        val nfa = NFA()
        val q1 = nfa.addState(isStart = true)
        val q2 = nfa.addState()
        val q3 = nfa.addState(isAcceptingState = true)
        nfa.addEdge(q1, Edge(NFA.EPSILON, null, q3))
        nfa.addEdge(q2, Edge("a", null, q2))
        nfa.addEdge(q2, Edge("b", null, q3))
        nfa.addEdge(q2, Edge("c", null, q2))
        nfa.addEdge(q1, Edge("d", null, q1))
        nfa.addEdge(q3, Edge("a", null, q3))
        nfa.addEdge(q1, Edge("d", null, q2))

        assertEquals("(d*d[ac]*ba*)|(d*a*)", nfa.toRegex())
    }
}
