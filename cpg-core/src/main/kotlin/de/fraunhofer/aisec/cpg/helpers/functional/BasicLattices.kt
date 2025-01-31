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
 * A complete lattice is an ordered structure of values of type [T]. [T] could be anything, where
 * common examples are sets, ranges, maps, tuples, but it can also have random names and a new data
 * structure which only make sense in a certain context. [T] depends on the analysis and typically
 * has to abstract the value for the specific purpose.
 *
 * This class is actually used to hold individual instances of the lattice's elements and to compute
 * bigger elements depending on these two elements.
 *
 * Implementations of this class have to implement the comparator, the least upper bound of two
 * lattices.
 */
interface LatticeElement<T> : Comparable<LatticeElement<T>> {
    val value: T

    /**
     * Computes the least upper bound of this lattice and [other]. It returns a new object and does
     * not modify either of the objects.
     */
    fun lub(other: LatticeElement<out T>): LatticeElement<in T>

    /** Duplicates the object, i.e., makes a deep copy. */
    fun duplicate(): LatticeElement<in T>
}

typealias PowersetLatticeElementT<V> = PowersetLatticeElement<IdentitySet<V>, V>

inline fun <reified V> emptyPowersetLattice() =
    PowersetLatticeElement<IdentitySet<V>, V>(identitySetOf())

/**
 * Implements the [LatticeElement] for a lattice over a set of nodes. The lattice itself is
 * constructed by the powerset.
 */
open class PowersetLatticeElement<V : IdentitySet<T>, T>(override val value: V) :
    LatticeElement<V> {
    override fun lub(other: LatticeElement<out V>): PowersetLatticeElement<in V, T> {
        val newElements = this.value.toIdentitySet()
        newElements += other.value
        return PowersetLatticeElement(newElements)
    }

    override fun duplicate() = PowersetLatticeElement(this.value.toIdentitySet() as V)

    override fun compareTo(other: LatticeElement<V>): Int {
        return if (this.value == other.value) {
            0
        } else if (this.value.containsAll(other.value)) {
            1
        } else {
            -1
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is PowersetLatticeElement<V, T> && this.value == other.value
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + value.hashCode()
    }
}

typealias MapLatticeElementT<K, V> = LatticeElement<Map<K, V>>

inline fun <reified K, T> emptyMapLatticeElement() =
    MapLatticeElement<K, LatticeElement<T>, T>(IdentityHashMap())

/** Implements the [LatticeElement] for a lattice over a map of nodes to another lattice. */
open class MapLatticeElement<K, V : LatticeElement<T>, T>(
    override val value: IdentityHashMap<K, V>
) : LatticeElement<IdentityHashMap<K, V>> {

    override fun lub(other: LatticeElement<out IdentityHashMap<K, V>>): MapLatticeElement<K, V, T> {
        val allKeys = other.value.keys.union(this.value.keys)
        val newMap =
            allKeys.fold(IdentityHashMap<K, V>()) { current, key ->
                val otherValue = other.value[key]
                val thisValue = this.value[key]
                val newValue =
                    if (thisValue != null && otherValue != null && thisValue < otherValue) {
                        thisValue.lub(otherValue) as? V
                    } else if (thisValue != null) {
                        thisValue
                    } else otherValue
                newValue?.let { current[key] = it }
                current
            }
        return MapLatticeElement(newMap)
    }

    override fun duplicate() =
        MapLatticeElement(
            IdentityHashMap(this.value.map { (k, v) -> Pair<K, V>(k, v.duplicate() as V) }.toMap())
        )

    override fun compareTo(other: LatticeElement<IdentityHashMap<K, V>>): Int {
        if (this == other) return 0
        if (
            this.value.keys.containsAll(other.value.keys) &&
                this.value.entries.all { (k, v) ->
                    other.value[k]?.let { otherV -> v >= otherV } != false
                }
        )
            return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        return other is MapLatticeElement<K, V, T> &&
            this.value.keys.size == other.value.keys.size &&
            this.value.keys.containsAll(other.value.keys) &&
            this.value.entries.all { (k, v) -> other.value[k] == v }
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + value.hashCode()
    }
}

open class TupleLatticeElement<U : LatticeElement<S>, V : LatticeElement<T>, S, T>(
    override val value: Pair<U, V>
) : LatticeElement<Pair<U, V>> {
    override fun lub(other: LatticeElement<out Pair<U, V>>) =
        TupleLatticeElement(
            Pair(
                this.value.first.lub(other.value.first) as U,
                this.value.second.lub(other.value.second) as V,
            )
        )

    override fun duplicate() =
        TupleLatticeElement(Pair(value.first.duplicate() as U, value.second.duplicate() as V))

    override fun compareTo(other: LatticeElement<Pair<U, V>>): Int {
        if (this.value.first == other.value.first && this.value.second == other.value.second)
            return 0
        if (this.value.first >= other.value.first && this.value.second >= other.value.second)
            return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TupleLatticeElement<U, V, S, T>) return false
        return other.value.first == this.value.first && other.value.second == this.value.second
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + value.hashCode()
    }

    operator fun component1() = this.value.first

    operator fun component2() = this.value.second
}

class TripleLatticeElement<
    U : LatticeElement<R>,
    V : LatticeElement<S>,
    W : LatticeElement<T>,
    R,
    S,
    T,
>(override val value: Triple<U, V, W>) : LatticeElement<Triple<U, V, W>> {
    override fun lub(other: LatticeElement<out Triple<U, V, W>>) =
        TripleLatticeElement(
            Triple(
                this.value.first.lub(other.value.first) as U,
                this.value.second.lub(other.value.second) as V,
                this.value.third.lub(other.value.third) as W,
            )
        )

    override fun duplicate() =
        TripleLatticeElement(
            Triple(
                value.first.duplicate() as U,
                value.second.duplicate() as V,
                value.third.duplicate() as W,
            )
        )

    override fun compareTo(other: LatticeElement<Triple<U, V, W>>): Int {
        if (
            this.value.first == other.value.first &&
                this.value.second == other.value.second &&
                this.value.third == other.value.third
        )
            return 0
        if (
            this.value.first >= other.value.first as U &&
                this.value.second >= other.value.second as V &&
                this.value.third >= other.value.third as W
        )
            return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TripleLatticeElement<U, V, W, R, S, T>) return false
        return other.value.first == this.value.first &&
            other.value.second == this.value.second &&
            other.value.third == this.value.third
    }

    override fun hashCode(): Int {
        return super.hashCode() * 31 + value.hashCode()
    }

    operator fun component1() = this.value.first

    operator fun component2() = this.value.second

    operator fun component3() = this.value.third
}
