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

private val REGEX_METACHARS = "\\^$.|?*+()[]{}".toSet()

private fun escapeRegex(s: String): String = buildString {
    for (c in s) {
        if (c in REGEX_METACHARS) append('\\')
        append(c)
    }
}

/**
 * Renders [CharSet] as a regex character class, e.g. `.`, `[abc]`, or (for [CharSet.Empty])
 * `[^\s\S]`.
 */
private fun CharSet.toRegexClass(): String =
    when (this) {
        is CharSet.Any -> "."
        is CharSet.Empty ->
            "[^\\s\\S]" // matches nothing, but is always a syntactically valid class
        is CharSet.Chars -> "[" + escapeRegex(chars.sorted().joinToString("")) + "]"
    }

/** Renders a repetition quantifier `{min,max}`/`{min,}`/`*` for a length interval. */
private fun LatticeInterval.toQuantifier(): String {
    val bounded = this as? LatticeInterval.Bounded ?: return "*"
    val lower = (bounded.lower as? LatticeInterval.Bound.Value)?.value ?: 0L
    val upper = bounded.upper as? LatticeInterval.Bound.Value
    return when {
        upper == null -> if (lower == 0L) "*" else "{$lower,}"
        lower == 0L && upper.value == 0L -> "{0}"
        lower == upper.value -> "{$lower}"
        else -> "{$lower,${upper.value}}"
    }
}

/**
 * The readable form of [StringPattern] used in reports and tests: [StringPattern.Const] escaped,
 * [StringPattern.Concat] juxtaposed, [StringPattern.Union] as `(a|b)`, [StringPattern.Star] as
 * `(x)*`/`(x){min,}`/`(x){min,max}`, [StringPattern.Unknown] as a character class derived from
 * [StringPattern.Unknown.charSet] followed by a length-derived quantifier from
 * [StringPattern.Unknown.length] (`.*` by default, when the length is unbounded).
 */
fun StringPattern.toRegexString(): String =
    when (this) {
        is StringPattern.Bottom -> "[^\\s\\S]"
        is StringPattern.Const -> escapeRegex(value)
        is StringPattern.Concat -> parts.joinToString("") { it.toRegexString() }
        is StringPattern.Union ->
            "(" + alternatives.map { it.toRegexString() }.sorted().joinToString("|") + ")"
        is StringPattern.Star -> {
            val quantifier =
                when {
                    max == null && min == 0 -> "*"
                    max == null -> "{$min,}"
                    min == max -> "{$min}"
                    else -> "{$min,$max}"
                }
            "(${inner.toRegexString()})$quantifier"
        }
        is StringPattern.Unknown -> charSet.toRegexClass() + length.toQuantifier()
    }

/** The compiled, anchored form of [toRegexString], suitable for [Regex.matches]. */
fun StringPattern.toRegex(): Regex = Regex("^" + toRegexString() + "$")
