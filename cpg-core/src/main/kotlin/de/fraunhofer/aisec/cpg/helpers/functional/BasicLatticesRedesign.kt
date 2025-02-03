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
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import java.io.Serializable
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
interface Lattice<T : Lattice.Element> {
    interface Element {
        fun compare(other: Element): Order

        fun duplicate(): Element
    }

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
class PowersetLattice<T>() : Lattice<PowersetLattice.Element<T>> {
    class Element<T>() : IdentitySet<T>(), Lattice.Element {
        constructor(set: Set<T>) : this() {
            addAll(set)
        }

        constructor(vararg entries: T) : this() {
            addAll(entries)
        }

        override fun equals(other: Any?): Boolean {
            return other is Element<T> && this.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            return when {
                other !is Element<T> -> Order.UNEQUAL
                super<IdentitySet>.equals(other) -> Order.EQUAL
                this.containsAll(other) -> Order.GREATER
                other.containsAll(this) -> Order.LESSER
                else -> Order.UNEQUAL
            }
        }

        override fun duplicate(): Element<T> {
            return Element(this)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    override val bottom: Element<T>
        get() = Element()

    override fun lub(one: Element<T>, two: Element<T>): Element<T> {
        return when (compare(one, two)) {
            Order.GREATER -> one.duplicate()
            Order.EQUAL -> one.duplicate()
            Order.LESSER -> two.duplicate()
            else -> {
                val result = one.duplicate()
                result += two
                result
            }
        }
    }

    override fun glb(one: Element<T>, two: Element<T>): Element<T> {
        return Element(one.intersect(two))
    }

    override fun compare(one: Element<T>, two: Element<T>): Order {
        return when {
            one == two -> Order.EQUAL
            one.containsAll(two) -> Order.GREATER
            two.containsAll(one) -> Order.LESSER
            else -> Order.UNEQUAL
        }
    }

    override fun duplicate(one: Element<T>): Element<T> {
        return one.duplicate()
    }
}

/**
 * Implements the [LatticeElement] for a lattice over a map of nodes to another lattice represented
 * by [innerLattice].
 */
class MapLattice<K, V : Lattice.Element>(val innerLattice: Lattice<V>) :
    Lattice<MapLattice.Element<K, V>> {
    class Element<K, V : Lattice.Element>() : IdentityHashMap<K, V>(), Lattice.Element {

        constructor(m: Map<K, V>) : this() {
            putAll(m)
        }

        constructor(vararg entries: Pair<K, V>) : this() {
            putAll(entries)
        }

        override fun equals(other: Any?): Boolean {
            return other is Element<K, V> && this.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            return when {
                other !is Element<K, V> -> Order.UNEQUAL
                this.keys == other.keys &&
                    this.entries.all { (k, v) ->
                        other[k]?.let { v.compare(it) == Order.EQUAL } == true
                    } -> Order.EQUAL
                this.keys.containsAll(other.keys) &&
                    this.entries.all { (k, v) ->
                        other[k]?.let { otherV ->
                            (v.compare(otherV) == Order.GREATER || v.compare(otherV) == Order.EQUAL)
                        } != false
                    } -> Order.GREATER
                other.keys.containsAll(this.keys) &&
                    other.entries.all { (k, v) ->
                        this[k]?.let { otherV ->
                            (v.compare(otherV) == Order.GREATER || v.compare(otherV) == Order.EQUAL)
                        } != false
                    } -> Order.LESSER
                else -> Order.UNEQUAL
            }
        }

        override fun duplicate(): Element<K, V> {
            return Element(this)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    override val bottom: Element<K, V>
        get() = MapLattice.Element()

    override fun lub(one: Element<K, V>, two: Element<K, V>): Element<K, V> {
        val allKeys = one.keys.toIdentitySet()
        allKeys += two.keys
        val newMap =
            allKeys.fold(MapLattice.Element<K, V>()) { current, key ->
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

    override fun glb(one: Element<K, V>, two: Element<K, V>): Element<K, V> {
        val allKeys = one.keys.intersect(two.keys).toIdentitySet()
        val newMap =
            allKeys.fold(Element<K, V>()) { current, key ->
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

    override fun compare(one: Element<K, V>, two: Element<K, V>): Order {
        return when {
            one.keys == two.keys &&
                one.entries.all { (k, v) ->
                    two[k]?.let { innerLattice.compare(v, it) == Order.EQUAL } == true
                } -> Order.EQUAL
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

    override fun duplicate(one: Element<K, V>): Element<K, V> {
        return Element(one.map { (k, v) -> Pair<K, V>(k, innerLattice.duplicate(v)) }.toMap())
    }
}

/**
 * Implements the [LatticeElement] for a lattice over two other lattices which are represented by
 * [innerLattice1] and [innerLattice2].
 */
class TupleLattice<S : Lattice.Element, T : Lattice.Element>(
    val innerLattice1: Lattice<S>,
    val innerLattice2: Lattice<T>,
) : Lattice<TupleLattice.Element<S, T>> {
    class Element<S : Lattice.Element, T : Lattice.Element>(val first: S, val second: T) :
        Serializable, Lattice.Element {
        override fun toString(): String = "($first, $second)"

        infix fun <A : Lattice.Element, B : Lattice.Element> A.to(that: B): Element<A, B> =
            Element(this, that)

        operator fun component1(): S = first

        operator fun component2(): T = second

        override fun equals(other: Any?): Boolean {
            return other is Element<S, T> && this.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            if (other !is Element<S, T>) return Order.UNEQUAL

            val result1 = this.first.compare(other.first)
            val result2 = this.second.compare(other.second)
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

        override fun duplicate(): Element<S, T> {
            return Element(first.duplicate() as S, second.duplicate() as T)
        }

        override fun hashCode(): Int {
            var result = first.hashCode()
            result = 31 * result + second.hashCode()
            return result
        }
    }

    override val bottom: Element<S, T>
        get() = Element(innerLattice1.bottom, innerLattice2.bottom)

    override fun lub(one: Element<S, T>, two: Element<S, T>): Element<S, T> {
        val result1 = innerLattice1.lub(one.first, two.first)
        val result2 = innerLattice2.lub(one.second, two.second)
        return Element(result1, result2)
    }

    override fun glb(one: Element<S, T>, two: Element<S, T>): Element<S, T> {
        val result1 = innerLattice1.glb(one.first, two.first)
        val result2 = innerLattice2.glb(one.second, two.second)
        return Element(result1, result2)
    }

    override fun compare(one: Element<S, T>, two: Element<S, T>): Order {
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

    override fun duplicate(one: Element<S, T>): Element<S, T> {
        return Element(innerLattice1.duplicate(one.first), innerLattice2.duplicate(one.second))
    }
}

/**
 * Implements the [LatticeElement] for a lattice over three other lattices which are represented by
 * [innerLattice1], [innerLattice2] and [innerLattice3].
 */
class TripleLattice<R : Lattice.Element, S : Lattice.Element, T : Lattice.Element>(
    val innerLattice1: Lattice<R>,
    val innerLattice2: Lattice<S>,
    val innerLattice3: Lattice<T>,
) : Lattice<TripleLattice.Element<R, S, T>> {
    class Element<R : Lattice.Element, S : Lattice.Element, T : Lattice.Element>(
        val first: R,
        val second: S,
        val third: T,
    ) : Serializable, Lattice.Element {
        override fun toString(): String = "($first, $second. $third)"

        operator fun component1(): R = first

        operator fun component2(): S = second

        operator fun component3(): T = third

        override fun equals(other: Any?): Boolean {
            return other is Element<R, S, T> && this.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            if (other !is Element<R, S, T>) return Order.UNEQUAL

            val result1 = this.first.compare(other.first)
            val result2 = this.second.compare(other.second)
            val result3 = this.third.compare(other.third)
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

        override fun duplicate(): Element<R, S, T> {
            return Element(first.duplicate() as R, second.duplicate() as S, third.duplicate() as T)
        }

        override fun hashCode(): Int {
            var result = first.hashCode()
            result = 31 * result + second.hashCode()
            return result
        }
    }

    override val bottom: Element<R, S, T>
        get() = Element(innerLattice1.bottom, innerLattice2.bottom, innerLattice3.bottom)

    override fun lub(one: Element<R, S, T>, two: Element<R, S, T>): Element<R, S, T> {
        val result1 = innerLattice1.lub(one.first, two.first)
        val result2 = innerLattice2.lub(one.second, two.second)
        val result3 = innerLattice3.lub(one.third, two.third)
        return Element(result1, result2, result3)
    }

    override fun glb(one: Element<R, S, T>, two: Element<R, S, T>): Element<R, S, T> {
        val result1 = innerLattice1.glb(one.first, two.first)
        val result2 = innerLattice2.glb(one.second, two.second)
        val result3 = innerLattice3.glb(one.third, two.third)
        return Element(result1, result2, result3)
    }

    override fun compare(one: Element<R, S, T>, two: Element<R, S, T>): Order {
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

    override fun duplicate(one: Element<R, S, T>): Element<R, S, T> {
        return Element(
            innerLattice1.duplicate(one.first),
            innerLattice2.duplicate(one.second),
            innerLattice3.duplicate(one.third),
        )
    }
}
