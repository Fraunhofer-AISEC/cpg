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

import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.Order

/**
 * An abstraction of the set of strings a node may evaluate to. Terms are immutable and always kept
 * in a normalised form, see [normalize].
 *
 * The constructors of [Concat], [Union] and [Star] are `internal`: instances must be built through
 * the smart constructors [concat], [union] and [star] (or through [normalize] itself), which are
 * the only places that establish the normal form. Constructing a raw, un-normalised term anywhere
 * else would silently violate the invariant every other function in this package relies on.
 *
 * Note: `toString()` cannot be overridden on the sealed interface itself (Kotlin interfaces cannot
 * implement methods of `Any`), so each case below overrides it individually to delegate to
 * [toRegexString].
 */
sealed interface StringPattern : Lattice.Element {
    /** No value at all: unreachable, or nothing is known to flow here. Bottom of the lattice. */
    object Bottom : StringPattern {
        override fun toString(): String = toRegexString()
    }

    /** Exactly one known string. */
    data class Const(val value: String) : StringPattern {
        override fun toString(): String = toRegexString()
    }

    /**
     * Sequence of patterns. Never nested, never contains [Bottom], adjacent [Const]s are merged.
     */
    data class Concat internal constructor(val parts: List<StringPattern>) : StringPattern {
        override fun toString(): String = toRegexString()
    }

    /** Alternatives, e.g. from a branching DFG or a `Conditional`. Never empty, never singleton. */
    data class Union internal constructor(val alternatives: Set<StringPattern>) : StringPattern {
        override fun toString(): String = toRegexString()
    }

    /** [inner] repeated between [min] and [max] times ([max] `== null` means unbounded). */
    data class Star
    internal constructor(val inner: StringPattern, val min: Int = 0, val max: Int? = null) :
        StringPattern {
        override fun toString(): String = toRegexString()
    }

    /**
     * An undetermined segment. [origin] records *why* it is unknown - a parameter, a file read, a
     * call we cannot model - which is what makes results explainable. [charSet] and [length] refine
     * it when we know something.
     *
     * `Top` is `Unknown(origin = null, charSet = CharSet.Any, length = LatticeInterval.TOP)` -
     * there is no separate object for it, so that refinement of an unknown is uniform.
     */
    data class Unknown(
        val origin: Node? = null,
        val reason: Reason = Reason.UNSUPPORTED,
        val charSet: CharSet = CharSet.Any,
        val length: LatticeInterval = LatticeInterval.TOP,
    ) : StringPattern {
        override fun toString(): String = toRegexString()
    }

    /** Why a [Unknown] segment is undetermined. */
    enum class Reason {
        PARAMETER,
        EXTERNAL_INPUT,
        UNSUPPORTED,
        BUDGET_EXCEEDED,
        WIDENED,
    }

    override fun compare(other: Lattice.Element): Order {
        require(other is StringPattern) {
            "$other should be of type StringPattern but is of type ${other.javaClass}"
        }
        val a = this
        val b = other
        return when {
            a == b -> Order.EQUAL
            subsumes(a, b) && !subsumes(b, a) -> Order.GREATER
            subsumes(b, a) && !subsumes(a, b) -> Order.LESSER
            else -> Order.UNEQUAL
        }
    }

    /** Terms are immutable, so duplication is a no-op. */
    override fun duplicate(): Lattice.Element = this
}

/** The constant empty string. */
val EMPTY_STRING_PATTERN: StringPattern = StringPattern.Const("")

/** Smart constructor for [StringPattern.Const]. */
fun const(s: String): StringPattern = StringPattern.Const(s)

/** Smart constructor for [StringPattern.Concat], normalising the result. */
fun concat(
    vararg parts: StringPattern,
    maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
): StringPattern = concat(parts.toList(), maxTermSize, maxTermDepth, maxUnionSize)

/** Smart constructor for [StringPattern.Concat], normalising the result. */
fun concat(
    parts: List<StringPattern>,
    maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
): StringPattern = normalize(StringPattern.Concat(parts), maxTermSize, maxTermDepth, maxUnionSize)

/** Smart constructor for [StringPattern.Union], normalising the result. */
fun union(
    vararg alts: StringPattern,
    maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
): StringPattern = union(alts.toList(), maxTermSize, maxTermDepth, maxUnionSize)

/** Smart constructor for [StringPattern.Union], normalising the result. */
fun union(
    alts: Collection<StringPattern>,
    maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
): StringPattern =
    normalize(StringPattern.Union(alts.toSet()), maxTermSize, maxTermDepth, maxUnionSize)

/** Smart constructor for [StringPattern.Star], normalising the result. */
fun star(
    inner: StringPattern,
    min: Int = 0,
    max: Int? = null,
    maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
): StringPattern =
    normalize(StringPattern.Star(inner, min, max), maxTermSize, maxTermDepth, maxUnionSize)
