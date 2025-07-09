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

/**
 * The [LatticeInterval] class implements the functionality of intervals that is needed for the
 * [AbstractIntervalEvaluator]. It is either a [BOTTOM] object signaling no knowledge or a [Bounded]
 * object with a lower and upper [Bound]. Each [Bound] can then be [Bound.NEGATIVE_INFINITE],
 * [Bound.INFINITE] or a [Bound.Value]. This class implements many convenience methods to handle the
 * [LatticeInterval].
 */
sealed class LatticeInterval : Comparable<LatticeInterval> {
    object BOTTOM : LatticeInterval()

    class Bounded(arg1: Bound, arg2: Bound) : LatticeInterval() {
        val lower: Bound
        val upper: Bound

        constructor(arg1: Int, arg2: Int) : this(Bound.Value(arg1), Bound.Value(arg2))

        constructor(arg1: Int, arg2: Bound) : this(Bound.Value(arg1), arg2)

        constructor(arg1: Bound, arg2: Int) : this(arg1, Bound.Value(arg2))

        // Automatically switch the arguments if the upper bound is lower than the lower bound
        init {
            if (arg1 > arg2) {
                lower = arg2
                upper = arg1
            } else {
                lower = arg1
                upper = arg2
            }
        }

        override fun toString(): String {
            return "[$lower, $upper]"
        }
    }

    // TODO: future iterations should support fractional values
    sealed class Bound : Comparable<Bound> {
        data class Value(val value: Int) : Bound() {
            override fun toString(): String {
                return value.toString()
            }
        }

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

        override fun toString(): String {
            return when (this) {
                is Value -> value.toString()
                is INFINITE -> "INFINITE"
                is NEGATIVE_INFINITE -> "NEGATIVE_INFINITE"
            }
        }
    }

    // Comparing two Intervals. They are treated as equal if they overlap
    // BOTTOM intervals are considered "smaller" than known intervals
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

    // Equals check only true if both Intervals are true or have the same boundaries
    // Not the same as a zero result in compareTo!
    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is LatticeInterval -> return false
            is BOTTOM -> this is BOTTOM
            is Bounded -> {
                when (this) {
                    is Bounded -> {
                        this.lower == other.lower && this.upper == other.upper
                    }
                    else -> false
                }
            }
            else -> false
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

