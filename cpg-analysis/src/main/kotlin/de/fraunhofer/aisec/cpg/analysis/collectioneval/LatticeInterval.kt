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
package de.fraunhofer.aisec.cpg.analysis.collectioneval

sealed class LatticeInterval {
    object BOTTOM : LatticeInterval()

    data class Bounded(val lower: Bound, val upper: Bound) : LatticeInterval() {
        constructor(lower: Int, upper: Int) : this(Bound.Value(lower), Bound.Value(upper))

        constructor(lower: Int, upper: Bound) : this(Bound.Value(lower), upper)

        constructor(lower: Bound, upper: Int) : this(lower, Bound.Value(upper))
    }

    sealed class Bound {
        data class Value(val value: Int) : Bound()

        data object TOP : Bound()
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
                else -> Bound.Value(0)
            }
        val upper: Bound =
            when {
                max(this.upper, other.upper) == this.upper -> {
                    this.upper
                }
                else -> Bound.TOP
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
                this.lower == Bound.Value(0) -> {
                    other.lower
                }
                else -> this.lower
            }
        val upper: Bound =
            when {
                this.upper == Bound.TOP -> {
                    other.upper
                }
                else -> this.upper
            }
        return Bounded(lower, upper)
    }

    private fun min(one: Bound, other: Bound): Bound {
        return when {
            one is Bound.TOP -> other
            other is Bound.TOP -> one
            one is Bound.Value && other is Bound.Value ->
                Bound.Value(kotlin.math.min(one.value, other.value))
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    private fun max(one: Bound, other: Bound): Bound {
        return when {
            one is Bound.TOP || other is Bound.TOP -> Bound.TOP
            one is Bound.Value && other is Bound.Value ->
                Bound.Value(kotlin.math.max(one.value, other.value))
            else -> throw IllegalArgumentException("Unsupported interval type")
        }
    }

    private fun addBounds(a: Bound, b: Bound): Bound {
        return when {
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value + b.value)
            a is Bound.TOP || b is Bound.TOP -> Bound.TOP
            else -> throw IllegalArgumentException("Unsupported bound type")
        }
    }

    private fun subtractBounds(a: Bound, b: Bound): Bound {
        return when {
            a is Bound.Value && b is Bound.Value -> Bound.Value(a.value - b.value)
            a is Bound.TOP || b is Bound.TOP -> Bound.TOP
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
