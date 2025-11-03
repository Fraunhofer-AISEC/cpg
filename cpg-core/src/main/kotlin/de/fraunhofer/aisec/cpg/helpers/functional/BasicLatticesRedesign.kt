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
import de.fraunhofer.aisec.cpg.helpers.ConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.toConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.PointsToState
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.ceil
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlinx.coroutines.*

var compareTime: Long = 0
var mapLatticeLubTime: Long = 0
var tupleLatticeLubTime: Long = 0
var maxLubSize = 0
var lubCounter = 0
var lubSizeCounter = 0

val CPU_CORES = Runtime.getRuntime().availableProcessors()
val MIN_CHUNK_SIZE = 10

/** Thread-safe map whose keys are compared by reference (===), not by equals(). */
open class ConcurrentIdentityMap<K, V>(expectedMaxSize: Int) : Map<K, V> {

    private val backing = ConcurrentHashMap<PointsToPass.IdKey<K>, V>(expectedMaxSize)

    override operator fun get(key: K): V? = backing[PointsToPass.IdKey(key)]

    open fun put(key: K, value: V): V? = backing.put(PointsToPass.IdKey(key), value)

    fun remove(key: K): V? = backing.remove(PointsToPass.IdKey(key))

    override fun containsKey(key: K): Boolean = backing.containsKey(PointsToPass.IdKey(key))

    override fun containsValue(value: V): Boolean = backing.containsValue(value)

    override val size: Int
        get() = backing.size

    override val keys: Set<K>
        get() = backing.keys.mapTo(IdentitySet(backing.size)) { it.ref }

    override val values: Collection<V>
        get() = backing.values

    override val entries: Set<Map.Entry<K, V>>
        get() =
            backing.entries.mapTo(IdentitySet(backing.size)) { (idKey, v) ->
                object : Map.Entry<K, V> {
                    override val key: K
                        get() = idKey.ref

                    override val value: V
                        get() = v
                }
            }

    override fun isEmpty(): Boolean {
        return backing.isEmpty()
    }

    fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V =
        backing.computeIfAbsent(PointsToPass.IdKey(key)) { mappingFunction(it.ref) }

    fun putAll(map: Map<out K, V>) {
        val wrapped = HashMap<PointsToPass.IdKey<K>, V>(map.size)
        for ((k, v) in map) {
            wrapped[PointsToPass.IdKey(k)] = v
        }
        backing.putAll(wrapped)
    }

    /** Inserts all entries from the given array of pairs. */
    fun putAll(pairs: Array<out Pair<K, V>>) = putAll(pairs.asIterable())

    /** Inserts all entries from the given [Iterable] of pairs. */
    fun putAll(pairs: Iterable<Pair<K, V>>) {
        val wrapped = HashMap<PointsToPass.IdKey<K>, V>()
        for ((k, v) in pairs) {
            wrapped[PointsToPass.IdKey(k)] = v
        }
        backing.putAll(wrapped)
    }

    /** Inserts all entries from the given [Sequence] of pairs. */
    fun putAll(pairs: Sequence<Pair<K, V>>) = putAll(pairs.asIterable())

    fun clear() = backing.clear()

    override fun hashCode() = backing.hashCode()
}

