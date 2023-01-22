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
class State(name: Int, isStart: Boolean = false, isAcceptingState: Boolean = false) {
    /**
     * Must only be changed through [FSM.changeStateProperty] as soon as they are part of a [FSM].
     */
    var name = name
        internal set

    /**
     * Must only be changed through [FSM.changeStateProperty] as soon as they are part of a [FSM].
     */
    var isStart = isStart
        internal set

    /**
     * Must only be changed through [FSM.changeStateProperty] as soon as they are part of a [FSM].
     */
    var isAcceptingState = isAcceptingState
        internal set

    /** Must only be changed through [addEdge]. */
    private val _outgoingEdges: MutableSet<Edge> = mutableSetOf()
    var outgoingEdges: Set<Edge>
        get() = _outgoingEdges
        set(value) {
            _outgoingEdges.clear()
            _outgoingEdges.addAll(value)
        }

    /**
     * Set by the [FSM] when this state is added to a [FSM]. This lambda should throw an exception
     * in case the edge is not allowed in the [FSM]. Once set by the [FSM], it is called in
     * [addEdge].
     */
    internal var edgeCheck: ((Edge) -> Unit)? = null

    fun addEdge(edge: Edge) {
        edgeCheck?.let { it(edge) }
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
    fun copy(
        name: Int = this.name,
        isStart: Boolean = this.isStart,
        isAcceptingState: Boolean = this.isAcceptingState
    ) =
        State(name = name, isStart = isStart, isAcceptingState = isAcceptingState).apply {
            outgoingEdges.forEach { addEdge(it) }
        }

    fun deepCopy(currentStates: MutableSet<State> = mutableSetOf()): MutableSet<State> {
        if (currentStates.contains(this)) {
            return currentStates
        }

        val newState = copy() // get a shallow copy
        newState._outgoingEdges
            .clear() // and then get rid of the shallowly copied edges -> when doing a deepCopy, we
        // must also create new edge objects
        currentStates.add(newState)

        for (edge in outgoingEdges) {
            edge.nextState.deepCopy(currentStates)
            newState.addEdge(
                edge.copy(nextState = currentStates.first { it.name == edge.nextState.name })
            )
        }
        return currentStates
    }
}
