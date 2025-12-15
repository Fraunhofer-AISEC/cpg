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
import de.fraunhofer.aisec.cpg.helpers.functional.ConcurrentIdentityHashMap
import java.lang.UnsupportedOperationException
import java.util.*
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
 * The magic size of 16 comes from the implementation of Java and is randomly chosen. The
 * [expectedMaxSize] should be 2^n but this will be enforced internally anyway.
 */
open class IdentitySet<T>(expectedMaxSize: Int = 16) : MutableSet<T> {
    /**
     * The backing hashmap for our set. The [IdentityHashMap] offers reference-equality for keys and
     * values. In this case we use it to determine, if a node is already in our set or not. The
     * value of the map is not used and is always true. A [Boolean] is used because it seems to be
     * the smallest data type possible.
     *
     * The map is twice the [expectedMaxSize] to avoid resizing too often which is expensive.
     */
    private val map: IdentityHashMap<T, Int> = IdentityHashMap(expectedMaxSize * 2)
    private val counter = AtomicInteger()

    override operator fun contains(element: T): Boolean {
        // We are using the backing reference-equality based map to check, if the element is already
        // in the set.
        return map.containsKey(element)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return this.size == other.size && this.containsAll(other)
    }

    override fun add(element: T): Boolean {
        // Since we are a Set, we only want to add elements that are not already there
        if (!contains(element)) {
            map[element] = counter.addAndGet(1)
            return true
        }

        return false
    }

    /**
     * Adds all [elements] to this [IdentitySet] without checking if they are already present. This
     * should only be used if this set is empty!
     */
    open fun addAllWithoutCheck(elements: IdentitySet<T>) {
        // We rely on the input set and add everything without checking if an element is already
        // present.
        for (element in elements) {
            map[element] = counter.addAndGet(1)
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { map.containsKey(it) }
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        return map.keys.iterator()
    }

    /**
     * Returns the contents of this [IdentitySet] as a sorted [List] according to order the nodes
     * were inserted to. This is particularly useful, if you need to look up values in the list
     * according to their "closeness" to the root AST node.
     */
    open fun toSortedList(): List<T> {
        return map.entries.sortedBy { it.value }.map { it.key }
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
        map.clear()
    }

    override fun remove(element: T): Boolean {
        return map.remove(element) != null
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
        return map.hashCode()
    }

    override val size: Int
        get() = map.size
}

open class ConcurrentIdentitySet<T>(expectedMaxSize: Int = 16) : MutableSet<T> {
    private val map: ConcurrentIdentityHashMap<T, Int> =
        ConcurrentIdentityHashMap(expectedMaxSize * 2)
    private val counter = AtomicInteger()

    override operator fun contains(element: T): Boolean {
        // We are using the backing reference-equality based map to check, if the element is already
        // in the set.
        return map.containsKey(element)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        return this.size == other.size && this.containsAll(other)
    }

    override fun add(element: T): Boolean {
        // Since we are a Set, we only want to add elements that are not already there
        if (!contains(element)) {
            map.put(element, counter.addAndGet(1))
            return true
        }

        return false
    }

    /**
     * Adds all [elements] to this [IdentitySet] without checking if they are already present. This
     * should only be used if this set is empty!
     */
    fun addAllWithoutCheck(elements: ConcurrentIdentitySet<T>) {
        // We rely on the input set and add everything without checking if an element is already
        // present.
        for (element in elements) {
            map.put(element, counter.addAndGet(1))
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { map.containsKey(it) }
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        return (map.keys as MutableSet).iterator()
    }

    /**
     * Returns the contents of this [IdentitySet] as a sorted [List] according to order the nodes
     * were inserted to. This is particularly useful, if you need to look up values in the list
     * according to their "closeness" to the root AST node.
     */
    fun toSortedList(): List<T> {
        return map.entries.sortedBy { it.value }.map { it.key }
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
        map.clear()
    }

    override fun remove(element: T): Boolean {
        return map.remove(element) != null
    }

    override fun removeIf(filter: Predicate<in T>): Boolean {
        return map.removeKeyIf(filter)
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
        return map.hashCode()
    }

    override val size: Int
        get() = map.size
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
