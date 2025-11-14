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
import kotlin.math.pow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The [LatticeInterval] class implements the functionality of intervals that is needed for the
 * [AbstractIntervalEvaluator]. It is either a [BOTTOM] object signaling no knowledge or a [Bounded]
 * object with a lower and upper [Bound]. Each [Bound] can then be [Bound.NEGATIVE_INFINITE],
 * [Bound.INFINITE] or a [Bound.Value]. This class implements many convenience methods to handle the
 * [LatticeInterval].
 */
sealed class LatticeInterval : Comparable<LatticeInterval> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(LatticeInterval::class.java)
    }

    /** Explicit representation of the bottom element of the lattice. */
    object BOTTOM : LatticeInterval()

    object TOP :
        LatticeInterval.Bounded(
            LatticeInterval.Bound.NEGATIVE_INFINITE,
            LatticeInterval.Bound.INFINITE,
        )

    fun duplicate(): LatticeInterval {
        return when (this) {
            is BOTTOM -> BOTTOM
            is TOP -> TOP
            is Bounded -> Bounded(lower, upper)
        }
    }

    /**
     * Explicit representation of an interval with a minimal value [lower] and a maximal value
     * [upper].
     */
    open class Bounded(arg1: Bound, arg2: Bound) : LatticeInterval() {
        val lower: Bound
        val upper: Bound

        constructor(
            arg1: Number,
            arg2: Number,
        ) : this(Bound.Value(arg1.toLong()), Bound.Value(arg2.toLong()))

        constructor(arg1: Long, arg2: Bound) : this(Bound.Value(arg1), arg2)

        constructor(arg1: Bound, arg2: Long) : this(arg1, Bound.Value(arg2))

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

    /**
     * Representation of a value of the interval. It can be a concrete integer ([Long]) value,
     * negative or positive infinity.
     */
    sealed class Bound : Comparable<Bound> {
        /** The [Bound] that represents a concrete integer value in the interval. */
        data class Value(val value: Long) : Bound() {
            override fun toString(): String {
                return value.toString()
            }
        }

        // necessary values for widening and narrowing
        /**
         * The [Bound] that represents negative infinity. It is used to represent the lower bound of
         * an interval that can go infinitely low.
         */
        data object NEGATIVE_INFINITE : Bound()

        /**
         * The [Bound] that represents positive infinity. It is used to represent the lower bound of
         * an interval that can go infinitely high.
         */
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

        // Addition operator
        operator fun plus(other: Bound): Bound {
            return when {
                this is INFINITE || other is INFINITE -> INFINITE
                this is NEGATIVE_INFINITE || other is NEGATIVE_INFINITE -> NEGATIVE_INFINITE
                this is Value && other is Value -> {
                    Value(this.value + other.value)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported bound type $this and $other")
                }
            }
        }

        // Subtraction operator
        operator fun minus(other: Bound): Bound {
            return when {
                this is INFINITE || other is NEGATIVE_INFINITE -> INFINITE
                this is NEGATIVE_INFINITE || other is INFINITE -> NEGATIVE_INFINITE
                this is Value && other is Value -> {
                    Value(this.value - other.value)
                }
                else -> throw IllegalArgumentException("Unsupported bound type $this and $other")
            }
        }
    }

    override fun compareTo(other: LatticeInterval): Int {
        // Comparing two Intervals. They are treated as equal if they overlap
        // BOTTOM intervals are considered "smaller" than known intervals
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

    override fun equals(other: Any?): Boolean {
        // Equals check is only true if both Intervals are true or have the same boundaries
        // Not the same as a zero result in compareTo!
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

    /**
     * Implements the left shift operation for intervals. It shifts the lower and upper bounds
     * according to the left shift operation defined by the [other] interval. The [maxBits]
     * parameter defines the bitwidth of the resulting datatype. The result is [BOTTOM] if either of
     * the values is [BOTTOM]. The result is [TOP] if either of the values is [TOP] or if the shift
     * operation is undefined (e.g., shifting by a negative value or exceeding the maximum bits).
     * Otherwise, we compute the new range by multiplying this interval with `[2^min(other),
     * 2^max(other)]`.
     */
    fun shl(other: LatticeInterval, maxBits: Int): LatticeInterval {
        if (this is BOTTOM || other is BOTTOM) return BOTTOM
        if (this is TOP || other is TOP) return TOP

        if (this is Bounded && other is Bounded) {
            val thisLower = this.lower
            val otherLower = other.lower
            val thisUpper = this.upper
            val otherUpper = other.upper
            if (
                thisLower is Bound.Value &&
                    thisUpper is Bound.Value &&
                    otherLower is Bound.Value &&
                    otherUpper is Bound.Value
            ) {
                return when {
                    otherLower.value < 0 -> {
                        TOP // Undefined behavior for negative shifts
                    }
                    otherUpper.value > maxBits -> {
                        TOP // Undefined behavior if we exceed the maximum bits
                    }
                    thisLower.value < 0 -> {
                        TOP // Undefined behavior for negative shifts
                    }
                    else -> {
                        val result =
                            this *
                                Bounded(
                                    2.toDouble().pow(otherLower.value.toDouble()).toLong(),
                                    2.toDouble().pow(otherUpper.value.toDouble()).toLong(),
                                )
                        if (
                            result is Bounded &&
                                result.upper is Bound.Value &&
                                result.upper.value >= 2.0.pow(maxBits - 1.0).toLong()
                        ) {
                            TOP // If the upper bound exceeds the maximum bits, we return TOP
                        } else result
                    }
                }
            } else return TOP
        } else {
            return TOP // Cannot determine bounds
        }
    }

    /**
     * Implements the right shift operation for intervals. It shifts the lower and upper bounds
     * according to the right shift operation defined by the [other] interval. The [maxBits]
     * parameter defines the bitwidth of the resulting datatype. The result is [BOTTOM] if either of
     * the values is [BOTTOM]. The result is [TOP] if either of the values is [TOP] or if the shift
     * operation is undefined (e.g., shifting by a negative value or exceeding the maximum bits).
     * Otherwise, we compute the new range by dividing this interval with `[2^min(other),
     * 2^max(other)]`.
     */
    fun shr(other: LatticeInterval, maxBits: Int): LatticeInterval {
        if (this is BOTTOM || other is BOTTOM) return BOTTOM
        if (this is TOP || other is TOP) return TOP

        if (this is Bounded && other is Bounded) {
            val thisLower = this.lower
            val otherLower = other.lower
            val thisUpper = this.upper
            val otherUpper = other.upper
            if (
                thisLower is Bound.Value &&
                    thisUpper is Bound.Value &&
                    otherLower is Bound.Value &&
                    otherUpper is Bound.Value
            ) {
                return when {
                    otherLower.value < 0 -> {
                        TOP // Undefined behavior for negative shifts
                    }

                    otherUpper.value > maxBits -> {
                        TOP // Undefined behavior if we exceed the maximum bits
                    }

                    thisLower.value < 0 -> {
                        TOP // Undefined behavior for negative shifts
                    }

                    else -> {
                        return this /
                            Bounded(
                                2.toDouble().pow(otherLower.value.toDouble()).toLong(),
                                2.toDouble().pow(otherUpper.value.toDouble()).toLong(),
                            )
                    }
                }
            } else return TOP
        } else {
            return TOP // Cannot determine bounds
        }
    }

    fun bitwiseAnd(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val minUpper = min(this.upper, other.upper)
                if (
                    (this.lower as? Bound.Value)?.value?.let { it >= 0 } == true &&
                        (other.lower as? Bound.Value)?.value?.let { it >= 0 } == true &&
                        minUpper is Bound.Value &&
                        minUpper.value > 0
                ) {
                    // We only compute this for non-negative values
                    Bounded(Bound.Value(0), minUpper)
                } else TOP
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    fun bitwiseOr(other: LatticeInterval): LatticeInterval {
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val maxLower = max(this.lower, other.lower)
                val maxUpper = max(this.upper, other.upper)
                if (maxUpper is Bound.Value) {
                    val upper =
                        2.0.pow((maxUpper.value.takeHighestOneBit() + 1).toDouble()).toLong() - 1
                    if (maxLower is Bound.Value && maxLower.value > 0) {
                        Bounded(maxLower, upper)
                    } else TOP
                } else TOP
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    operator fun plus(other: LatticeInterval): LatticeInterval {
        // Addition operator
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                try {
                    val newLower = addBounds(this.lower, other.lower, BoundType.LOWER)
                    val newUpper = addBounds(this.upper, other.upper, BoundType.UPPER)
                    Bounded(newLower, newUpper)
                } catch (e: IllegalArgumentException) {
                    // Catch the exception if the operation is not defined and return TOP
                    log.warn("Plus not defined for $this and $other: ${e.message}")
                    TOP
                }
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    operator fun minus(other: LatticeInterval): LatticeInterval {
        // Subtraction operator
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                try {
                    val newLower = subtractBounds(this.lower, other.upper)
                    val newUpper = subtractBounds(this.upper, other.lower)
                    Bounded(newLower, newUpper)
                } catch (e: IllegalArgumentException) {
                    // Catch the exception if the operation is not defined and return TOP
                    log.warn("Minus not defined for $this and $other: ${e.message}")
                    TOP
                }
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    operator fun times(other: LatticeInterval): LatticeInterval {
        // Multiplication operator
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                try {
                    val newLower = multiplyBounds(this.lower, other.lower)
                    val newUpper = multiplyBounds(this.upper, other.upper)
                    Bounded(newLower, newUpper)
                } catch (e: IllegalArgumentException) {
                    // Catch the exception if the operation is not defined and return TOP
                    log.warn("Times not defined for $this and $other: ${e.message}")
                    TOP
                }
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    operator fun div(other: LatticeInterval): LatticeInterval {
        // Division operator
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                try {

                    val newLower = divideBounds(this.lower, other.lower)
                    val newUpper = divideBounds(this.upper, other.upper)
                    Bounded(newLower, newUpper)
                } catch (e: IllegalArgumentException) {
                    // Catch the exception if the operation is not defined and return TOP
                    log.warn("Division not defined for $this and $other: ${e.message}")
                    TOP
                }
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    operator fun rem(other: LatticeInterval): LatticeInterval {
        // Modulo operator
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                try {
                    val lowerBracket = modulateBounds(this.lower, other.lower)
                    val upperBracket = modulateBounds(this.upper, other.upper)
                    lowerBracket.join(upperBracket)
                } catch (e: IllegalArgumentException) {
                    // Catch the exception if the operation is not defined and return TOP
                    log.warn("Modulo not defined for $this and $other: ${e.message}")
                    TOP
                }
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    fun join(other: LatticeInterval): LatticeInterval {
        // Join Operation
        return when {
            this is BOTTOM || other is BOTTOM -> BOTTOM
            this is Bounded && other is Bounded -> {
                val newLower = min(this.lower, other.lower)
                val newUpper = max(this.upper, other.upper)
                Bounded(newLower, newUpper)
            }
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    fun meet(other: LatticeInterval): LatticeInterval {
        // Meet Operation
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
            else -> {
                log.warn("Unsupported interval type $this and $other")
                TOP
            }
        }
    }

    fun widen(other: LatticeInterval): LatticeInterval {
        // Widening
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

    fun narrow(other: LatticeInterval): LatticeInterval {
        // Narrowing
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
            else -> throw IllegalArgumentException("Unsupported interval type $one and $other")
        }
    }

    private fun max(one: Bound, other: Bound): Bound {
        return when {
            one is Bound.INFINITE || other is Bound.NEGATIVE_INFINITE -> one
            other is Bound.INFINITE || one is Bound.NEGATIVE_INFINITE -> other
            one is Bound.Value && other is Bound.Value ->
                Bound.Value(kotlin.math.max(one.value, other.value))
            else -> throw IllegalArgumentException("Unsupported interval type $one and $other")
        }
    }

    private fun addBounds(a: Bound, b: Bound, type: BoundType): Bound {
        return when {
            // -∞ + ∞ is defined as -∞ or as ∞ depending on the type
            a is Bound.INFINITE && b !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.NEGATIVE_INFINITE && b is Bound.INFINITE ->
                if (type == BoundType.LOWER) Bound.NEGATIVE_INFINITE else Bound.INFINITE
            b is Bound.NEGATIVE_INFINITE && a is Bound.INFINITE ->
                if (type == BoundType.LOWER) Bound.NEGATIVE_INFINITE else Bound.INFINITE
            a is Bound.NEGATIVE_INFINITE && b !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.INFINITE && a !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            b is Bound.NEGATIVE_INFINITE && a !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value + b.value)
            else -> throw IllegalArgumentException("Unsupported bound type $a and $b")
        }
    }

    private fun subtractBounds(a: Bound, b: Bound): Bound {
        return when {
            // ∞ - ∞ is defined as ∞
            a is Bound.INFINITE && b is Bound.INFINITE -> Bound.INFINITE
            a is Bound.INFINITE && b !is Bound.INFINITE -> Bound.INFINITE
            a is Bound.NEGATIVE_INFINITE && b !is Bound.NEGATIVE_INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.INFINITE && a !is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            b is Bound.NEGATIVE_INFINITE && a !is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value - b.value)
            else -> throw IllegalArgumentException("Unsupported bound type $a and $b")
        }
    }

    private fun multiplyBounds(a: Bound, b: Bound): Bound {
        return when {
            // ∞ * 0 is defined as 0
            a is Bound.INFINITE && b == Bound.Value(0) -> Bound.Value(0)
            a is Bound.NEGATIVE_INFINITE && b == Bound.Value(0) -> Bound.Value(0)
            a is Bound.INFINITE && b > Bound.Value(0) -> Bound.INFINITE
            a is Bound.INFINITE && b < Bound.Value(0) -> Bound.NEGATIVE_INFINITE
            a > Bound.Value(0) && b is Bound.INFINITE -> Bound.INFINITE
            a < Bound.Value(0) && b is Bound.INFINITE -> Bound.NEGATIVE_INFINITE
            a is Bound.NEGATIVE_INFINITE && b > Bound.Value(0) -> Bound.NEGATIVE_INFINITE
            a is Bound.NEGATIVE_INFINITE && b < Bound.Value(0) -> Bound.INFINITE
            a > Bound.Value(0) && b is Bound.NEGATIVE_INFINITE -> Bound.NEGATIVE_INFINITE
            a < Bound.Value(0) && b is Bound.NEGATIVE_INFINITE -> Bound.INFINITE
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value * b.value)
            else -> throw IllegalArgumentException("Unsupported bound type $a and $b")
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
            else -> throw IllegalArgumentException("Unsupported bound type $a and $b")
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
            else -> throw IllegalArgumentException("Unsupported bound type $a and $b")
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

enum class BoundType {
    LOWER,
    UPPER,
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
    override fun duplicate(): State<Node, LatticeInterval> {
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
        other: State<Node, LatticeInterval>
    ): Pair<State<Node, LatticeInterval>, Boolean> {
        var update = false
        for ((node, newLattice) in other) {
            update = push(node, newLattice) || update
        }
        return Pair(this, update)
    }
}
