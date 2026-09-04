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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import java.lang.UnsupportedOperationException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate

/**
 * This class implements the [MutableSet] interface with an underlying map and reference-equality
 * instead of object-equality. That means, objects are only considered equal, if they are the *same*
 * object. This logic is primarily implemented by the underlying [IdentityHashMap].
 *
 * The use case of this [MutableSet] is quite simple: In order to avoid loops while traversing in
 * the CPG AST we often need to store [Node] objects in a work-list (usually a set), in order to
 * filter out nodes that were already visited or processed (for example, see
 * [SubgraphWalker.flattenAST]. However, using a normal set triggers object-equality functions, such
 * as [Node.hashCode] or even worse [Node.equals], if the hashcode is the same. This can potentially
 * be very resource-intensive if nodes are very similar but not the *same*, in a work-list however
 * we only want just to avoid to place the exact node twice.
 *
 * The default [expectedMaxSize] is deliberately small: the vast majority of these sets hold a
 * handful of elements at most, and the backing table is by far the largest part of a small set.
 * Sets that grow beyond it pay an amortized O(n) rehash, which is cheap compared to reserving space
 * for 16 elements in every single one of them. Pass a bigger value if you know the set will be
 * large.
 */
open class IdentitySet<T>(private val expectedMaxSize: Int = 4) : MutableSet<T> {
    /**
     * The backing hashmap for our set. The [IdentityHashMap] offers reference-equality for keys and
     * values. In this case we use it to determine, if a node is already in our set or not. The
     * value of the map is not used and is always true. A [Boolean] is used because it seems to be
     * the smallest data type possible.
     *
     * It is allocated lazily on the first insertion: a great many [IdentitySet]s (e.g. every node's
     * `typeObservers`) stay empty for their whole lifetime, and an [IdentityHashMap] eagerly
     * allocates its backing table in its constructor. Keeping this `null` until something is added
     * avoids that allocation for the empty case.
     *
     * Note that we pass [expectedMaxSize] on unchanged: [IdentityHashMap] already reserves room for
     * its load factor internally (it sizes its table for `3 * expectedMaxSize / 2` *interleaved*
     * key/value slots), so inflating the size here again quadruples the table array. For a set that
     * holds a single element, that is the difference between 165 and 622 bytes, and there are
     * millions of these sets in a large graph.
     */
    private var map: IdentityHashMap<T, Int>? = null
    private val counter = AtomicInteger()

    /** Returns the backing map, allocating it on first use. */
    private fun ensureMap(): IdentityHashMap<T, Int> {
        return map ?: IdentityHashMap<T, Int>(expectedMaxSize).also { map = it }
    }

    override operator fun contains(element: T): Boolean {
        // We are using the backing reference-equality based map to check, if the element is already
        // in the set.
        return map?.containsKey(element) == true
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return this.size == other.size && this.containsAll(other)
    }

    override fun add(element: T): Boolean {
        // Since we are a Set, we only want to add elements that are not already there
        if (!contains(element)) {
            ensureMap()[element] = counter.addAndGet(1)
            return true
        }

        return false
    }

