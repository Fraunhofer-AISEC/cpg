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

/** A representation of a non-deterministic finite automaton (NFA). */
class NFA(states: Set<State> = setOf()) : FSM(states) {
    companion object {
        @JvmStatic val EPSILON: String = "ε"
    }

    /** Create a shallow copy */
    override fun copy() = NFA(states = states)

    /**
     * Compute the ε-closure for this ε-NFA and then use the
     * [powerset construction](https://en.wikipedia.org/wiki/Powerset_construction) algorithm (
     * [example](https://www.javatpoint.com/automata-conversion-from-nfa-with-null-to-dfa)) to
     * convert it to a [DFA]
     */
    fun toDfa(): DFA {
        /**
         * Recursively compute the ε-closure for the given set of states (i.e., all states reachable
         * by ε-transitions from any of the states in the set)
         */
        fun getEpsilonClosure(states: MutableSet<State>): Set<State> {
            for (epsilonEdges in
                states.map { state -> state.outgoingEdges.filter { edge -> edge.op == EPSILON } }) {
                states.addAll(getEpsilonClosure(epsilonEdges.map { it.nextState }.toMutableSet()))
            }
            return states
        }

        check(states.count { it.isStart } == 1) {
            "To convert a NFA to a DFA, the NFA must contain exactly one start state"
        }

        val dfa = DFA() // new empty DFA which is incrementally extended
        val epsilonClosures =
            mutableMapOf<
                Set<State>,
                State,
            >() // used to remember which DFA state an ε-closure of NFA states maps to
        val statesToExplore =
            ArrayDeque<
                Pair<State, Set<State>>
            >() // a queue to remember which states still have to be explored

        // Set up the basis on which to explore the current NFA
        // start with finding the ε-closures of the starting state
        val startStateClosure = getEpsilonClosure(mutableSetOf(states.first { it.isStart }))
        // add the new start state to the DFA corresponding to a set of NFA states
        var nextDfaState =
            dfa.addState(
                isStart = startStateClosure.any { it.isStart },
                isAcceptingState = startStateClosure.any { it.isAcceptingState },
            )
        epsilonClosures +=
            startStateClosure to
                nextDfaState // remember which DFA state maps to the startStateClosure
        // and add it to the yet to be explored states
        statesToExplore.add(nextDfaState to startStateClosure)

        // do the same thing for the rest of the NFA
        // by walking through the NFA starting with the start state, this algorithm only converts
        // the
        // reachable part of the NFA
        while (statesToExplore.isNotEmpty()) {
            // get the state to explore next (starts with the new start state created above)
            val (currentDfaState, epsilonClosure) = statesToExplore.removeFirst()
            // for each state in the epsilonClosure of the currently explored state, we have to get
            // all possible transitions/edges
            // and group them by their 'name' (the base and op attributes)
            val allPossibleEdges =
                epsilonClosure
                    .flatMap { state -> state.outgoingEdges.filter { edge -> edge.op != EPSILON } }
                    .groupBy { it.base to it.op }
            // then we follow each transition/edge for the current epsilonClosure
            for ((transitionBaseToOp, edges) in allPossibleEdges) {
                val (transitionBase, transitionOp) = transitionBaseToOp
                // because multiple states in the current epsilonClosure might have edges with the
                // same 'name' but to different states
                // we again have to get the epsilonClosure of the target states
                val transitionClosure = getEpsilonClosure(edges.map { it.nextState }.toMutableSet())
                if (transitionClosure in epsilonClosures) {
                    // if the transitionClosure is already in the DFA, get the DFA state it
                    // corresponds to
                    epsilonClosures[transitionClosure]?.let { nextDfaState = it }
                } else {
                    // else create a new DFA state and add it to the known and to be explored states
                    nextDfaState =
                        dfa.addState(
                            isAcceptingState = transitionClosure.any { it.isAcceptingState }
                        )
                    statesToExplore.add(nextDfaState to transitionClosure)
                    epsilonClosures += transitionClosure to nextDfaState
                }
                // either way, we must create an edge connecting the states
                currentDfaState.addEdge(
                    Edge(base = transitionBase, op = transitionOp, nextState = nextDfaState)
                )
            }
        }
        return dfa
    }
}
