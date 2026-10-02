/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.passes.PointsToPass
import java.lang.UnsupportedOperationException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf

/**
 * A thread-safe map whose keys are compared by reference (`===`), not by `equals()`, and which can
 * hand out a snapshot of itself in constant time.
 *
 * Unlike [ConcurrentIdentityHashMap], which owns a [java.util.concurrent.ConcurrentHashMap] and
 * therefore has to copy every entry to be copied, this one stores its entries in a persistent hash
 * map. Copying is then just reading the current reference: the copy and the original share the
 * whole spine, and inserting into either of them rebuilds only the handful of nodes along one path.
 *
 * This is what makes it affordable to keep one state per live EOG edge in [Lattice.iterateEOG]:
 * where a copy used to cost one map slot per entry, it now costs a single object.
 *
 * Note that the entries themselves are *not* copied and are therefore reachable from both maps. It
 * is up to the subclass to make sure that nobody modifies a shared entry in place; see
 * [ConcurrentMapLattice.Element] for how the lattices do it.
 *
 * The mutating operations are implemented as compare-and-set loops rather than under a lock. As a
 * consequence, the function passed to [compute] or [computeIfAbsent] may be invoked more than once
 * if another thread writes to the same map concurrently, and only the result of the winning attempt
 * is stored. Such a function must therefore be free of side effects.
 */
open class PersistentIdentityMap<K, V>() : Map<K, V> {

    /**
     * Ignores its argument. A persistent map does not have a backing table that we could size in
     * advance; the parameter only exists so that this class is a drop-in replacement for
     * [ConcurrentIdentityHashMap].
     */
    constructor(@Suppress("UNUSED_PARAMETER") expectedMaxSize: Int) : this()

    private val backing: AtomicReference<PersistentMap<PointsToPass.IdKey<K>, V>> =
        AtomicReference(persistentHashMapOf())

    /**
     * The entries of this map at this very moment. Since the map is persistent, the returned value
     * is an immutable snapshot which is unaffected by later modifications, and obtaining it is
     * free.
     */
    protected fun snapshot(): PersistentMap<PointsToPass.IdKey<K>, V> = backing.get()

    /** Replaces the contents of this map by [newContents]. */
    protected fun restore(newContents: PersistentMap<PointsToPass.IdKey<K>, V>) {
        backing.set(newContents)
    }

    /**
     * Repeatedly applies [transformation] to the current contents until it can be installed without
     * anybody else having written in the meantime, and returns the contents it was applied to.
     */
    private inline fun updateAndGetPrevious(
        transformation:
            (PersistentMap<PointsToPass.IdKey<K>, V>) -> PersistentMap<PointsToPass.IdKey<K>, V>
    ): PersistentMap<PointsToPass.IdKey<K>, V> {
        while (true) {
            val previous = backing.get()
            val next = transformation(previous)
            if (next === previous || backing.compareAndSet(previous, next)) return previous
        }
    }

    override operator fun get(key: K): V? = backing.get()[PointsToPass.IdKey(key)]

    open fun put(key: K, value: V): V? {
        val idKey = PointsToPass.IdKey(key)
        return updateAndGetPrevious { it.put(idKey, value) }[idKey]
    }

    fun remove(key: K): V? {
        val idKey = PointsToPass.IdKey(key)
        return updateAndGetPrevious { it.remove(idKey) }[idKey]
    }

    fun removeKeyIf(filter: Predicate<in K>): Boolean {
        var removed = false
        updateAndGetPrevious { previous ->
            removed = false
            previous.mutate { draft ->
                for (key in previous.keys) {
                    if (filter.test(key.ref)) {
                        draft.remove(key)
                        removed = true
                    }
                }
            }
        }
        return removed
    }

    override fun containsKey(key: K): Boolean = backing.get().containsKey(PointsToPass.IdKey(key))

    override fun containsValue(value: V): Boolean = backing.get().containsValue(value)

    override val size: Int
        get() = backing.get().size

    override fun isEmpty(): Boolean = backing.get().isEmpty()

    override val keys: Set<K>
        get() = KeySetView()

    override val values: Collection<V>
        get() = backing.get().values

    override val entries: Set<Map.Entry<K, V>>
        get() = EntrySetView()

    /**
     * Stores the result of [mappingFunction] under [key] if there is no entry for it yet, and
     * returns the value which is stored afterwards.
     *
     * [mappingFunction] must be free of side effects, see the note on the class.
     */
    open fun computeIfAbsent(key: K, mappingFunction: (K) -> V): V {
        val idKey = PointsToPass.IdKey(key)
        // We do not use compute() here so that we do not even call the mapping function in the
        // common case that the key is already there.
        while (true) {
            val previous = backing.get()
            val existing = previous[idKey]
            if (existing != null) return existing
            val value = mappingFunction(key)
            if (backing.compareAndSet(previous, previous.put(idKey, value))) return value
        }
    }

