/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers.functional

import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import java.util.IdentityHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.fold
import kotlin.collections.plusAssign
import kotlin.collections.set

/** Used to identify the order of elements */
enum class Order {
    GREATER,
    EQUAL,
    LESSER,
    UNEQUAL,
}

/**
 * A lattice is a partially ordered structure of values of type [T]. [T] could be anything, where
 * common examples are sets, ranges, maps, tuples, but it can also have random names and a new data
 * structure which only make sense in a certain context. [T] depends on the analysis and typically
 * has to abstract the value for the specific purpose.
 *
 * This class provides functionality to
 * - compute the least upper bound (also called join) of two elements.
 * - compute the greatest lower bound (also called meet) of two elements.
 * - compare two elements.
 * - duplicate/clone an element.
 *
 * Note: We do not store the elements spanning the lattice because it would cost too much memory for
 * non-trivial examples.
 */
interface Lattice<T> {

    /** The smallest possible element in the lattice */
    val bottom: T

    /** Computes the least upper bound (join) of [one] and [two] */
    fun lub(one: T, two: T): T

    /** Computes the greatest lower bound (meet) of [one] and [two] */
    fun glb(one: T, two: T): T

    /**
     * Compares [one] and [two]. Returns
     * - [Order.GREATER] if one is greater than two (this also means that `lub(one, two) == one` and
     *   `glb(one, two) == two`).
     * - [Order.EQUAL] if one is the same as two (this also means that `lub(one, two) == one == two`
     *   and `glb(one, two) == one == two`).
     * - [Order.LESSER] if two is greater than one (this also means that `lub(one, two) == two` and
     *   `glb(one, two) = one`).
     * - [Order.UNEQUAL] in all other cases (this also means that `one != two` and `two != lub(one,
     *   two) != one` and `two != glb(one, two) != one`).
     */
    fun compare(one: T, two: T): Order

    /** Returns a copy of [one]. */
    fun duplicate(one: T): T

    /**
     * Computes a fixpoint by iterating over the EOG beginning with the [startEdges] and a state
     * [startState]. This means, it keeps applying [transformation] until the state does no longer
     * change. With state, we mean a mapping between the [EvaluationOrder] edges to the value of
     * [LatticeElement] which represents possible values (or abstractions thereof) that they hold.
     */
    fun iterateEOGEvenMoreNew(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: (EvaluationOrder, T) -> T,
    ): T {
        val globalState = IdentityHashMap<EvaluationOrder, T>()
        val finalState = IdentityHashMap<EvaluationOrder, T>()
        for (startEdge in startEdges) {
            globalState[startEdge] = startState
        }
        val edgesList = mutableListOf<EvaluationOrder>()
        startEdges.forEach { edgesList.add(it) }

        while (edgesList.isNotEmpty()) {
            val nextEdge = edgesList.first()
            edgesList.removeFirst()

            // Compute the effects of "nextEdge" on the state by applying the transformation to its
            // state.
            val nextGlobal = globalState[nextEdge] ?: continue
            val newState = transformation(nextEdge, nextGlobal)
            if (nextEdge.end.nextEOGEdges.isEmpty()) {
                finalState[nextEdge] = newState
            }
            nextEdge.end.nextEOGEdges.forEach {
                // We continue with the nextEOG edge if we haven't seen it before or if we updated
                // the
                // state in comparison to the previous time we were there.
                val oldGlobalIt = globalState[it]
                val newGlobalIt = (oldGlobalIt?.let { this.lub(newState, it) } ?: newState)
                globalState[it] = newGlobalIt
                if (it !in edgesList && (oldGlobalIt == null || newGlobalIt != oldGlobalIt)) {
                    edgesList.add(0, it)
                }
            }
        }

        return finalState.values.fold(finalState.values.firstOrNull()) { state, value ->
            state?.let { lub(it, value) }
        } ?: startState
    }
}

/** Implements a [Lattice] whose elements are the powerset of a given set of values. */
class PowersetLattice<T>() : Lattice<Set<T>> {
    override val bottom: Set<T>
        get() = identitySetOf()

    override fun lub(one: Set<T>, two: Set<T>): Set<T> {
        return when (compare(one, two)) {
            Order.GREATER -> duplicate(one)
            Order.EQUAL -> duplicate(one)
            Order.LESSER -> duplicate(two)
            else -> {
                val result = one.toIdentitySet()
                result += two
                result
            }
        }
    }

