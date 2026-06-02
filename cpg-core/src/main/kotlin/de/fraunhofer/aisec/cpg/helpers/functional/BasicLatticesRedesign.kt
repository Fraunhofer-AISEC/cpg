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

import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.branchOf
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.Loop
import de.fraunhofer.aisec.cpg.graph.forEachMaybeParallel
import de.fraunhofer.aisec.cpg.helpers.ConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.toConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.PointsToState
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.ceil
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlinx.coroutines.*

val CPU_CORES = Runtime.getRuntime().availableProcessors()
val MIN_CHUNK_SIZE = 100

/** Thread-safe map whose keys are compared by reference (===), not by equals(). */
open class ConcurrentIdentityHashMap<K, V>(expectedMaxSize: Int = 32) : Map<K, V> {

    private val backing = ConcurrentHashMap<PointsToPass.IdKey<K>, V>(expectedMaxSize)

    override operator fun get(key: K): V? = backing[PointsToPass.IdKey(key)]

    open fun put(key: K, value: V): V? = backing.put(PointsToPass.IdKey(key), value)

    fun remove(key: K): V? = backing.remove(PointsToPass.IdKey(key))

    fun removeKeyIf(filter: Predicate<in K>): Boolean {
        var removed = false
        val each: MutableIterator<PointsToPass.IdKey<K>> = this.backing.keys.iterator()
        while (each.hasNext()) {
            if (filter.test(each.next().ref)) {
                each.remove()
                removed = true
            }
        }
        return removed
    }

    override fun containsKey(key: K): Boolean = backing.containsKey(PointsToPass.IdKey(key))

    override fun containsValue(value: V): Boolean = backing.containsValue(value)

    override val size: Int
        get() = backing.size

    private val keySetView =
        object : AbstractMutableSet<K>() {
            override val size: Int
                get() = backing.size

            override fun add(element: K): Boolean {
                throw UnsupportedOperationException("Cannot add a key without a value")
            }

            override fun clear() = backing.clear()

            override fun contains(element: K): Boolean =
                this@ConcurrentIdentityHashMap.containsKey(element)

            override fun containsAll(elements: Collection<K>): Boolean =
                elements.all { contains(it) }

            override fun hashCode(): Int = backing.keys.sumOf { it.hashCode() }

            override fun isEmpty(): Boolean = backing.isEmpty()

            override fun iterator(): MutableIterator<K> {
                val iterator = backing.keys.iterator()
                return object : MutableIterator<K> {
                    override fun hasNext(): Boolean = iterator.hasNext()

                    override fun next(): K = iterator.next().ref

                    override fun remove() = iterator.remove()
                }
            }

            override fun remove(element: K): Boolean =
                this@ConcurrentIdentityHashMap.remove(element) != null
        }

    override val keys: Set<K>
        get() = keySetView

    override val values: Collection<V>
        get() = backing.values

    private val entrySetView =
        object : AbstractMutableSet<Map.Entry<K, V>>() {
            override val size: Int
                get() = backing.size

            override fun add(element: Map.Entry<K, V>): Boolean {
                throw UnsupportedOperationException("Cannot add an entry through the entry view")
            }

            override fun clear() = backing.clear()

            override fun contains(element: Map.Entry<K, V>): Boolean {
                val key = PointsToPass.IdKey(element.key)
                return backing.containsKey(key) && backing[key] == element.value
            }

            override fun hashCode(): Int =
                backing.entries.sumOf { (idKey, value) ->
                    System.identityHashCode(idKey.ref) xor (value?.hashCode() ?: 0)
                }

            override fun isEmpty(): Boolean = backing.isEmpty()

            override fun iterator(): MutableIterator<Map.Entry<K, V>> {
                val iterator = backing.entries.iterator()
                return object : MutableIterator<Map.Entry<K, V>> {
                    override fun hasNext(): Boolean = iterator.hasNext()

                    override fun next(): Map.Entry<K, V> {
                        val entry = iterator.next()
                        return object : Map.Entry<K, V> {
                            override val key: K
                                get() = entry.key.ref

                            override val value: V
                                get() = entry.value

                            override fun equals(other: Any?): Boolean =
                                other is Map.Entry<*, *> &&
                                    other.key === key &&
                                    other.value == value

                            override fun hashCode(): Int =
                                System.identityHashCode(key) xor (value?.hashCode() ?: 0)

                            override fun toString(): String = "$key=$value"
                        }
                    }

                    override fun remove() = iterator.remove()
                }
            }