    /**
     * Replaces the value stored under [key] by the result of [remappingFunction], which receives
     * the key and the current value (or `null` if there is none), and returns the new value.
     * Removes the entry if the function returns `null`.
     *
     * [remappingFunction] must be free of side effects, see the note on the class.
     */
    fun compute(key: K, remappingFunction: (K, V?) -> V?): V? {
        val idKey = PointsToPass.IdKey(key)
        while (true) {
            val previous = backing.get()
            val newValue = remappingFunction(key, previous[idKey])
            val next =
                if (newValue == null) previous.remove(idKey) else previous.put(idKey, newValue)
            if (next === previous || backing.compareAndSet(previous, next)) return newValue
        }
    }

    /**
     * Copies every entry of [other] into this map, storing [transform] of the value. This reuses
     * the key wrappers of [other] and does not go through [put], so it is both cheaper than
     * [putAll] and unaffected by whatever a subclass does in [put].
     */
    protected fun putAllTransformed(other: PersistentIdentityMap<K, V>, transform: (V) -> V) {
        val source = other.backing.get()
        updateAndGetPrevious { previous ->
            previous.mutate { draft ->
                for ((key, value) in source) {
                    draft[key] = transform(value)
                }
            }
        }
    }

    fun putAll(map: Map<out K, V>) = putAll(map.entries.map { (key, value) -> key to value })

    /** Inserts all entries from the given array of pairs. */
    fun putAll(pairs: Array<out Pair<K, V>>) = putAll(pairs.asIterable())

    /** Inserts all entries from the given [Sequence] of pairs. */
    fun putAll(pairs: Sequence<Pair<K, V>>) = putAll(pairs.asIterable())

    /** Inserts all entries from the given [Iterable] of pairs. */
    fun putAll(pairs: Iterable<Pair<K, V>>) {
        updateAndGetPrevious { previous ->
            previous.mutate { draft ->
                for ((key, value) in pairs) {
                    draft[PointsToPass.IdKey(key)] = value
                }
            }
        }
    }

    override fun toString(): String =
        backing.get().entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${key.ref}=$value"
        }

    /**
     * A view of the keys. It reads the current contents on every access, so it observes
     * modifications which happen after it was obtained, but any single iteration runs over one
     * consistent snapshot.
     */
    private inner class KeySetView : AbstractMutableSet<K>() {
        override val size: Int
            get() = this@PersistentIdentityMap.size

        override fun add(element: K): Boolean =
            throw UnsupportedOperationException("Cannot add a key without a value")

        override fun clear() = restore(persistentHashMapOf())

        override fun contains(element: K): Boolean = containsKey(element)

        override fun isEmpty(): Boolean = this@PersistentIdentityMap.isEmpty()

        override fun hashCode(): Int = snapshot().keys.sumOf { it.hashCode() }

        override fun iterator(): MutableIterator<K> {
            val iterator = snapshot().keys.iterator()
            return object : MutableIterator<K> {
                private var last: K? = null

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): K = iterator.next().ref.also { last = it }

                override fun remove() {
                    this@PersistentIdentityMap.remove(
                        last ?: throw IllegalStateException("next() has not been called yet")
                    )
                }
            }
        }

        override fun remove(element: K): Boolean =
            this@PersistentIdentityMap.remove(element) != null
    }

    /** A view of the entries, with the same semantics as [KeySetView]. */
    private inner class EntrySetView : AbstractMutableSet<Map.Entry<K, V>>() {
        override val size: Int
            get() = this@PersistentIdentityMap.size

        override fun add(element: Map.Entry<K, V>): Boolean =
            throw UnsupportedOperationException("Cannot add an entry through the entry view")

        override fun clear() = restore(persistentHashMapOf())

        override fun contains(element: Map.Entry<K, V>): Boolean {
            val current = snapshot()
            val key = PointsToPass.IdKey(element.key)
            return current.containsKey(key) && current[key] == element.value
        }

        override fun isEmpty(): Boolean = this@PersistentIdentityMap.isEmpty()

        override fun hashCode(): Int =
            snapshot().entries.sumOf { (key, value) ->
                System.identityHashCode(key.ref) xor (value?.hashCode() ?: 0)
            }

        override fun iterator(): MutableIterator<Map.Entry<K, V>> {
            val iterator = snapshot().entries.iterator()
            return object : MutableIterator<Map.Entry<K, V>> {
                private var last: K? = null

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): Map.Entry<K, V> {
                    val entry = iterator.next()
                    last = entry.key.ref
                    return IdentityEntry(entry.key.ref, entry.value)
                }

                override fun remove() {
                    this@PersistentIdentityMap.remove(
                        last ?: throw IllegalStateException("next() has not been called yet")
                    )
                }
            }
        }

        override fun remove(element: Map.Entry<K, V>): Boolean {
            val idKey = PointsToPass.IdKey(element.key)
            var removed = false
            updateAndGetPrevious { previous ->
                removed = previous[idKey] == element.value
                if (removed) previous.remove(idKey) else previous
            }
            return removed
        }
    }

    /** An entry of this map, which compares its key by reference just like the map does. */
    private class IdentityEntry<K, V>(override val key: K, override val value: V) :
        Map.Entry<K, V> {
        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && other.key === key && other.value == value

        override fun hashCode(): Int = System.identityHashCode(key) xor (value?.hashCode() ?: 0)

        override fun toString(): String = "$key=$value"
    }
}
