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
package de.fraunhofer.aisec.cpg.frontends.golang.verified

import java.io.ByteArrayOutputStream

/**
 * Canonical S-expressions, the wire format of the verified translation in `cpg-verified` (see
 * `CpgVerified/Wire/Sexp.lean`). Atoms are length-prefixed UTF-8 strings, lists are enclosed in
 * parentheses.
 */
sealed interface Sexp {
    data class Atom(val value: String) : Sexp {
        override fun toString() = value
    }

    data class SList(val items: List<Sexp>) : Sexp {
        override fun toString() = items.joinToString(" ", "(", ")")
    }

    fun encodeTo(out: ByteArrayOutputStream) {
        when (this) {
            is Atom -> {
                val bytes = value.toByteArray()
                out.writeBytes("${bytes.size}:".toByteArray())
                out.writeBytes(bytes)
            }
            is SList -> {
                out.write('('.code)
                items.forEach { it.encodeTo(out) }
                out.write(')'.code)
            }
        }
    }

    companion object {
        /** Parses a sequence of expressions, separated by optional whitespace. */
        fun parseAll(bytes: ByteArray): List<Sexp> {
            val result = mutableListOf<Sexp>()
            var i = 0
            while (i < bytes.size) {
                if (bytes[i].toInt().toChar().isWhitespace()) {
                    i++
                    continue
                }
                val (sexp, next) = parseAt(bytes, i)
                result += sexp
                i = next
            }
            return result
        }

        private fun parseAt(bytes: ByteArray, start: Int): Pair<Sexp, Int> {
            var i = start
            if (bytes[i] == '('.code.toByte()) {
                i++
                val items = mutableListOf<Sexp>()
                while (bytes[i] != ')'.code.toByte()) {
                    val (item, next) = parseAt(bytes, i)
                    items += item
                    i = next
                }
                return SList(items) to i + 1
            }

            val colon = (i until bytes.size).first { bytes[it] == ':'.code.toByte() }
            val length = String(bytes, i, colon - i).toInt()
            return Atom(String(bytes, colon + 1, length)) to colon + 1 + length
        }
    }
}

fun atom(value: Any) = Sexp.Atom(value.toString())

fun sexpOf(vararg items: Sexp) = Sexp.SList(items.toList())

fun sexpOf(items: List<Sexp>) = Sexp.SList(items)
