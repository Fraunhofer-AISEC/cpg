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

    /** Long-lived REPL compiler — both compiles snippets and serves completion. */
    val compiler: KJvmReplCompilerWithIdeServices = KJvmReplCompilerWithIdeServices(hostConfig)

    /** REPL-aware evaluator that links each snippet's ClassLoader to its predecessors. */
    private val replEvaluator: BasicJvmReplEvaluator =
        BasicJvmReplEvaluator(BasicJvmScriptEvaluator())

    private val lineCounter = AtomicInteger(0)
    private val renderer = NodeLinkRenderer()

    /**
     * The raw value of the most recent successful eval. Used by meta-commands like `:flow` that
     * want to re-use the last query result without making the user re-type the expression.
     */
    var lastValue: Any? = null
        private set

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
