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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import java.util.IdentityHashMap

/**
 * An abstract class representing complete lattices, i.e., an ordered structure of values of type
 * [T]. Implementations of this class have to implement the comparator, the least upper bound of two
 * lattices.
 */
abstract class Lattice<T>(open val elements: T) : Comparable<Lattice<T>> {
    /**
     * Computes the least upper bound of this lattice and [other]. It returns a new object and does
     * not modify either of the objects.
     */
    abstract fun lub(other: Lattice<T>): Lattice<T>

    /** Duplicates the object, i.e., makes a deep copy. */
    abstract fun duplicate(): Lattice<T>
}

/**
 * Implements the [Lattice] over a set of nodes. The lattice itself is constructed by the powerset.
 */
class PowersetLattice(override val elements: Set<Node>) : Lattice<Set<Node>>(elements) {
    override fun lub(other: Lattice<Set<Node>>) =
        PowersetLattice(other.elements.union(this.elements))
    override fun duplicate() = PowersetLattice(this.elements.toSet())
    override fun compareTo(other: Lattice<Set<Node>>): Int {
        return if (this.elements.containsAll(other.elements)) {
            if (this.elements.size > other.elements.size) 1 else 0
        } else {
            -1
        }
    }
}

/**
 * Stores the current state. I.e., it maps a [Node] to a [Lattice]. It provides some useful
 * functions e.g. to check if the mapping has to be updated (e.g. because there are new nodes or
 * because a new lattice is bigger than the old one).
 */
open class State<V> : IdentityHashMap<Node, Lattice<V>>() {

    /**
     * It updates this state by adding all new nodes in [other] to `this` and by computing the least
     * upper bound for each entry.
     *
     * Returns this and a flag which states if there was any update necessary (or if `this` is equal
     * before and after running the method).
     */
    open fun lub(other: State<V>): Pair<State<V>, Boolean> {
        var update = false
        for ((node, newLattice) in other) {
            update = push(node, newLattice) || update
        }
        return Pair(this, update)
    }

    /**
     * Checks if an update is necessary, i.e., if [other] contains nodes which are not present in
     * `this` and if the lattice of a node in [other] "is bigger" than the respective lattice in
     * `this`. It does not modify anything.
     */
    open fun needsUpdate(other: State<V>): Boolean {
        var update = false
        for ((node, newLattice) in other) {
            update = update || node !in this || newLattice > this[node]!!
        }
        return update
    }

    /** Deep copies this object. */
    open fun duplicate(): State<V> {
        val clone = State<V>()
        for ((key, value) in this) {
            clone[key] = value.duplicate()
        }
        return clone
    }

    /**
     * Adds a new mapping from [newNode] to (a copy of) [newLattice] to this object if [newNode]
     * does not exist in this state yet. If it already exists, it computes the least upper bound of
     * [newLattice] and the current one for [newNode]. It returns if the state has changed.
     */
    open fun push(newNode: Node, newLattice: Lattice<V>?): Boolean {
        if (newLattice == null) {
            return false
        }
        if (newNode in this && this[newNode]!! >= newLattice) {
            // newLattice is "smaller" than the currently stored one. We don't add it anything.
            return false
        } else if (newNode in this) {
            // newLattice is "bigger" than the currently stored one. We update it to the least
            // upper bound
            this[newNode] = this[newNode]!!.lub(newLattice)
        } else {
            this[newNode] = newLattice.duplicate()
        }
        return true
    }
}

/**
 * A worklist. Essentially, it stores mappings of nodes to the states which are available there and
 * determines which nodes have to be analyzed.
 */
class Worklist<V>() {
    /** A mapping of nodes to the state which is currently available there. */
    var globalState: MutableMap<Node, State<V>> = mutableMapOf()
        private set

    /** A list of all nodes which have already been visited. */
    private val alreadySeen = mutableListOf<Node>()

    constructor(globalState: MutableMap<Node, State<V>> = mutableMapOf()) : this() {
        this.globalState = globalState
    }

    /**
     * The actual worklist, i.e., elements which still have to be analyzed and the state which
     * should be considered there.
     */
    private val nodeOrder: MutableList<Pair<Node, State<V>>> = mutableListOf()

    /**
     * Adds [newNode] and the [state] to the [globalState] (i.e., computes the [State.lub] of the
     * current state there and [state]). Returns true if there was an update.
     */
    fun update(newNode: Node, state: State<V>): Boolean {
        val (newGlobalState, update) = globalState[newNode]?.lub(state) ?: Pair(state, true)
        if (update) {
            globalState[newNode] = newGlobalState
        }
        return update
    }

