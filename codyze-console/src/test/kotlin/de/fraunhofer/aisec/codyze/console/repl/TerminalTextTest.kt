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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalTextTest {

    private val esc = "\u001B"
    private val bel = "\u0007"

    @Test
    fun `leaves ordinary text unchanged`() {
        assertEquals("hello, wörld → 42", TerminalText.sanitize("hello, wörld → 42"))
        assertEquals("a\nb\tc", TerminalText.sanitize("a\nb\tc"))
    }

    @Test
    fun `escapes C0, DEL and C1 control characters`() {
        assertEquals("a\\x1b]0;evil\\x07b", TerminalText.sanitize("a$esc]0;evil${bel}b"))
        assertEquals("\\x7f\\x9b", TerminalText.sanitize("\u007F\u009B"))
        assertEquals("a\\x0db", TerminalText.sanitize("a\rb"))
    }

    @Test
    fun `escapes newlines in OSC parameters`() {
        assertEquals("a\\x0ab\\x09c", TerminalText.sanitizeOsc("a\nb\tc"))
    }

    @Test
    fun `OSC helpers cannot be terminated early by their parameter`() {
        val title = TerminalTitle.set("project$bel$esc]0;owned")
        assertEquals(1, title.count { it == '\u0007' })
        assertEquals(1, title.count { it == '\u001B' })
        assertTrue(title.endsWith(bel))

        val notification = TerminalNotification.show("done$bel${esc}[2J")
        assertEquals(1, notification.count { it == '\u0007' })
        assertEquals(1, notification.count { it == '\u001B' })

        val status = TabStatus.set(status = "busy;indicator=#ff0000$bel")
        assertEquals("$esc]21337;status=busy,indicator=#ff0000\\x07$bel", status)
    }

    @Test
    fun `renderer does not emit escape sequences from rendered values`() {
        val renderer = NodeLinkRenderer(color = false, linkScheme = LinkScheme.NONE)

        val node = Reference().apply { name = Name("evil$esc]8;;https://example.com$esc\\") }
        val values =
            listOf(
                "plain $esc[2J string",
                node,
                listOf(node, "x$bel"),
                mapOf("key$esc[31m" to "value"),
            )

        values.forEach { value ->
            val rendered = renderer.render(value)
            assertFalse(
                rendered.contains(esc),
                "escape sequence in ${rendered.replace(esc, "ESC")}",
            )
            assertFalse(rendered.contains(bel), "BEL in $rendered")
        }
    }
}
