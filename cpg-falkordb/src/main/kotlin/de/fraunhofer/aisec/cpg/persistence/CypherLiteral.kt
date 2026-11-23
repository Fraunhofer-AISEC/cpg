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
package de.fraunhofer.aisec.cpg.persistence

import kotlin.uuid.Uuid

/**
 * FalkorDB does not have a binary channel for query parameters. Instead, parameters are prepended
 * to the query itself in a `CYPHER <name>=<literal> ...` preamble, which the server then parses.
 * Client libraries are therefore responsible for turning values into valid Cypher literals.
 *
 * The Java client ships such a serializer, but it only supports scalars and lists — not maps. Since
 * we persist a chunk of nodes as a *list of property maps*, we render the literals ourselves.
 *
 * Note that this is not string concatenation of untrusted input into a query: every value is
 * rendered as a self-contained, properly escaped literal, so a value can never escape into the
 * query structure. [CypherLiteralTest] covers the escaping rules.
 */
object CypherLiteral {

    /**
     * Renders [value] as a Cypher literal.
     *
     * Supported types are `null`, [Boolean], [Number], [String], [Char], [Enum], [Uuid], [Map],
     * [Collection] and arrays. Everything else falls back to its string representation, which
     * mirrors what the CPG would store as a property value anyway.
     */
    fun render(value: Any?): String {
        return when (value) {
            null -> "null"
            is Boolean -> value.toString()
            // Double.toString may produce "NaN" or "Infinity", which are not valid Cypher literals
            is Double -> if (value.isFinite()) value.toString() else "null"
            is Float -> if (value.isFinite()) value.toString() else "null"
            is Number -> value.toString()
            is String -> renderString(value)
            is Char -> renderString(value.toString())
            is Enum<*> -> renderString(value.name)
            is Uuid -> renderString(value.toString())
            is Map<*, *> -> renderMap(value)
            is Collection<*> -> value.joinToString(", ", "[", "]") { render(it) }
            is Array<*> -> value.joinToString(", ", "[", "]") { render(it) }
            is BooleanArray -> value.joinToString(", ", "[", "]") { render(it) }
            is ByteArray -> value.joinToString(", ", "[", "]") { render(it) }
            is CharArray -> value.joinToString(", ", "[", "]") { render(it) }
            is ShortArray -> value.joinToString(", ", "[", "]") { render(it) }
            is IntArray -> value.joinToString(", ", "[", "]") { render(it) }
            is LongArray -> value.joinToString(", ", "[", "]") { render(it) }
            is FloatArray -> value.joinToString(", ", "[", "]") { render(it) }
            is DoubleArray -> value.joinToString(", ", "[", "]") { render(it) }
            else -> renderString(value.toString())
        }
    }

    /**
     * Renders a map as a Cypher map literal. Keys are always quoted with backticks, so that
     * property names which happen to collide with Cypher keywords are handled correctly. Entries
     * with a `null` value are skipped, since FalkorDB does not store `null` properties anyway.
     */
    private fun renderMap(map: Map<*, *>): String {
        return map.entries
            .filter { it.value != null }
            .joinToString(", ", "{", "}") { (key, value) ->
                "${renderIdentifier(key.toString())}: ${render(value)}"
            }
    }

    /**
     * Quotes an identifier (a property name, label or relationship type) with backticks. A backtick
     * inside the identifier is escaped by doubling it, as required by Cypher.
     */
    fun renderIdentifier(identifier: String): String {
        return "`${identifier.replace("`", "``")}`"
    }

    /**
     * Renders a string as a double-quoted Cypher literal.
     *
     * FalkorDB's Cypher lexer understands the `\\`, `\"`, `\n`, `\r` and `\t` escape sequences, but
     * *not* `\uXXXX`. Any other character — including raw control characters and multi-byte UTF-8 —
     * is passed through verbatim, which is safe because the query is transported as a binary-safe
     * Redis bulk string.
     */
    private fun renderString(value: String): String {
        val builder = StringBuilder(value.length + 2)
        builder.append('"')
        for (char in value) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(char)
            }
        }
        builder.append('"')
        return builder.toString()
    }

    /**
     * Builds a query that binds [parameters] in a `CYPHER` preamble and then runs [query].
     *
     * Keeping the parameters out of the query body means that the body itself stays identical
     * across all chunks, which allows FalkorDB to reuse the cached execution plan.
     */
    fun withParameters(parameters: Map<String, Any?>, query: String): String {
        if (parameters.isEmpty()) {
            return query
        }

        return parameters.entries.joinToString(
            separator = " ",
            prefix = "CYPHER ",
            postfix = " $query",
        ) { (name, value) ->
            "$name=${render(value)}"
        }
    }
}
