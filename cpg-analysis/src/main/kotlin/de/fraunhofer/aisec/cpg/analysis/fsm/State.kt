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

/**
 * A simple class representing a state in an FSM.
 * - [name] is the name of the State and must be unique for the FSM.
 * - [isStart] indicates if it is the starting state.
 * - [isAcceptingState] indicates if this State accepts the FSM (in our case, this means that the
 * order of statements was correct).
 */
sealed class State(name: Int, isStart: Boolean = false, isAcceptingState: Boolean = false) {
    /** Must only be changed through [FSM.changeStateProperty] as soon as they part of a [FSM]. */
    internal var _name = name
    val name
        get() = _name

    /** Must only be changed through [FSM.changeStateProperty] as soon as they part of a [FSM]. */
    internal var _isStart = isStart
    val isStart
        get() = _isStart

    /** Must only be changed through [FSM.changeStateProperty] as soon as they part of a [FSM]. */
    internal var _isAcceptingState = isAcceptingState
    val isAcceptingState
        get() = _isAcceptingState

    protected val _outgoingEdges: MutableSet<Edge> = mutableSetOf()
    val outgoingEdges: Set<Edge>
        get() = _outgoingEdges

    open fun addEdge(edge: Edge) {
        _outgoingEdges.add(edge)
    }

    // equals method using only the name property
    override fun equals(other: Any?): Boolean {
        return (other as? State)?.name?.equals(name) == true
    }

    // hashCode method using only the name property
    override fun hashCode() = name.hashCode()

    override fun toString(): String {
        return (if (isStart) "(S) q$name" else "q$name") + (if (isAcceptingState) " (A)" else "")
    }

    /** Create a shallow copy */
    protected abstract fun copy(
        name: Int = this.name,
        isStart: Boolean = this.isStart,
        isAcceptingState: Boolean = this.isAcceptingState
    ): State

    fun deepCopy(currentStates: MutableSet<State> = mutableSetOf()): MutableSet<State> {
        if (currentStates.contains(this)) {
            return currentStates
        }

        val newState =
            copy(
                name = name,
                isStart = isStart,
                isAcceptingState = isAcceptingState
            ) // get a shallow copy
        newState._outgoingEdges
            .clear() // and then get rid of the shallowly copied edges -> when doing a deepCopy, we
        // must also create new edge objects
        currentStates.add(newState)

        for (edge in outgoingEdges) {
            edge.nextState.deepCopy(currentStates)
            newState.addEdge(
                Edge(
                    op = edge.op,
                    base = edge.base,
                    nextState = currentStates.first { it.name == edge.nextState.name }
                )
            )
        }
        return currentStates
    }
}

/**
 * A simple class representing a state in a DFA.
 * - [name] is the name of the State and must be unique for the FSM.
 * - [isStart] indicates if it is the starting state.
 * - [isAcceptingState] indicates if this State accepts the FSM (in our case, this means that the
 * order of statements was correct).
 */
class DfaState
internal constructor(name: Int, isStart: Boolean = false, isAcceptingState: Boolean = false) :
    State(name = name, isStart = isStart, isAcceptingState = isAcceptingState) {
    override fun addEdge(edge: Edge) {
        check(edge.op != NFA.EPSILON) { "A DFA must not contain EPSILON edges!" }
        check(outgoingEdges.none { e -> e.matches(edge) && e.nextState != edge.nextState }) {
            "State already has an outgoing edge with the same label but a different target!"
        }
        _outgoingEdges.add(edge)
    }

    /** Create a shallow copy */
    override fun copy(name: Int, isStart: Boolean, isAcceptingState: Boolean) =
        DfaState(name = name, isStart = isStart, isAcceptingState = isAcceptingState).apply {
            this@DfaState.outgoingEdges.forEach { addEdge(it) }
        }
}

/**
 * A simple class representing a state in a NFA.
 * - [name] is the name of the State and must be unique for the FSM.
 * - [isStart] indicates if it is the starting state.
 * - [isAcceptingState] indicates if this State accepts the FSM (in our case, this means that the
 * order of statements was correct).
 */
class NfaState
internal constructor(name: Int, isStart: Boolean = false, isAcceptingState: Boolean = false) :
    State(name = name, isStart = isStart, isAcceptingState = isAcceptingState) {
    /** Create a shallow copy */
    override fun copy(name: Int, isStart: Boolean, isAcceptingState: Boolean) =
        NfaState(name = name, isStart = isStart, isAcceptingState = isAcceptingState).apply {
            this@NfaState.outgoingEdges.forEach { addEdge(it) }
        }
}
