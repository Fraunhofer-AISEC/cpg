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
import java.util.concurrent.TimeUnit

/** Terminal inline-image protocols we know how to speak. */
enum class ImageProtocol {
    ITERM2,
    KITTY,
}

/**
 * Detects whether the host terminal supports an inline-image protocol, purely from environment
 * variables — no escape-sequence round-trip (unlike [ThemeDetector]), since there's no single query
 * all three protocols answer consistently. Returns `null` when nothing is recognized, in which case
 * callers should fall back to writing a file and opening it externally.
 */
object TerminalImageSupport {

    fun detect(env: (String) -> String? = System::getenv): ImageProtocol? {
        val termProgram = env("TERM_PROGRAM").orEmpty()
        val term = env("TERM").orEmpty()
        return when {
            // Kitty itself, and other terminals (Ghostty, recent Konsole) that adopted its
            // graphics protocol.
            env("KITTY_WINDOW_ID") != null -> ImageProtocol.KITTY
            term == "xterm-kitty" -> ImageProtocol.KITTY
            termProgram == "ghostty" -> ImageProtocol.KITTY
            // iTerm2's protocol, also adopted by WezTerm.
            termProgram == "iTerm.app" -> ImageProtocol.ITERM2
            termProgram == "WezTerm" -> ImageProtocol.ITERM2
            else -> null
        }
    }
}

/**
 * Best-effort wrapper around the `mmdc` (mermaid-cli) binary. Rendering Mermaid diagrams requires a
 * real DOM/layout engine (dagre, etc.) — there's no pure-JVM equivalent — so we shell out to the
 * user's own install rather than bundling Puppeteer/Chromium with Codyze. Everything here is
 * best-effort: a missing binary, a sandboxing failure, or a timeout all just mean "no inline
 * image", never a hard error.
 */
object MermaidCli {

    private const val RENDER_TIMEOUT_SECONDS = 20L

    /** Cached: whether `mmdc` is runnable on this machine. Checked once per process. */
    val isAvailable: Boolean by lazy {
        runCatching {
                val process =
                    ProcessBuilder("mmdc", "--version")
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectErrorStream(true)
                        .start()
                process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
            }
            .getOrDefault(false)
    }

    /**
     * Renders [mermaidSource] to a PNG via `mmdc`, using a transparent background so the image
     * blends with the terminal's own background regardless of [theme]. Returns `null` on any
     * failure — missing binary, non-zero exit, or timeout.
     */
    fun renderToPng(mermaidSource: String, theme: Theme): File? =
        runCatching {
                val input =
                    File.createTempFile("codyze-dfg-", ".mmd").apply {
                        writeText(mermaidSource)
                        deleteOnExit()
                    }
                val output = File.createTempFile("codyze-dfg-", ".png").apply { deleteOnExit() }
                val mermaidTheme = if (theme == Theme.DARK) "dark" else "default"
                val process =
                    ProcessBuilder(
                            "mmdc",
                            "-i",
                            input.absolutePath,
                            "-o",
                            output.absolutePath,
                            "-b",
                            "transparent",
                            "-t",
                            mermaidTheme,
                        )
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectErrorStream(true)
                        .start()
                val finished = process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    return@runCatching null
                }
                if (process.exitValue() != 0 || !output.exists() || output.length() == 0L) {
                    return@runCatching null
                }
                output
            }
            .getOrNull()
}

/** Encodes a PNG [file] as an inline-image escape sequence for the given [protocol]. */
object TerminalImageEncoder {

    // Kitty recommends chunking base64 payloads so no single escape sequence exceeds ~4KB.
    private const val KITTY_CHUNK_SIZE = 4096

    fun encode(file: File, protocol: ImageProtocol): String {
        val bytes = file.readBytes()
        val base64 = Base64.getEncoder().encodeToString(bytes)
        return when (protocol) {
            ImageProtocol.ITERM2 -> encodeIterm2(base64, bytes.size)
            ImageProtocol.KITTY -> encodeKitty(base64)
        }
    }

    private fun encodeIterm2(base64: String, byteCount: Int): String {
        val args = "inline=1;size=$byteCount;width=auto;preserveAspectRatio=1"
        return "]1337;File=$args:$base64"
    }

    private fun encodeKitty(base64: String): String {
        val chunks = base64.chunked(KITTY_CHUNK_SIZE)
        val sb = StringBuilder()
        chunks.forEachIndexed { index, chunk ->
            val more = if (index == chunks.lastIndex) 0 else 1
            val control = if (index == 0) "a=T,f=100,m=$more" else "m=$more"
            sb.append("_G").append(control).append(';').append(chunk).append("\\")
        }
        return sb.toString()
    }
}
