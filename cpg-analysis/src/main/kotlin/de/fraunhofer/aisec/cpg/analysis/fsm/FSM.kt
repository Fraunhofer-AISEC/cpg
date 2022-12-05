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

/** A representation of a generic finite state automaton. Can either a [NFA] or a [DFA]. */
sealed class FSM(states: Set<State>) {

    private val _states: MutableSet<State> = mutableSetOf()
    val states: Set<State>
        get() = _states
    private val nextStateName
        get() = if (states.isEmpty()) 1 else states.maxOf { it.name } + 1

    init {
        require(states.count { it.isStart } <= 1) {
            "Cannot create a FSM with multiple start states"
        }
        for (state in states) addState(state)
    }

    /**
     * Checks whether the given object is an [FSM] and whether it accepts the same language as this
     * [FSM]
     */
    override fun equals(other: Any?) = if (other is FSM) acceptsSameLanguage(this, other) else false

    /**
     * This function is set as [State.edgeCheck] inside [addState]. In case the [edge] must not be
     * added to the [state], this function must throw an exception.
     */
    open fun checkEdge(state: State, edge: Edge) {}

    private fun checkState(state: State) {
        for (edge in state.outgoingEdges) checkEdge(state, edge)
        if (state.isStart) {
            require(states.singleOrNull { it.isStart } == null) {
                "This FSM already has a start state."
            }
        }
    }

    /** Generates a new state and adds it to this FSM. */
    fun addState(isStart: Boolean = false, isAcceptingState: Boolean = false): State {
        val newState =
            State(name = nextStateName, isStart = isStart, isAcceptingState = isAcceptingState)
        addState(newState)
        return newState
    }

    /**
     * States are differentiated by their name. Therefore, adding an already existing state a second
     * time will override the previous state.
     */
    protected fun addState(state: State) {
        if (state !in states) {
            checkState(state) // assert that the state can be added to this FSM
            state.edgeCheck = { edge: Edge ->
                checkEdge(state, edge)
            } // tell the state what kind of edges to accept
            _states.add(state)
        }
    }

    /** Creates an edge between two nodes with a given label (operator and optional base). */
    fun addEdge(from: State, edge: Edge) {
        addState(from)
        from.addEdge(
            edge
        ) // because [addState] sets [State.edgeCheck], the state now checks whether the edge can be
        // added to this FSM
        addState(edge.nextState)
    }

    /**
     * Safely change a property of a state contained in this [FSM]. Before changing the property,
     * this method makes sure that e.g., no other start state exists or that no other state already
     * has the suggested [name].
     *
     * State properties should only be changed through this method as soon as they part of a [FSM].
     *
     * @return true if the property was changed, false otherwise.
     */
    fun changeStateProperty(
        state: State,
        name: Int? = null,
        isStart: Boolean? = null,
        isAcceptingState: Boolean? = null
    ): Boolean {
        if (name != null) {
            if (states.any { it.name == name }) return false
            else {
                state.name = name
            }
        }
        if (isStart != null) {
            if (isStart && states.any { it.isStart }) return false else state.isStart = isStart
        }
        if (isAcceptingState != null) {
            state.isAcceptingState = isAcceptingState
        }
        return true
    }

    /**
     * Same as [changeStateProperty] but throws an [IllegalStateException] if the property could not
     * be changed.
     */
    fun checkedChangeStateProperty(
        state: State,
        name: Int? = null,
        isStart: Boolean? = null,
        isAcceptingState: Boolean? = null
    ) =
        check(
            changeStateProperty(
                state = state,
                name = name,
                isStart = isStart,
                isAcceptingState = isAcceptingState
            )
        )

    fun renameStatesToBeDifferentFrom(otherFsm: FSM) {
        otherFsm.states.forEach {
            otherFsm.checkedChangeStateProperty(
                it,
                name = it.name + maxOf(states.maxOf { it.name }, otherFsm.states.maxOf { it.name })
            )
        }
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
                    "\tq${s.name} [shape=doublecircle];\n"
                } else {
                    "\tq${s.name} [shape=circle];\n"
                }
            if (s.isStart) {
                edges += "\t\"\" -> q${s.name};\n"
            }

            for (e in s.outgoingEdges) {
                edges += "\tq${s.name} -> q${e.nextState.name} [label=\"${e.toDotLabel()}\"];\n"
            }
        }
        return "$str$edges}"
    }

    /** Creates a shallow copy. */
    abstract fun copy(): FSM

    /** Creates a deep copy of this FSM to enable multiple independent branches of execution. */
    open fun deepCopy(): FSM {
        val newFSM = copy()
        if (newFSM.states.isEmpty()) return newFSM

        val startingState = this.states.singleOrNull { it.isStart }
        checkNotNull(startingState) { "Only FSMs with a single starting state can be deep copied" }
        val newStates = startingState.deepCopy()
        newFSM._states.clear()
        newStates.forEach { newFSM.addState(it) }

        return newFSM
    }
}
