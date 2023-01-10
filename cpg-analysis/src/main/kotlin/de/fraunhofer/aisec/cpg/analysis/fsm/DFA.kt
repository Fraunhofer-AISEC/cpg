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

import de.fraunhofer.aisec.cpg.graph.Node

/**
 * Holds the information generated during an order evaluation using the [DFAOrderEvaluator]. It is
 * used to provide full traceability of the order evaluation in [DFA.executionTrace].
 */
data class Trace(val state: State, val cpgNode: Node, val edge: Edge)

/** A representation of a deterministic finite automaton (DFA). */
class DFA(states: Set<State> = setOf()) : FSM(states) {
    private val _executionTrace = mutableListOf<Trace>()
    val executionTrace: List<Trace>
        get() = _executionTrace
    val currentState: State?
        get() = executionTrace.lastOrNull()?.edge?.nextState ?: states.singleOrNull { it.isStart }

    /** True, if the DFA is currently in an accepting state. */
    val isAccepted: Boolean
        get() = currentState?.isAcceptingState == true

    override fun checkEdge(state: State, edge: Edge) {
        require(edge.op != NFA.EPSILON) { "A DFA state must not contain EPSILON edges!" }
        require(edge.op != "") {
            "The empty String is a reserved op for DFAs."
        } // reserved for [initializeOrderEvaluation]
        require(
            state.outgoingEdges.none { e -> e.matches(edge) && e.nextState != edge.nextState }
        ) {
            "State already has an outgoing edge with the same label but a different target!"
        }
    }

    /**
     * Associates the start state with a [cpgNode].
     *
     * Must be called before calling [makeTransitionWithOp] to initialize the order evaluation.
     */
    fun initializeOrderEvaluation(cpgNode: Node) {
        val startState = states.singleOrNull { it.isStart }
        checkNotNull(startState) {
            "To perform an order evaluation on a DFA, the DFA must have a start state. This DFA does not have a start state."
        }
        _executionTrace.clear() // necessary when re-using the same [DFA] object for multiple order evaluations
        _executionTrace.add(
            Trace(state = startState, cpgNode = cpgNode, edge = Edge("", nextState = startState))
        )
    }

    /**
     * Checks if the transition with operator [op] is possible from the current state of the FSM. If
     * so, it updates the state of the FSM and returns `true`. If no transition is possible, returns
     * `false`. Collects the old state, the edge and the [cpgNode] in the [executionTrace] in a
     * [Trace] and appends it to the [executionTrace]
     *
     * Before calling this, initialize the orderEvaluation with [initializeOrderEvaluation]
     */
    fun makeTransitionWithOp(op: String, cpgNode: Node): Boolean {
        checkNotNull(currentState) {
            "Cannot perform a transition because the FSM does not have a starting state!"
        }
        check(executionTrace.isNotEmpty()) {
            "Before performing transitions, you must call [initializeOrderEvaluation] first."
        }

        val possibleEdges = currentState!!.outgoingEdges.filter { e -> e.op == op }
        val edgeToFollow = possibleEdges.singleOrNull()
        return if (edgeToFollow != null) {
            _executionTrace.add(
                Trace(state = currentState!!, cpgNode = cpgNode, edge = edgeToFollow)
            )
            true
        } else {
            false
        }
    }

    /** Create a shallow copy */
    override fun copy() = DFA(states = states)

    /** Creates a deep copy the DFA to enable multiple independent branches of execution. */
    override fun deepCopy(): DFA {
        val newDFA = super.deepCopy() as DFA

        // for a DFA, we must also copy the executionTrace
        for (trace in executionTrace) {
            val traceState = newDFA.states.single { it.name == trace.state.name }
            val traceNextState = newDFA.states.single { it.name == trace.edge.nextState.name }
            newDFA._executionTrace.add(
                trace.copy(state = traceState, edge = trace.edge.copy(nextState = traceNextState))
            )
        }
        return newDFA
    }
}