    /**
     * Adds all [elements] to this [IdentitySet] without checking if they are already present. This
     * should only be used if this set is empty!
     */
    open fun addAllWithoutCheck(elements: IdentitySet<T>) {
        if (elements.isEmpty()) {
            return
        }
        // We rely on the input set and add everything without checking if an element is already
        // present.
        val backing = ensureMap()
        for (element in elements) {
            backing[element] = counter.addAndGet(1)
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        val backing = map ?: return elements.isEmpty()
        return elements.all { backing.containsKey(it) }
    }

    override fun isEmpty(): Boolean {
        return map?.isEmpty() != false
    }

    override fun iterator(): MutableIterator<T> {
        @Suppress("UNCHECKED_CAST")
        return map?.keys?.iterator() ?: (EmptyMutableIterator as MutableIterator<T>)
    }

    /**
     * Returns the contents of this [IdentitySet] as a sorted [List] according to order the nodes
     * were inserted to. This is particularly useful, if you need to look up values in the list
     * according to their "closeness" to the root AST node.
     */
    open fun toSortedList(): List<T> {
        return map?.entries?.sortedBy { it.value }?.map { it.key } ?: listOf()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        // We need to keep track, whether we modified the set
        var modified = false

        elements.forEach {
            if (add(it)) {
                modified = true
            }
        }

        return modified
    }

    override fun clear() {
        map?.clear()
    }

    override fun remove(element: T): Boolean {
        return map?.remove(element) != null
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        // We need to keep track, whether we modified the set
        var modified = false

        elements.forEach {
            if (remove(it)) {
                modified = true
            }
        }

        return modified
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun hashCode(): Int {
        return map?.hashCode() ?: 0
    }

    override val size: Int
        get() = map?.size ?: 0
}

/**
 * The concurrent sibling of [IdentitySet]: a [MutableSet] which compares its elements by reference
 * instead of by [Object.equals] and which is safe to use from multiple threads.
 *
 * All elements live in a *single* [ConcurrentHashMap], keyed by [keyFor] of the element. The
 * default key is a reference-equality wrapper, so - as in [IdentitySet] - only the very same object
 * counts as already contained. Subclasses may override [keyFor] to index their elements
 * differently, for example [de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice.Element],
 * which uses structural keys for the element types whose reference identity is not meaningful.
 *
 * Keeping everything in one map is what makes this class cheap enough to allocate millions of times
 * during a points-to analysis: a set holding a single element costs the set itself, the map, its
 * table and one key wrapper, and nothing else.
 */
open class ConcurrentIdentitySet<T>(expectedMaxSize: Int = 16) : MutableSet<T> {
    /**
     * The backing map: the key is [keyFor] of the element, the value is the element itself (boxed
     * into [NullElement] if it is `null`, since a [ConcurrentHashMap] cannot hold `null` values).
     *
     * Note that we must not inflate [expectedMaxSize] here: a [ConcurrentHashMap] already reserves
     * room for its load factor internally, so multiplying the size again doubles the table array of
     * every single set. There are millions of these sets in a points-to analysis, so this matters.
     */
    private val map: ConcurrentHashMap<Any, Any> = ConcurrentHashMap(expectedMaxSize)

    /**
     * Returns the key under which [element] is stored. Two elements are the same element for this
     * set if and only if their keys are equal, so this method defines the set's notion of equality.
     *
     * The default implementation wraps the element in a reference-equality key.
     */
    protected open fun keyFor(element: T): Any = PointsToPass.IdKey(element)

    /**
     * Called by every method which modifies this set, before it does so. Subclasses can use it to
     * enforce invariants; the default implementation does nothing.
     */
    protected open fun onMutate() {}

    override operator fun contains(element: T): Boolean {
        // We are using the backing map to check, if the element is already in the set.
        return map.containsKey(keyFor(element))
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return this.size == other.size && this.containsAll(other)
    }

    override fun add(element: T): Boolean {
        onMutate()
        // Since we are a Set, we only want to add elements that are not already there
        return map.putIfAbsent(keyFor(element), box(element)) == null
    }

    /**
     * Adds all [elements] to this [ConcurrentIdentitySet] without checking if they are already
     * present. This should only be used if this set is empty!
     *
     * Note that we still have to compute [keyFor] for every element: the keys of another set were
     * computed by *its* [keyFor] and are not necessarily the keys this set would use.
     */
    open fun addAllWithoutCheck(elements: Iterable<T>) {
        onMutate()
        for (element in elements) {
            map.put(keyFor(element), box(element))
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { map.containsKey(keyFor(it)) }
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        val iterator = map.values.iterator()
        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): T = unbox(iterator.next())

            override fun remove() {
                onMutate()
                iterator.remove()
            }
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        // We need to keep track, whether we modified the set
        var modified = false

        elements.forEach {
            if (add(it)) {
                modified = true
            }
        }

        return modified
    }

    override fun clear() {
        onMutate()
        map.clear()
    }

    override fun remove(element: T): Boolean {
        onMutate()
        return map.remove(keyFor(element)) != null
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        onMutate()
        var removed = false
        val iterator = map.values.iterator()
        while (iterator.hasNext()) {
            if (filter.test(unbox(iterator.next()))) {
                iterator.remove()
                removed = true
            }
        }
        return removed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        // We need to keep track, whether we modified the set
        var modified = false

        elements.forEach {
            if (remove(it)) {
                modified = true
            }
        }

        return modified
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    /**
     * Note that we only hash the *keys*: the elements themselves may have an expensive (and, for
     * [Node]s, deeply structural) [hashCode], and summing the keys is enough to stay consistent
     * with [equals], which also only looks at the keys.
     */
    override fun hashCode(): Int {
        return map.keys.sumOf { it.hashCode() }
    }

    override val size: Int
        get() = map.size

    private fun box(element: T): Any = element ?: NullElement

    @Suppress("UNCHECKED_CAST")
    private fun unbox(value: Any): T = if (value === NullElement) null as T else value as T
}

/** Marker for a `null` element, which a [ConcurrentHashMap] cannot store as a value. */
private object NullElement

/** A shared, allocation-free empty [MutableIterator], used for empty [IdentitySet]s. */
private object EmptyMutableIterator : MutableIterator<Any?> {
    override fun hasNext() = false

    override fun next(): Any? = throw NoSuchElementException()

    override fun remove() = throw IllegalStateException()
}

fun <T> identitySetOf(vararg elements: T): IdentitySet<T> {
    val set = IdentitySet<T>(elements.size)
    for (element in elements) set.add(element)

    return set
}

fun <T> concurrentIdentitySetOf(vararg elements: T): ConcurrentIdentitySet<T> {
    val set = ConcurrentIdentitySet<T>(elements.size)
    for (element in elements) set.add(element)

    return set
}

infix fun <T> IdentitySet<T>.union(other: Iterable<T>): IdentitySet<T> {
    val set = IdentitySet<T>(this.size * 2)
    set += this
    set += other
    return set
}

fun <T> Collection<T>.toIdentitySet(): IdentitySet<T> {
    val set = IdentitySet<T>(this.size)
    set += this
    return set
}

fun <T> Collection<T>.toConcurrentIdentitySet(): ConcurrentIdentitySet<T> {
    val set = ConcurrentIdentitySet<T>(this.size)
    set += this
    return set
}
