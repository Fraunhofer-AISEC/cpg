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

/**
 * A simple class representing a state in an FSM.
 * - [name] is the name of the State and must be unique for the FSM.
 * - [isStart] indicates if it is the starting state.
 * - [isAcceptingState] indicates if this State accepts the FSM (in our case, this means that the
 * order of statements was correct).
 */
class State(val name: String, val isStart: Boolean = false, var isAcceptingState: Boolean = false) :
    Cloneable {

    val outgoingEdges = mutableSetOf<BaseOpEdge>()

    fun addOutgoingEdge(edge: BaseOpEdge) {
        outgoingEdges.add(edge)
    }

    /**
     * Returns a [Pair] holding the next [State] when the edge with the operation [op] is executed
     * and the [BaseOpEdge] which is executed. If no matching edge exists for this State, returns
     * `null`.
     */
    fun nextNodeWithLabelOp(op: String): Pair<State, BaseOpEdge>? {
        val nextStates = outgoingEdges.filter { e -> e.op == op }
        if (nextStates.isNotEmpty()) {
            return Pair(nextStates[0].nextState, nextStates[0])
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        return (other as? State)?.name?.equals(name) == true
    }

    override fun toString(): String {
        return (if (isStart) "(S) " else "") + name + (if (isAcceptingState) " (A)" else "")
    }

    public override fun clone(): State {
        val newState = State(name, isStart, isAcceptingState)
        newState.outgoingEdges.addAll(outgoingEdges)
        return newState
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
