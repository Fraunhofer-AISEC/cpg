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
import de.fraunhofer.aisec.cpg.graph.statements.LoopStatement
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import java.io.Serializable
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.fold
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.math.ceil

/** Used to identify the order of elements */
enum class Order {
    LESSER,
    EQUAL,
    GREATER,
    UNEQUAL,
}

/**
 * Computes the order of multiple elements passed in [orders] as follows:
 * - If everything is [Order.EQUAL], it's [Order.EQUAL]
 * - If everything is [Order.EQUAL] or [Order.LESSER], it's [Order.LESSER]
 * - If everything is [Order.EQUAL] or [Order.GREATER], it's [Order.GREATER]
 * - Otherwise, it's [Order.UNEQUAL]
 */
fun compareMultiple(vararg orders: Order) =
    when {
        orders.all { it == Order.EQUAL } -> Order.EQUAL
        orders.all { it == Order.EQUAL || it == Order.LESSER } -> Order.LESSER
        orders.all { it == Order.EQUAL || it == Order.GREATER } -> Order.GREATER
        else -> Order.UNEQUAL
    }

interface HasWidening<T : Lattice.Element> {
    /**
     * Computes the widening of [one] and [two]. This is used to ensure that the fixpoint iteration
     * converges (faster).
     *
     * @param one The first element to widen
     * @param two The second element to widen
     * @return The widened element
     */
    fun widen(one: T, two: T): T
}

interface HasNarrowing<T : Lattice.Element> {
    /**
     * Computes the narrowing of [one] and [two]. This is used to ensure that the fixpoint iteration
     * converges (faster) without too much overapproximation.
     *
     * @param one The first element to narrow
     * @param two The second element to narrow
     * @return The narrowed element
     */
    fun narrow(one: T, two: T): T
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
 * Note: We usually do not want (nor have to) store the elements spanning the lattice because it
 * would cost too much memory for non-trivial examples. But if a user wants to do so, we provide the
 * property [elements]. It can be used to store all, no or some of the elements spanning the lattice
 * and currently has no real effect.
 */
interface Lattice<T : Lattice.Element> {
    enum class Strategy {
        PRECISE,
        WIDENING,
        WIDENING_NARROWING,
        NARROWING,
    }

    /**
     * Represents a single element of the [Lattice]. It also provides the functionality to compare
     * and duplicate the element.
     */
    interface Element {
        /**
         * Compares this element to [other].
         *
         * @throws IllegalArgumentException if [other] is not an instance of this implementation of
         *   Element
         */
        fun compare(other: Element): Order

        /** Duplicates this element, i.e., it creates a new object with equal contents. */
        fun duplicate(): Element
    }

    /** Allows storing all elements which are part of this lattice */
    var elements: Set<T>

    /** The smallest possible element in the lattice */
    val bottom: T

