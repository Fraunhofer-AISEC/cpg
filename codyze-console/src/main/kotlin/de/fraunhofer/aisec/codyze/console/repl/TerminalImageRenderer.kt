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
 * Sets iTerm2's session badge — a persistent watermark drawn in the corner of the terminal pane,
 * independent of scrollback/prompt — via the `SetBadgeFormat` OSC 1337 subcommand (the same family
 * used for inline images). Used by [ReplLoop] to show the REPL's live idle/evaluating state,
 * similar in spirit to how iTerm2's own Claude Code integration drives its Session Status tool, but
 * using only the plain escape-sequence protocol rather than iTerm2's Python API — no setup required
 * beyond detecting [ImageProtocol.ITERM2].
 *
 * Only call this once [TerminalImageSupport.detect] has returned [ImageProtocol.ITERM2] — it's an
 * iTerm2-proprietary sequence, and unlike inline images there's no Kitty/Ghostty equivalent to fall
 * back to.
 */
object ItermBadge {
    fun set(text: String): String {
        val base64 = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        return "\u001B]1337;SetBadgeFormat=$base64\u0007"
    }
}

/**
 * Sets the terminal's window/tab title via the `OSC 2` escape sequence — the same universal,
 * decades-old mechanism (supported by virtually every terminal emulator, not just iTerm2/Kitty)
 * that shells and tools like Claude Code's own CLI already use to show the current task in the tab
 * bar. Unlike [ItermBadge] (which needs a non-empty Badge template configured in the iTerm2 profile
 * to render at all) this has no prerequisite — it just works.
 */
object TerminalTitle {
    fun set(text: String): String = "\u001B]2;$text\u0007"
}

/** Outcome of [MermaidCli.renderToPng]. */
sealed class MermaidRenderResult {
    data class Success(val file: File) : MermaidRenderResult()

    /** [reason] is mmdc's own diagnostic output (or an exception message) — shown to the user. */
    data class Failure(val reason: String) : MermaidRenderResult()
}

/**
 * Best-effort wrapper around the `mmdc` (mermaid-cli) binary. Rendering Mermaid diagrams requires a
 * real DOM/layout engine (dagre, etc.) — there's no pure-JVM equivalent — so we shell out to the
 * user's own install rather than bundling Puppeteer/Chromium with Codyze. A missing binary, a
 * sandboxing failure, or a timeout all surface as [MermaidRenderResult.Failure] with a reason —
 * never a hard error — but callers get told *why*, rather than silently falling back.
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
     * blends with the terminal's own background regardless of [theme].
     */
    fun renderToPng(mermaidSource: String, theme: Theme): MermaidRenderResult =
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
                        .redirectErrorStream(true)
                        .start()
                // Drain stdout+stderr concurrently with waitFor() so a chatty `mmdc` (Puppeteer
                // debug output, etc.) can't fill the pipe buffer and deadlock the process.
                val outputText = StringBuilder()
                val drainThread =
                    Thread {
                            process.inputStream.bufferedReader().forEachLine {
                                outputText.appendLine(it)
                            }
                        }
                        .apply {
                            isDaemon = true
                            start()
                        }

                val finished = process.waitFor(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    return@runCatching MermaidRenderResult.Failure(
                        "mmdc timed out after ${RENDER_TIMEOUT_SECONDS}s"
                    )
                }
                drainThread.join(2_000)

                if (process.exitValue() != 0 || !output.exists() || output.length() == 0L) {
                    // mmdc (Node/Puppeteer) prints the actual error message first, then a
                    // "    at ..." stack trace below it — so skip stack frames and the generic
                    // "Generating ... chart" preamble to find the line that actually explains
                    // what went wrong, rather than grabbing the last (deepest/least useful) frame.
                    val reason =
                        outputText
                            .lineSequence()
                            .map { it.trim() }
                            .firstOrNull {
                                it.isNotBlank() &&
                                    !it.startsWith("at ") &&
                                    !it.startsWith("Generating ")
                            } ?: "mmdc exited with code ${process.exitValue()}"
                    return@runCatching MermaidRenderResult.Failure(reason)
                }
                MermaidRenderResult.Success(output)
            }
            .getOrElse { e ->
                MermaidRenderResult.Failure(e.message ?: e::class.simpleName ?: "unknown error")
            }
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