    /**
     * Pushes [newNode] and the [state] to the worklist or updates the currently available entry for
     * the node. Returns `true` if there was a change which means that the node has to be analyzed.
     * If it returns `false`, the [newNode] wasn't added to the worklist as the state didn't change.
     */
    fun push(newNode: Node, state: State<V>): Boolean {
        val currentEntry = nodeOrder.find { it.first == newNode }
        val update: Boolean
        val newEntry =
            if (currentEntry != null) {
                val (newState, update2) = currentEntry.second.lub(state)
                update = update2
                if (update) {
                    nodeOrder.remove(currentEntry)
                }
                Pair(currentEntry.first, newState)
            } else {
                update = true
                Pair(newNode, state)
            }
        if (update) nodeOrder.add(newEntry)
        return update
    }

    /** Determines if there are still elements to analyze */
    fun isNotEmpty() = nodeOrder.isNotEmpty()
    /** Determines if there are no more elements to analyze */
    fun isEmpty() = nodeOrder.isEmpty()

    /** Removes a [Node] from the worklist and returns the [Node] together with its [State] */
    fun pop(): Pair<Node, State<V>> {
        val node = nodeOrder.removeFirst()
        alreadySeen.add(node.first)
        return node
    }

    /** Checks if [currentNode] has already been visited before. */
    fun hasAlreadySeen(currentNode: Node) = currentNode in alreadySeen

    /** Computes the meet over paths for all the states in [globalState]. */
    fun mop(): State<V> {
        val firstKey = globalState.keys.firstOrNull()
        val state = globalState[firstKey]
        for ((_, v) in globalState) {
            state?.lub(v)
        }

        return state ?: State()
    }
}

/** A very simple implementation of the worklist algorithm. */
class EOGWorklist {
    /**
     * Iterates through the worklist of the Evaluation Order Graph starting at [startNode] and with
     * the [State] [startState]. For each node, the [transformation] is applied which should update
     * the state.
     *
     * [transformation] receives the current [Node] popped from the worklist, the [State] at this
     * node which is considered for this analysis and even the current [Worklist]. The worklist is
     * given if we have to add more elements out-of-order e.g. because the EOG is traversed in an
     * order which is not useful for this analysis. The [transformation] has to return the updated
     * [State] and an indication if we expect that the state has changed. This is necessary because
     * not every transition in the EOG will really lead to an update of the current state depending
     * on the analysis.
     */
    fun <V> iterateEOG(
        startNode: Node,
        startState: State<V>,
        transformation: (Node, State<V>, Worklist<V>) -> Pair<State<V>, Boolean>
    ): State<V> {
        val worklist = Worklist(mutableMapOf(Pair(startNode, startState)))
        worklist.push(startNode, startState)

        while (worklist.isNotEmpty()) {
            val (nextNode, state) = worklist.pop()

            val (newState, expectedUpdate) = transformation(nextNode, state.duplicate(), worklist)
            if (worklist.update(nextNode, newState) || !expectedUpdate) {
                nextNode.nextEOG.forEach { worklist.push(it, newState.duplicate()) }
            }
        }
        return worklist.mop()
    }
}

/**
 * A state which actually holds a state for all nodes, one only for declarations and one for
 * ReturnStatements.
 */
class DFGPassState<V>(
    /** A mapping of a [Node] to its [Lattice]. */
    var generalState: State<V> = State(),
    /** A mapping of [Declaration] to its [Lattice]. */
    var declarationsState: State<V> = State(),
    /** The [returnStatements] which are reachable. */
    var returnStatements: State<V> = State()
) : State<V>() {
    override fun duplicate(): DFGPassState<V> {
        return DFGPassState(generalState.duplicate(), declarationsState.duplicate())
    }

    override fun lub(other: State<V>): Pair<State<V>, Boolean> {
        return if (other is DFGPassState) {
            val (_, generalUpdate) = generalState.lub(other.generalState)
            val (_, declUpdate) = declarationsState.lub(other.declarationsState)
            Pair(this, generalUpdate || declUpdate)
        } else {
            val (_, generalUpdate) = generalState.lub(other)
            Pair(this, generalUpdate)
        }
    }

    override fun needsUpdate(other: State<V>): Boolean {
        return if (other is DFGPassState) {
            generalState.needsUpdate(other.generalState) ||
                declarationsState.needsUpdate(other.declarationsState)
        } else {
            generalState.needsUpdate(other)
        }
    }

    override fun push(newNode: Node, newLattice: Lattice<V>?): Boolean {
        return generalState.push(newNode, newLattice)
    }

    /** Pushes the [newNode] and its [newLattice] to the [declarationsState]. */
    fun pushToDeclarationsState(newNode: Declaration, newLattice: Lattice<V>?): Boolean {
        return declarationsState.push(newNode, newLattice)
    }
}