            override fun remove(element: Map.Entry<K, V>): Boolean =
                backing.remove(PointsToPass.IdKey(element.key), element.value)
        }

    override val entries: Set<Map.Entry<K, V>>
        get() = entrySetView

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConcurrentIdentityHashMap<*, *>

        if (backing != other.backing) return false
        if (size != other.size) return false
        if (keys != other.keys) return false
        if (values != other.values) return false
        if (entries != other.entries) return false

        return true
    }
}

class EqualLinkedHashSet<T> : LinkedHashSet<T>() {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
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

/** Used to track the timeout of all functions being currently analyzed * */
val timeouts = mutableListOf<Long>()

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
    var elements: ConcurrentIdentitySet<T>

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
     * [Lattice] which represents possible values (or abstractions thereof) that they hold. The
     * [timeout] can be used to limit the time spent in this function. If the timeout is reached and
     * the fixpoint is not reached yet, we return `null`. If [timeout] is `null`, we will not time
     * out.
     */
    fun iterateEOG(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: suspend (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy = Strategy.PRECISE,
        timeout: Long? = null,
    ): Pair<T, Boolean> {
        return runBlocking {
            /*            if (timeout != null) {
                withTimeoutOrNull(timeout) {
                    iterateEogInternal(startEdges, startState, transformation, strategy)
                }
            } else {*/
            iterateEogInternal(startEdges, startState, transformation, strategy, timeout)
            //            }
        }
    }

    suspend fun iterateEogInternal(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: suspend (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy,
        timeout: Long?,
    ): Pair<T, Boolean> {
        // mark the time when we started the calculation to know when we stop
        val startTime = TimeSource.Monotonic.markNow()
        if (timeout != null) {
            timeouts.addLast(timeout)
        }

        val globalState = IdentityHashMap<EvaluationOrder, T>()
        var finalState: T = this.bottom
        for (startEdge in startEdges) {
            globalState.put(startEdge, startState)
        }

        // This list contains the edge(s) (probably only one unless we made a mistake) of the
        // current basic block that we are currently processing. We select this one with priority
        // over the other options.
        val currentBBEdgesList = mutableListOf<EvaluationOrder>()
        // The second priority are edges that point to a node within the same loop
        // A high priority in the SCC-Label indicates a high priority in the queue
        val sccEdgesQueue =
            PriorityQueue<Pair<Int, EvaluationOrder>>(compareByDescending { it.first })
        // This list contains the edge(s) that are the next branch(es) to process. We process these
        // after the current basic block has been processed.
        val nextBranchEdgesList = mutableListOf<EvaluationOrder>()
        // This list contains the merge points that we have to process. We process these after the
        // current basic block and the next branches have been processed to reduce the amount of
        // merges.
        val mergePointsEdgesMap = IdentityHashMap<EvaluationOrder, MutableSet<Pair<Node, Node>>>()

        fun IdentityHashMap<EvaluationOrder, MutableSet<Pair<Node, Node>>>.hasCandidate(): Boolean {
            return this.entries.any { (_, v) -> v.isEmpty() }
        }

        fun IdentityHashMap<EvaluationOrder, MutableSet<Pair<Node, Node>>>
            .removeIncomingEdgeFromMergePoint(
            mergePointNextEdge: EvaluationOrder,
            incomingEdge: EvaluationOrder,
        ) {
            if (mergePointNextEdge !in this) {
                this[mergePointNextEdge] =
                    mergePointNextEdge.start.prevEOGEdges
                        .map { Pair(it.end, it.start) }
                        .toMutableSet()
            }
            this[mergePointNextEdge]?.removeIf {
                it.first == incomingEdge.end && it.second == incomingEdge.start
            }
        }

        fun IdentityHashMap<EvaluationOrder, MutableSet<Pair<Node, Node>>>.removeCandidate():
            EvaluationOrder {
            // We want an element that were we erased all values
            // If there are multiple, we take the one with the highest scc
            val key =
                this.entries
                    .filter { (_, v) -> v.isEmpty() }
                    .maxByOrNull { (k, _) -> k.scc ?: 0 }
                    ?.key ?: this.keys.first()
            this.remove(key)
            return key
        }

        startEdges.forEach { nextBranchEdgesList.add(it) }

        var debugCounter = 0L

        while (
            currentBBEdgesList.isNotEmpty() ||
                nextBranchEdgesList.isNotEmpty() ||
                mergePointsEdgesMap.isNotEmpty() ||
                sccEdgesQueue.isNotEmpty()
        ) {
            debugCounter++

            if (debugCounter % 100 == 0L && timeouts.isNotEmpty()) {
                TranslationManager.Companion.log.trace(
                    "Looping. debugCounter: $debugCounter, timeout: $timeout, startTime: ${startTime.elapsedNow().toLong(DurationUnit.MILLISECONDS)}, timeouts.last: ${timeouts.last()}"
                )
            }

            val nextEdge =
                if (currentBBEdgesList.isNotEmpty()) {
                    // If we have edges in the current basic block, we take these. We prefer to
                    // finish with the whole Basic Block before moving somewhere else.
                    currentBBEdgesList.removeFirst()
                } else if (sccEdgesQueue.isNotEmpty()) {
                    // if we have edges pointing into the same SCC, that's our next priority
                    sccEdgesQueue.poll().second
                } else if (mergePointsEdgesMap.hasCandidate()) {
                    mergePointsEdgesMap.removeCandidate()
                } else if (nextBranchEdgesList.isNotEmpty()) {
                    // If we have points splitting up the EOG, we prefer to process these before
                    // merging the EOG again. This is to hopefully reduce the number of merges
                    // that we have to compute and that we hopefully reduce the number of
                    // re-processing the same basic blocks.
                    nextBranchEdgesList.removeFirst()
                } else {
                    mergePointsEdgesMap.removeCandidate()
                }

            // Compute the effects of "nextEdge" on the state by applying the transformation to
            // its state.
            val nextGlobal = globalState[nextEdge] ?: continue

            // Either immediately before or after this edge, there's a branching node. In these
            // cases, we definitely want to check if there's an update to the state.
            val isNoBranchingPoint =
                nextEdge.end.nextEOGEdges.size == 1 &&
                    nextEdge.end.prevEOGEdges.size == 1 &&
                    nextEdge.start.nextEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.size == 1
            // Either before or after this edge, there's a branching node within two steps
            // (start, end and the nodes before/after these). We have to ensure that we copy the
            // state for all these nodes to enable the update checks conducted on the branching
            // edges. We need one more step for this, otherwise we will fail recognizing the updates
            // for a node "x" which is a branching edge because the next node would already modify
            // the state of x.
            val isNotNearStartOrEndOfBasicBlock =
                isNoBranchingPoint &&
                    nextEdge.end.nextEOGEdges.single().end.nextEOGEdges.size == 1 &&
                    nextEdge.end.nextEOGEdges.single().end.prevEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.single().start.nextEOGEdges.size == 1 &&
                    nextEdge.start.prevEOGEdges.single().start.prevEOGEdges.size == 1

            if (
                timeout == null ||
                    startTime.elapsedNow().toLong(DurationUnit.MILLISECONDS) < timeouts.last()
            ) {
                @Suppress("UNCHECKED_CAST")
                val newState =
                    transformation(
                        this@Lattice,
                        nextEdge,
                        if (isNotNearStartOrEndOfBasicBlock) nextGlobal
                        else nextGlobal.duplicate() as T,
                    )
                nextEdge.end.nextEOGEdges.forEach {
                    // We continue with the nextEOG edge if we haven't seen it before or if we
                    // updated the state in comparison to the previous time we were there.

                    val oldGlobalIt = globalState[it]

                    // If we're on the loop head (some node is Loop), and we use
                    // WIDENING or WIDENING_NARROWING, we have to apply the widening/narrowing
                    // here (if oldGlobalIt is not null).
                    val newGlobalIt =
                        if (
                            nextEdge.end.branchOf(Loop::class) &&
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
                            val result =
                                if (!isNoBranchingPoint && oldGlobalIt != null) {
                                    // It's a merge point and we've been here before. Use lub to
                                    // merge the different states.
                                    this@Lattice.lub(
                                        one = newState,
                                        two = oldGlobalIt,
                                        allowModify = isNotNearStartOrEndOfBasicBlock,
                                    )
                                } else {
                                    // We have no oldGlobalIt => no other choice than taking the
                                    // current new state
                                    // If it's not near a branch (most importantly merge points),
                                    // the existing state should already have been computed on a
                                    // "merge" before, so we don't need to lub here (already
                                    // built-in in the new result)
                                    newState
                                }
                            result
                        }

                    globalState.put(it, newGlobalIt)

                    if (
                        it !in currentBBEdgesList &&
                            it !in nextBranchEdgesList &&
                            (isNoBranchingPoint ||
                                oldGlobalIt == null ||
                                // If we deal with PointsToState Elements, we use their special
                                // parallelCompare function, otherwise, we resort to the
                                // traditional compare
                                ((newGlobalIt as? PointsToState.Element)?.parallelCompare(
                                    oldGlobalIt
                                )
                                    ?: (newGlobalIt as? ConcurrentMapLattice.Element<*, *>)
                                        ?.parallelCompare(oldGlobalIt)
                                    ?: newGlobalIt.compare(oldGlobalIt)) in
                                    setOf(Order.GREATER, Order.UNEQUAL))
                    ) {
                        if (
                            // We might be at the merge point.
                            // In comparison to a loop entry, a merge point has multiple
                            // prevEOGEdges
                            // without SCC-Label and at least one nextEOGEdge without
                            it.start.prevEOGEdges.filter { it.scc == null }.size > 1 &&
                                it.start.nextEOGEdges.any { it.scc == null }
                        ) {
                            // This edge brings us to a merge point, so we add it to the list of
                            // merge points.
                            mergePointsEdgesMap.removeIncomingEdgeFromMergePoint(it, nextEdge)
                        } else if (nextEdge.end.nextEOGEdges.size > 1) {
                            // If we have multiple next edges, we add the ones that stay inside the
                            // loop
                            // (AKA have an SCC label) to the SCCEdgesList
                            // The other edges we add to the list of edges of to next basic block
                            // (outside the loop, or for a branch).
                            // We will process these after the current basic block has been
                            // processed
                            // (probably very soon).
                            val sccPriority = it.scc
                            if (sccPriority != null) sccEdgesQueue.add(Pair(sccPriority, it))
                            else nextBranchEdgesList.add(0, it)
                        } else {
                            // If we have only one next edge, we add it to the current basic
                            // block edges list.
                            currentBBEdgesList.add(0, it)
                        }
                    }
                }

                if (
                    nextEdge.end.nextEOGEdges.isEmpty() ||
                        (currentBBEdgesList.isEmpty() &&
                            nextBranchEdgesList.isEmpty() &&
                            mergePointsEdgesMap.isEmpty() &&
                            sccEdgesQueue.isEmpty())
                ) {
                    finalState = this@Lattice.lub(finalState, newState, false)
                }
            } else {
                TranslationManager.Companion.log.info(
                    "Reached analysis timeout for ${startEdges.first().start.name.localName}, stopping further analysis"
                )
                // We are done, so we remove the current timeout
                timeouts.removeLast()
                /*                if (timeouts.isNotEmpty())
                Pass.Companion.log.info(
                    "+++ called iterateEOGInternal on a recursive call that exceeded the time. We have ${timeouts.size} existing timeouts in the queue which we increased by the timeout: ${timeouts.map { it }}"
                )*/
                val r = this@Lattice.lub(finalState, nextGlobal, false)
                Pass.Companion.log.info("Finished calculating final lub")
                return Pair(r, true)
            }
        }

        // We are done, so we remove the current timeout
        if (timeout != null) {
            timeouts.removeLast()
        }
        return Pair(finalState, false)
    }
}

/** Implements a [Lattice] whose elements are the powerset of a given set of values. */
class PowersetLattice<T>() : Lattice<PowersetLattice.Element<T>> {
    override lateinit var elements: ConcurrentIdentitySet<Element<T>>

    class Element<T>(expectedMaxSize: Int) :
        ConcurrentIdentitySet<T>(expectedMaxSize), Lattice.Element {

        // Secondary track indexes to accelerate 'contains', 'equals', and 'compare' to O(1)
        private val nodeIndex = ConcurrentHashMap<PointsToPass.NodeWithPropertiesKey, T>()
        private val pairIndex = ConcurrentHashMap<PairKey, Pair<*, *>>()

        private class PairKey(val first: Any?, val second: Any?) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is PairKey) return false
                return this.first === other.first && this.second == other.second
            }

            override fun hashCode(): Int {
                return 31 * System.identityHashCode(first) + (second?.hashCode() ?: 0)
            }
        }

