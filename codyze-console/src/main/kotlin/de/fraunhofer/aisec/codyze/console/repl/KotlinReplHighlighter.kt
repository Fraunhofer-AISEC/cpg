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

import java.util.regex.Pattern
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

/**
 * Light syntax highlighter for Kotlin REPL input. Colors keywords, string literals, numeric
 * literals, line comments, and `:`-prefixed meta-commands.
 *
 * One pass over the buffer with a single combined regex — cheap enough to run on every keystroke.
 */
class KotlinReplHighlighter : Highlighter {

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        val sb = AttributedStringBuilder()

        // Meta-commands like `:help` are highlighted as a whole-line token.
        if (buffer.startsWith(":")) {
            sb.style(STYLE_META).append(buffer).style(AttributedStyle.DEFAULT)
            return sb.toAttributedString()
        }

        val matcher = TOKEN_PATTERN.matcher(buffer)
        var cursor = 0
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                sb.style(AttributedStyle.DEFAULT).append(buffer, cursor, matcher.start())
            }
            val style =
                when {
                    matcher.group("kw") != null -> STYLE_KEYWORD
                    matcher.group("str") != null -> STYLE_STRING
                    matcher.group("num") != null -> STYLE_NUMBER
                    matcher.group("cmt") != null -> STYLE_COMMENT
                    else -> AttributedStyle.DEFAULT
                }
            sb.style(style).append(buffer, matcher.start(), matcher.end())
            cursor = matcher.end()
        }
        if (cursor < buffer.length) {
            sb.style(AttributedStyle.DEFAULT).append(buffer, cursor, buffer.length)
        }
        sb.style(AttributedStyle.DEFAULT)
        return sb.toAttributedString()
    }

    override fun setErrorPattern(errorPattern: Pattern?) {
        // No-op: we don't currently flag errors inline.
    }

    override fun setErrorIndex(errorIndex: Int) {
        // No-op.
    }

    companion object {
        // Subset of Kotlin's hard + common soft keywords. `\b` anchors avoid matching inside
        // identifiers (e.g. `variable` would otherwise match `var`).
        private val KEYWORDS =
            listOf(
                "as",
                "break",
                "class",
                "continue",
                "do",
                "else",
                "false",
                "for",
                "fun",
                "if",
                "import",
                "in",
                "interface",
                "is",
                "null",
                "object",
                "package",
                "return",
                "super",
                "this",
                "throw",
                "true",
                "try",
                "typealias",
                "val",
                "var",
                "when",
                "while",
                "by",
                "catch",
                "finally",
                "get",
                "set",
                "where",
                "companion",
                "data",
                "enum",
                "internal",
                "lateinit",
                "open",
                "override",
                "private",
                "protected",
                "public",
                "sealed",
                "suspend",
                "vararg",
            )

        private val TOKEN_PATTERN: Pattern =
            Pattern.compile(
                buildString {
                    append("(?<kw>\\b(?:")
                    append(KEYWORDS.joinToString("|"))
                    append(")\\b)")
                    // Double-quoted string (handles escaped quotes); also triple-quoted raw string.
                    append("|(?<str>\"\"\"(?:[^\"\\\\]|\\\\.|\"(?!\"\"))*\"\"\"")
                    append("|\"(?:[^\"\\\\\\n]|\\\\.)*\")")
                    // Numeric literal: int, long (L), float (f/F), double; supports hex and binary.
                    append("|(?<num>\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|[0-9][0-9_]*")
                    append("(?:\\.[0-9_]+)?(?:[eE][+-]?[0-9]+)?)[LfFdD]?\\b)")
                    // Line comment to end-of-buffer.
                    append("|(?<cmt>//[^\\n]*)")
                }
            )

        // Foreground colors chosen to be readable on both light and dark terminals.
        private val STYLE_KEYWORD = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)
        private val STYLE_STRING = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
        private val STYLE_NUMBER = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)
        private val STYLE_COMMENT =
            AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT).faint()
        private val STYLE_META = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold()
    }
}
