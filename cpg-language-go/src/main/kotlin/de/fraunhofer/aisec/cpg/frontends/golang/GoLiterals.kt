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
package de.fraunhofer.aisec.cpg.frontends.golang

import java.io.ByteArrayOutputStream
import java.math.BigInteger

/**
 * Parses the source text of a Go [integer literal](https://go.dev/ref/spec#Integer_literals):
 * decimal, `0x` hexadecimal, `0o` octal, `0b` binary or a legacy octal literal with a leading `0`.
 * Returns `null` if the literal is malformed.
 */
fun parseGoIntLiteral(raw: String): BigInteger? {
    val digits = raw.replace("_", "")
    val (radix, body) =
        if (digits.length > 1 && digits[0] == '0') {
            when (digits[1]) {
                'x',
                'X' -> 16 to digits.substring(2)
                'o',
                'O' -> 8 to digits.substring(2)
                'b',
                'B' -> 2 to digits.substring(2)
                else -> 8 to digits.substring(1)
            }
        } else {
            10 to digits
        }

    return body.toBigIntegerOrNull(radix)
}

/**
 * Parses the source text of a Go [string literal](https://go.dev/ref/spec#String_literals). Raw
 * strings (in back quotes) drop carriage returns, interpreted strings (in double quotes) have their
 * escape sequences decoded. Returns `null` if the literal is malformed.
 */
fun parseGoStringLiteral(raw: String): String? {
    if (raw.length < 2 || raw.first() != raw.last()) {
        return null
    }

    val body = raw.substring(1, raw.length - 1)
    return when (raw.first()) {
        '`' -> body.replace("\r", "")
        '"' -> {
            // Go strings are byte sequences: \x and octal escapes denote single bytes, everything
            // else is UTF-8 encoded
            val bytes = ByteArrayOutputStream()
            val decoded =
                unescapeGo(
                    body,
                    onCodePoint = { bytes.writeBytes(String(Character.toChars(it)).toByteArray()) },
                    onByte = { bytes.write(it) },
                )
            if (decoded) bytes.toString(Charsets.UTF_8) else null
        }
        else -> null
    }
}

/**
 * Parses the source text of a Go [rune literal](https://go.dev/ref/spec#Rune_literals) into its
 * Unicode code point. Returns `null` if the literal is malformed.
 */
fun parseGoRuneLiteral(raw: String): Int? {
    if (raw.length < 3 || raw.first() != '\'' || raw.last() != '\'') {
        return null
    }

    val values = mutableListOf<Int>()
    val decoded =
        unescapeGo(
            raw.substring(1, raw.length - 1),
            onCodePoint = { values += it },
            onByte = { values += it },
        )
    return if (decoded && values.size == 1) values.single() else null
}

/**
 * Decodes the escape sequences in the body of an interpreted string or rune literal. Characters and
 * `\u`/`\U` escapes are reported as code points, `\x` and octal escapes as bytes. Returns `false`
 * if the body contains a malformed escape sequence.
 */
private fun unescapeGo(body: String, onCodePoint: (Int) -> Unit, onByte: (Int) -> Unit): Boolean {
    var i = 0
    while (i < body.length) {
        val c = body.codePointAt(i)
        if (c != '\\'.code) {
            onCodePoint(c)
            i += Character.charCount(c)
            continue
        }

        val escape = body.getOrNull(i + 1) ?: return false
        i += 2

        // Reads `n` digits in the given radix starting at `start`
        fun digits(start: Int, n: Int, radix: Int): Int? =
            body
                .substring(start, (start + n).coerceAtMost(body.length))
                .takeIf { it.length == n }
                ?.toIntOrNull(radix)

        when (escape) {
            'a' -> onCodePoint(0x07)
            'b' -> onCodePoint(0x08)
            'f' -> onCodePoint(0x0C)
            'n' -> onCodePoint(0x0A)
            'r' -> onCodePoint(0x0D)
            't' -> onCodePoint(0x09)
            'v' -> onCodePoint(0x0B)
            '\\',
            '\'',
            '"' -> onCodePoint(escape.code)
            'x' -> {
                onByte(digits(i, 2, 16) ?: return false)
                i += 2
            }
            'u' -> {
                onCodePoint(digits(i, 4, 16) ?: return false)
                i += 4
            }
            'U' -> {
                val codePoint = digits(i, 8, 16) ?: return false
                if (!Character.isValidCodePoint(codePoint)) return false
                onCodePoint(codePoint)
                i += 8
            }
            in '0'..'7' -> {
                val value = digits(i - 1, 3, 8) ?: return false
                if (value > 255) return false
                onByte(value)
                i += 2
            }
            else -> return false
        }
    }

    return true
}
