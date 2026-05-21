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

import org.jline.terminal.Terminal

/**
 * Coarse light/dark mode hint for the REPL renderer.
 *
 * The default SGR "dim" attribute (`ESC[2m`) renders as light gray on light terminal backgrounds —
 * which is unreadable in VS Code's light theme and similar. With [Theme] threaded through
 * [NodeLinkRenderer], the renderer can swap "dim" for a darker explicit gray on [LIGHT] terminals.
 */
enum class Theme {
    DARK,
    LIGHT,
}

/**
 * Detects the terminal's color theme, in this priority order:
 * 1. The `CODYZE_THEME` env var (`light`, `dark`, or `auto`) — explicit user override.
 * 2. OSC 11 query for the terminal's background color (works in iTerm2, VS Code's integrated
 *    terminal, Ghostty, WezTerm, Alacritty, kitty, modern xterm) — compute luminance and pick.
 * 3. Fall back to [Theme.DARK].
 *
 * The OSC 11 query is best-effort: any I/O error or timeout (default 200 ms) silently falls through
 * to the default rather than blocking REPL startup.
 */
object ThemeDetector {

    // OSC 11 background-color query: ESC ] 11 ; ? ST  (ST = ESC \). Terminal replies with the
    // background color (or doesn't reply at all if unsupported).
    private const val ESC = "\u001B"
    private const val ST = "$ESC\\"
    private const val BEL = "\u0007"
    private const val OSC11_QUERY = "$ESC]11;?$ST"
    private const val QUERY_TIMEOUT_MS = 200L

    fun detect(terminal: Terminal? = null, env: (String) -> String? = System::getenv): Theme {
        env("CODYZE_THEME")?.lowercase()?.let {
            when (it) {
                "light" -> return Theme.LIGHT
                "dark" -> return Theme.DARK
                else -> Unit // "auto" / unrecognized / empty → fall through to auto-detect
            }
        }
        if (terminal != null && terminal.type != Terminal.TYPE_DUMB) {
            queryBackgroundLuminance(terminal)?.let {
                // Boundary at 0.5 matches what most desktop-theme detectors use; the gap between
                // a typical "light" theme (~0.95) and a typical "dark" theme (~0.1) is huge so
                // the exact cutoff doesn't matter much.
                return if (it > 0.5) Theme.LIGHT else Theme.DARK
            }
        }
        return Theme.DARK
    }

    /**
     * Sends an OSC 11 background-color query and parses the response into a relative luminance in
     * `[0, 1]`. Returns null if the terminal doesn't reply within [QUERY_TIMEOUT_MS] or the reply
     * can't be parsed.
     *
     * Expected response shape: `ESC]11;rgb:RRRR/GGGG/BBBB ST` where each channel is 1–4 hex digits
     * and ST is either `ESC\` or BEL (`0x07`). The terminal is held in raw mode while we read so
     * the escape sequence reply doesn't end up on the user's line.
     */
    private fun queryBackgroundLuminance(terminal: Terminal): Double? {
        return try {
            val prev = terminal.attributes
            terminal.enterRawMode()
            try {
                val reader = terminal.reader()
                // Discard anything already buffered (e.g. stray keystrokes during startup) so we
                // don't conflate them with the OSC reply.
                while (reader.available() > 0) reader.read(1L)

                terminal.writer().print(OSC11_QUERY)
                terminal.writer().flush()

                val sb = StringBuilder()
                val deadline = System.currentTimeMillis() + QUERY_TIMEOUT_MS
                while (System.currentTimeMillis() < deadline) {
                    val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
                    val c = reader.read(remaining)
                    if (c < 0) break // EOF or timeout
                    sb.append(c.toChar())
                    if (sb.endsWith(ST) || sb.endsWith(BEL)) break
                }

                val match =
                    Regex("rgb:([0-9a-fA-F]+)/([0-9a-fA-F]+)/([0-9a-fA-F]+)").find(sb)
                        ?: return null
                val (rHex, gHex, bHex) = match.destructured
                val r = normalize(rHex)
                val g = normalize(gHex)
                val b = normalize(bHex)
                // ITU-R BT.601 luma — close enough as a "is this background bright?" heuristic.
                0.299 * r + 0.587 * g + 0.114 * b
            } finally {
                terminal.attributes = prev
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun normalize(hex: String): Double {
        val v = hex.toLong(16)
        val max = (1L shl (4 * hex.length)) - 1
        return v.toDouble() / max.toDouble()
    }
}
