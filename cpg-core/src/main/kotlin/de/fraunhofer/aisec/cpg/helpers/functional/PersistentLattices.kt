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

import de.fraunhofer.aisec.cpg.helpers.ConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf

/**
 * Like [ConcurrentMapLattice], a [Lattice] over a map of [K] to another lattice represented by
 * [innerLattice], but backed by a [PersistentMap] (structural sharing, from
 * `kotlinx.collections.immutable`) instead of a mutable hash map.
 *
 * This makes [Element.duplicate] O(1) instead of O(n): since the backing map is never mutated in
 * place, two [Element]s can safely share the same underlying [PersistentMap], and [lub] builds a
 * new, structurally-shared map via [PersistentMap.put] rather than copying entry by entry. This
 * matters because [Lattice.iterateEogInternal] calls [Lattice.Element.duplicate] on essentially
 * every EOG edge near a branch; for state that is large and changes little between iterations (e.g.
 * in [de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass], where a single branch condition
 * can dominate hundreds of statements), the previous deep-copy-per-branch cost dominated runtime.
 *
 * To preserve this safety property, [lub] always computes updated values via `innerLattice.lub(
 * ..., allowModify = false, ...)`, regardless of the [allowModify] argument passed to [lub]: since
 * values already stored in a [PersistentMap] may be shared by multiple [Element]s (via
 * [Element.duplicate] or a prior [lub]), mutating one in place could silently corrupt the other.
 * [allowModify] is therefore accepted (to match the [Lattice] interface) but has no effect here.
 *
 * Keys are compared by reference identity (like [ConcurrentMapLattice]), not by [Any.equals], since
 * [de.fraunhofer.aisec.cpg.graph.Node] (and its subclasses, e.g.
 * [de.fraunhofer.aisec.cpg.graph.overlays.BasicBlock]) define structural equals/hashCode, and two
 * distinct nodes that happen to compare structurally equal must not collide as the same key.
 */
open class PersistentMapLattice<K, V : Lattice.Element>(val innerLattice: Lattice<V>) :
    Lattice<PersistentMapLattice.Element<K, V>> {
    override lateinit var elements: ConcurrentIdentitySet<Element<K, V>>

    open class Element<K, V : Lattice.Element>(
        internal val map: PersistentMap<PointsToPass.IdKey<K>, V>
    ) : Lattice.Element {

        constructor() : this(persistentHashMapOf())

        constructor(
            m: Map<K, V>
        ) : this(
            m.entries.fold(persistentHashMapOf<PointsToPass.IdKey<K>, V>()) { acc, (k, v) ->
                acc.put(PointsToPass.IdKey(k), v)
            }
        )

        constructor(entries: Collection<Pair<K, V>>) : this(entries.toMap())

        constructor(vararg entries: Pair<K, V>) : this(entries.toMap())

        /** Copy-constructs from another [Element], reusing its [PersistentMap] (this is O(1)). */
        constructor(other: Element<K, V>) : this(other.map)

        val size: Int
            get() = map.size

        val keys: Set<K>
            get() = map.keys.mapTo(LinkedHashSet()) { it.ref }

        val entries: List<Pair<K, V>>
            get() = map.entries.map { it.key.ref to it.value }

        operator fun get(key: K): V? = map[PointsToPass.IdKey(key)]

        operator fun iterator(): Iterator<Pair<K, V>> = entries.iterator()

        /** Returns a new [Element] with [key] mapped to [value]; does not modify this [Element]. */
        fun put(key: K, value: V): Element<K, V> = Element(map.put(PointsToPass.IdKey(key), value))

        /** Returns the entries (as `(K, V)` pairs) for which [predicate] holds. */
        fun filter(predicate: (Pair<K, V>) -> Boolean): List<Pair<K, V>> {
            return entries.filter(predicate)
        }

        override fun equals(other: Any?): Boolean {
            return other is Element<*, *> && this.compare(other) == Order.EQUAL
        }

        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element<*, *>)
                throw IllegalArgumentException(
                    "$other should be of type PersistentMapLattice.Element<K, V> but is of type ${other.javaClass}"
                )
            @Suppress("UNCHECKED_CAST") val otherMap = other as Element<K, V>

            val otherKeySetIsBigger = otherMap.map.keys.any { it !in this.map.keys }

            var someGreater = false
            var someLesser = otherKeySetIsBigger
            for ((k, v) in this.map) {
                val otherV = otherMap.map[k]
                if (otherV != null) {
                    when (v.compare(otherV)) {
                        Order.EQUAL -> {
                            /* Nothing to do*/
                        }
                        Order.GREATER -> {
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
                    if (someLesser) return Order.UNEQUAL
                    someGreater = true
                }
            }
            return if (!someGreater && !someLesser) {
                Order.EQUAL
            } else if (someLesser && !someGreater) {
                Order.LESSER
            } else if (!someLesser && someGreater) {
                Order.GREATER
            } else {
                Order.UNEQUAL
            }
        }

        // O(1): the underlying PersistentMap is never mutated in place (see the class-level
        // documentation), so it is always safe for the duplicate to share it with the original.
        override fun duplicate(): Element<K, V> = Element(map)

        override fun hashCode(): Int = map.hashCode()
    }

    override val bottom: Element<K, V>
        get() = Element()

    override suspend fun lub(
        one: Element<K, V>,
        two: Element<K, V>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): Element<K, V> {
        var result = one.map
        for ((key, v) in two.map) {
            val existing = result[key]
            val newValue =
                if (existing != null) {
                    if (existing === v) {
                        existing
                    } else {
                        innerLattice.lub(existing, v, allowModify = false, widen = widen, 1)
                    }
                } else {
                    v
                }
            result = result.put(key, newValue)
        }
        return Element(result)
    }

    override suspend fun glb(one: Element<K, V>, two: Element<K, V>): Element<K, V> {
        var result = persistentHashMapOf<PointsToPass.IdKey<K>, V>()
        for ((key, v) in one.map) {
            val otherValue = two.map[key]
            if (otherValue != null) {
                result = result.put(key, innerLattice.glb(v, otherValue))
            }
        }
        return Element(result)
    }

    override fun compare(one: Element<K, V>, two: Element<K, V>): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element<K, V>): Element<K, V> {
        return one.duplicate()
    }
}
