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
 * Sets the terminal's window title and icon/tab name via the `OSC 0` escape sequence — a universal,
 * decades-old mechanism supported by virtually every terminal emulator (not just iTerm2/Kitty),
 * with no profile configuration prerequisite. Used by [ReplLoop] to show the REPL's live
 * idle/evaluating state.
 *
 * `OSC 2` alone only sets the *window* title, not the tab label — terminals treat "icon name" (set
 * by `OSC 1`, or together with the window title by `OSC 0`) as what's shown on the tab itself.
 */
object TerminalTitle {
    fun set(text: String): String = "\u001B]0;$text\u0007"
}

/**
 * Sets iTerm2's Tab Status (3.7+) — a colored dot and subtitle shown directly on the tab — via the
 * public `OSC 21337` escape sequence. No Python API or profile Trigger configuration needed; a
 * third-party open-source project (`claude-code-iterm2-tab-status`) uses this exact same public
 * sequence to replicate iTerm2's own Claude Code status dot for other CLI tools.
 *
 * Any parameter left `null` is left unchanged by iTerm2; pass an empty string to clear a field.
 * [indicatorColor] is in `#RRGGBB` form.
 */
object TabStatus {
    fun set(status: String? = null, indicatorColor: String? = null): String {
        val fields = buildList {
            status?.let { add("status=$it") }
            indicatorColor?.let { add("indicator=$it") }
        }
        return "\u001B]21337;${fields.joinToString(";")}\u0007"
    }
}

/**
 * Sends a desktop notification via `OSC 9` (supported by iTerm2 and several other terminals). Used
 * by [ReplLoop]/[ReplCommand] for analyses slow enough that the user might have tabbed away.
 */
object TerminalNotification {
    fun show(message: String): String = "\u001B]9;$message\u0007"
}

/**
 * Shell-integration marks (`OSC 133`), the same protocol real shells use (and iTerm2, VS Code,
 * WezTerm, etc. understand) to support jump-to-previous-prompt navigation and marking failed
 * commands in the scrollbar. Wiring these into the REPL's read-eval loop gives it that same native
 * navigation, as if each evaluated line were an ordinary shell command.
 */
object ShellIntegrationMarks {
    /** Marks the start of a prompt — call right before rendering/reading the next prompt. */
    fun promptStart(): String = "\u001B]133;A\u0007"

    /** Marks the start of a command's execution/output — call right before evaluating input. */
    fun commandStart(): String = "\u001B]133;C\u0007"

    /** Marks the end of a command, with its exit status — call right after evaluation completes. */
    fun commandEnd(exitCode: Int): String = "\u001B]133;D;$exitCode\u0007"
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
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    // A hung `mmdc` would otherwise keep running for the REPL's whole lifetime
                    // even though we've already given up on it and cached availability as false.
                    process.destroyForcibly()
                    false
                } else {
                    process.exitValue() == 0
                }
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
