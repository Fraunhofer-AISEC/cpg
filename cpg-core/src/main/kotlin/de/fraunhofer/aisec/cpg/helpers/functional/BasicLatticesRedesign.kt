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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.Loop
import de.fraunhofer.aisec.cpg.graph.forEachMaybeParallel
import de.fraunhofer.aisec.cpg.graph.isBranchOf
import de.fraunhofer.aisec.cpg.helpers.ConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
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
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.*

val CPU_CORES = Runtime.getRuntime().availableProcessors()
val MIN_CHUNK_SIZE = 100

/**
 * The number of entries below which we never bother to look for dead states while iterating the
 * EOG. See `pruneGlobalState` in [Lattice.iterateEogInternal].
 */
const val MIN_GLOBAL_STATE_PRUNE_SIZE = 256

/**
 * The number of state entries an [Lattice.iterateEOG] run may keep alive before we warn about it.
 *
 * A single entry of a points-to state costs roughly 500 bytes, so this corresponds to about a
 * gigabyte. If an analysis runs out of memory, the last function warned about here is the one to
 * look at.
 */
const val LARGE_STATE_ENTRY_WARN_THRESHOLD = 2_000_000L

/**
 * The number of edges after which [IterationStatistics] takes its first sample of how much state an
 * [Lattice.iterateEOG] run keeps alive. Every further sample is taken after twice as many edges as
 * the previous one, so the sampling costs are logarithmic in the length of the run.
 */
private const val FIRST_STATE_SAMPLE_AFTER_EDGES = 64

/** The number of states [IterationStatistics] looks at when it takes a sample. */
private const val STATES_PER_SAMPLE = 8

/**
 * Bookkeeping for a single [Lattice.iterateEOG] run. Memory consumption of the analysis is driven
 * by the product of [peakLiveStates] and the number of entries in each of them, neither of which is
 * visible from the outside, so we report both.
 *
 * We report *while* iterating and not only at the end, because a run that exhausts the heap never
 * reaches the end: without the intermediate reports, the log would name every function but the one
 * that actually caused the problem.
 */
private class IterationStatistics(private val startEdges: List<EvaluationOrder>) {
    /** The number of edges we took off a worklist. */
    var processedEdges = 0
        private set

    /**
     * The high-water mark of the number of states we kept alive at the same time, sampled before
     * pruning.
     */
    var peakLiveStates = 0
        private set

    /** The number of entries of the resulting state, or -1 if we did not get that far. */
    var finalStateEntries = -1

    /** The number of entries of the biggest state we looked at, or -1 if we never sampled one. */
    private var sampledStateEntries = -1

    /** The number of processed edges at which we take the next sample. */
    private var nextSampleEdge = FIRST_STATE_SAMPLE_AFTER_EDGES

    /** The number of entries above which the next warning is due. */
    private var nextWarnEntries = LARGE_STATE_ENTRY_WARN_THRESHOLD

    private val name: String
        get() = startEdges.firstOrNull()?.start?.name?.localName ?: "<unknown>"

    /**
     * Records that we are about to process another edge while [liveStates] states are alive, and
     * returns our current estimate of the total number of entries they hold.
     *
     * Counting the entries of a state is linear in its size, so we only do that every now and then
     * (see [FIRST_STATE_SAMPLE_AFTER_EDGES]) and for a few states only (see [STATES_PER_SAMPLE]).
     * [states] is therefore not a collection but a function: we do not even want to iterate the
     * states unless we are going to sample them.
     */
    fun sample(liveStates: Int, states: () -> Iterable<Lattice.Element>): Long {
        processedEdges++
        if (liveStates > peakLiveStates) {
            peakLiveStates = liveStates
        }

        if (processedEdges >= nextSampleEdge) {
            nextSampleEdge *= 2
            sampledStateEntries =
                states().take(STATES_PER_SAMPLE).maxOfOrNull { it.entryCount() } ?: 0
        }

        val entries = liveStates.toLong() * sampledStateEntries.coerceAtLeast(0)
        if (entries > nextWarnEntries) {
            Pass.log.warn(
                "The analysis of {} is keeping {} states of about {} entries alive at the same time ({} entries in total). This may exhaust the heap.",
                name,
                liveStates,
                sampledStateEntries,
                entries,
            )
            // Only warn again once the problem has become noticeably worse.
            nextWarnEntries = entries * 2
        }

        return entries
    }

