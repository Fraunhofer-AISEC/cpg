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

import de.fraunhofer.aisec.cpg.graph.Node

/** A representation of a deterministic finite automaton (DFA). */
class DFA : Cloneable {
    companion object {
        @JvmStatic val EPSILON: String = "ε"
    }

    var states = mutableSetOf<State>()
    private var stateCounter = 1
    var currentState: State? = null
    var executionTrace = mutableListOf<Triple<State, Node, BaseOpEdge>>()

    /**
     * Creates an edge between two nodes with a given label (operator and optional base).
     *
     * It checks if [from] already has an outgoing edge with the same [base] and [op] but to another
     * target node. If so, it does not add the edge end returns `false`, otherwise it creates the
     * edge and returns `true`.
     */
    fun addEdge(from: State, to: State, op: String, base: String?): Boolean {
        states.add(from)
        states.add(to)

        if (from.outgoingEdges.any { e -> e.matches(base, op) && e.nextState != to }) {
            throw FSMBuilderException(
                "State already has an outgoing edge with the same label but a different target!"
            )
        }
        from.addOutgoingEdge(BaseOpEdge(op, base, to))
        return true
    }

    /** Generates a new state and adds it to this FSM. */
    fun addState(isStart: Boolean = false, isAcceptingState: Boolean = false): State {
        val newState = State("q$stateCounter", isStart, isAcceptingState)
        if (isStart) {
            currentState = newState
        }
        states.add(newState)
        stateCounter++
        return newState
    }

    /**
     * Generates the string representing this FSM in DOT format. This allows a simple visualization
     * of the resulting automaton.
     */
    fun toDotString(): String {
        var str = "digraph fsm {\n\t\"\" [shape=point];\n"
        var edges = ""
        for (s in states) {
            str +=
                if (s.isAcceptingState) {
                    "\t${s.name} [shape=doublecircle];\n"
                } else {
                    "\t${s.name} [shape=circle];\n"
                }
            if (s.isStart) {
                edges += "\t\"\" -> ${s.name};\n"
            }

            for (e in s.outgoingEdges) {
                edges += "\t${s.name} -> ${e.nextState.name} [label=\"${e.toDotLabel()}\"];\n"
            }
        }
        return "$str$edges}"
    }

    /**
     * Checks if the transition with operator [op] is possible from the current state of the FSM. If
     * so, it updates the state of the FSM and returns `true`. If no transition is possible, returns
     * `false`. Collects the old state, the edge and the cpg [node] in the [executionTrace]
     */
    fun makeTransitionWithOp(op: String, node: Node): Boolean {
        if (currentState == null) {
            throw Exception(
                "Cannot make transition because the FSM does not have a starting state!"
            )
        }

        var newState = currentState?.nextNodeWithLabelOp(op)
        val retVal = newState != null
        while (newState != null) {
            executionTrace.add(Triple(currentState!!, node, newState.second))
            currentState = newState.first
            // Directly follow the ε edges.
            newState = currentState?.nextNodeWithLabelOp(EPSILON)
        }
        return retVal
    }

    /** Copies the FSM to enable multiple independent branches of execution. */
    public override fun clone(): DFA {
        val newDFA = DFA()
        newDFA.currentState = this.currentState?.clone()
        newDFA.states = this.states
        newDFA.executionTrace = mutableListOf()
        newDFA.executionTrace.addAll(this.executionTrace)
        return newDFA
    }

    /** Checks if the FSM is currently in an accepting state. */
    fun isAccepted(): Boolean {
        return currentState?.isAcceptingState == true
    }

    override fun equals(other: Any?): Boolean {
        val res =
            other != null &&
                other is DFA &&
                other.currentState!!.equals(currentState) &&
                other.stateCounter == stateCounter &&
                other.states == states
        if (res) {
            for (s in states) {
                val otherState = (other as DFA).states.first { otherS -> s.name == otherS.name }
                if (otherState.outgoingEdges != s.outgoingEdges) {
                    return false
                }
            }
        }

        return res
    }
}