        // We make the new element a bit bigger than the current size to avoid resizing
        constructor(set: Set<T>) : this(ceil(set.size * 1.5).toInt()) {
            addAllWithoutCheck(set as? ConcurrentIdentitySet<T> ?: set.toConcurrentIdentitySet())
            buildIndexFromCurrentElements()
        }

        constructor() : this(16)

        // We make the new element a bit bigger than the current size to avoid resizing
        constructor(vararg entries: T) : this(ceil(entries.size * 1.5).toInt()) {
            addAll(entries) // standard addAll loops and calls our overridden add()
        }

        /**
         * Rebuilds the secondary indexes from scratch. Crucial when batch operations like
         * [addAllWithoutCheck] bypass the standard [add] method.
         */
        fun buildIndexFromCurrentElements() {
            nodeIndex.clear()
            pairIndex.clear()
            for (item in this) {
                when (item) {
                    is Pair<*, *> -> pairIndex[PairKey(item.first, item.second)] = item
                    is PointsToPass.NodeWithPropertiesKey -> nodeIndex[item] = item
                }
            }
        }

        override fun add(element: T): Boolean {
            when (element) {
                is Pair<*, *> -> {
                    val key = PairKey(element.first, element.second)
                    if (pairIndex.containsKey(key)) return false
                    val added = super.add(element)
                    if (added) {
                        pairIndex[key] = element
                    }
                    return added
                }
                is PointsToPass.NodeWithPropertiesKey -> {
                    if (nodeIndex.containsKey(element)) return false
                    val added = super.add(element)
                    if (added) {
                        nodeIndex[element] = element
                    }
                    return added
                }
                else -> {
                    return super.add(element)
                }
            }
        }