class EqualLinkedHashSet<T> : LinkedHashSet<T>() {
    override fun equals(other: Any?): Boolean {
        return other is LinkedHashSet<*> &&
            this.size == other.size &&
            this.all { t -> other.any { it == t } }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

fun <T> equalLinkedHashSetOf(vararg elements: T): EqualLinkedHashSet<T> {
    val set = EqualLinkedHashSet<T>()
    set.addAll(elements)
    return set
}

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

/**
 * Extension that splits an IdentitySet<T> into at most [maxParts] subsets, each containing **at
 * least [minPartSize] elements**.
 *
 * Rules
 * 1. If the set is empty ➜ returns an empty list.
 * 2. If the set has < [minPartSize] elements ➜ returns a single subset with all elements.
 * 3. Otherwise the number of created subsets k is k = min(maxParts, size / [minPartSize]) (integer
 *    division, k ≥ 1) so every subset can have at least [minPartSize] elements.
 */
fun <T> Collection<T>.splitInto(
    maxParts: Int = CPU_CORES,
    minPartSize: Int = MIN_CHUNK_SIZE,
): List<List<T>> {
    require(maxParts > 0) { "maxParts must be positive" }

    if (isEmpty()) return emptyList()
    if (size < minPartSize) return listOf(this.toList())

    // Determine number of chunks
    val k = minOf(maxParts, size / minPartSize) // k ≥ 1
    val base = size / k // minimum size for each chunk
    val extra = size % k

    // split the Collection into chunks
    val list = this.toList()
    var index = 0
    return List(k) { i ->
        val partSize = base + if (i < extra) 1 else 0
        list.subList(index, index + partSize).also { index += partSize }
    }
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

        /** Does the actual, concurrent work */
        // suspend fun innerCompare(other: Element): Order

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
    suspend fun lub(
        one: T,
        two: T,
        allowModify: Boolean = false,
        widen: Boolean = false,
        // On how many cores do we want to do the work?
        concurrencyCounter: Int = CPU_CORES,
    ): T

    /** Computes the greatest lower bound (meet) of [one] and [two] */
    suspend fun glb(one: T, two: T): T

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
    suspend fun iterateEOG(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: suspend (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy = Strategy.PRECISE,
    ): T {
        var finalStateCalcTime: Long = 0
        var pointsToStateLub: Long = 0
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
                    // merging the EOG again. This is to hopefully reduce the number of merges
                    // that
                    // we have to compute and that we hopefully reduce the number of
                    // re-processing
                    // the same basic blocks.
                    nextBranchEdgesList.removeFirst()
                } else {
                    // We have a merge point, we try to process this after having processed all
                    // branches leading there.
                    mergePointsEdgesList.removeFirst()
                }

            // Compute the effects of "nextEdge" on the state by applying the transformation to
            // its
            // state.
            val nextGlobal = globalState[nextEdge] ?: continue

            // Either immediately before or after this edge, there's a branching node. In these
            // cases, we definitely want to check if there's an update to the state.
            val isNoBranchingPoint =
                nextEdge.end.nextEOGEdges.size == 1 &&
                    nextEdge.end.prevEOGEdges.size == 1 &&
                    nextEdge.start.nextEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.size == 1
            // Either before or after this edge, there's a branching node within two steps
            // (start,
            // end and the nodes before/after these). We have to ensure that we copy the state
            // for
            // all these nodes to enable the update checks conducted ib the branching edges. We
            // need
            // one more step for this, otherwise we will fail recognizing the updates for a node
            // "x"
            // which is a branching edge because the next node would already modify the state of
            // x.
            val isNotNearStartOrEndOfBasicBlock =
                isNoBranchingPoint &&
                    nextEdge.end.nextEOGEdges.single().end.nextEOGEdges.size == 1 &&
                    nextEdge.end.nextEOGEdges.single().end.prevEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.single().start.nextEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.single().start.prevEOGEdges.size == 1

            val newState =
                transformation(
                    this@Lattice,
                    nextEdge,
                    if (isNotNearStartOrEndOfBasicBlock) nextGlobal else nextGlobal.duplicate() as T,
                )
            nextEdge.end.nextEOGEdges.forEach {
                // We continue with the nextEOG edge if we haven't seen it before or if we
                // updated
                // the state in comparison to the previous time we were there.

                val oldGlobalIt = globalState[it]

                // If we're on the loop head (some node is LoopStatement), and we use WIDENING
                // or
                // WIDENING_NARROWING, we have to apply the widening/narrowing here (if
                // oldGlobalIt
                // is not null).
                val newGlobalIt =
                    if (
                        nextEdge.end is LoopStatement &&
                            (strategy == Strategy.WIDENING ||
                                strategy == Strategy.WIDENING_NARROWING) &&
                            oldGlobalIt != null
                    ) {
                        this@Lattice.lub(
                            one = newState,
                            two = oldGlobalIt,
                            allowModify = isNotNearStartOrEndOfBasicBlock,
                            widen = true,
                        )
                    } else if (strategy == Strategy.NARROWING) {
                        TODO()
                    } else {
                        val (result, time) =
                            measureTimedValue {
                                (oldGlobalIt?.let {
                                    this@Lattice.lub(
                                        one = newState,
                                        two = it,
                                        allowModify = isNotNearStartOrEndOfBasicBlock,
                                    )
                                } ?: newState)
                            }
                        pointsToStateLub += time.toLong(DurationUnit.MILLISECONDS)
                        result
                    }

                globalState[it] = newGlobalIt

                if (
                    it !in currentBBEdgesList &&
                        it !in nextBranchEdgesList &&
                        it !in mergePointsEdgesList &&
                        (isNoBranchingPoint ||
                            oldGlobalIt == null ||
                            // If we deal with PointsToState Elements, we use their special
                            // parallelCompare function, otherwise, we resort to the traditional
                            // compare
                            ((newGlobalIt as? PointsToState.Element)?.parallelCompare(oldGlobalIt)
                                ?: (newGlobalIt as? MapLattice.Element<*, *>)?.parallelCompare(
                                    oldGlobalIt
                                )
                                ?: newGlobalIt.compare(oldGlobalIt)) in
                                setOf(Order.GREATER, Order.UNEQUAL))
                ) {
                    if (it.start.prevEOGEdges.size > 1) {
                        // This edge brings us to a merge point, so we add it to the list of
                        // merge
                        // points.
                        mergePointsEdgesList.add(0, it)
                    } else if (nextEdge.end.nextEOGEdges.size > 1) {
                        // If we have multiple next edges, we add this edge to the list of edges
                        // of
                        // a next basic block.
                        // We will process these after the current basic block has been
                        // processed
                        // (probably very soon).
                        nextBranchEdgesList.add(0, it)
                    } else {
                        // If we have only one next edge, we add it to the current basic block
                        // edges
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
                finalStateCalcTime += measureTimeMillis {
                    finalState = this@Lattice.lub(finalState, newState, false)
                }
            }
        }

        println(
            "+++ final state lub calculations took $finalStateCalcTime, PointsToState lub $pointsToStateLub, compareTime: ${compareTime/1000000}, tupleLattice lub time: ${tupleLatticeLubTime/1000000}, mapLattice lub time: ${mapLatticeLubTime/1000000}, maxLubsize: $maxLubSize, lubCounter: $lubCounter, lubSizeCounter: $lubSizeCounter"
        )
        compareTime = 0
        mapLatticeLubTime = 0
        tupleLatticeLubTime = 0
        maxLubSize = 0
        lubCounter = 0
        lubSizeCounter = 0

        return finalState
    }
}

/** Implements a [Lattice] whose elements are the powerset of a given set of values. */
class PowersetLattice<T>() : Lattice<PowersetLattice.Element<T>> {
    override lateinit var elements: Set<Element<T>>

    class Element<T>(expectedMaxSize: Int) :
        ConcurrentIdentitySet<T>(expectedMaxSize), Lattice.Element {
        // We make the new element a big bigger than the current size to avoid resizing
        constructor(set: Set<T>) : this(ceil(set.size * 1.5).toInt()) {
            addAllWithoutCheck(set as? ConcurrentIdentitySet<T> ?: set.toConcurrentIdentitySet())
        }

        constructor() : this(16)

        // We make the new element a big bigger than the current size to avoid resizing
        constructor(vararg entries: T) : this(ceil(entries.size * 1.5).toInt()) {
            addAll(entries)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Element<*> || this.size != other.size) return false

            this@Element.forEach { t ->
                val isEqual =
                    if (t is Pair<*, *>)
                        other.any {
                            it is Pair<*, *> && it.first === t.first && it.second == t.second
                        }
                    else if (t is PointsToPass.NodeWithPropertiesKey) other.any { it == t }
                    else t in other

                if (!isEqual) {
                    return false
                }
            }
            return true
        }

        suspend fun parallelEquals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Element<*> || this.size != other.size) return false

            var ret = true
            coroutineScope {
                try {
                    this@Element.splitInto(CPU_CORES).forEach { chunk ->
                        launch(Dispatchers.Default) {
                            for (t in chunk) {
                                ensureActive()
                                val isEqual =
                                    if (t is Pair<*, *>)
                                        other.any {
                                            it is Pair<*, *> &&
                                                it.first === t.first &&
                                                it.second == t.second
                                        }
                                    else if (t is PointsToPass.NodeWithPropertiesKey)
                                        other.any {
                                            it is PointsToPass.NodeWithPropertiesKey && it == t
                                        }
                                    else t in other

                                if (!isEqual) {
                                    ret = false
                                    // cancel all coroutines
                                    cancel()
                                }
                            }
                        }
                    }
                } catch (_: CancellationException) {
                    ret = false
                }
            }
            return ret
        }

        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<T>)
                throw IllegalArgumentException(
                    "$other should be of type PowersetLattice.Element<T> but is of type ${other.javaClass}"
                )
            val otherOnly = Element(other)
            val (thisOnly, duration) =
                measureTimedValue {
                    this.filterTo(IdentitySet<T>()) { t ->
                        !when (t) {
                            is Pair<*, *> -> {
                                otherOnly.removeIf { o ->
                                    o is Pair<*, *> && o.first === t.first && o.second == t.second
                                }
                            }

                            is PointsToPass.NodeWithPropertiesKey -> {
                                otherOnly.removeIf { o ->
                                    o is PointsToPass.NodeWithPropertiesKey && o == t
                                }
                            }

                            else -> otherOnly.remove(t)
                        }
                    }
                }
            compareTime += duration.toLong(DurationUnit.NANOSECONDS)
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

