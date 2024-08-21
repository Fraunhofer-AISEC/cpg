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
package de.fraunhofer.aisec.cpg.analysis.abstracteval

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.LatticeElement
import de.fraunhofer.aisec.cpg.helpers.State

sealed class LatticeInterval : Comparable<LatticeInterval> {
    object BOTTOM : LatticeInterval()

    data class Bounded(val lower: Bound, val upper: Bound) : LatticeInterval() {
        constructor(lower: Int, upper: Int) : this(Bound.Value(lower), Bound.Value(upper))

        constructor(lower: Int, upper: Bound) : this(Bound.Value(lower), upper)

        constructor(lower: Bound, upper: Int) : this(lower, Bound.Value(upper))
    }

    sealed class Bound : Comparable<Bound> {
        data class Value(val value: Int) : Bound()

        // necessary values for widening and narrowing
        data object NEGATIVE_INFINITE : Bound()

        data object INFINITE : Bound()

        override fun compareTo(other: Bound): Int {
            return when {
                this is NEGATIVE_INFINITE && other !is NEGATIVE_INFINITE -> -1
                this is INFINITE && other !is INFINITE -> 1
                other is NEGATIVE_INFINITE && this !is NEGATIVE_INFINITE -> 1
                other is INFINITE && this !is INFINITE -> -1
                this is Value && other is Value -> this.value.compareTo(other.value)
                else -> 0
            }
        }
    }

    // Comparing two Intervals. They are treated as equal if they overlap
    override fun compareTo(other: LatticeInterval): Int {
        return when {
            this is BOTTOM && other !is BOTTOM -> -1
            other is BOTTOM && this !is BOTTOM -> 1
            this is Bounded && other is Bounded -> {
                when {
                    this.lower > other.upper -> 1
                    this.upper < other.lower -> -1
                    else -> 0
                }
            }
            else -> 0
        }
    }