    // Multiplication operator
    operator fun times(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val newLower = multiplyBounds(this.lower, other.lower)
                val newUpper = multiplyBounds(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    // Division operator
    operator fun div(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val newLower = divideBounds(this.lower, other.lower)
                val newUpper = divideBounds(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    // Modulo operator
    operator fun rem(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val lowerBracket = modulateBounds(this.lower, other.lower)
                val upperBracket = modulateBounds(this.upper, other.upper)
                lowerBracket.join(upperBracket)
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
            // Check if the overlap at all
            this.compareTo(other) != 0 -> BOTTOM
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
            // -∞ + ∞ is not a defined operation
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
            // ∞ - ∞ is not a defined operation
            a is Bound.INFINITE && b !is Bound.INFINITE -> Bound.INFINITE
            a is Bound.NEGATIVE_INFINITE && b !is Bound.NEGATIVE_INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.INFINITE && a !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.NEGATIVE_INFINITE && a !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value - b.value)
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    private fun multiplyBounds(a: Bound, b: Bound): Bound {
        return when {
            // ∞ * 0 is not a defined operation
            a is Bound.INFINITE && b > Bound.Value(0) -> Bound.INFINITE
            a is Bound.INFINITE && b < Bound.Value(0) -> Bound.NEGATIVE_INFINITE
            a > Bound.Value(0) && b is Bound.INFINITE -> Bound.INFINITE
            a < Bound.Value(0) && b is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            a is Bound.NEGATIVE_INFINITE && b > Bound.Value(0) -> Bound.NEGATIVE_INFINITE
            a is Bound.NEGATIVE_INFINITE && b < Bound.Value(0) -> Bound.INFINITE
            a > Bound.Value(0) && b is Bound.NEGATIVE_INFINITE -> Bound.NEGATIVE_INFINITE
            a < Bound.Value(0) && b is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value * b.value)
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    private fun divideBounds(a: Bound, b: Bound): Bound {
        return when {
            // ∞ / ∞ is not a defined operation
            // x / 0 is not a defined operation
            a is Bound.INFINITE && b > Bound.Value(0) && b !is Bound.INFINITE -> Bound.INFINITE
            a is Bound.INFINITE && b < Bound.Value(0) && b !is Bound.NEGATIVE_INFINITE ->
                Bound.NEGATIVE_INFINITE
            a is Bound.NEGATIVE_INFINITE && b > Bound.Value(0) && b !is Bound.INFINITE ->
                Bound.NEGATIVE_INFINITE
            a is Bound.NEGATIVE_INFINITE && b < Bound.Value(0) && b !is Bound.NEGATIVE_INFINITE ->
                Bound.INFINITE
            // We estimate x / ∞ as 0 (with x != ∞)
            a !is Bound.NEGATIVE_INFINITE &&
                a !is Bound.INFINITE &&
                (b is Bound.NEGATIVE_INFINITE || b is Bound.INFINITE) -> Bound.Value(0)
            a is Bound.Value && b is Bound.Value && b != Bound.Value(0) ->
                Bound.Value(a.value / b.value)
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    // ∞ mod b can be any number [0, b], therefore we need to return an Interval
    private fun modulateBounds(a: Bound, b: Bound): LatticeInterval {
        return when {
            // x mod 0 is not a defined operation
            // we approximate ∞ mod x as any number [0, b] (with x != ∞)
            // x mod -∞ is not a defined operation
            a == Bound.Value(0) -> Bounded(0, 0)
            (a is Bound.INFINITE || a is Bound.NEGATIVE_INFINITE) && b != Bound.Value(0) ->
                Bounded(0, b)
            b is Bound.INFINITE -> Bounded(a, a)
            a is Bound.Value && b is Bound.Value && b != Bound.Value(0) ->
                Bounded(a.value % b.value, a.value % b.value)
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    override fun toString(): String {
        return when (this) {
            is BOTTOM -> "BOTTOM"
            is Bounded -> this.toString()
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

/**
 * The [LatticeElement] that is used for worklist iteration. It wraps a single element of the type
 * [LatticeInterval].
 */
class IntervalLattice(override val elements: LatticeInterval) :
    LatticeElement<LatticeInterval>(elements) {
    override fun compareTo(other: LatticeElement<LatticeInterval>): Int {
        return elements.compareTo(other.elements)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IntervalLattice) {
            return false
        }
        return this.elements == other.elements
    }

    /** Returns true iff [other] is fully within this */
    fun contains(other: LatticeElement<LatticeInterval>): Boolean {
        if (this.elements is LatticeInterval.BOTTOM || other.elements is LatticeInterval.BOTTOM) {
            return false
        }
        val thisInterval = this.elements as LatticeInterval.Bounded
        val otherInterval = other.elements as LatticeInterval.Bounded

        return (thisInterval.lower <= otherInterval.lower &&
            thisInterval.upper >= otherInterval.upper)
    }

    /** The least upper bound of two Intervals is given by the join operation. */
    override fun lub(other: LatticeElement<LatticeInterval>): LatticeElement<LatticeInterval> {
        return IntervalLattice(this.elements.join(other.elements))
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

/**
 * A [State] that maps analyzed [Node]s to their [LatticeInterval]. Whenever new information for a
 * known node is pushed, we join it with the previous known value to properly handle branch merges.
 */
class IntervalState : State<Node, LatticeInterval>() {
    /**
     * Adds a new mapping from [newNode] to (a copy of) [newLatticeElement] to this object if
     * [newNode] does not exist in this state yet. If it already exists, it will compute the [lub]
     * over the new [LatticeElement] and the previous one. It returns whether the [LatticeElement]
     * has changed.
     */
    override fun push(
        newNode: de.fraunhofer.aisec.cpg.graph.Node,
        newLatticeElement: LatticeElement<LatticeInterval>?,
    ): Boolean {
        if (newLatticeElement == null) {
            return false
        }
        val current = this[newNode]
        if (current != null) {
            // Calculate the join of the new Element and the previous (propagated) value for the
            // node
            val joinedElement = current.lub(newLatticeElement)
            // Use the joinedElement if it differs from before
            if (joinedElement != this[newNode]) {
                this[newNode] = joinedElement
                return true
            }
            return false
        } else {
            this[newNode] = newLatticeElement
        }
        return true
    }

    /**
     * Implements the same duplication as the parent function, but returns a [IntervalState] object
     * instead.
     */
    override fun duplicate(): State<de.fraunhofer.aisec.cpg.graph.Node, LatticeInterval> {
        val clone = IntervalState()
        for ((key, value) in this) {
            clone[key] = value.duplicate()
        }
        return clone
    }

    /**
     * Implements the same [lub] function as the parent, but uses the [push] function from
     * [LatticeInterval]
     */
    override fun lub(
        other: State<de.fraunhofer.aisec.cpg.graph.Node, LatticeInterval>
    ): Pair<State<de.fraunhofer.aisec.cpg.graph.Node, LatticeInterval>, Boolean> {
        var update = false
        for ((node, newLattice) in other) {
            update = push(node, newLattice) || update
        }
        return Pair(this, update)
    }
}
