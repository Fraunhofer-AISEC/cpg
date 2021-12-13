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

/** A representation of a deterministic finite automaton (DFA). */
class DFA : FSM() {
    /**
     * Creates an edge between two nodes with a given label (operator and optional base).
     *
     * It checks if [from] already has an outgoing edge with the same [base] and [op] but to another
     * target node. If so, it does not add the edge end returns `false`, otherwise it creates the
     * edge and returns `true`.
     */
    override fun addEdge(from: State, to: State, op: String, base: String?): Boolean {
        if (!states.contains(from)) {
            states.add(from)
        }
        if (!states.contains(to)) {
            states.add(to)
        }
        if (from.outgoingEdges.any { e -> e.matches(base, op) && e.nextState != to }) {
            throw FSMBuilderException(
                "State already has an outgoing edge with the same label but a different target!"
            )
        }
        from.addOutgoingEdge(BaseOpEdge(op, base, to))
        return true
    }
}