        // Note: If your framework's base class uses 'Any?' for remove, change 'T' to 'Any?'
        override fun remove(element: T): Boolean {
            val removed = super.remove(element)
            if (removed) {
                when (element) {
                    is Pair<*, *> -> pairIndex.remove(PairKey(element.first, element.second))
                    is PointsToPass.NodeWithPropertiesKey -> nodeIndex.remove(element)
                }
            }
            return removed
        }

        override fun clear() {
            super.clear()
            nodeIndex.clear()
            pairIndex.clear()
        }

        /** High-performance O(1) containment check utilizing our secondary indexes. */
        fun containsFast(element: Any?): Boolean {
            return when (element) {
                is Pair<*, *> -> pairIndex.containsKey(PairKey(element.first, element.second))
                is PointsToPass.NodeWithPropertiesKey -> nodeIndex.containsKey(element)
                else -> (element as? T)?.let { super.contains(it) } ?: false
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Element<*> || this.size != other.size) return false

            for (item in this) {
                if (!other.containsFast(item)) {
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
                    this@Element.forEachMaybeParallel { t ->
                        ensureActive()
                        if (!other.containsFast(t)) {
                            ret = false
                            cancel()
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

            if (other !is Element<*>)
                throw IllegalArgumentException(
                    "$other should be of type PowersetLattice.Element<T> but is of type ${other.javaClass}"
                )

            var hasThisOnly = false
            var hasOtherOnly = false

            // 1. Check if 'this' contains elements missing in 'other'
            for (item in this) {
                if (!other.containsFast(item)) {
                    hasThisOnly = true
                    break // Short-circuit instantly
                }
            }

            // 2. Check if 'other' contains elements missing in 'this'
            for (item in other) {
                if (!this.containsFast(item)) {
                    hasOtherOnly = true
                    break // Short-circuit instantly
                }
            }

            return when {
                !hasThisOnly && !hasOtherOnly -> Order.EQUAL
                hasThisOnly && hasOtherOnly -> Order.UNEQUAL
                hasThisOnly -> Order.GREATER
                else -> Order.LESSER
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
        result.buildIndexFromCurrentElements() // Force index generation after raw batch load!
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
    override lateinit var elements: ConcurrentIdentitySet<Element<K, V>>

    open class Element<K, V : Lattice.Element>(expectedMaxSize: Int) :
        ConcurrentIdentityHashMap<K, V>(expectedMaxSize), Lattice.Element {

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
                    "$other should be of type ConcurrentMapLattice.Element<K, V> but is of type ${other.javaClass}"
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
            @Suppress("KotlinConstantConditions")
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
                    "$other should be of type ConcurrentMapLattice.Element<K, V> but is of type ${other.javaClass}"
                )

            if (this.size < MIN_CHUNK_SIZE) {
                return compare(other)
            }

            val otherKeySetIsBigger = other.keys.any { it !in this.keys }

            // We can check if the entries are equal, greater or lesser
            val someGreater = AtomicBoolean(false)
            val someLesser = AtomicBoolean(otherKeySetIsBigger)

            val ret = AtomicReference<Order?>(null)

            coroutineScope {
                this@Element.entries.forEachMaybeParallel { (k, v) ->
                    // We can't return in the coroutines, so we only set the return value
                    // there. If we have a return value, we can stop here
                    if (ret.load() != null) return@forEachMaybeParallel
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
        coroutineScope {
            if (allowModify) {
                // TODO: Would it be more efficient here to clone two.entries and iterate over the
                // clone? This would avoid concurrent-access checks
                two.entries.forEachMaybeParallel(parallelism = concurrencyCounter) { (k, v) ->
                    val entry = one[k]
                    if (entry == null) {
                        // This key is not in "one", so we add the value from "two"
                        // to "one"
                        one.put(k, v)
                    } else if (two[k] != null && entry.compare(two[k]!!) != Order.EQUAL) {
                        // This key already exists in "one" and the values in one and
                        // two are different, so we have to compute the lub of the values
                        one[k]?.let { oneValue ->
                            innerLattice.lub(
                                oneValue,
                                v,
                                allowModify = true,
                                widen = widen,
                                // We already run on $CPU_CORES coroutines, so we
                                // don't need any additional ones
                                1,
                            )
                        }
                    }
                }
                result = one
            } else {
                val allKeys =
                    IdentitySet<K>(one.keys.size + two.keys.size).apply {
                        addAll(one.keys)
                        addAll(two.keys)
                    }
                result = Element()
                allKeys.forEachMaybeParallel { key ->
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
        return@coroutineScope result
    }

    override suspend fun glb(one: Element<K, V>, two: Element<K, V>): Element<K, V> =
        coroutineScope {
            val allKeys = one.keys.intersect(two.keys).toIdentitySet()

            val newMap = Element<K, V>(allKeys.size)

            allKeys.forEachMaybeParallel { key ->
                val otherValue = two[key]
                val thisValue = one[key]
                val newValue =
                    if (thisValue != null && otherValue != null) {
                        innerLattice.glb(thisValue, otherValue)
                    } else innerLattice.bottom
                newMap.put(key, newValue)
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
 * Like [MapLattice], but [Element] is backed by [HashMap] rather than [IdentityHashMap] so keys are
 * compared by `equals` instead of reference identity.
 *
 * Use this when keys are value types (autoboxed `Int`, `String`, …) or any other class where two
 * instances that compare equal should map to the same entry. The default [MapLattice] is correct
 * when keys are CPG `Node`s (or other reference-typed entities) where identity *is* the intended
 * equality; using it with value-typed keys silently produces duplicate entries after [lub] across
 * branches.
 */
open class HashMapLattice<K, V : Lattice.Element>(val innerLattice: Lattice<V>) :
    Lattice<HashMapLattice.Element<K, V>> {
    override lateinit var elements: ConcurrentIdentitySet<Element<K, V>>

    open class Element<K, V : Lattice.Element>(expectedMaxSize: Int) :
        HashMap<K, V>(expectedMaxSize), Lattice.Element {

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

        // Element equality is defined via the lattice order (two elements are equal iff their
        // compare result is EQUAL), not via Map.equals which compares entry-by-entry against
        // the wrong notion of value equality.
        override fun equals(other: Any?): Boolean {
            return other is Element<*, *> && this@Element.compare(other) == Order.EQUAL
        }

        /**
         * Pointwise lattice order: maps are compared key-by-key against the [innerLattice]'s order.
         * The result is GREATER if every key in `this` has a value `>=` the corresponding value in
         * `other` (and `this` has at least one extra key or a strictly greater value), LESSER if
         * the inverse holds, EQUAL if both maps have the same keys with EQUAL values, and UNEQUAL
         * when some keys go one way and some the other (incomparable).
         */
        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<*, *>)
                throw IllegalArgumentException(
                    "$other should be of type HashMapLattice.Element<K, V> but is of type ${other.javaClass}"
                )

            @Suppress("UNCHECKED_CAST") val otherTyped = other as Element<K, V>
            // `other` having a key we don't already counts as `this < other` up front.
            val otherKeySetIsBigger = otherTyped.keys.any { it !in this.keys }

            var someGreater = false
            var someLesser = otherKeySetIsBigger
            this.entries.forEach { (k, v) ->
                val otherV = otherTyped[k]
                if (otherV != null) {
                    when (v.compare(otherV)) {
                        Order.EQUAL -> {}
                        Order.GREATER -> {
                            // If we already saw a key going the other way, the maps are
                            // pointwise-incomparable.
                            if (someLesser) return Order.UNEQUAL
                            someGreater = true
                        }
                        Order.LESSER -> {
                            if (someGreater) return Order.UNEQUAL
                            someLesser = true
                        }
                        Order.UNEQUAL -> return Order.UNEQUAL
                    }
                } else {
                    // Key present in `this`, missing in `other` -> contributes "this is greater".
                    if (someLesser) return Order.UNEQUAL
                    someGreater = true
                }
            }
            @Suppress("KotlinConstantConditions")
            return when {
                !someGreater && !someLesser -> Order.EQUAL
                someLesser && !someGreater -> Order.LESSER
                !someLesser && someGreater -> Order.GREATER
                else -> Order.UNEQUAL
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun duplicate(): Element<K, V> {
            // Deep-copy: clone every value via the inner lattice's duplicate so callers can
            // mutate the result without aliasing the original's value lattices.
            return Element(this.map { (k, v) -> Pair<K, V>(k, v.duplicate() as V) })
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }

    override val bottom: Element<K, V>
        get() = Element()

    override suspend fun lub(
        one: Element<K, V>,
        two: Element<K, V>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element<K, V> = coroutineScope {
        val result: Element<K, V>
        if (allowModify) {
            // In-place merge: walk `two`'s entries, add or lub them into `one`. Used on the
            // worklist's hot path where callers already own `one` and don't need a new map.
            two.entries.forEachMaybeParallel { (k, v) ->
                val entry = one[k]
                if (entry == null) {
                    // Key only in `two` -> copy it across.
                    one.put(k, v)
                } else if (two[k] != null && entry.compare(two[k]!!) != Order.EQUAL) {
                    // Key in both with different values -> lub them in-place.
                    one[k]?.let { oneValue ->
                        // The outer forEachMaybeParallel already spawns CPU_CORES coroutines;
                        // tell the inner lub not to spawn more.
                        innerLattice.lub(oneValue, v, allowModify = true, widen = widen, 1)
                    }
                }
            }
            result = one
        } else {
            // Pure variant: build a fresh map so neither input is mutated. Used when the caller
            // needs both `one` and `two` to survive (e.g. forking branches).
            val allKeys = HashSet<K>(one.keys.size + two.keys.size)
            allKeys.addAll(one.keys)
            allKeys.addAll(two.keys)
            val newMap = ConcurrentHashMap<K, V>(allKeys.size)
            allKeys.forEachMaybeParallel { key ->
                val thisValue = one[key]
                val otherValue = two[key]
                val newValue =
                    if (thisValue != null && otherValue != null) {
                        innerLattice.lub(thisValue, otherValue, allowModify = false, widen, 1)
                    } else thisValue ?: otherValue
                newValue?.let { newMap.put(key, it) }
            }
            result = Element(newMap)
        }
        return@coroutineScope result
    }

    override suspend fun glb(one: Element<K, V>, two: Element<K, V>): Element<K, V> {
        // Pointwise glb: only keys present in BOTH maps survive; their values are glb'd. Keys
        // missing from either side drop out (treated as `bottom` for the absent side, and
        // `glb(x, bottom) = bottom` which we don't bother storing explicitly).
        val allKeys = one.keys.intersect(two.keys)
        val newMap = ConcurrentHashMap<K, V>()
        allKeys.forEachMaybeParallel { key ->
            val thisValue = one[key]
            val otherValue = two[key]
            val newValue =
                if (thisValue != null && otherValue != null) {
                    innerLattice.glb(thisValue, otherValue)
                } else innerLattice.bottom
            newMap.put(key, newValue)
        }
        return Element(newMap)
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
    override lateinit var elements: ConcurrentIdentitySet<Element<S, T>>

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

        @Suppress("UNCHECKED_CAST")
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
    override lateinit var elements: ConcurrentIdentitySet<Element<R, S, T>>

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

        @Suppress("UNCHECKED_CAST")
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