    /**
     * Computes the least upper bound (join) of [one] and [two]. [allowModify] determines if [one]
     * is modified if there is no element greater than each other (if set to `true`) or if a new
     * [Lattice.Element] is returned (if set to `false`).
     */
    fun lub(one: T, two: T, allowModify: Boolean = false, widen: Boolean = false): T

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
     * [Lattice] which represents possible values (or abstractions thereof) that they hold.
     */
    fun iterateEOG(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy = Strategy.PRECISE,
    ): T {
        val globalState = IdentityHashMap<EvaluationOrder, T>()
        var finalState: T = this.bottom
        for (startEdge in startEdges) {
            globalState[startEdge] = startState
        }
        // This list contains the edge(s) (probably only one unless we made a mistake) of the
        // current basic block that we are currently processing. We select this one with priority
        // over the other options.
        val currentBBEdgesList = mutableListOf<EvaluationOrder>()
        // This list contains the edge(s) that are the next branch(es) to process. We process these
        // after the current basic block has been processed.
        val nextBranchEdgesList = mutableListOf<EvaluationOrder>()
        // This list contains the merge points that we have to process. We process these after the
        // current basic block and the next branches have been processed to reduce the amount of
        // merges.
        val mergePointsEdgesList = mutableListOf<EvaluationOrder>()
        startEdges.forEach { nextBranchEdgesList.add(it) }

        while (
            currentBBEdgesList.isNotEmpty() ||
                nextBranchEdgesList.isNotEmpty() ||
                mergePointsEdgesList.isNotEmpty()
        ) {
            val nextEdge =
                if (currentBBEdgesList.isNotEmpty()) {
                    // If we have edges in the current basic block, we take these. We prefer to
                    // finish with the whole Basic Block before moving somewhere else.
                    currentBBEdgesList.removeFirst()
                } else if (nextBranchEdgesList.isNotEmpty()) {
                    // If we have points splitting up the EOG, we prefer to process these before
                    // merging the EOG again. This is to hopefully reduce the number of merges that
                    // we have to compute and that we hopefully reduce the number of re-processing
                    // the same basic blocks.
                    nextBranchEdgesList.removeFirst()
                } else {
                    // We have a merge point, we try to process this after having processed all
                    // branches leading there.
                    mergePointsEdgesList.removeFirst()
                }

            // Compute the effects of "nextEdge" on the state by applying the transformation to its
            // state.
            val nextGlobal = globalState[nextEdge] ?: continue

            // Either immediately before or after this edge, there's a branching node. In these
            // cases, we definitely want to check if there's an update to the state.
            val isNoBranchingPoint =
                nextEdge.end.nextEOGEdges.size == 1 &&
                    nextEdge.end.prevEOGEdges.size == 1 &&
                    nextEdge.start.nextEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.size == 1
            //  Either before or after this edge, there's a branching node within two steps (start,
            // end and the nodes before/after these). We have to ensure that we copy the state for
            // all these nodes to enable the update checks conducted ib the branching edges. We need
            // one more step for this, otherwise we will fail recognizing the updates for a node "x"
            // which is a branching edge because the next node would already modify the state of x.
            val isNotNearStartOrEndOfBasicBlock =
                isNoBranchingPoint &&
                    nextEdge.end.nextEOGEdges.single().end.nextEOGEdges.size == 1 &&
                    nextEdge.end.nextEOGEdges.single().end.prevEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.single().start.nextEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.single().start.prevEOGEdges.size == 1

            val newState =
                transformation(
                    this,
                    nextEdge,
                    if (isNotNearStartOrEndOfBasicBlock) nextGlobal else nextGlobal.duplicate() as T,
                )
            nextEdge.end.nextEOGEdges.forEach {
                // We continue with the nextEOG edge if we haven't seen it before or if we updated
                // the state in comparison to the previous time we were there.

                val oldGlobalIt = globalState[it]

                // If we're on the loop head (some node is LoopStatement), and we use WIDENING or
                // WIDENING_NARROWING, we have to apply the widening/narrowing here (if oldGlobalIt
                // is not null).
                val newGlobalIt =
                    if (
                        nextEdge.end is LoopStatement &&
                            (strategy == Strategy.WIDENING ||
                                strategy == Strategy.WIDENING_NARROWING) &&
                            oldGlobalIt != null
                    ) {
                        this.lub(
                            one = newState,
                            two = oldGlobalIt,
                            allowModify = isNotNearStartOrEndOfBasicBlock,
                            widen = true,
                        )
                    } else if (strategy == Strategy.NARROWING) {
                        TODO()
                    } else {
                        (oldGlobalIt?.let {
                            this.lub(
                                one = newState,
                                two = it,
                                allowModify = isNotNearStartOrEndOfBasicBlock,
                            )
                        } ?: newState)
                    }

                globalState[it] = newGlobalIt
                if (
                    it !in currentBBEdgesList &&
                        it !in nextBranchEdgesList &&
                        it !in mergePointsEdgesList &&
                        (isNoBranchingPoint ||
                            oldGlobalIt == null ||
                            newGlobalIt.compare(oldGlobalIt) == Order.GREATER ||
                            newGlobalIt.compare(oldGlobalIt) == Order.UNEQUAL)
                ) {
                    if (it.start.prevEOGEdges.size > 1) {
                        // This edge brings us to a merge point, so we add it to the list of merge
                        // points.
                        mergePointsEdgesList.add(0, it)
                    } else if (nextEdge.end.nextEOGEdges.size > 1) {
                        // If we have multiple next edges, we add this edge to the list of edges of
                        // a next basic block.
                        // We will process these after the current basic block has been processed
                        // (probably very soon).
                        nextBranchEdgesList.add(0, it)
                    } else {
                        // If we have only one next edge, we add it to the current basic block edges
                        // list.
                        currentBBEdgesList.add(0, it)
                    }
                }
            }

            if (
                nextEdge.end.nextEOGEdges.isEmpty() ||
                    (currentBBEdgesList.isEmpty() &&
                        nextBranchEdgesList.isEmpty() &&
                        mergePointsEdgesList.isEmpty())
            ) {
                finalState = this.lub(finalState, newState, false)
            }
        }

        return finalState
    }
}

/** Implements a [Lattice] whose elements are the powerset of a given set of values. */
class PowersetLattice<T>() : Lattice<PowersetLattice.Element<T>> {
    override lateinit var elements: Set<Element<T>>

