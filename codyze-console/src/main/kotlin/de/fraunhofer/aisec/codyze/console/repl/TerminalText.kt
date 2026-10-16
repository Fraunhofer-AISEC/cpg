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

/**
 * Makes untrusted text safe to print to a terminal.
 *
 * Text that originates from an analyzed project (names, string literals, file paths, query
 * descriptions) may contain control characters. Printed verbatim, these could emit escape sequences
 * of their own — e.g. a forged OSC 8 hyperlink, a changed window title or a cleared screen. Every
 * piece of such text must therefore pass through this encoder before it is combined with the escape
 * sequences that the REPL itself emits.
 */
object TerminalText {

    /**
     * Replaces control characters in [text] with a visible `\xNN` escape: C0 controls (including
     * ESC and BEL), DEL and C1 controls (`0x80`–`0x9F`, which some terminals interpret like
     * ESC-sequences). Newlines and tabs are kept if [allowNewlines] is `true`.
     */
    fun sanitize(text: String, allowNewlines: Boolean = true): String {
        if (text.none { it.isControl(allowNewlines) }) return text
        return buildString(text.length) {
            for (c in text) {
                if (c.isControl(allowNewlines)) {
                    append("\\x").append(c.code.toString(16).padStart(2, '0'))
                } else {
                    append(c)
                }
            }
        }
    }

    /**
     * Sanitizes [text] for use as a parameter of an OSC sequence (e.g. a window title), where not
     * even a newline may appear.
     */
    fun sanitizeOsc(text: String): String = sanitize(text, allowNewlines = false)

    private fun Char.isControl(allowNewlines: Boolean): Boolean =
        when {
            allowNewlines && (this == '\n' || this == '\t') -> false
            else -> code in 0x00..0x1F || code == 0x7F || code in 0x80..0x9F
        }
}