        override fun add(element: T): Boolean {
            if (
                element is Pair<*, *> &&
                    this.any {
                        it is Pair<*, *> &&
                            it.first === element.first &&
                            it.second == element.second
                    }
            ) {
                return false
            } else if (
                element is PointsToPass.NodeWithPropertiesKey &&
                    this.any { it is PointsToPass.NodeWithPropertiesKey && it == element }
            )
                return false
            return super.add(element)
        }
    }

    override val bottom: Element<T>
        get() = Element()

    override suspend fun lub(
        one: Element<T>,
        two: Element<T>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
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

    override suspend fun glb(one: Element<T>, two: Element<T>): Element<T> {
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
open class ConcurrentMapLattice<K, V : Lattice.Element>(val innerLattice: Lattice<V>) :
    Lattice<ConcurrentMapLattice.Element<K, V>> {
    override lateinit var elements: Set<Element<K, V>>

    /**
     * Splits a MapLattice.Element<K,V> into at most [maxParts] smaller MapLattice.Element<K,V>
     * objects, each containing **at least [minPartSize] entries**.
     *
     * Rules
     * 1. Empty map ➜ empty list.
     * 2. Less than [minPartSize] entries ➜ single element containing all entries.
     * 3. Otherwise k = min(maxParts, size / minPartSize) subsets are created so that every subset
     *    has ≥ [minPartSize] entries and their union equals the original map.
     */
    fun <K, V : Lattice.Element> Element<K, V>.splitInto(
        maxParts: Int = CPU_CORES,
        minPartSize: Int = MIN_CHUNK_SIZE,
    ): List<Element<K, V>> {
        require(maxParts > 0) { "maxParts must be positive" }

        if (isEmpty()) return emptyList()
        if (size < minPartSize) return listOf(this)

        // -- determine the real number of subsets we can build --
        val k = minOf(maxParts, size / minPartSize) // k ≥ 1
        val base = size / k // minimal size of every subset (≥ minPartSize)
        val extra = size % k // first 'extra' subsets get +1 entry

        // -- create the subsets --
        val entriesList = entries.toList()
        var index = 0
        return List(k) { i ->
            val partSize = base + if (i < extra) 1 else 0
            Element<K, V>(partSize).apply {
                repeat(partSize) {
                    val (key, value) = entriesList[index++]
                    //                    this[key] = value
                    put(key, value)
                }
            }
        }
    }

    open class Element<K, V : Lattice.Element>(expectedMaxSize: Int) :
        ConcurrentIdentityMap<K, V>(expectedMaxSize), Lattice.Element {

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
            return other is Element<K, V> && this@Element.compare(other) == Order.EQUAL
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

        @OptIn(ExperimentalAtomicApi::class)
        suspend fun parallelCompare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<K, V>)
                throw IllegalArgumentException(
                    "$other should be of type MapLattice.Element<K, V> but is of type ${other.javaClass}"
                )

            val otherKeySetIsBigger = other.keys.any { it !in this.keys }

            // We can check if the entries are equal, greater or lesser
            val someGreater = AtomicBoolean(false)
            val someLesser = AtomicBoolean(otherKeySetIsBigger)

            val ret = AtomicReference<Order?>(null)

            coroutineScope {
                this@Element.entries.splitInto().forEach { chunk ->
                    // We can't return in the coroutines, so we only set the return value
                    // there. If we have a return value, we can stop here
                    launch(Dispatchers.Default) {
                        for ((k, v) in chunk) {
                            if (ret.load() != null) return@launch
                            val otherV = other[k]
                            if (otherV != null) {
                                // Do not use parallelCompare since that would be too many
                                // coroutines
                                when (v.compare(otherV)) {
                                    Order.EQUAL -> {
                                        /* Nothing to do*/
                                    }

                                    Order.GREATER -> {
                                        if (someLesser.load()) {
                                            ret.store(Order.UNEQUAL)
                                            cancel()
                                        }
                                        someGreater.store(true)
                                    }

                                    Order.LESSER -> {
                                        if (someGreater.load()) {
                                            ret.store(Order.UNEQUAL)
                                            cancel()
                                        }
                                        someLesser.store(true)
                                    }

                                    Order.UNEQUAL -> {
                                        ret.store(Order.UNEQUAL)
                                        someLesser.store(true)
                                        someGreater.store(true)
                                        cancel()
                                    }
                                }
                            } else {
                                // key is missing in other, so this is greater
                                someGreater.store(true)
                                if (someLesser.load()) {
                                    ret.store(Order.UNEQUAL)
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }

            return if (!someGreater.load() && !someLesser.load()) {
                // All entries are the same, so the maps are equal
                Order.EQUAL
            } else if (someLesser.load() && !someGreater.load()) {
                // Some entries are equal, some are lesser and none are greater, so this map is
                // lesser.
                Order.LESSER
            } else if (!someLesser.load() && someGreater.load()) {
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
        get() = ConcurrentMapLattice.Element()

    override suspend fun lub(
        one: Element<K, V>,
        two: Element<K, V>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element<K, V> = coroutineScope {
        var result: Element<K, V>
        if (concurrencyCounter == CPU_CORES && !allowModify) {
            lubCounter++
            if (one.size > maxLubSize) maxLubSize = one.size
            if (two.size > maxLubSize) maxLubSize = two.size
            lubSizeCounter += one.size
            lubSizeCounter += two.size
        }
        var tmpTime = measureNanoTime {
            if (allowModify) {
                two.splitInto(concurrencyCounter)
                    .map { chunk ->
                        launch(Dispatchers.Default) {
                            for ((k, v) in chunk) {
                                val entry = one[k]
                                if (entry == null) {
                                    // This key is not in "one", so we add the value from "two"
                                    // to "one"
                                    one.put(k, v)
                                } else if (
                                    two[k] != null && entry.compare(two[k]!!) != Order.EQUAL
                                ) {
                                    // This key already exists in "one" and the values in one and
                                    // two are different,
                                    // so we have to compute the lub of the values
                                    one[k]?.let { oneValue ->
                                        innerLattice.lub(
                                            oneValue,
                                            v,
                                            allowModify = true,
                                            widen = widen,
                                            // We already run on $CPU_CORES coroutines, so we don't
                                            // need any additional ones
                                            1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .joinAll()
                result = one
            } else {
                val allKeys =
                    IdentitySet<K>(one.keys.size + two.keys.size).apply {
                        addAll(one.keys)
                        addAll(two.keys)
                    }
                result = Element()
                allKeys.splitInto(concurrencyCounter).map { chunk ->
                    launch(Dispatchers.Default) {
                        for (key in chunk) {
                            val otherValue = two[key]
                            val thisValue = one[key]
                            val newValue =
                                if (thisValue != null && otherValue != null) {
                                    innerLattice.lub(
                                        one = thisValue,
                                        two = otherValue,
                                        allowModify = false,
                                        widen = widen,
                                        // We already run on $CPU_CORES coroutines, so we don't
                                        // need any additional ones
                                        1,
                                    )
                                } else thisValue ?: otherValue
                            newValue?.let { result.put(key, it) }
                        }
                    }
                }
            }
        }
        if (concurrencyCounter == CPU_CORES && !allowModify) {
            mapLatticeLubTime += tmpTime
            //            println("---- $tmpTime")
        }
        return@coroutineScope result
    }

    override suspend fun glb(one: Element<K, V>, two: Element<K, V>): Element<K, V> =
        coroutineScope {
            val allKeys = one.keys.intersect(two.keys).toIdentitySet()

            val newMap = Element<K, V>(allKeys.size)

            allKeys.splitInto().forEach { chunk ->
                launch(Dispatchers.Default) {
                    for (key in chunk) {
                        val otherValue = two[key]
                        val thisValue = one[key]
                        val newValue =
                            if (thisValue != null && otherValue != null) {
                                innerLattice.glb(thisValue, otherValue)
                            } else innerLattice.bottom
                        newMap.put(key, newValue)
                    }
                }
            }

            return@coroutineScope newMap
        }

    override fun compare(one: Element<K, V>, two: Element<K, V>): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element<K, V>): Element<K, V> {
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

    /**
     * Splits a MapLattice.Element<K,V> into at most [maxParts] smaller MapLattice.Element<K,V>
     * objects, each containing **at least [minPartSize] entries**.
     *
     * Rules
     * 1. Empty map ➜ empty list.
     * 2. Less than [minPartSize] entries ➜ single element containing all entries.
     * 3. Otherwise k = min(maxParts, size / minPartSize) subsets are created so that every subset
     *    has ≥ [minPartSize] entries and their union equals the original map.
     */
    fun <K, V : Lattice.Element> Element<K, V>.splitInto(
        maxParts: Int = CPU_CORES,
        minPartSize: Int = MIN_CHUNK_SIZE,
    ): List<Element<K, V>> {
        require(maxParts > 0) { "maxParts must be positive" }

        if (isEmpty()) return emptyList()
        if (size < minPartSize) return listOf(this)

        // -- determine the real number of subsets we can build --
        val k = minOf(maxParts, size / minPartSize) // k ≥ 1
        val base = size / k // minimal size of every subset (≥ minPartSize)
        val extra = size % k // first 'extra' subsets get +1 entry

        // -- create the subsets --
        val entriesList = entries.toList()
        var index = 0
        return List(k) { i ->
            val partSize = base + if (i < extra) 1 else 0
            Element<K, V>(partSize).apply {
                repeat(partSize) {
                    val (key, value) = entriesList[index++]
                    //                    this[key] = value
                    put(key, value)
                }
            }
        }
    }

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
            return other is Element<K, V> && this@Element.compare(other) == Order.EQUAL
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

        @OptIn(ExperimentalAtomicApi::class)
        suspend fun parallelCompare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<K, V>)
                throw IllegalArgumentException(
                    "$other should be of type MapLattice.Element<K, V> but is of type ${other.javaClass}"
                )

            val otherKeySetIsBigger = other.keys.any { it !in this.keys }

            // We can check if the entries are equal, greater or lesser
            val someGreater = AtomicBoolean(false)
            val someLesser = AtomicBoolean(otherKeySetIsBigger)

            val ret = AtomicReference<Order?>(null)

            coroutineScope {
                this@Element.entries.splitInto().forEach { chunk ->
                    // We can't return in the coroutines, so we only set the return value
                    // there. If we have a return value, we can stop here
                    launch(Dispatchers.Default) {
                        for ((k, v) in chunk) {
                            if (ret.load() != null) return@launch
                            val otherV = other[k]
                            if (otherV != null) {
                                // Do not use parallelCompare since that would be too many
                                // coroutines
                                when (v.compare(otherV)) {
                                    Order.EQUAL -> {
                                        /* Nothing to do*/
                                    }

                                    Order.GREATER -> {
                                        if (someLesser.load()) {
                                            ret.store(Order.UNEQUAL)
                                            cancel()
                                        }
                                        someGreater.store(true)
                                    }

                                    Order.LESSER -> {
                                        if (someGreater.load()) {
                                            ret.store(Order.UNEQUAL)
                                            cancel()
                                        }
                                        someLesser.store(true)
                                    }

                                    Order.UNEQUAL -> {
                                        ret.store(Order.UNEQUAL)
                                        someLesser.store(true)
                                        someGreater.store(true)
                                        cancel()
                                    }
                                }
                            } else {
                                // key is missing in other, so this is greater
                                someGreater.store(true)
                                if (someLesser.load()) {
                                    ret.store(Order.UNEQUAL)
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }

            return if (!someGreater.load() && !someLesser.load()) {
                // All entries are the same, so the maps are equal
                Order.EQUAL
            } else if (someLesser.load() && !someGreater.load()) {
                // Some entries are equal, some are lesser and none are greater, so this map is
                // lesser.
                Order.LESSER
            } else if (!someLesser.load() && someGreater.load()) {
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

    override suspend fun lub(
        one: Element<K, V>,
        two: Element<K, V>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element<K, V> = coroutineScope {
        var result: Element<K, V>
        if (concurrencyCounter == CPU_CORES && !allowModify) {
            lubCounter++
            if (one.size > maxLubSize) maxLubSize = one.size
            if (two.size > maxLubSize) maxLubSize = two.size
            lubSizeCounter += one.size
            lubSizeCounter += two.size
        }
        var tmpTime = measureNanoTime {
            if (allowModify) {
                two.splitInto(concurrencyCounter)
                    .map { chunk ->
                        launch(Dispatchers.Default) {
                            for ((k, v) in chunk) {
                                val entry = one[k]
                                if (entry == null) {
                                    // This key is not in "one", so we add the value from "two"
                                    // to "one"
                                    one.put(k, v)
                                } else if (
                                    two[k] != null && entry.compare(two[k]!!) != Order.EQUAL
                                ) {
                                    // This key already exists in "one" and the values in one and
                                    // two are different,
                                    // so we have to compute the lub of the values
                                    one[k]?.let { oneValue ->
                                        innerLattice.lub(
                                            oneValue,
                                            v,
                                            allowModify = true,
                                            widen = widen,
                                            // We already run on $CPU_CORES coroutines, so we don't
                                            // need any additional ones
                                            1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .joinAll()
                result = one
            } else {
                val allKeys =
                    IdentitySet<K>(one.keys.size + two.keys.size).apply {
                        addAll(one.keys)
                        addAll(two.keys)
                    }
                val newMap = ConcurrentIdentityMap<K, V>(allKeys.size)
                allKeys
                    .splitInto(concurrencyCounter)
                    .map { chunk ->
                        launch(Dispatchers.Default) {
                            for (key in chunk) {
                                val otherValue = two[key]
                                val thisValue = one[key]
                                val newValue =
                                    if (thisValue != null && otherValue != null) {
                                        innerLattice.lub(
                                            one = thisValue,
                                            two = otherValue,
                                            allowModify = false,
                                            widen = widen,
                                            // We already run on $CPU_CORES coroutines, so we
                                            // don't
                                            // need any additional ones
                                            1,
                                        )
                                    } else thisValue ?: otherValue
                                newValue?.let { newMap.put(key, it) }
                            }
                        }
                    }
                    .joinAll()
                result = Element(newMap)
            }
        }
        if (concurrencyCounter == CPU_CORES && !allowModify) {
            mapLatticeLubTime += tmpTime
            //            println("---- $tmpTime")
        }
        return@coroutineScope result
    }

    override suspend fun glb(one: Element<K, V>, two: Element<K, V>): Element<K, V> {
        val allKeys = one.keys.intersect(two.keys).toIdentitySet()

        val newMap = Element<K, V>(allKeys.size)
        coroutineScope {
            val concurrentProcesses =
                allKeys.map { key ->
                    async {
                        val otherValue = two[key]
                        val thisValue = one[key]
                        val newValue =
                            if (thisValue != null && otherValue != null) {
                                innerLattice.glb(thisValue, otherValue)
                            } else innerLattice.bottom
                        key to newValue
                    }
                }
            concurrentProcesses.awaitAll().forEach { (key, value) ->
                value.let {
                    //                    newMap[key] = it
                    newMap.put(key, it)
                }
            }
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
            return other is Element<S, T> && this@Element.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order /*= coroutineScope*/ {
            if (this === other) return /*@coroutineScope*/ Order.EQUAL

            if (other !is Element<S, T>)
                throw IllegalArgumentException(
                    "$other should be of type TupleLattice.Element<S, T> but is of type ${other.javaClass}"
                )

            /*            val result1 = async { this@Element.first.compare(other.first) }
            val result2 = async { this@Element.second.compare(other.second) }
            return@coroutineScope compareMultiple(result1.await(), result2.await())*/
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

    override suspend fun lub(
        one: Element<S, T>,
        two: Element<S, T>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element<S, T> {
        val result: Element<S, T>
        tupleLatticeLubTime += measureNanoTime {
            result =
                if (allowModify) {
                    innerLattice1.lub(
                        one = one.first,
                        two = two.first,
                        allowModify = true,
                        widen = widen,
                    )
                    val second =
                        innerLattice2.lub(
                            one = one.second,
                            two = two.second,
                            allowModify = true,
                            widen = widen,
                        )

                    one
                } else {
                    val first =
                        innerLattice1.lub(
                            one = one.first,
                            two = two.first,
                            allowModify = false,
                            widen = widen,
                        )
                    val second =
                        innerLattice2.lub(
                            one = one.second,
                            two = two.second,
                            allowModify = false,
                            widen = widen,
                        )
                    Element(first, second)
                }
        }
        return result
    }

    override suspend fun glb(one: Element<S, T>, two: Element<S, T>): Element<S, T> {
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
open class TripleLattice<R : Lattice.Element, S : Lattice.Element, T : Lattice.Element>(
    val innerLattice1: Lattice<R>,
    val innerLattice2: Lattice<S>,
    val innerLattice3: Lattice<T>,
) : Lattice<TripleLattice.Element<R, S, T>> {
    override lateinit var elements: Set<Element<R, S, T>>

    open class Element<R : Lattice.Element, S : Lattice.Element, T : Lattice.Element>(
        val first: R,
        val second: S,
        val third: T,
    ) : Serializable, Lattice.Element {
        override fun toString(): String = "($first, $second. $third)"

        operator fun component1(): R = first

        operator fun component2(): S = second

        operator fun component3(): T = third

        override fun equals(other: Any?): Boolean {
            return other is Element<R, S, T> && this@Element.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order /*= coroutineScope*/ {
            if (this === other) return /*@coroutineScope*/ Order.EQUAL

            if (other !is Element<R, S, T>)
                throw IllegalArgumentException(
                    "$other should be of type TripleLattice.Element<R, S, T> but is of type ${other.javaClass}"
                )

            /*            val result1 = async { this@Element.first.compare(other.first) }
            val result2 = async { this@Element.second.compare(other.second) }
            val result3 = async { this@Element.third.compare(other.third) }
            return@coroutineScope compareMultiple(result1.await(), result2.await(), result3.await())*/
            val result1 = this@Element.first.compare(other.first)
            val result2 = this@Element.second.compare(other.second)
            val result3 = this@Element.third.compare(other.third)
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

    override suspend fun lub(
        one: Element<R, S, T>,
        two: Element<R, S, T>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element<R, S, T> = coroutineScope {
        return@coroutineScope if (allowModify) {
            innerLattice1.lub(one = one.first, two = two.first, allowModify = true, widen = widen)
            innerLattice2.lub(one = one.second, two = two.second, allowModify = true, widen = widen)
            innerLattice3.lub(one = one.third, two = two.third, allowModify = true, widen = widen)
            one
        } else {
            val first =
                innerLattice1.lub(
                    one = one.first,
                    two = two.first,
                    allowModify = false,
                    widen = widen,
                )
            val second =
                innerLattice2.lub(
                    one = one.second,
                    two = two.second,
                    allowModify = false,
                    widen = widen,
                )
            val third =
                innerLattice3.lub(
                    one = one.third,
                    two = two.third,
                    allowModify = false,
                    widen = widen,
                )
            Element(first, second, third)
        }
    }

    override suspend fun glb(one: Element<R, S, T>, two: Element<R, S, T>): Element<R, S, T> {
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
