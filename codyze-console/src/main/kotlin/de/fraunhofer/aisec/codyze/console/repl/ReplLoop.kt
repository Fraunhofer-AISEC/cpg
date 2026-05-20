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

import de.fraunhofer.aisec.codyze.console.AnalyzeRequestJSON
import de.fraunhofer.aisec.codyze.console.ConsoleService
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Reference
import org.jline.reader.UserInterruptException
import org.jline.reader.Widget
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.TerminalBuilder

/**
 * Runs the interactive REPL loop against an already-analyzed [ConsoleService].
 *
 * Handles JLine setup (terminal, completer, multi-line parser, persistent history) and the
 * read-eval-print loop including the `:`-prefixed meta-commands.
 */
class ReplLoop(
    private val consoleService: ConsoleService,
    private val replService: ReplService = ReplService(consoleService),
) {
    private val historyFile: Path =
        Path.of(System.getProperty("user.home"), ".codyze", "repl_history")
    private val renderer = NodeLinkRenderer()
    private val sessionLines = mutableListOf<String>()

    fun run() {
        Files.createDirectories(historyFile.parent)
        val terminal = TerminalBuilder.builder().system(true).build()
        val reader: LineReader =
            LineReaderBuilder.builder()
                .terminal(terminal)
                .appName("codyze-repl")
                .completer(KotlinReplCompleter(replService))
                .parser(KotlinReplParser())
                .highlighter(KotlinReplHighlighter())
                .variable(LineReader.HISTORY_FILE, historyFile)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%P  > ")
                // AUTO_LIST: show the candidate menu immediately on first TAB instead of
                // requiring a double-tap.
                // AUTO_MENU: with multiple candidates, the menu auto-pops on TAB.
                // LIST_PACKED: render the menu in compact column form so 30+ candidates fit.
                .option(LineReader.Option.AUTO_LIST, true)
                .option(LineReader.Option.AUTO_MENU, true)
                .option(LineReader.Option.LIST_PACKED, true)
                .option(LineReader.Option.LIST_AMBIGUOUS, true)
                // Kotlin uses `!!` as the non-null assertion operator. JLine defaults to bash-
                // style history expansion which would rewrite `m!!` to `m<last-command>` — we
                // disable that entirely so REPL input is taken verbatim.
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .build()

        printReadyLine(terminal.writer())
        terminal.writer().flush()

        while (true) {
            val line =
                try {
                    reader.readLine(prompt())
                } catch (_: UserInterruptException) {
                    // Ctrl-C: clear the line and continue.
                    continue
                } catch (_: EndOfFileException) {
                    // Ctrl-D: exit.
                    break
                }

            val trimmed = line?.trim().orEmpty()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith(":")) {
                if (!handleMeta(trimmed, terminal.writer())) break
                terminal.writer().flush()
                continue
            }

            sessionLines.add(line!!)
            when (val res = replService.eval(line)) {
                is ReplEvalResult.Value -> terminal.writer().println(res.rendered)
                is ReplEvalResult.UnitResult -> Unit
                // DiagnosticFormatter already prefixes with "error:" / "warning:" + color,
                // so we just print its output verbatim.
                is ReplEvalResult.CompileError -> terminal.writer().println(res.message)
                is ReplEvalResult.RuntimeError -> terminal.writer().println(res.message)
            }
            terminal.writer().flush()
        }
        terminal.close()
    }

    private fun prompt(): String = "codyze> "

    /**
     * Binds `.` to a widget that inserts the dot at the cursor and then immediately triggers the
     * completion menu — so typing `result.` pops up the member list without a TAB press,
     * IntelliSense-style.
     *
     * We write the dot character directly into the buffer (rather than delegating to the
     * `self-insert` widget, which depends on the "last binding" context and doesn't compose cleanly
     * when called from inside another widget). Then we trigger `complete-word`, which — combined
     * with `AUTO_LIST` — renders the candidate menu.
     *
     * The binding is registered against all of JLine's standard key maps (emacs, viins, vicmd,
     * main) so the trigger works regardless of the user's current input mode.
     */
    private fun installDotCompleteWidget(reader: LineReader) {
        val impl = reader as? LineReaderImpl ?: return
        val widget = Widget {
            impl.buffer.write('.'.code)
            impl.callWidget(LineReader.COMPLETE_WORD)
            true
        }
        impl.widgets[DOT_COMPLETE] = widget
        val ref = Reference(DOT_COMPLETE)
        listOf(LineReader.MAIN, LineReader.EMACS, "viins").forEach { mapName ->
            impl.keyMaps[mapName]?.bind(ref, ".")
        }
    }

    /** Prints a short "ready" summary once analysis is finished. */
    private fun printReadyLine(out: java.io.PrintWriter) {
        val tr = replService.translationResult
        if (tr != null) {
            val tus = tr.components.flatMap { it.translationUnits }.size
            out.println()
            out.println(
                "${DIM}Analysis complete: ${tr.components.size} component(s), $tus translation unit(s).${RESET}"
            )
        } else {
            out.println(
                "${DIM}(no analysis loaded — use :reload <path> to analyze a project)${RESET}"
            )
        }
        out.println()
    }

    companion object {
        /**
         * Prints the CODYZE ASCII banner + a "what's next" hint to [out]. Called from [ReplCommand]
         * before analysis starts so the user sees the banner immediately and knows the slow log
         * noise that follows is expected work, not a freeze.
         */
        fun printStartupBanner(out: java.io.PrintStream, sourceDir: String?) {
            out.println(CYAN + CODYZE_BANNER + RESET)
            out.println(
                "${BOLD}Codyze REPL${RESET}${DIM} — Kotlin scripting against the analyzed CPG.${RESET}"
            )
            if (sourceDir != null) {
                out.println("${DIM}Analyzing $sourceDir … (this may take a few seconds)${RESET}")
            }
            out.println(
                "${DIM}Type :help once you're at the prompt. TAB completes; '.' pops a menu.${RESET}"
            )
        }

        private const val DOT_COMPLETE = "codyze-dot-complete"
        private const val ESC = "\u001B"
        private const val RESET = "$ESC[0m"
        private const val DIM = "$ESC[2m"
        private const val BOLD = "$ESC[1m"
        private const val CYAN = "$ESC[36m"

        // Big Money-nw figlet rendering of "CODYZE" — matches the file-header CPG art.
        private val CODYZE_BANNER =
            """
            |  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\    ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\   ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\  ${'$'}${'$'}\     ${'$'}${'$'}\  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\
            | ${'$'}${'$'}  __${'$'}${'$'}\  ${'$'}${'$'}  __${'$'}${'$'}\  ${'$'}${'$'}  __${'$'}${'$'}\ \${'$'}${'$'}\   ${'$'}${'$'}  |\____${'$'}${'$'}  |${'$'}${'$'}  _____|
            | ${'$'}${'$'} /  \__|${'$'}${'$'} /  ${'$'}${'$'} | ${'$'}${'$'} |  ${'$'}${'$'} | \${'$'}${'$'}\ ${'$'}${'$'}  /     ${'$'}${'$'}  / ${'$'}${'$'} |
            | ${'$'}${'$'} |      ${'$'}${'$'} |  ${'$'}${'$'} | ${'$'}${'$'} |  ${'$'}${'$'} |  \${'$'}${'$'}${'$'}${'$'}  /     ${'$'}${'$'}  /  ${'$'}${'$'}${'$'}${'$'}${'$'}\
            | ${'$'}${'$'} |      ${'$'}${'$'} |  ${'$'}${'$'} | ${'$'}${'$'} |  ${'$'}${'$'} |   \${'$'}${'$'}  /     ${'$'}${'$'}  /   ${'$'}${'$'}  __|
            | ${'$'}${'$'} |  ${'$'}${'$'}\ ${'$'}${'$'} |  ${'$'}${'$'} | ${'$'}${'$'} |  ${'$'}${'$'} |    ${'$'}${'$'} |     ${'$'}${'$'}  /    ${'$'}${'$'} |
            | \${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  | ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  | ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  |    ${'$'}${'$'} |    ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\ ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\
            |  \______/  \______/  \________/     \__|    \________|\________|
            """
                .trimMargin()
    }

    /** Returns false to signal exit. */
    private fun handleMeta(input: String, out: java.io.PrintWriter): Boolean {
        val parts = input.removePrefix(":").trim().split(Regex("\\s+"), limit = 2)
        val cmd = parts[0]
        val arg = parts.getOrNull(1)?.trim().orEmpty()

        when (cmd) {
            "help",
            "h",
            "?" -> printHelp(out)
            "quit",
            "q",
            "exit" -> return false
            "reload" -> reloadAnalysis(arg, out)
            "imports" -> printImports(out)
            "save" -> saveSession(arg, out)
            "result" -> printResultSummary(out)
            "flow" -> exportFlow(arg, out)
            "dfg" -> exportDfg(arg, out)
            else -> out.println("Unknown command: :$cmd  (try :help)")
        }
        return true
    }

    private fun printHelp(out: java.io.PrintWriter) {
        out.println(
            """
            |Meta-commands:
            |  :help                show this message
            |  :quit                exit the REPL (also Ctrl-D)
            |  :reload [<path>]     re-analyze; with <path>, analyze a new source directory
            |  :imports             list the auto-imported packages
            |  :result              show a one-line summary of the loaded TranslationResult
            |  :save <file>         write all evaluated lines to <file> as a .cpg.query.kts script
            |  :flow [<expr>]       export the last (or freshly evaluated) QueryTree as SARIF
            |                       and open it (VS Code SARIF Viewer renders the path)
            |  :dfg [<expr>]       open the DFG of a Node as mermaid in VS Code
            |
            |Bindings:
            |  result               the current TranslationResult (always available)
            |
            |Tips:
            |  Press TAB for code completion. Open braces are continued across lines.
            |  Returned Nodes print with clickable file:line links (OSC 8) in modern terminals.
            """
                .trimMargin()
        )
    }

    private fun reloadAnalysis(arg: String, out: java.io.PrintWriter) {
        val sourceDir =
            arg.ifEmpty {
                consoleService.lastProject?.config?.sourceLocations?.firstOrNull()?.absolutePath
                    ?: run {
                        out.println("No previous analysis to reload — pass a path: :reload <dir>")
                        return
                    }
            }
        out.println("Analyzing $sourceDir …")
        out.flush()
        runBlocking { consoleService.analyze(AnalyzeRequestJSON(sourceDir = sourceDir)) }
        out.println("Analysis complete.")
    }

    /**
     * `:flow [<expression>]` — exports the last (or freshly-evaluated) query result as a SARIF
     * document and opens it in the OS default `.sarif` handler. With VS Code's SARIF Viewer
     * extension installed, this renders each codeflow as a clickable step-through panel.
     *
     * No argument: re-uses `replService.lastValue`. With argument: evaluates the expression, then
     * exports its result.
     */
    private fun exportFlow(arg: String, out: java.io.PrintWriter) {
        if (arg.isNotEmpty()) {
            // Run the expression so its return value is captured in lastValue.
            when (val res = replService.eval(arg)) {
                is ReplEvalResult.CompileError -> {
                    out.println(res.message)
                    return
                }
                is ReplEvalResult.RuntimeError -> {
                    out.println(res.message)
                    return
                }
                else -> Unit
            }
        }
        val value = replService.lastValue
        if (value == null) {
            out.println(
                "No value to export. Run a dataFlow/executionPath query first, or pass an expression: :flow <expr>"
            )
            return
        }
        val file = FlowExporter.export(value)
        if (file == null) {
            out.println(
                "Could not extract any node-paths from the last result (got ${value::class.simpleName}). " +
                    "`:flow` works best on QueryTree results from dataFlow/executionPath."
            )
            return
        }
        out.println("${DIM}Wrote SARIF flow to ${file.absolutePath} — opening …${RESET}")
        FlowExporter.openInOs(file)
    }

    /**
     * `:dfg [<expression>]` — opens the DFG of a node as a mermaid graph in VS Code.
     *
     * No argument: uses `replService.lastValue`. With argument: evaluates the expression first.
     */
    private fun exportDfg(arg: String, out: java.io.PrintWriter) {
        val node =
            if (arg.isNotEmpty()) {
                when (val res = replService.eval(arg)) {
                    is ReplEvalResult.CompileError -> {
                        out.println(res.message)
                        return
                    }
                    is ReplEvalResult.RuntimeError -> {
                        out.println(res.message)
                        return
                    }
                    else -> replService.lastValue
                }
            } else {
                replService.lastValue
            }

        if (node !is de.fraunhofer.aisec.cpg.graph.Node) {
            out.println(
                "No node to visualize. Run an expression that returns a Node, " +
                    "or pass one: :dfg <expr>"
            )
            return
        }

        val result = openDFG(node)
        out.println(result)
    }

    private fun printImports(out: java.io.PrintWriter) {
        // Mirror the imports in CpgQueryScript so the user knows what's already in scope.
        listOf(
                "de.fraunhofer.aisec.cpg.*",
                "de.fraunhofer.aisec.cpg.graph.*",
                "de.fraunhofer.aisec.cpg.graph.declarations.*",
                "de.fraunhofer.aisec.cpg.graph.statements.*",
                "de.fraunhofer.aisec.cpg.graph.expressions.*",
                "de.fraunhofer.aisec.cpg.graph.types.*",
                "de.fraunhofer.aisec.cpg.query.*",
            )
            .forEach { out.println("  import $it") }
    }

    private fun printResultSummary(out: java.io.PrintWriter) {
        val tr = replService.translationResult
        if (tr == null) {
            out.println("No analysis loaded.")
            return
        }
        out.println(renderer.render(tr.components.flatMap { it.translationUnits }))
    }

    private fun saveSession(arg: String, out: java.io.PrintWriter) {
        if (arg.isEmpty()) {
            out.println("Usage: :save <file>")
            return
        }
        val file = File(arg)
        file.writeText(sessionLines.joinToString("\n") + "\n")
        out.println("Wrote ${sessionLines.size} lines to ${file.absolutePath}")
    }
}