    class Element<T>(expectedMaxSize: Int) : IdentitySet<T>(expectedMaxSize), Lattice.Element {

        // We make the new element a big bigger than the current size to avoid resizing
        constructor(set: Set<T>) : this(ceil(set.size * 1.5).toInt()) {
            addAllWithoutCheck(set as? IdentitySet<T> ?: set.toIdentitySet())
        }

        constructor() : this(16)

        // We make the new element a big bigger than the current size to avoid resizing
        constructor(vararg entries: T) : this(ceil(entries.size * 1.5).toInt()) {
            addAll(entries)
        }

        override fun equals(other: Any?): Boolean {
            return other is Element<T> && super<IdentitySet>.equals(other)
        }

        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<T>)
                throw IllegalArgumentException(
                    "$other should be of type PowersetLattice.Element<T> but is of type ${other.javaClass}"
                )
            val otherOnly = Element(other)
            val thisOnly =
                this.filterTo(IdentitySet<T>()) { t ->
                    !if (t is Pair<*, *>) {
                        otherOnly.removeIf { o ->
                            o is Pair<*, *> && o.first === t.first && o.second == t.second
                        }
                    } else otherOnly.remove(t)
                }
            return when {
                otherOnly.isEmpty() && thisOnly.isEmpty() -> {
                    Order.EQUAL
                }
                thisOnly.isNotEmpty() && otherOnly.isNotEmpty() -> {
                    Order.UNEQUAL
                }
                thisOnly.isNotEmpty() -> {
                    // This set is greater than the other set
                    Order.GREATER
                }
                else -> {
                    // The other set is greater than this set
                    Order.LESSER
                }
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

    override fun lub(
        one: Element<T>,
        two: Element<T>,
        allowModify: Boolean,
        widen: Boolean,
    ): Element<T> {
        if (allowModify) {
            one += two
            return one
        }

        val result = Element<T>(one.size + two.size)
        result.addAllWithoutCheck(one)
        result += two
        return result
    }

    override fun glb(one: Element<T>, two: Element<T>): Element<T> {
        return Element(one.intersect(two))
    }

    override fun compare(one: Element<T>, two: Element<T>): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element<T>): Element<T> {
        return one.duplicate()
    }
}

/**
 * Implements the [Lattice] for a lattice over a map of nodes to another lattice represented by
 * [innerLattice].
 */
open class MapLattice<K, V : Lattice.Element>(val innerLattice: Lattice<V>) :
    Lattice<MapLattice.Element<K, V>> {
    override lateinit var elements: Set<Element<K, V>>

    open class Element<K, V : Lattice.Element>(expectedMaxSize: Int) :
        IdentityHashMap<K, V>(expectedMaxSize), Lattice.Element {

        constructor() : this(32)

        constructor(m: Map<K, V>) : this(m.size) {
            putAll(m)
        }

        constructor(entries: Collection<Pair<K, V>>) : this(entries.size) {
            putAll(entries)
        }

        constructor(vararg entries: Pair<K, V>) : this(entries.size) {
            putAll(entries)
        }

        override fun equals(other: Any?): Boolean {
            return other is Element<K, V> && this.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<K, V>)
                throw IllegalArgumentException(
                    "$other should be of type MapLattice.Element<K, V> but is of type ${other.javaClass}"
                )

            val otherKeySetIsBigger = other.keys.any { it !in this.keys }

            // We can check if the entries are equal, greater or lesser
            var someGreater = false
            var someLesser = otherKeySetIsBigger
            this.entries.forEach { (k, v) ->
                val otherV = other[k]
                if (otherV != null) {
                    when (v.compare(otherV)) {
                        Order.EQUAL -> {
                            /* Nothing to do*/
                        }
                        Order.GREATER -> {
                            if (someLesser) {
                                return Order.UNEQUAL
                            }
                            someGreater = true
                        }
                        Order.LESSER -> {
                            if (someGreater) {
                                return Order.UNEQUAL
                            }
                            someLesser = true
                        }
                        Order.UNEQUAL -> {
                            return Order.UNEQUAL
                        }
                    }
                } else {
                    if (someLesser) {
                        return Order.UNEQUAL
                    }
                    someGreater = true // key is missing in other, so this is greater
                }
            }
            return if (!someGreater && !someLesser) {
                // All entries are the same, so the maps are equal
                Order.EQUAL
            } else if (someLesser && !someGreater) {
                // Some entries are equal, some are lesser and none are greater, so this map is
                // lesser.
                Order.LESSER
            } else if (!someLesser && someGreater) {
                // Some entries are equal, some are greater but none are lesser, so this map is
                // greater.
                Order.GREATER
            } else {
                // Some entries are greater and some are lesser, so the maps are unequal
                Order.UNEQUAL
            }
        }

        override fun duplicate(): Element<K, V> {
            return Element(this.map { (k, v) -> Pair<K, V>(k, v.duplicate() as V) })
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    override val bottom: Element<K, V>
        get() = MapLattice.Element()

    override fun lub(
        one: Element<K, V>,
        two: Element<K, V>,
        allowModify: Boolean,
        widen: Boolean,
    ): Element<K, V> {
        if (allowModify) {
            two.forEach { (k, v) ->
                if (!one.containsKey(k)) {
                    // This key is not in "one", so we add the value from "two" to "one"
                    one[k] = v
                } else {
                    // This key already exists in "one", so we have to compute the lub of the values
                    one[k]?.let { oneValue ->
                        innerLattice.lub(one = oneValue, two = v, allowModify = true, widen = widen)
                    }
                }
            }
            return one
        }
        val allKeys = one.keys.toIdentitySet()
        allKeys += two.keys
        val newMap =
            allKeys.fold(Element<K, V>(allKeys.size)) { current, key ->
                val otherValue = two[key]
                val thisValue = one[key]
                val newValue =
                    if (thisValue != null && otherValue != null) {
                        innerLattice.lub(
                            one = thisValue,
                            two = otherValue,
                            allowModify = false,
                            widen = widen,
                        )
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
                    } else innerLattice.bottom
                current[key] = newValue
                current
            }
        return newMap
    }

    override fun compare(one: Element<K, V>, two: Element<K, V>): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element<K, V>): Element<K, V> {
        return one.duplicate()
    }
}

/**
 * Implements the [Lattice] for a lattice over two other lattices which are represented by
 * [innerLattice1] and [innerLattice2].
 */
open class TupleLattice<S : Lattice.Element, T : Lattice.Element>(
    val innerLattice1: Lattice<S>,
    val innerLattice2: Lattice<T>,
) : Lattice<TupleLattice.Element<S, T>> {
    override lateinit var elements: Set<Element<S, T>>

    open class Element<S : Lattice.Element, T : Lattice.Element>(val first: S, val second: T) :
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
            if (this === other) return Order.EQUAL

            if (other !is Element<S, T>)
                throw IllegalArgumentException(
                    "$other should be of type TupleLattice.Element<S, T> but is of type ${other.javaClass}"
                )

            val result1 = this.first.compare(other.first)
            val result2 = this.second.compare(other.second)
            return compareMultiple(result1, result2)
        }

