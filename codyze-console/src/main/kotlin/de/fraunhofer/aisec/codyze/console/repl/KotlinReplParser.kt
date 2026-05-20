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
package de.fraunhofer.aisec.codyze.console.repl

import org.jline.reader.EOFError
import org.jline.reader.ParsedLine
import org.jline.reader.Parser
import org.jline.reader.impl.DefaultParser

/**
 * JLine [Parser] that lets the user keep typing across multiple lines while braces, parens, or
 * brackets are still open — important for Kotlin lambdas, `if/else` blocks, and the chained query
 * idioms (`result.functions.filter { … }`).
 *
 * When called by the line reader in [Parser.ParseContext.ACCEPT_LINE] context (i.e. the user
 * pressed Enter), we throw [EOFError] if the input is unterminated, which signals JLine to render a
 * continuation prompt and read the next line. For [Parser.ParseContext.COMPLETE] context (TAB
 * press) we delegate to [DefaultParser] to get word boundaries.
 */
class KotlinReplParser : Parser {

    private val wordSplitter = DefaultParser().apply { setEscapeChars(charArrayOf()) }

    override fun parse(line: String, cursor: Int, context: Parser.ParseContext): ParsedLine {
        if (context == Parser.ParseContext.ACCEPT_LINE) {
            val balance = bracketBalance(line)
            if (balance > 0) {
                throw EOFError(-1, cursor, "Unclosed bracket")
            }
            if (insideOpenString(line)) {
                throw EOFError(-1, cursor, "Unclosed string")
            }
        }
        // For completion, we need '.' and other Kotlin punctuation to act as word
        // boundaries — otherwise JLine treats `result.` as one word and tries to match
        // candidates against that whole prefix (none of `functions`, `calls`, … start with
        // `result.`, so the menu stays empty). DefaultParser splits only on whitespace.
        if (context == Parser.ParseContext.COMPLETE) {
            return completionParsedLine(line, cursor)
        }
        return wordSplitter.parse(line, cursor, context)
    }

    /**
     * Produces a [ParsedLine] where the "word being completed" is just the text from the most
     * recent word-boundary (whitespace, `.`, `(`, `[`, `{`, `,`, `=`, `<`, `>`) up to the cursor.
     * The IDE-services completer already does its own context analysis on the full [line], so it
     * always returns candidates relative to the cursor position — we only need to give JLine the
     * right "current word" so it doesn't filter them out.
     */
    private fun completionParsedLine(line: String, cursor: Int): ParsedLine {
        var wordStart = cursor
        while (wordStart > 0 && !isWordBoundary(line[wordStart - 1])) wordStart--
        val word = line.substring(wordStart, cursor)
        val wordCursor = cursor - wordStart
        return object : ParsedLine {
            override fun word(): String = word

            override fun wordCursor(): Int = wordCursor

            override fun wordIndex(): Int = 0

            override fun words(): List<String> = listOf(word)

            override fun line(): String = line

            override fun cursor(): Int = cursor
        }
    }

    private fun isWordBoundary(c: Char): Boolean =
        c.isWhitespace() ||
            c == '.' ||
            c == '(' ||
            c == '[' ||
            c == '{' ||
            c == ',' ||
            c == '=' ||
            c == '<' ||
            c == '>' ||
            c == '!' ||
            c == ':' ||
            c == ';' ||
            c == '?' ||
            c == '|' ||
            c == '&' ||
            c == '+' ||
            c == '-' ||
            c == '*' ||
            c == '/'

    /**
     * Returns the bracket depth at end of input. Comments and string literals are skipped so `"{"`,
     * `/* { */` and `// {` don't push the depth up.
     */
    private fun bracketBalance(s: String): Int {
        var depth = 0
        var i = 0
        while (i < s.length) {
            when (val c = s[i]) {
                '/' ->
                    when {
                        i + 1 < s.length && s[i + 1] == '/' -> {
                            i = s.indexOf('\n', i).let { if (it < 0) s.length else it }
                            continue
                        }
                        i + 1 < s.length && s[i + 1] == '*' -> {
                            val end = s.indexOf("*/", i + 2)
                            i = if (end < 0) s.length else end + 2
                            continue
                        }
                    }
                '"' -> {
                    if (i + 2 < s.length && s[i + 1] == '"' && s[i + 2] == '"') {
                        val end = s.indexOf("\"\"\"", i + 3)
                        i = if (end < 0) s.length else end + 3
                        continue
                    }
                    val end = findStringEnd(s, i + 1)
                    i = if (end < 0) s.length else end + 1
                    continue
                }
                '\'' -> {
                    val end = findStringEnd(s, i + 1)
                    i = if (end < 0) s.length else end + 1
                    continue
                }
                '(',
                '[',
                '{' -> depth++
                ')',
                ']',
                '}' -> depth--
                else -> Unit
            }
            i++
        }
        return depth
    }

    private fun insideOpenString(s: String): Boolean {
        // Quick heuristic: count unescaped " chars. Odd → unterminated.
        var i = 0
        var open = false
        while (i < s.length) {
            val c = s[i]
            if (c == '"') {
                if (i + 2 < s.length && s[i + 1] == '"' && s[i + 2] == '"') {
                    val end = s.indexOf("\"\"\"", i + 3)
                    if (end < 0) return true
                    i = end + 3
                    continue
                }
                open = !open
            } else if (c == '\\' && open) {
                i++ // skip escaped char
            }
            i++
        }
        return open
    }

    private fun findStringEnd(s: String, from: Int): Int {
        var i = from
        while (i < s.length) {
            when (s[i]) {
                '\\' -> i++ // skip escaped char
                '"',
                '\'' -> return i
            }
            i++
        }
        return -1
    }
}
