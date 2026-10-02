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

import de.fraunhofer.aisec.codyze.console.ConsoleService
import de.fraunhofer.aisec.codyze.console.CpgQueryScript
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.printDFG
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmReplEvaluator
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices

/** Outcome of evaluating one REPL line. */
sealed class ReplEvalResult {
    data class Value(val rendered: String) : ReplEvalResult()

    data object UnitResult : ReplEvalResult()

    data class CompileError(val message: String) : ReplEvalResult()

    data class RuntimeError(val message: String) : ReplEvalResult()
}

/**
 * Owns the REPL session.
 *
 * Holds the analyzed [TranslationResult], a long-lived [KJvmReplCompilerWithIdeServices] (used for
 * both compilation and code completion — sharing one compiler instance is what makes completion
 * type-aware against previously declared symbols), and a [BasicJvmReplEvaluator] that handles the
 * classloader chaining between snippets so `val foo = …` on line 1 is visible on line 2.
 *
 * The compile step returns a `LinkedSnippet<KJvmCompiledScript>` — a linked-list node referencing
 * the new snippet plus its predecessors. [BasicJvmReplEvaluator] consumes the linked snippet and
 * internally tracks the matching evaluated-snippet chain so generated REPL classes can resolve each
 * other across `ClassLoader`s.
 */
class ReplService(private val consoleService: ConsoleService) {

    private val hostConfig = defaultJvmScriptingHostConfiguration
    private val compilationConfig: ScriptCompilationConfiguration =
        createJvmCompilationConfigurationFromTemplate<CpgQueryScript>()

    /**
     * Long-lived REPL compiler — both compiles snippets and serves completion. `var`, not `val`:
     * [resetSession] replaces it after a `:reload` so declarations from before the reload can't be
     * referenced against a [TranslationResult] that no longer exists.
     */
    var compiler: KJvmReplCompilerWithIdeServices = KJvmReplCompilerWithIdeServices(hostConfig)
        private set

    /** REPL-aware evaluator that links each snippet's ClassLoader to its predecessors. */
    private var replEvaluator: BasicJvmReplEvaluator =
        BasicJvmReplEvaluator(BasicJvmScriptEvaluator())

    private val lineCounter = AtomicInteger(0)

    /**
     * Terminal theme used when rendering eval results. Set by [ReplLoop] after detecting the host
     * terminal's background; defaults to [Theme.DARK] for non-TTY / startup contexts. Re-renders
     * pick up the change because [renderer] is rebuilt on each assignment.
     */
    var theme: Theme = Theme.DARK
        set(value) {
            field = value
            renderer = NodeLinkRenderer(theme = value)
        }

    private var renderer = NodeLinkRenderer(theme = theme)

    /** Public render hook so callers (e.g. `:result` meta) reuse the same theme-aware renderer. */
    fun render(value: Any?): String = renderer.render(value)

    /**
     * The raw value of the most recent successful eval. Used by meta-commands like `:flow` that
     * want to re-use the last query result without making the user re-type the expression.
     */
    var lastValue: Any? = null
        private set

    /** Clears [lastValue] without affecting compiler/evaluator state — used after pre-warming. */
    fun clearLastValue() {
        lastValue = null
    }

    /**
     * Discards the compiler/evaluator chain and resets [lastValue] and the line counter. Call this
     * after a successful `:reload` — otherwise previously-declared REPL variables/functions (e.g.
     * `val n = result.functions.first()`) remain resolvable and keep returning objects bound to the
     * [TranslationResult] that was just replaced, even though [clearLastValue] alone suggests a
     * clean slate.
     */
    fun resetSession() {
        compiler = KJvmReplCompilerWithIdeServices(hostConfig)
        replEvaluator = BasicJvmReplEvaluator(BasicJvmScriptEvaluator())
        lineCounter.set(0)
        lastValue = null
    }

    val translationResult: TranslationResult?
        get() = consoleService.getTranslationResult()?.analysisResult?.translationResult

    /** Compilation config used for both compile and complete — must be the same instance. */
    fun compilationConfig(): ScriptCompilationConfiguration = compilationConfig

    /**
     * Builds a fresh evaluation config bound to the current [TranslationResult]. Recreated per
     * evaluation so a `:reload` swaps in the new result without rebuilding the compiler state.
     */
    private fun evaluationConfig(): ScriptEvaluationConfiguration {
        val tr =
            translationResult
                ?: error("No analysis result available. Run analysis first or use :reload.")
        return createJvmEvaluationConfigurationFromTemplate<CpgQueryScript> { constructorArgs(tr) }
    }