    /** Logs what the finished - or abandoned - run kept alive. */
    fun report() {
        // The final state is the union of all end states, so its size is an upper bound for the
        // size of every intermediate state. If we never got there, we have to make do with the last
        // state we sampled.
        val entriesPerState = if (finalStateEntries >= 0) finalStateEntries else sampledStateEntries
        val peakEntries = peakLiveStates.toLong() * entriesPerState.coerceAtLeast(0)
        if (peakEntries > LARGE_STATE_ENTRY_WARN_THRESHOLD) {
            Pass.log.warn(
                "The analysis of {} kept up to {} states of up to {} entries alive at the same time ({} entries in total). This may exhaust the heap.",
                name,
                peakLiveStates,
                entriesPerState,
                peakEntries,
            )
        } else if (Pass.log.isDebugEnabled) {
            Pass.log.debug(
                "Iterated the EOG of {} in {} steps, keeping up to {} states of up to {} entries alive ({} entries in total).",
                name,
                processedEdges,
                peakLiveStates,
                entriesPerState,
                peakEntries,
            )
        }
    }
}

/**
 * The number of entries this element holds, summed over all nested containers. Together with the
 * number of states that are alive at the same time, this is what determines the memory consumption
 * of an [Lattice.iterateEOG] run, so we use it for the diagnostics in [IterationStatistics].
 */