    override fun glb(one: Set<T>, two: Set<T>): Set<T> {
        return one.intersect(two).toIdentitySet()
    }

    override fun compare(one: Set<T>, two: Set<T>): Order {
        return when {
            one == two -> Order.EQUAL
            one.containsAll(two) -> Order.GREATER
            two.containsAll(one) -> Order.LESSER
            else -> Order.UNEQUAL
        }
    }

    override fun duplicate(one: Set<T>): Set<T> {
        return one.toIdentitySet()
    }
}

/**
 * Implements the [LatticeElement] for a lattice over a map of nodes to another lattice represented
 * by [innerLattice].
 */
class MapLattice<K, V>(val innerLattice: Lattice<V>) : Lattice<Map<K, V>> {
    override val bottom: Map<K, V>
        get() = IdentityHashMap()

    override fun lub(one: Map<K, V>, two: Map<K, V>): Map<K, V> {
        val allKeys = one.keys.toIdentitySet()
        allKeys += two.keys
        val newMap =
            allKeys.fold(IdentityHashMap<K, V>()) { current, key ->
                val otherValue = two[key]
                val thisValue = one[key]
                val newValue =
                    if (thisValue != null && otherValue != null) {
                        innerLattice.lub(thisValue, otherValue)
                    } else thisValue ?: otherValue
                newValue?.let { current[key] = it }
                current
            }
        return newMap
    }

    override fun glb(one: Map<K, V>, two: Map<K, V>): Map<K, V> {
        val allKeys = one.keys.intersect(two.keys).toIdentitySet()
        val newMap =
            allKeys.fold(IdentityHashMap<K, V>()) { current, key ->
                val otherValue = two[key]
                val thisValue = one[key]
                val newValue =
                    if (thisValue != null && otherValue != null) {
                        innerLattice.glb(thisValue, otherValue)
                    } else thisValue ?: otherValue
                newValue?.let { current[key] = it }
                current
            }
        return newMap
    }

    override fun compare(one: Map<K, V>, two: Map<K, V>): Order {
        return when {
            one == two -> Order.EQUAL
            one.keys.containsAll(two.keys) &&
                one.entries.all { (k, v) ->
                    two[k]?.let { otherV ->
                        (innerLattice.compare(v, otherV) == Order.GREATER ||
                            innerLattice.compare(v, otherV) == Order.EQUAL)
                    } != false
                } -> Order.GREATER
            two.keys.containsAll(one.keys) &&
                two.entries.all { (k, v) ->
                    one[k]?.let { otherV ->
                        (innerLattice.compare(v, otherV) == Order.GREATER ||
                            innerLattice.compare(v, otherV) == Order.EQUAL)
                    } != false
                } -> Order.LESSER
            else -> Order.UNEQUAL
        }
    }

    override fun duplicate(one: Map<K, V>): Map<K, V> {
        return IdentityHashMap(
            one.map { (k, v) -> Pair<K, V>(k, innerLattice.duplicate(v)) }.toMap()
        )
    }
}

/**
 * Implements the [LatticeElement] for a lattice over two other lattices which are represented by
 * [innerLattice1] and [innerLattice2].
 */
class TupleLattice<S, T>(val innerLattice1: Lattice<S>, val innerLattice2: Lattice<T>) :
    Lattice<Pair<S, T>> {
    override val bottom: Pair<S, T>
        get() = Pair(innerLattice1.bottom, innerLattice2.bottom)

    override fun lub(one: Pair<S, T>, two: Pair<S, T>): Pair<S, T> {
        val result1 = innerLattice1.lub(one.first, two.first)
        val result2 = innerLattice2.lub(one.second, two.second)
        return Pair(result1, result2)
    }

    override fun glb(one: Pair<S, T>, two: Pair<S, T>): Pair<S, T> {
        val result1 = innerLattice1.glb(one.first, two.first)
        val result2 = innerLattice2.glb(one.second, two.second)
        return Pair(result1, result2)
    }

    override fun compare(one: Pair<S, T>, two: Pair<S, T>): Order {
        val result1 = innerLattice1.compare(one.first, two.first)
        val result2 = innerLattice2.compare(one.second, two.second)
        return when {
            result1 == Order.EQUAL && result2 == Order.EQUAL -> Order.EQUAL
            result1 == Order.GREATER && result2 == Order.GREATER -> Order.GREATER
            result1 == Order.EQUAL && result2 == Order.GREATER -> Order.GREATER
            result1 == Order.GREATER && result2 == Order.EQUAL -> Order.GREATER
            result1 == Order.LESSER && result2 == Order.LESSER -> Order.LESSER
            result1 == Order.EQUAL && result2 == Order.LESSER -> Order.LESSER
            result1 == Order.LESSER && result2 == Order.EQUAL -> Order.LESSER
            else -> Order.UNEQUAL
        }
    }

    override fun duplicate(one: Pair<S, T>): Pair<S, T> {
        return Pair(innerLattice1.duplicate(one.first), innerLattice2.duplicate(one.second))
    }
}

