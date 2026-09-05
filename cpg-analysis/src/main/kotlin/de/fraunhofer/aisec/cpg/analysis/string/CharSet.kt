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
package de.fraunhofer.aisec.cpg.analysis.string

/**
 * A small, dependency-free abstraction of the set of characters that may occur at some position in
 * a string. Forms a finite-height lattice `Empty <= Chars <= Any`.
 *
 * [union] and [intersect] are idempotent, commutative and associative: idempotence and
 * commutativity follow directly from [Set.union]/[Set.intersect] on the underlying [Chars.chars],
 * and collapsing to [Any] once [MAX_EXPLICIT] is exceeded is itself commutative and associative
 * (the collapse only depends on the resulting set size, not on the order in which operands were
 * combined).
 */
sealed interface CharSet {
    /** No character is possible. Bottom of the [CharSet] lattice. */
    object Empty : CharSet

    /**
     * An explicit, non-empty set of possible characters. Never larger than [MAX_EXPLICIT] - callers
     * that would exceed the cap must use [Any] instead. Use [charsOf] rather than the constructor
     * directly to get this normalisation for free.
     */
    data class Chars(val chars: Set<Char>) : CharSet

    /** Any character is possible. Top of the [CharSet] lattice. */
    object Any : CharSet

    companion object {
        /** Above this many explicit characters, we collapse to [Any] rather than track them. */
        const val MAX_EXPLICIT: Int = 64
    }
}

/** Builds a [CharSet] from the given characters, collapsing to [CharSet.Any] if too large. */
fun charsOf(chars: Set<Char>): CharSet =
    when {
        chars.isEmpty() -> CharSet.Empty
        chars.size > CharSet.MAX_EXPLICIT -> CharSet.Any
        else -> CharSet.Chars(chars)
    }

fun charsOf(vararg chars: Char): CharSet = charsOf(chars.toSet())

/** `true` iff [c] is a character this [CharSet] admits. */
operator fun CharSet.contains(c: Char): Boolean =
    when (this) {
        is CharSet.Empty -> false
        is CharSet.Chars -> c in chars
        is CharSet.Any -> true
    }

/** The least upper bound of `this` and [other] in the [CharSet] lattice. */
infix fun CharSet.union(other: CharSet): CharSet =
    when {
        this is CharSet.Any || other is CharSet.Any -> CharSet.Any
        this is CharSet.Empty -> other
        other is CharSet.Empty -> this
        this is CharSet.Chars && other is CharSet.Chars -> charsOf(this.chars union other.chars)
        else -> CharSet.Any
    }

/** The greatest lower bound of `this` and [other] in the [CharSet] lattice. */
infix fun CharSet.intersect(other: CharSet): CharSet =
    when {
        this is CharSet.Empty || other is CharSet.Empty -> CharSet.Empty
        this is CharSet.Any -> other
        other is CharSet.Any -> this
        this is CharSet.Chars && other is CharSet.Chars -> charsOf(this.chars intersect other.chars)
        else -> CharSet.Empty
    }
