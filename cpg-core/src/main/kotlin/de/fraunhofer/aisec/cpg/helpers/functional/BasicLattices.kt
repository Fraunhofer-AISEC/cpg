/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import java.util.IdentityHashMap
import kotlin.Pair
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.toMap

/**
 * A complete lattice is an ordered structure of values of type [T]. [T] could be anything, e.g., a
 * set, a new data structure (like a range), or anything else. [T] depends on the analysis and
 * typically has to abstract the value for the specific purpose.
 *
 * This class is actually used to hold individual instances of the lattice's elements and to compute
 * bigger elements depending on these two elements.
 *
 * Implementations of this class have to implement the comparator, the least upper bound of two
 * lattices.
 */
interface LatticeElement<T> : Comparable<LatticeElement<T>> {
    val elements: T

    /**
     * Computes the least upper bound of this lattice and [other]. It returns a new object and does
     * not modify either of the objects.
     */
    fun lub(other: LatticeElement<out T>): LatticeElement<in T>

    /** Duplicates the object, i.e., makes a deep copy. */
    fun duplicate(): LatticeElement<in T>
}

typealias PowersetLatticeT<V> = PowersetLattice<IdentitySet<V>, V>

inline fun <reified V> emptyPowersetLattice() = PowersetLattice<IdentitySet<V>, V>(identitySetOf())

/**
 * Implements the [LatticeElement] for a lattice over a set of nodes. The lattice itself is
 * constructed by the powerset.
 */
open class PowersetLattice<V : IdentitySet<T>, T>(override val elements: V) : LatticeElement<V> {
    override fun lub(other: LatticeElement<out V>): PowersetLattice<in V, T> {
        val newElements = this.elements.toIdentitySet()
        newElements += other.elements
        return PowersetLattice(newElements)
    }

    override fun duplicate() = PowersetLattice(this.elements.toIdentitySet() as V)

    override fun compareTo(other: LatticeElement<V>): Int {
        return if (this.elements == other.elements) {
            0
        } else if (this.elements.containsAll(other.elements)) {
            1
        } else {
            -1
        }
    }

    override fun equals(other: Any?): Boolean {
        // The call of `toSet` ensures that we don't get stuck for different types of sets.
        return other is PowersetLattice<V, T> && this.elements.toSet() == other.elements.toSet()
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + elements.hashCode()
    }
}

typealias MapLatticeT<K, V> = LatticeElement<Map<K, V>>

inline fun <reified K, T> emptyMapLattice() = MapLattice<K, LatticeElement<T>, T>(IdentityHashMap())

/** Implements the [LatticeElement] for a lattice over a map of nodes to another lattice. */
open class MapLattice<K, V : LatticeElement<T>, T>(override val elements: IdentityHashMap<K, V>) :
    LatticeElement<IdentityHashMap<K, V>> {

    override fun lub(other: LatticeElement<out IdentityHashMap<K, V>>): MapLattice<K, V, T> {
        val allKeys = other.elements.keys.union(this.elements.keys)
        val newMap =
            allKeys.fold(IdentityHashMap<K, V>()) { current, key ->
                val otherValue = other.elements[key]
                val thisValue = this.elements[key]
                val newValue =
                    if (thisValue != null && otherValue != null && thisValue < otherValue) {
                        thisValue.lub(otherValue) as? V
                    } else if (thisValue != null) {
                        thisValue
                    } else otherValue
                newValue?.let { current[key] = it }
                current
            }
        return MapLattice(newMap)
    }

    override fun duplicate() =
        MapLattice(
            IdentityHashMap(
                this.elements.map { (k, v) -> Pair<K, V>(k, v.duplicate() as V) }.toMap()
            )
        )

    override fun compareTo(other: LatticeElement<IdentityHashMap<K, V>>): Int {
        if (this == other) return 0
        if (
            this.elements.keys.containsAll(other.elements.keys) &&
                this.elements.entries.all { (k, v) ->
                    other.elements[k]?.let { otherV -> v >= otherV } != false
                }
        )
            return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        return other is MapLattice<K, V, T> &&
            this.elements.keys.size == other.elements.keys.size &&
            this.elements.keys.containsAll(other.elements.keys) &&
            this.elements.entries.all { (k, v) -> other.elements[k] == v }
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + elements.hashCode()
    }
}

open class TupleLattice<U : LatticeElement<S>, V : LatticeElement<T>, S, T>(
    override val elements: Pair<U, V>
) : LatticeElement<Pair<U, V>> {
    override fun lub(other: LatticeElement<out Pair<U, V>>) =
        TupleLattice(
            Pair(
                this.elements.first.lub(other.elements.first) as U,
                this.elements.second.lub(other.elements.second) as V,
            )
        )

    override fun duplicate() =
        TupleLattice(Pair(elements.first.duplicate() as U, elements.second.duplicate() as V))

    override fun compareTo(other: LatticeElement<Pair<U, V>>): Int {
        if (
            this.elements.first == other.elements.first &&
                this.elements.second == other.elements.second
        )
            return 0
        if (
            this.elements.first >= other.elements.first &&
                this.elements.second >= other.elements.second
        )
            return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TupleLattice<U, V, S, T>) return false
        return other.elements.first == this.elements.first &&
            other.elements.second == this.elements.second
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + elements.hashCode()
    }

    operator fun component1() = this.elements.first

    operator fun component2() = this.elements.second
}

class TripleLattice<U : LatticeElement<R>, V : LatticeElement<S>, W : LatticeElement<T>, R, S, T>(
    override val elements: Triple<U, V, W>
) : LatticeElement<Triple<U, V, W>> {
    override fun lub(other: LatticeElement<out Triple<U, V, W>>) =
        TripleLattice(
            Triple(
                this.elements.first.lub(other.elements.first) as U,
                this.elements.second.lub(other.elements.second) as V,
                this.elements.third.lub(other.elements.third) as W,
            )
        )

    override fun duplicate() =
        TripleLattice(
            Triple(
                elements.first.duplicate() as U,
                elements.second.duplicate() as V,
                elements.third.duplicate() as W,
            )
        )

    override fun compareTo(other: LatticeElement<Triple<U, V, W>>): Int {
        if (
            this.elements.first == other.elements.first &&
                this.elements.second == other.elements.second &&
                this.elements.third == other.elements.third
        )
            return 0
        if (
            this.elements.first >= other.elements.first as U &&
                this.elements.second >= other.elements.second as V &&
                this.elements.third >= other.elements.third as W
        )
            return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TripleLattice<U, V, W, R, S, T>) return false
        return other.elements.first == this.elements.first &&
            other.elements.second == this.elements.second &&
            other.elements.third == this.elements.third
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + elements.hashCode()
    }

    operator fun component1() = this.elements.first

    operator fun component2() = this.elements.second

    operator fun component3() = this.elements.third
}
