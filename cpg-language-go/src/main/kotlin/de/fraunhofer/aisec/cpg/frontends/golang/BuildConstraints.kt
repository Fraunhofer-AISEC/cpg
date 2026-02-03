/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import java.util.StringTokenizer

/**
 * Go allows to specify so-called
 * [build constraints](https://pkg.go.dev/cmd/go#hdr-Build_constraints) on files, to specify
 * conditions on which they should be built (or not).
 *
 * For example, the following build constraint specifies that the file is either built on linux and
 * darwin arm-based machines:
 * ```
 * //go:build (linux && arm64) || (darwin && arm64)
 * ```
 *
 * These build constraints consist of several expressions (tags) that can be combined with `||` and
 * `&&` as well as negated with `!`.
 *
 * This interface serves as the bases for all these expressions and contains a companion object to
 * parse such an expression (without the `//go:build` prefix) using [fromString].
 */
fun interface BuildConstraintExpression {
    /**
     * Evaluates whether the current expression satisfies the set of [tags]. Tags can be for example
     * the target operating system (e.g., `linux`), the target architecture (e.g. `arm64`), the Go
     * version (e.g. `go1.20`) or other custom tags supplied to the build process.
     */
    fun evaluate(tags: Set<String>): Boolean

    companion object {
        /**
         * This function can be used to translate a string-based build constraint (without the
         * `//go:build` prefix) into a [BuildConstraintExpression].
         */
        fun fromString(str: String): BuildConstraintExpression? {
            val tokenizer = StringTokenizer(str.replace(" ", ""), "!&|()", true)

            return tokenizer.readExpression()
        }

        private fun StringTokenizer.readExpression(): BuildConstraintExpression? {
            if (!this.hasMoreTokens()) {
                return null
            }

            when (val token = nextToken()) {
                !in listOf("!", "(", ")", "&", "|") -> {
                    // A tag
                    val tag = BuildConstraintTag(token)

                    // We need to check, if there is more
                    if (hasMoreTokens()) {
                        // If there is more, there needs to be an operator code
                        val op = readOperatorCode() ?: return null

                        // There needs to be another expression
                        val rhs = readExpression() ?: return null

                        return BuildConstraintBinaryExpression(op, tag, rhs)
                    } else {
                        return tag
                    }
                }
                "!" -> {
                    // A unary expression
                    val expr = readExpression() ?: return null
                    return BuildConstraintUnaryExpression("!", expr)
                }
                "(" -> {
                    val inner = this.nextToken(")")
                    val expr = fromString(inner) ?: return null

                    return BuildConstraintParenthesis(expr)
                }
                else -> return null
            }
        }

        private fun StringTokenizer.readOperatorCode(): String? {
            if (!this.hasMoreTokens()) {
                return null
            }

            val char1 = this.nextToken()
            if (char1 != "|" && char1 != "&") {
                return null
            }

            if (!this.hasMoreTokens()) {
                return null
            }

            val char2 = this.nextToken()
            if (char2 != char1) {
                return null
            }

            return char1 + char2
        }
    }
}

/**
 * A unary build constraint expression. Currently, only `!` as [operatorCode] for a negation is
 * supported.
 */
class BuildConstraintUnaryExpression(
    val operatorCode: String,
    val expr: BuildConstraintExpression,
) : BuildConstraintExpression {
    override fun evaluate(tags: Set<String>): Boolean {
        if (operatorCode == "!") {
            return !expr.evaluate(tags)
        } else {
            TODO()
        }
    }
}

/**
 * A binary build constraint expression. Currently, only `&&` and `||` as [operatorCode] are
 * supported.
 */
class BuildConstraintBinaryExpression(
    val operatorCode: String,
    val lhs: BuildConstraintExpression,
    val rhs: BuildConstraintExpression,
) : BuildConstraintExpression {
    override fun evaluate(tags: Set<String>): Boolean {
        return when (operatorCode) {
            "&&" -> lhs.evaluate(tags) && rhs.evaluate(tags)
            "||" -> lhs.evaluate(tags) || rhs.evaluate(tags)
            else -> TODO()
        }
    }
}

/**
 * A simple parenthesis around a build constraint, which can be used to logically group expressions.
 */
class BuildConstraintParenthesis(val expr: BuildConstraintExpression) :
    BuildConstraintExpression by expr

/**
 * The usage of a tag as a build constraint expression. This is the simplest form of expression and
 * will return true if the [tag] is contained in the list of available tags.
 */
class BuildConstraintTag(val tag: String) : BuildConstraintExpression {
    override fun evaluate(tags: Set<String>): Boolean {
        return tag in tags
    }
}