/**
 * Implements the [LatticeElement] for a lattice over three other lattices which are represented by
 * [innerLattice1], [innerLattice2] and [innerLattice3].
 */
class TripleLattice<R, S, T>(
    val innerLattice1: Lattice<R>,
    val innerLattice2: Lattice<S>,
    val innerLattice3: Lattice<T>,
) : Lattice<Triple<R, S, T>> {
    override val bottom: Triple<R, S, T>
        get() = Triple(innerLattice1.bottom, innerLattice2.bottom, innerLattice3.bottom)

    override fun lub(one: Triple<R, S, T>, two: Triple<R, S, T>): Triple<R, S, T> {
        val result1 = innerLattice1.lub(one.first, two.first)
        val result2 = innerLattice2.lub(one.second, two.second)
        val result3 = innerLattice3.lub(one.third, two.third)
        return Triple(result1, result2, result3)
    }

    override fun glb(one: Triple<R, S, T>, two: Triple<R, S, T>): Triple<R, S, T> {
        val result1 = innerLattice1.glb(one.first, two.first)
        val result2 = innerLattice2.glb(one.second, two.second)
        val result3 = innerLattice3.glb(one.third, two.third)
        return Triple(result1, result2, result3)
    }

    override fun compare(one: Triple<R, S, T>, two: Triple<R, S, T>): Order {
        val result1 = innerLattice1.compare(one.first, two.first)
        val result2 = innerLattice2.compare(one.second, two.second)
        val result3 = innerLattice3.compare(one.third, two.third)
        return when {
            result1 == Order.EQUAL && result2 == Order.EQUAL && result3 == Order.EQUAL ->
                Order.EQUAL
            result1 == Order.GREATER && result2 == Order.GREATER && result3 == Order.GREATER ->
                Order.GREATER
            result1 == Order.GREATER && result2 == Order.GREATER && result3 == Order.EQUAL ->
                Order.GREATER
            result1 == Order.GREATER && result2 == Order.EQUAL && result3 == Order.GREATER ->
                Order.GREATER
            result1 == Order.GREATER && result2 == Order.EQUAL && result3 == Order.EQUAL ->
                Order.GREATER
            result1 == Order.EQUAL && result2 == Order.GREATER && result3 == Order.GREATER ->
                Order.GREATER
            result1 == Order.EQUAL && result2 == Order.GREATER && result3 == Order.EQUAL ->
                Order.GREATER
            result1 == Order.EQUAL && result2 == Order.EQUAL && result3 == Order.GREATER ->
                Order.GREATER
            result1 == Order.LESSER && result2 == Order.LESSER && result3 == Order.LESSER ->
                Order.LESSER
            result1 == Order.LESSER && result2 == Order.LESSER && result3 == Order.EQUAL ->
                Order.LESSER
            result1 == Order.LESSER && result2 == Order.EQUAL && result3 == Order.LESSER ->
                Order.LESSER
            result1 == Order.LESSER && result2 == Order.EQUAL && result3 == Order.EQUAL ->
                Order.LESSER
            result1 == Order.EQUAL && result2 == Order.LESSER && result3 == Order.LESSER ->
                Order.LESSER
            result1 == Order.EQUAL && result2 == Order.LESSER && result3 == Order.EQUAL ->
                Order.LESSER
            result1 == Order.EQUAL && result2 == Order.EQUAL && result3 == Order.LESSER ->
                Order.LESSER
            else -> Order.UNEQUAL
        }
    }

    override fun duplicate(one: Triple<R, S, T>): Triple<R, S, T> {
        return Triple(
            innerLattice1.duplicate(one.first),
            innerLattice2.duplicate(one.second),
            innerLattice3.duplicate(one.third),
        )
    }
}