    private val diagnosticFormatter = DiagnosticFormatter()

    /** Compiles + evaluates one line of REPL input and returns a rendered result. */
    fun eval(line: String): ReplEvalResult {
        if (translationResult == null) {
            return ReplEvalResult.RuntimeError(
                "No analysis result available. Run analysis first or use :reload."
            )
        }

        val sourceName = "Line_${lineCounter.incrementAndGet()}.cpg.query.kts"
        val source: SourceCode = line.toScriptSource(sourceName)

        return runBlocking {
            val compiled = compiler.compile(source, compilationConfig)
            when (compiled) {
                is ResultWithDiagnostics.Failure ->
                    ReplEvalResult.CompileError(
                        diagnosticFormatter.format(line, compiled.reports).ifEmpty {
                            "Unknown error"
                        }
                    )
                is ResultWithDiagnostics.Success -> {
                    val evalResult = replEvaluator.eval(compiled.value, evaluationConfig())
                    when (evalResult) {
                        is ResultWithDiagnostics.Failure ->
                            ReplEvalResult.RuntimeError(
                                diagnosticFormatter.format(line, evalResult.reports).ifEmpty {
                                    "Unknown error"
                                }
                            )
                        is ResultWithDiagnostics.Success ->
                            renderReturn(evalResult.value.get().result)
                    }
                }
            }
        }
    }

    private fun renderReturn(returnValue: ResultValue): ReplEvalResult =
        when (returnValue) {
            is ResultValue.Value -> {
                lastValue = returnValue.value
                ReplEvalResult.Value(renderer.render(returnValue.value))
            }
            is ResultValue.Unit -> ReplEvalResult.UnitResult
            is ResultValue.Error -> {
                val err = returnValue.error
                val msg = err.message ?: err::class.qualifiedName ?: err.toString()
                val trace = err.stackTraceToString().lines().take(8).joinToString("\n")
                ReplEvalResult.RuntimeError("$msg\n$trace")
            }
            else -> ReplEvalResult.UnitResult
        }

    private fun formatDiagnostics(reports: List<ScriptDiagnostic>): String =
        reports
            .filter { it.severity >= ScriptDiagnostic.Severity.WARNING }
            .joinToString("\n") { diag ->
                val loc = diag.location?.let { " (${it.start.line}:${it.start.col})" } ?: ""
                "${diag.severity}: ${diag.message}$loc"
            }
            .ifEmpty { "Unknown error" }
}

/**
 * Opens the DFG of a node as a mermaid graph.
 *
 * If the host terminal speaks an inline-image protocol (iTerm2, Kitty, Ghostty, WezTerm — see
 * [TerminalImageSupport]) and `mmdc` (mermaid-cli) is installed, renders the graph to a PNG and
 * prints it directly into the terminal. Otherwise falls back to writing a `.mermaid` file and
 * opening it externally (VS Code, or the OS default handler).
 */
fun openDFG(node: de.fraunhofer.aisec.cpg.graph.Node, theme: Theme = Theme.DARK): String {
    val mermaidWithWrapper = node.printDFG()
    // Remove the markdown code fence wrapper
    val mermaid = mermaidWithWrapper.removeSurrounding("```mermaid\n", "\n```")

    val protocol = TerminalImageSupport.detect()
    var renderFailureReason: String? = null
    if (protocol != null && MermaidCli.isAvailable) {
        when (val result = MermaidCli.renderToPng(mermaid, theme)) {
            is MermaidRenderResult.Success -> {
                print(TerminalImageEncoder.encode(result.file, protocol))
                System.out.flush()
                result.file.delete()
                return ""
            }
            is MermaidRenderResult.Failure -> renderFailureReason = result.reason
        }
    }

    val file =
        File(
            System.getProperty("java.io.tmpdir"),
            "codyze-dfg-${System.currentTimeMillis()}.mermaid",
        )
    file.writeText(mermaid)

    // `.mermaid` has no OS-registered handler anywhere, so only attempt to open it when VS Code
    // is actually detected (vscode://file always works); otherwise a generic "open" just produces
    // a confusing "no application knows how to open this file" error for no benefit.
    val openedExternally = FlowExporter.openInOs(file, allowGenericOpen = false)
    val prefix = renderFailureReason?.let { "mermaid-cli failed to render inline ($it); " } ?: ""
    return when {
        openedExternally -> "${prefix}Opened DFG: ${file.name}"
        else ->
            "${prefix}Wrote DFG to ${file.absolutePath} — open it with a mermaid-aware viewer " +
                "(VS Code + the Mermaid extension, or paste it into https://mermaid.live)."
    }
}