        override fun duplicate(): Element<S, T> {
            return Element(first.duplicate() as S, second.duplicate() as T)
        }

        override fun hashCode(): Int {
            return 31 * first.hashCode() + second.hashCode()
        }
    }

    override val bottom: Element<S, T>
        get() = Element(innerLattice1.bottom, innerLattice2.bottom)

    override fun lub(
        one: Element<S, T>,
        two: Element<S, T>,
        allowModify: Boolean,
        widen: Boolean,
    ): Element<S, T> {
        return if (allowModify) {
            innerLattice1.lub(one = one.first, two = two.first, allowModify = true, widen = widen)
            innerLattice2.lub(one = one.second, two = two.second, allowModify = true, widen = widen)
            one
        } else {
            Element(
                innerLattice1.lub(
                    one = one.first,
                    two = two.first,
                    allowModify = false,
                    widen = widen,
                ),
                innerLattice2.lub(
                    one = one.second,
                    two = two.second,
                    allowModify = false,
                    widen = widen,
                ),
            )
        }
    }

    override fun glb(one: Element<S, T>, two: Element<S, T>): Element<S, T> {
        return Element(
            innerLattice1.glb(one.first, two.first),
            innerLattice2.glb(one.second, two.second),
        )
    }

    override fun compare(one: Element<S, T>, two: Element<S, T>): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element<S, T>): Element<S, T> {
        return one.duplicate()
    }
}

