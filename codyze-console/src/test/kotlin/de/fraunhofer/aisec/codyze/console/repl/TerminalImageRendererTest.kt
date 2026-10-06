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

import java.io.File
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalImageRendererTest {

    private val esc = "\u001B"
    private val bel = "\u0007"
    private val st = "$esc\\"

    @Test
    fun `detects Kitty via KITTY_WINDOW_ID`() {
        val env = mapOf("KITTY_WINDOW_ID" to "1")
        assertEquals(ImageProtocol.KITTY, TerminalImageSupport.detect { env[it] })
    }

    @Test
    fun `detects Kitty via TERM`() {
        val env = mapOf("TERM" to "xterm-kitty")
        assertEquals(ImageProtocol.KITTY, TerminalImageSupport.detect { env[it] })
    }

    @Test
    fun `detects Kitty for Ghostty`() {
        val env = mapOf("TERM_PROGRAM" to "ghostty")
        assertEquals(ImageProtocol.KITTY, TerminalImageSupport.detect { env[it] })
    }

    @Test
    fun `detects iTerm2`() {
        val env = mapOf("TERM_PROGRAM" to "iTerm.app")
        assertEquals(ImageProtocol.ITERM2, TerminalImageSupport.detect { env[it] })
    }

    @Test
    fun `detects iTerm2 protocol for WezTerm`() {
        val env = mapOf("TERM_PROGRAM" to "WezTerm")
        assertEquals(ImageProtocol.ITERM2, TerminalImageSupport.detect { env[it] })
    }

    @Test
    fun `returns null for unrecognized terminals`() {
        val env = mapOf("TERM_PROGRAM" to "Apple_Terminal", "TERM" to "xterm-256color")
        assertNull(TerminalImageSupport.detect { env[it] })
    }

    @Test
    fun `returns null when no env vars are set`() {
        assertNull(TerminalImageSupport.detect { null })
    }

    @Test
    fun `iTerm2 encoding wraps base64 payload in OSC 1337 with a BEL terminator`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val file = File.createTempFile("test-image-", ".png").apply { writeBytes(bytes) }

        val sequence = TerminalImageEncoder.encode(file, ImageProtocol.ITERM2)

        val expectedBase64 = Base64.getEncoder().encodeToString(bytes)
        assertTrue(sequence.startsWith("$esc]1337;File="))
        assertTrue(sequence.contains("size=${bytes.size}"))
        assertTrue(sequence.contains(":$expectedBase64"))
        assertTrue(sequence.endsWith(bel))
    }

    @Test
    fun `Kitty encoding chunks large payloads with correct continuation markers`() {
        // Large enough to require multiple 4096-byte base64 chunks.
        val bytes = ByteArray(10_000) { it.toByte() }
        val file = File.createTempFile("test-image-", ".png").apply { writeBytes(bytes) }

        val sequence = TerminalImageEncoder.encode(file, ImageProtocol.KITTY)

        // Split on the ST terminator to recover each chunk (dropping the trailing empty
        // string after the final terminator).
        val chunks = sequence.split(st).filter { it.isNotEmpty() }
        assertTrue(chunks.size >= 2, "expected multiple chunks for a 10KB payload")
        assertTrue(chunks.first().startsWith("${esc}_Ga=T,f=100,m=1;"))
        assertTrue(chunks.last().startsWith("${esc}_Gm=0;"))
        chunks.drop(1).dropLast(1).forEach { assertTrue(it.startsWith("${esc}_Gm=1;")) }
    }
}
