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
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeLinkRendererTest {

    private val esc = "\u001B"

    private fun plain(scheme: LinkScheme = LinkScheme.NONE) =
        NodeLinkRenderer(color = false, linkScheme = scheme)

    private fun node(name: String, uri: String? = null, line: Int = 3, column: Int = 5) =
        Reference().apply {
            this.name = Name(name)
            if (uri != null) {
                location = PhysicalLocation(URI(uri), Region(line, column, line, column + 1))
            }
        }

    @Test
    fun `renders null and scalars`() {
        assertEquals("null", plain().render(null))
        assertEquals("42", plain().render(42))
        assertEquals("text", plain().render("text"))
    }

    @Test
    fun `renders a node with its kind and a file line`() {
        val rendered = plain().render(node("x", "file:///src/dir/a.kt"))
        assertContains(rendered, "x : Reference")
        assertContains(rendered, "a.kt:3")
    }

    @Test
    fun `without color there is no escape sequence, not even a hyperlink`() {
        val rendered = plain(LinkScheme.FILE).render(node("x", "file:///src/a.kt"))
        assertFalse(rendered.contains(esc), rendered)
    }

    @Test
    fun `with color the location is an OSC 8 hyperlink for the configured scheme`() {
        val renderer = NodeLinkRenderer(color = true, linkScheme = LinkScheme.VSCODE)
        val rendered = renderer.render(node("x", "file:///src/a.kt", line = 3, column = 5))
        assertContains(rendered, "$esc]8;;vscode://file/src/a.kt:3:5")
    }

    @Test
    fun `a control character in a path is neither shown nor part of the link target`() {
        val renderer = NodeLinkRenderer(color = true, linkScheme = LinkScheme.FILE)
        val rendered = renderer.render(node("x", "file:///src/a%1B%5D8;;evil%07.kt"))
        // The only escapes left are the ones the renderer emits itself: the OSC 8 open/close
        // of one link, the colors and the reset.
        assertFalse(rendered.contains("\u0007evil"), rendered)
        assertFalse(rendered.contains("$esc]8;;evil"), rendered)
        assertContains(rendered, "\\x1b")
    }

    @Test
    fun `a location without a line is just the uri`() {
        val n = node("x", "file:///src/a.kt", line = 0)
        assertContains(plain().render(n), "file:///src/a.kt")
    }

    @Test
    fun `renders collections with a count and an index per item`() {
        val rendered = plain().render(listOf("a", "b"))
        assertContains(rendered, "(2 items)")
        assertContains(rendered, "[0] a")
        assertContains(rendered, "[1] b")
        assertEquals("[]", plain().render(emptyList<String>()))
    }

    @Test
    fun `long collections are capped`() {
        val rendered = plain().render(List(500) { it })
        assertContains(rendered, "(500 items)")
        assertContains(rendered, "more")
        assertTrue(rendered.lines().size < 500, "all ${rendered.lines().size} lines rendered")
    }

    @Test
    fun `renders maps and arrays`() {
        val map = plain().render(mapOf("k" to 1))
        assertContains(map, "k -> 1")
        assertEquals("{}", plain().render(emptyMap<String, Int>()))
        assertContains(plain().render(arrayOf("a", "b")), "(2 items)")
    }

    @Test
    fun `link schemes build urls`() {
        assertEquals("file:///a.kt#L3", LinkScheme.FILE.urlFor("/a.kt", 3, 5))
        assertEquals("vscode://file/a.kt:3:5", LinkScheme.VSCODE.urlFor("/a.kt", 3, 5))
        assertEquals("vscode://file/a.kt:3", LinkScheme.VSCODE.urlFor("/a.kt", 3, 0))
        assertEquals(null, LinkScheme.NONE.urlFor("/a.kt", 3, 5))
    }

    @Test
    fun `the link scheme can be chosen from the environment`() {
        assertEquals(
            LinkScheme.VSCODE,
            LinkScheme.detect { if (it == "CODYZE_LINK_SCHEME") "VSCode" else null },
        )
        assertEquals(
            LinkScheme.NONE,
            LinkScheme.detect { if (it == "CODYZE_LINK_SCHEME") "none" else null },
        )
        assertEquals(
            LinkScheme.VSCODE,
            LinkScheme.detect { if (it == "TERM_PROGRAM") "vscode" else null },
        )
        assertEquals(LinkScheme.FILE, LinkScheme.detect { null })
    }
}
