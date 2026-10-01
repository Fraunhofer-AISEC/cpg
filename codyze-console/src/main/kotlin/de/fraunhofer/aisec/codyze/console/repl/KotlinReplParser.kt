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
            val scan = scan(line)
            if (scan.bracketDepth > 0) {
                throw EOFError(-1, cursor, "Unclosed bracket")
            }
            if (scan.incomplete) {
                throw EOFError(-1, cursor, "Unclosed string, char, or comment")
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
     * Result of [scan]: bracket depth at end of input, and whether a string, char, or block comment
     * was left open.
     */
    private data class ScanResult(val bracketDepth: Int, val incomplete: Boolean)

    /** Lexical states tracked by [scan]. */
    private enum class ScanMode {
        NORMAL,
        LINE_COMMENT,
        BLOCK_COMMENT,
        STRING,
        TRIPLE_STRING,
        CHAR,
    }

    /**
     * Single lexical pass over [s] that tracks comments, raw/triple-quoted strings, regular
     * strings, and character literals so that none of them are mistaken for bracket or quote
     * syntax. Unlike scanning brackets and quotes independently, this guarantees both checks agree
     * on what's actually inside a string/comment — e.g. `'"'` (a char literal containing a double
     * quote) or `val x = 1 // "` (a comment containing a quote) no longer confuse the other scan.
     */
    private fun scan(s: String): ScanResult {
        var depth = 0
        var mode = ScanMode.NORMAL
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when (mode) {
                ScanMode.NORMAL ->
                    when {
                        c == '/' && i + 1 < s.length && s[i + 1] == '/' -> {
                            mode = ScanMode.LINE_COMMENT
                            i += 2
                        }
                        c == '/' && i + 1 < s.length && s[i + 1] == '*' -> {
                            mode = ScanMode.BLOCK_COMMENT
                            i += 2
                        }
                        c == '"' && s.startsWith("\"\"\"", i) -> {
                            mode = ScanMode.TRIPLE_STRING
                            i += 3
                        }
                        c == '"' -> {
                            mode = ScanMode.STRING
                            i++
                        }
                        c == '\'' -> {
                            mode = ScanMode.CHAR
                            i++
                        }
                        c == '(' || c == '[' || c == '{' -> {
                            depth++
                            i++
                        }
                        c == ')' || c == ']' || c == '}' -> {
                            depth--
                            i++
                        }
                        else -> i++
                    }
                ScanMode.LINE_COMMENT -> {
                    val nl = s.indexOf('\n', i)
                    i = if (nl < 0) s.length else nl + 1
                    mode = ScanMode.NORMAL
                }
                ScanMode.BLOCK_COMMENT -> {
                    val end = s.indexOf("*/", i)
                    if (end < 0) {
                        i = s.length
                    } else {
                        i = end + 2
                        mode = ScanMode.NORMAL
                    }
                }
                ScanMode.STRING ->
                    when (c) {
                        '\\' -> i += 2
                        '"' -> {
                            mode = ScanMode.NORMAL
                            i++
                        }
                        else -> i++
                    }
                ScanMode.TRIPLE_STRING -> {
                    if (s.startsWith("\"\"\"", i)) {
                        mode = ScanMode.NORMAL
                        i += 3
                    } else {
                        i++
                    }
                }
                ScanMode.CHAR ->
                    when (c) {
                        '\\' -> i += 2
                        '\'' -> {
                            mode = ScanMode.NORMAL
                            i++
                        }
                        else -> i++
                    }
            }
        }
        val unterminated =
            mode == ScanMode.STRING ||
                mode == ScanMode.TRIPLE_STRING ||
                mode == ScanMode.CHAR ||
                mode == ScanMode.BLOCK_COMMENT
        return ScanResult(depth, unterminated)
    }
}
