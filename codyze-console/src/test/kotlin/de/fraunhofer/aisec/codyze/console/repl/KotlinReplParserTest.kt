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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jline.reader.EOFError
import org.jline.reader.Parser

class KotlinReplParserTest {

    private val parser = KotlinReplParser()

    private fun accept(line: String) =
        parser.parse(line, line.length, Parser.ParseContext.ACCEPT_LINE)

    private fun assertComplete(line: String) {
        accept(line)
    }

    private fun assertIncomplete(line: String) {
        assertFailsWith<EOFError>("expected '$line' to ask for more input") { accept(line) }
    }

    @Test
    fun `plain statements are complete`() {
        assertComplete("result.functions.size")
        assertComplete("val x = 1")
        assertComplete("foo(bar[1], { x -> x })")
    }

    @Test
    fun `an open bracket asks for more input`() {
        assertIncomplete("foo(")
        assertIncomplete("val xs = listOf(1,\n2")
        assertIncomplete("result.calls.filter {")
        assertIncomplete("a[")
    }

    @Test
    fun `a closing bracket without an opening one is left to the compiler`() {
        assertComplete(")")
    }

    @Test
    fun `an unterminated string or char asks for more input`() {
        assertIncomplete("val s = \"abc")
        assertIncomplete("val s = \"\"\"abc")
        assertIncomplete("val c = '")
    }

    @Test
    fun `brackets inside strings, chars and comments are ignored`() {
        assertComplete("val s = \"(\"")
        assertComplete("val s = \"\"\"(\n[\n{\"\"\"")
        assertComplete("val c = '('")
        assertComplete("val x = 1 // (")
        assertComplete("val x = 1 /* ( [ { */")
    }

    @Test
    fun `quotes inside other literals and comments do not open a string`() {
        // a double quote in a char literal, an apostrophe in a string, a quote in a comment
        assertComplete("val c = '\"'")
        assertComplete("val s = \"it's\"")
        assertComplete("val x = 1 // \"")
        assertComplete("val x = 1 /* \" ' */")
        assertComplete("val s = \"\"\"a \" b\"\"\"")
    }

    @Test
    fun `escapes do not end a string or char`() {
        assertComplete("val s = \"a\\\"b\"")
        assertIncomplete("val s = \"a\\\"")
        assertComplete("val c = '\\''")
    }

    @Test
    fun `an unterminated block comment asks for more input`() {
        assertIncomplete("/* comment")
        assertIncomplete("val x = 1 /* a")
        assertComplete("/* comment */")
    }

    @Test
    fun `block comments nest`() {
        assertIncomplete("/* outer /* inner */")
        assertIncomplete("/* a /* b /* c */ */")
        assertComplete("/* outer /* inner */ done */")
    }

    @Test
    fun `a line comment ends at the end of the line`() {
        assertIncomplete("foo( // )")
        assertComplete("foo( // comment\n)")
    }

    @Test
    fun `completion splits words at punctuation`() {
        val line = "result.functions.fil"
        val parsed = parser.parse(line, line.length, Parser.ParseContext.COMPLETE)
        assertEquals("fil", parsed.word())
        assertEquals(3, parsed.wordCursor())

        val afterDot = "result."
        assertEquals(
            "",
            parser.parse(afterDot, afterDot.length, Parser.ParseContext.COMPLETE).word(),
        )

        val inCall = "foo(bar, ba"
        assertEquals("ba", parser.parse(inCall, inCall.length, Parser.ParseContext.COMPLETE).word())
    }

    @Test
    fun `an incomplete line is accepted while completing`() {
        // Only ACCEPT_LINE may ask for a continuation; TAB on an open bracket must still complete
        val line = "foo(bar."
        assertEquals("", parser.parse(line, line.length, Parser.ParseContext.COMPLETE).word())
    }
}