    // Addition operator
    operator fun plus(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val newLower = addBounds(this.lower, other.lower)
                val newUpper = addBounds(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    // Subtraction operator
    operator fun minus(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val newLower = subtractBounds(this.lower, other.lower)
                val newUpper = subtractBounds(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    // Join Operation
    fun join(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val newLower = min(this.lower, other.lower)
                val newUpper = max(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    // Meet Operation
    fun meet(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM -> other
            other is BOTTOM -> this
            this is Bounded && other is Bounded -> {
                val newLower = max(this.lower, other.lower)
                val newUpper = min(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    // Widening
    fun widen(other: LatticeInterval): LatticeInterval {
        if (this !is Bounded) {
            return other
        } else if (other !is Bounded) {
            return this
        }
        val lower: Bound =
            when {
                max(this.lower, other.lower) == other.lower -> {
                    this.lower
                }
                else -> Bound.NEGATIVE_INFINITE
            }
        val upper: Bound =
            when {
                max(this.upper, other.upper) == this.upper -> {
                    this.upper
                }
                else -> Bound.INFINITE
            }
        return Bounded(lower, upper)
    }

    // Narrowing
    fun narrow(other: LatticeInterval): LatticeInterval {
        if (this !is Bounded || other !is Bounded) {
            return BOTTOM
        }
        val lower: Bound =
            when {
                this.lower == Bound.NEGATIVE_INFINITE -> {
                    other.lower
                }
                else -> this.lower
            }
        val upper: Bound =
            when {
                this.upper == Bound.INFINITE -> {
                    other.upper
                }
                else -> this.upper
            }
        return Bounded(lower, upper)
    }

    private fun min(one: Bound, other: Bound): Bound {
        return when {
            one is Bound.INFINITE || other is Bound.NEGATIVE_INFINITE -> other
            other is Bound.INFINITE || one is Bound.NEGATIVE_INFINITE -> one
            one is Bound.Value && other is Bound.Value ->
                Bound.Value(kotlin.math.min(one.value, other.value))
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    private fun max(one: Bound, other: Bound): Bound {
        return when {
            one is Bound.INFINITE || other is Bound.NEGATIVE_INFINITE -> one
            other is Bound.INFINITE || one is Bound.NEGATIVE_INFINITE -> other
            one is Bound.Value && other is Bound.Value ->
                Bound.Value(kotlin.math.max(one.value, other.value))
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    private fun addBounds(a: Bound, b: Bound): Bound {
        return when {
            // -∞ + ∞ is not an allowed operation
            a is Bound.INFINITE && b !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.NEGATIVE_INFINITE && b !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.INFINITE && a !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            b is Bound.NEGATIVE_INFINITE && a !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value + b.value)
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    private fun subtractBounds(a: Bound, b: Bound): Bound {
        return when {
            // ∞ - ∞ is not an allowed operation
            a is Bound.INFINITE && b !is Bound.INFINITE -> Bound.INFINITE
            a is Bound.NEGATIVE_INFINITE && b !is Bound.NEGATIVE_INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.INFINITE && a !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.NEGATIVE_INFINITE && a !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value - b.value)
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    override fun toString(): String {
        return when (this) {
            is BOTTOM -> "BOTTOM"
            is Bounded -> "[$lower, $upper]"
        }
    }
}

/**
 * The [LatticeElement] that is used for worklist iteration. It wraps a single element of the type
 * [LatticeInterval]
 */
class IntervalLattice(override val elements: LatticeInterval) :
    LatticeElement<LatticeInterval>(elements) {
    override fun compareTo(other: LatticeElement<LatticeInterval>): Int {
        return this.compareTo(other)
    }

    // Returns true whenever other is fully within this
    fun contains(other: LatticeElement<LatticeInterval>): Boolean {
        if (this.elements is LatticeInterval.BOTTOM || other.elements is LatticeInterval.BOTTOM) {
            return false
        }
        val thisInterval = this.elements as LatticeInterval.Bounded
        val otherInterval = other.elements as LatticeInterval.Bounded

        return (thisInterval.lower <= otherInterval.lower && thisInterval.upper >= otherInterval.upper)
    }

    // TODO: What is the LUB and why does a single Element need to implement this operation?
    //  is seems to just be the operation performed by the worklist... in our case widening (and
    // then narrowing)
    // Use widening as the operation in question
    override fun lub(other: LatticeElement<LatticeInterval>): LatticeElement<LatticeInterval> {
        return IntervalLattice(this.elements.widen(other.elements))
    }

    fun widen(other: IntervalLattice): IntervalLattice {
        return IntervalLattice(this.elements.widen(other.elements))
    }

    fun narrow(other: IntervalLattice): IntervalLattice {
        return IntervalLattice(this.elements.narrow(other.elements))
    }

    override fun duplicate(): LatticeElement<LatticeInterval> {
        return when {
            elements is LatticeInterval.Bounded ->
                IntervalLattice(LatticeInterval.Bounded(elements.lower, elements.upper))
            else -> IntervalLattice(LatticeInterval.BOTTOM)
        }
    }
}

class IntervalState(
    private val mode: Mode
) : State<Node, LatticeInterval>() {
    var function: (IntervalLattice, IntervalLattice) -> IntervalLattice

    /**
     * An enum that holds the current mode of operation as this State may be used to apply either widening or narrowing
     */
    enum class Mode {
        WIDEN,
        NARROW
    }

    init {
        function = when (mode) {
            Mode.WIDEN -> IntervalLattice::widen
            else -> IntervalLattice::narrow
        }
    }

    /**
     * Checks if an update is necessary. This applies in the following cases:
     *  - If [other] contains nodes which are not present in `this`
     *  - If we want to apply widening and any new interval is not fully contained within the old interval
     *  - If we want to apply narrowing and any old interval is not fully contained within the new interval
     * Otherwise, it does not modify anything.
     */
    override fun needsUpdate(other: State<de.fraunhofer.aisec.cpg.graph.Node, LatticeInterval>): Boolean {
        var update = false
        for ((node, newLattice) in other) {
            newLattice as IntervalLattice // TODO: does this cast make sense?
            val current = this[node] as? IntervalLattice
            update = update || intervalNeedsUpdate(current, newLattice, mode)
        }
        return update
    }

    private fun intervalNeedsUpdate(current: IntervalLattice?, newLattice: IntervalLattice, mode: Mode): Boolean {
        return when (mode) {
            Mode.WIDEN -> current == null || !current.contains(newLattice)
            else -> current == null || !newLattice.contains(current)
        }
    }

    /**
     * Adds a new mapping from [newNode] to (a copy of) [newLatticeElement] to this object if
     * [newNode] does not exist in this state yet.
     * If it already exists, it computes either widening or narrowing between the `current` and the new interval.
     * It returns whether the state has changed.
     */
    override fun push(
        newNode: de.fraunhofer.aisec.cpg.graph.Node,
        newLatticeElement: LatticeElement<LatticeInterval>?
    ): Boolean {
        if (newLatticeElement == null) {
            return false
        }
        val current = this[newNode] as? IntervalLattice
        newLatticeElement as IntervalLattice
        // here we use our "intervalNeedsUpdate" function to determine if we have to do something
        if (current != null && intervalNeedsUpdate(current, newLatticeElement, mode)) {
            when (mode) {
                Mode.WIDEN -> this[newNode] = current.widen(newLatticeElement)
                else -> this[newNode] = current.narrow(newLatticeElement)
            }
        } else if (current != null) {
            return false
        }
        else {
            this[newNode] = newLatticeElement
        }
        return true
    }
}