/**
 * Implements the [Lattice] for a lattice over three other lattices which are represented by
 * [innerLattice1], [innerLattice2] and [innerLattice3].
 */
class TripleLattice<R : Lattice.Element, S : Lattice.Element, T : Lattice.Element>(
    val innerLattice1: Lattice<R>,
    val innerLattice2: Lattice<S>,
    val innerLattice3: Lattice<T>,
) : Lattice<TripleLattice.Element<R, S, T>> {
    override lateinit var elements: Set<Element<R, S, T>>

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
            if (this === other) return Order.EQUAL

            if (other !is Element<R, S, T>)
                throw IllegalArgumentException(
                    "$other should be of type TripleLattice.Element<R, S, T> but is of type ${other.javaClass}"
                )

            val result1 = this.first.compare(other.first)
            val result2 = this.second.compare(other.second)
            val result3 = this.third.compare(other.third)
            return compareMultiple(result1, result2, result3)
        }

        override fun duplicate(): Element<R, S, T> {
            return Element(first.duplicate() as R, second.duplicate() as S, third.duplicate() as T)
        }

        override fun hashCode(): Int {
            return 31 * (31 * first.hashCode() + second.hashCode()) + third.hashCode()
        }
    }

    override val bottom: Element<R, S, T>
        get() = Element(innerLattice1.bottom, innerLattice2.bottom, innerLattice3.bottom)

    override fun lub(
        one: Element<R, S, T>,
        two: Element<R, S, T>,
        allowModify: Boolean,
        widen: Boolean,
    ): Element<R, S, T> {
        return if (allowModify) {
            innerLattice1.lub(one = one.first, two = two.first, allowModify = true, widen = widen)
            innerLattice2.lub(one = one.second, two = two.second, allowModify = true, widen = widen)
            innerLattice3.lub(one = one.third, two = two.third, allowModify = true, widen = widen)
            one
        } else {
            Element(
                innerLattice1.lub(
                    one = one.first,
                    two = two.first,
                    allowModify = false,
                    widen = widen,
                ),
                innerLattice2.lub(
                    one = one.second,
                    two = two.second,
                    allowModify = false,
                    widen = widen,
                ),
                innerLattice3.lub(
                    one = one.third,
                    two = two.third,
                    allowModify = false,
                    widen = widen,
                ),
            )
        }
    }

    override fun glb(one: Element<R, S, T>, two: Element<R, S, T>): Element<R, S, T> {
        return Element(
            innerLattice1.glb(one.first, two.first),
            innerLattice2.glb(one.second, two.second),
            innerLattice3.glb(one.third, two.third),
        )
    }

    override fun compare(one: Element<R, S, T>, two: Element<R, S, T>): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element<R, S, T>): Element<R, S, T> {
        return one.duplicate()
    }
}
