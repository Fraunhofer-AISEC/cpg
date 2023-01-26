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
                Set<State>, State
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
                isAcceptingState = startStateClosure.any { it.isAcceptingState }
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
        while (statesToExplore.size > 0) {
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
                    nextDfaState = epsilonClosures[transitionClosure]!!
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
                    Edge(
                        base = transitionBase,
                        op = transitionOp,
                        nextState = nextDfaState,
                    )
                )
            }
        }
        return dfa
    }

    /**
     * Creates a regular expression of the NFA with the state elimination strategy. It enriches the
     * edges to retrieve a GNFA and finally has a regex. Unfortunately, it is not optimized or super
     * readable.
     */
    fun toRegex(): String {
        fun List<Edge>.combineToRegex(): String {
            var result = ""
            val singleChars = mutableListOf<String>()
            for (edge in this) {
                if (edge.op.length == 1) {
                    // Only one character
                    singleChars.add(edge.op)
                } else if (edge.op.isNotEmpty()) {
                    result = if (result.isEmpty()) edge.op else "$result|${edge.op}"
                }
            }
            if (singleChars.size > 1) {
                result += "[" + singleChars.joinToString("") + "]"
            } else {
                result += singleChars.joinToString("")
            }

            return if ("|" in result) "($result)" else result
        }

        fun getSelfLoopOfState(toReplace: State): String {
            // First, we get the loop(s) to the same node.
            var selfLoop =
                toReplace.outgoingEdges
                    .filter { it.nextState == toReplace && it.op != EPSILON }
                    .combineToRegex()
            // EPSILON wouldn't change anything here because we put the asterisk operator around it.
            // So, we just remove such an edge.
            // There's a loop, so we surround it with brackets and put the * operator
            if (selfLoop.isNotEmpty()) selfLoop = "$selfLoop*"

            return selfLoop
        }

        fun getOutgoingEdgesMap(toReplace: State): Map<State, String> {
            val result = mutableMapOf<State, String>()
            // How can we reach the respective nodes?
            for ((k, v) in toReplace.outgoingEdges.groupBy { it.nextState }) {
                // Only consider edges to other nodes.
                if (k == toReplace) continue

                // Collect the different branches in one regex
                var regex = v.filter { it.op != EPSILON }.combineToRegex()

                // We put the ? around this regex because we can bypass it with the EPSILON edge.
                if (regex.isNotEmpty() && v.any { it.op == EPSILON }) regex += "?"

                result[k] = regex
            }
            return result
        }

        fun replaceStateWithRegex(toReplace: State, remainingStates: MutableSet<State>) {
            val selfLoop = getSelfLoopOfState(toReplace)
            // We add the self-loop string to the front because it affects every single outgoing
            // edge.
            val outgoingMap = getOutgoingEdgesMap(toReplace).mapValues { (_, v) -> selfLoop + v }
            // Iterate over all states and their edges which have a transition to toReplace.
            // We replace this edge with edges to all nodes in outgoingMap and assemble the
            // respective string
            for (state in remainingStates) {
                val newEdges = mutableSetOf<Edge>()
                // Get the regex from state to the state to replace. There might be multiple options
                val outgoingEdges = state.outgoingEdges.filter { it.nextState == toReplace }
                var regexToReplace = outgoingEdges.filter { it.op != EPSILON }.combineToRegex()

                // If there's an EPSILON edge from state to toReplace, everything is optional
                if (outgoingEdges.any { it.op == EPSILON && regexToReplace.isNotEmpty() })
                    regexToReplace += "?"

                // We add edges from this state to each state reachable from the one to remove.
                // It's the string to reach the state to be removed + the option(s) to reach the
                // next hop
                if (outgoingEdges.isNotEmpty()) {
                    for ((key, value) in outgoingMap.entries) {
                        newEdges.add(Edge(regexToReplace + value, null, key))
                    }
                }

                // We also need all the edges to other states
                newEdges.addAll(state.outgoingEdges.filter { it.nextState != toReplace })

                state.outgoingEdges = newEdges
            }
        }

        val copy = this.deepCopy()
        val stateSet = copy.states.toMutableSet()
        // We generate a new start state to make the termination a bit easier.
        val oldStart = stateSet.first { it.isStart }
        oldStart.isStart = false
        val newStartState = copy.addState(true, false)
        newStartState.addEdge(Edge(EPSILON, null, oldStart))
        stateSet.add(newStartState)

        // We also generate a new end state
        val newEndState = copy.addState(false, true)
        stateSet
            .filter { it.isAcceptingState && it != newEndState }
            .forEach { it.addEdge(Edge(EPSILON, null, newEndState)) }
        stateSet.add(newEndState)

        while (stateSet.isNotEmpty()) {
            val toProcess =
                stateSet.firstOrNull { it != newStartState && it != newEndState }
                    ?: return newStartState.outgoingEdges.joinToString("|") { "(${it.op})" }

            stateSet.remove(toProcess)
            replaceStateWithRegex(toProcess, stateSet)
        }

        return newStartState.outgoingEdges.joinToString("|") { "(${it.op})" }
    }
}