private fun Lattice.Element.entryCount(): Int =
    when (this) {
        is TupleLattice.Element<*, *> -> first.entryCount() + second.entryCount()
        is TripleLattice.Element<*, *, *> ->
            first.entryCount() + second.entryCount() + third.entryCount()
        is ConcurrentMapLattice.Element<*, *> -> size
        is HashMapLattice.Element<*, *> -> size
        is PowersetLattice.Element<*> -> size
        else -> 1
    }

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

    open fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V =
        backing.computeIfAbsent(PointsToPass.IdKey(key)) { mappingFunction(it.ref) }

    /**
     * Atomically replaces the value stored under [key] by the result of [remappingFunction], which
     * receives the key and the current value (or `null` if there is none). Removes the entry if the
     * function returns `null`.
     */
    fun compute(key: K, remappingFunction: (K, V?) -> V?): V? =
        backing.compute(PointsToPass.IdKey(key)) { k, v -> remappingFunction(k.ref, v) }

    /**
     * Copies every entry of [other] into this map, storing [transform] of the value. This reuses
     * the key wrappers of [other] and does not go through [put], so it is both cheaper than
     * [putAll] and unaffected by whatever a subclass does in [put].
     */
    protected fun putAllTransformed(other: ConcurrentIdentityHashMap<K, V>, transform: (V) -> V) {
        other.backing.forEach { (key, value) -> backing.put(key, transform(value)) }
    }

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
val timeouts = mutableListOf<Duration>()

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

        /**
         * Whether this kind of element can be handed to more than one owner instead of being
         * copied, see [isShared].
         *
         * Elements which cannot detect (and reject) a modification of themselves must not opt in;
         * they are deep-copied instead, which is what every [Element] did before copy-on-write was
         * introduced.
         */
        val supportsSharing: Boolean
            get() = false

        /**
         * Whether this element is reachable from more than one owner and must therefore not be
         * modified any more.
         *
         * Copies of a state - which the [Lattice.iterateEOG] worklist creates for every basic block
         * - share their entries instead of deep-copying them, which is what keeps the memory
         *   consumption of an analysis with many live states manageable. Whoever wants to modify a
         *   shared entry has to ask its owner for a private copy first, see
         *   [ConcurrentMapLattice.Element.getForUpdate]; a modification of a shared element is a
         *   bug and the element is expected to say so rather than to silently corrupt the other
         *   owners' states.
         *
         * Setting this to `true` on an element which does not [support sharing][supportsSharing]
         * has no effect, so a `false` result after setting it means "this one has to be copied".
         */
        var isShared: Boolean
            get() = false
            set(@Suppress("UNUSED_PARAMETER") value) {}
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
     *
     * [maxStateEntries] is the same kind of budget for memory instead of time: it limits the number
     * of entries this run may keep alive, i.e. the number of states times the number of entries in
     * each of them. Exceeding it ends the analysis exactly like a timeout does. It is unlimited by
     * default, because - unlike a timeout - a run that is too big for the heap takes the whole
     * analysis down with it, so the right value depends on the heap the caller is willing to spend.
     * As a rule of thumb, an entry of a points-to state costs about 500 bytes.
     */
    fun iterateEOG(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: suspend (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy = Strategy.PRECISE,
        timeout: Duration = Duration.INFINITE,
        maxStateEntries: Long = Long.MAX_VALUE,
    ): Pair<T, Boolean> {
        return runBlocking {
            iterateEogInternal(
                startEdges,
                startState,
                transformation,
                strategy,
                timeout,
                maxStateEntries,
            )
        }
    }

    suspend fun iterateEogInternal(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: suspend (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy,
        timeout: Duration,
        maxStateEntries: Long = Long.MAX_VALUE,
    ): Pair<T, Boolean> {
        // [timeouts] is a stack of the budgets of all analyses that are currently running (an
        // analysis can trigger a nested one, e.g., to compute a function summary). We remember the
        // depth we started at and restore it in the "finally" below. This guarantees that our entry
        // is removed on every exit path, including an exception thrown out of [transformation]. If
        // we leaked entries here, all subsequent analyses would measure their runtime against a
        // stale budget.
        val timeoutStackDepth = timeouts.size
        if (timeout != Duration.INFINITE) {
            timeouts.addLast(timeout)
        }

        val statistics = IterationStatistics(startEdges)
        try {
            val result =
                iterateEogWorklist(
                    startEdges,
                    startState,
                    transformation,
                    strategy,
                    timeout,
                    maxStateEntries,
                    statistics,
                )
            statistics.finalStateEntries = result.first.entryCount()
            return result
        } finally {
            statistics.report()
            while (timeouts.size > timeoutStackDepth) {
                timeouts.removeLast()
            }
        }
    }

    /**
     * The actual worklist algorithm behind [iterateEogInternal]. The [timeout] budget it observes
     * has already been pushed onto [timeouts] by the caller, which is also responsible for removing
     * it again.
     */
    private suspend fun iterateEogWorklist(
        startEdges: List<EvaluationOrder>,
        startState: T,
        transformation: suspend (Lattice<T>, EvaluationOrder, T) -> T,
        strategy: Strategy,
        timeout: Duration,
        maxStateEntries: Long,
        statistics: IterationStatistics,
    ): Pair<T, Boolean> {
        // mark the time when we started the calculation to know when we stop
        val startTime = TimeSource.Monotonic.markNow()

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
                    mergePointNextEdge.start.prevEOGEdges.mapTo(mutableSetOf()) {
                        Pair(it.end, it.start)
                    }
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

        /**
         * The size [globalState] has to exceed before we try to prune it again. See
         * [pruneGlobalState].
         */
        var nextPruneSize = MIN_GLOBAL_STATE_PRUNE_SIZE

        /**
         * Drops all entries of [globalState] which can never be read again.
         *
         * [globalState] holds one - deeply copied - state per [EvaluationOrder] edge and nothing
         * ever removed an entry, so its peak size is the number of visited edges times the size of
         * a state. For large functions, this dominates the memory consumption of the analysis, even
         * though most of these states are dead long before the fixpoint is reached.
         *
         * There are exactly two places which read `globalState[e]`: for the edge that we pull off
         * one of the worklists, and for the successors of the edge we are currently processing.
         * Consequently, `globalState[e]` can only be read again if `e` is still waiting in one of
         * the worklists, or if `e` can be written again, which in turn requires that some edge in a
         * worklist can reach `e` by walking forward along the EOG. The set of live edges is
         * therefore the closure of the worklists' contents under "successor of", and everything
         * outside of it is garbage.
         *
         * Note that this only frees memory; it never changes which states are computed, because we
         * only remove entries that are provably not read anymore.
         *
         * This must be called while all pending edges are in the worklists, i.e. before an edge is
         * taken off one of them.
         */
        fun pruneGlobalState() {
            if (globalState.size <= nextPruneSize) {
                return
            }

            val live = IdentitySet<EvaluationOrder>(globalState.size)
            val stack = ArrayDeque<EvaluationOrder>()
            fun markLive(edge: EvaluationOrder) {
                if (live.add(edge)) {
                    stack.addLast(edge)
                }
            }

            currentBBEdgesList.forEach(::markLive)
            nextBranchEdgesList.forEach(::markLive)
            sccEdgesQueue.forEach { (_, edge) -> markLive(edge) }
            mergePointsEdgesMap.keys.forEach(::markLive)

            while (stack.isNotEmpty()) {
                stack.removeLast().end.nextEOGEdges.forEach(::markLive)
            }

            val iterator = globalState.keys.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() !in live) {
                    iterator.remove()
                }
            }

            // Prune again once the state has grown considerably, so that the cost of a prune (which
            // is linear in the number of reachable edges) is amortized over the entries it removes.
            nextPruneSize = maxOf(MIN_GLOBAL_STATE_PRUNE_SIZE, globalState.size * 2)
        }

        startEdges.forEach { nextBranchEdgesList.add(it) }

        while (
            currentBBEdgesList.isNotEmpty() ||
                nextBranchEdgesList.isNotEmpty() ||
                mergePointsEdgesMap.isNotEmpty() ||
                sccEdgesQueue.isNotEmpty()
        ) {
            currentCoroutineContext().ensureActive()

            // Sample the retention before pruning: that high-water mark is what actually has to fit
            // into the heap.
            val liveEntries = statistics.sample(globalState.size) { globalState.values }

            // All edges which are still to be processed are in one of the worklists at this point,
            // so this is the only place where we can determine which states are still live.
            pruneGlobalState()

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

            if (liveEntries > maxStateEntries) {
                // We are out of memory budget. We stop here in exactly the same way as we do when
                // we run out of time: the caller gets what we have computed so far, together with
                // the information that this is not a fixpoint.
                Pass.log.warn(
                    "Exceeded the budget of {} state entries for {}, stopping further analysis",
                    maxStateEntries,
                    startEdges.first().start.name.localName,
                )
                finalState = this@Lattice.lub(finalState, nextGlobal, false)
                return Pair(finalState, true)
            }

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

            val remainingTime =
                if (timeout != Duration.INFINITE) timeouts.last() - startTime.elapsedNow()
                else Duration.INFINITE
            @Suppress("UNCHECKED_CAST")
            val newState =
                transformation(
                    this@Lattice,
                    nextEdge,
                    if (isNotNearStartOrEndOfBasicBlock) nextGlobal else nextGlobal.duplicate() as T,
                )
            try {
                withTimeout(remainingTime) {
                    nextEdge.end.nextEOGEdges.forEach {
                        currentCoroutineContext().ensureActive()
                        // We continue with the nextEOG edge if we haven't seen it before or if we
                        // updated the state in comparison to the previous time we were there.
                        val oldGlobalIt = globalState[it]

                        // If we're on the loop head (some node is Loop), and we use
                        // WIDENING or WIDENING_NARROWING, we have to apply the widening/narrowing
                        // here (if oldGlobalIt is not null).
                        val newGlobalIt =
                            if (
                                nextEdge.end.isBranchOf<Loop>() &&
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
                                        // If it's not near a branch (most importantly merge
                                        // points),
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
                                // prevEOGEdges without SCC-Label and at least one nextEOGEdge
                                // without
                                it.start.prevEOGEdges.any { it.scc == null } &&
                                    it.start.nextEOGEdges.any { it.scc == null }
                            ) {
                                // This edge brings us to a merge point, so we add it to the list of
                                // merge points.
                                mergePointsEdgesMap.removeIncomingEdgeFromMergePoint(it, nextEdge)
                            } else if (nextEdge.end.nextEOGEdges.size > 1) {
                                // If we have multiple next edges, we add the ones that stay inside
                                // the
                                // loop  (AKA have an SCC label) to the SCCEdgesList
                                // The other edges we add to the list of edges of to next basic
                                // block
                                // (outside the loop, or for a branch).
                                // We will process these after the current basic block has been
                                // processed (probably very soon).
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
                }
            } catch (_: TimeoutCancellationException) {
                Pass.log.info(
                    "Reached analysis timeout for ${startEdges.first().start.name.localName}, stopping further analysis"
                )
                // Note that our caller pops the timeout we pushed, on every exit path.
                finalState = this@Lattice.lub(finalState, newState, false)
                Pass.log.info("Finished calculating final lub")
                return Pair(finalState, true)
            }
        }

        return Pair(finalState, false)
    }
}

/**
 * Prepares [value] to be handed to a second owner: if it [supports sharing][Lattice.Element
 * .supportsSharing], it is marked as [shared][Lattice.Element.isShared] and returned as it is,
 * otherwise the caller gets a private deep copy of it.
 */
@Suppress("UNCHECKED_CAST")
internal fun <V : Lattice.Element> shareValue(value: V): V {
    if (!value.supportsSharing) {
        return value.duplicate() as V
    }
    value.isShared = true
    return value
}

/** Implements a [Lattice] whose elements are the powerset of a given set of values. */
class PowersetLattice<T>() : Lattice<PowersetLattice.Element<T>> {
    override lateinit var elements: ConcurrentIdentitySet<Element<T>>

    class Element<T>(expectedMaxSize: Int) :
        ConcurrentIdentitySet<T>(expectedMaxSize), Lattice.Element {

        /**
         * Points-to sets contain elements whose reference identity is meaningless, because they are
         * created on the fly while transferring a state: a [Pair] or a
         * [PointsToPass.NodeWithPropertiesKey] describing the same nodes must count as one element,
         * no matter how often it was constructed. For those we therefore key the set by a
         * structural key instead of by reference. Everything else - in particular [Node]s - keeps
         * the reference semantics of [ConcurrentIdentitySet].
         *
         * This is the only place which knows about the special element types; [add], [remove],
         * [contains] and hence [equals] and [compare] all agree on it because they all go through
         * this method.
         */
        override fun keyFor(element: T): Any =
            when (element) {
                is Pair<*, *> -> PairKey(element.first, element.second)
                // This one is its own key already: it compares its node by reference and its
                // properties structurally.
                is PointsToPass.NodeWithPropertiesKey -> element
                else -> super.keyFor(element)
            }

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
            addAllWithoutCheck(set)
        }

        // Points-to sets are tiny (usually a single element), and there are millions of them, so we
        // start small and accept the occasional resize instead of reserving 16 slots up front.
        constructor() : this(2)

        // We make the new element a bit bigger than the current size to avoid resizing
        constructor(vararg entries: T) : this(ceil(entries.size * 1.5).toInt()) {
            addAll(entries) // standard addAll loops and calls add(), which uses our keyFor()
        }

        /**
         * O(1) containment check. Unlike [contains] this accepts an arbitrary object, which is
         * handy when comparing two sets of unrelated element types.
         */
        @Suppress("UNCHECKED_CAST")
        fun containsFast(element: Any?): Boolean = contains(element as T)

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

        override val supportsSharing: Boolean
            get() = true

        override var isShared: Boolean = false

        /**
         * A shared set belongs to several states at once, so modifying it would silently change all
         * of them. This is always a programming error: the owner of the entry has to hand out a
         * private copy first, see [ConcurrentMapLattice.Element.getForUpdate].
         */
        override fun onMutate() {
            check(!isShared) {
                "Tried to modify a points-to set which is shared between several states. Ask for a private copy (getForUpdate) before modifying it."
            }
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
        PersistentIdentityMap<K, V>(expectedMaxSize), Lattice.Element {

        constructor() : this(32)

        /**
         * Copies [m] into a new map. [m] keeps its values, so the two maps share them and everyone
         * who wants to modify an entry has to ask for a private copy first, see [getForUpdate].
         */
        constructor(m: Map<K, V>) : this(m.size) {
            if (m is PersistentIdentityMap<K, V>) {
                putAllTransformed(m) { shareValue(it) }
            } else {
                for ((key, value) in m) {
                    putShared(key, value)
                }
            }
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

        /**
         * Copy-on-write: the copy starts out with the very same values as this map, both sides
         * marked as [shared][Lattice.Element.isShared]. Only the entries which are actually
         * modified afterwards - via [getForUpdate] - are ever copied, and the analysis modifies
         * only a handful of entries per state.
         *
         * If every value may be shared, the copy also shares the map itself, so that it does not
         * even cost one map slot per entry. Values which do not
         * [support sharing][Lattice.Element.supportsSharing] have to be deep-copied, and then we
         * have to build a new map for them anyway.
         */
        override fun duplicate(): Element<K, V> {
            val snapshot = snapshot()
            val copy = Element<K, V>()
            if (snapshot.values.all { it.supportsSharing }) {
                snapshot.values.forEach { it.isShared = true }
                copy.restore(snapshot)
            } else {
                copy.putAllTransformed(this) { shareValue(it) }
            }
            return copy
        }

        /**
         * Returns the value stored under [key], guaranteeing that it is not shared with any other
         * [Element], so that the caller may modify it in place. If it currently is shared, this
         * map's entry is atomically replaced by a private copy.
         *
         * Use this - and not [get] - whenever the returned value (or anything reachable from it) is
         * going to be modified. Everything a plain [get] returns has to be treated as read-only.
         */
        @Suppress("UNCHECKED_CAST")
        fun getForUpdate(key: K): V? =
            compute(key) { _, value ->
                if (value == null || !value.isShared) value else value.duplicate() as V
            }

        /**
         * Like [getForUpdate], but stores (and returns) the result of [mappingFunction] if there is
         * no entry for [key] yet. Callers of this one always intend to modify the entry, so it
         * privatizes a shared value just like [getForUpdate] does.
         */
        @Suppress("UNCHECKED_CAST")
        override fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V =
            compute(key) { k, value ->
                when {
                    value == null -> mappingFunction(k)
                    value.isShared -> value.duplicate() as V
                    else -> value
                }
            }!!

        /**
         * Stores [value] under [key]. The caller hands the value over: it must not keep a reference
         * to it and modify it later. Use [putShared] if it does.
         */
        override fun put(key: K, value: V): V? {
            check(!value.isShared) {
                "Tried to store a shared value in a state. Either hand over a private copy or use putShared."
            }
            return super.put(key, value)
        }

        /**
         * Stores [value] under [key] while the caller keeps its own reference to it. Both sides
         * have to go through [getForUpdate] before they may modify the entry.
         */
        fun putShared(key: K, value: V): V? = super.put(key, shareValue(value))

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
                        // to "one". "two" keeps its own reference to it, so the two maps
                        // share the entry from now on.
                        one.putShared(k, v)
                    } else if (two[k] != null && entry.compare(two[k]!!) != Order.EQUAL) {
                        // This key already exists in "one" and the values in one and
                        // two are different, so we have to compute the lub of the values.
                        // We modify "one"'s value in place, so we need a private copy of it.
                        one.getForUpdate(k)?.let { oneValue ->
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
                    if (thisValue != null && otherValue != null) {
                        result.put(
                            key,
                            innerLattice.lub(
                                one = thisValue,
                                two = otherValue,
                                allowModify = false,
                                widen = widen,
                                // We already run on $CPU_CORES coroutines, so we don't
                                // need any additional ones
                                1,
                            ),
                        )
                    } else {
                        // Only one of the two maps has this key, so its value carries over
                        // unchanged. We share it with that map instead of copying it.
                        (thisValue ?: otherValue)?.let { result.putShared(key, it) }
                    }
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

        // A tuple is immutable itself, so it can be shared whenever both of its components can:
        // sharing it means sharing them.
        override val supportsSharing: Boolean
            get() = first.supportsSharing && second.supportsSharing

        // A freshly created tuple can still wrap components which are shared with somebody else, so
        // we have to ask them as well. Otherwise, we would hand out a tuple as private although
        // modifying its components is not allowed.
        override var isShared: Boolean = false
            get() = field || first.isShared || second.isShared
            set(value) {
                field = value
                if (value) {
                    first.isShared = true
                    second.isShared = true
                }
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

        // A triple is immutable itself, so it can be shared whenever all of its components can:
        // sharing it means sharing them.
        override val supportsSharing: Boolean
            get() = first.supportsSharing && second.supportsSharing && third.supportsSharing

        // A freshly created triple can still wrap components which are shared with somebody else,
        // so
        // we have to ask them as well. Otherwise, we would hand out a triple as private although
        // modifying its components is not allowed.
        override var isShared: Boolean = false
            get() = field || first.isShared || second.isShared || third.isShared
            set(value) {
                field = value
                if (value) {
                    first.isShared = true
                    second.isShared = true
                    third.isShared = true
                }
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
