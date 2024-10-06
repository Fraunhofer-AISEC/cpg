/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.console

import de.fraunhofer.aisec.cpg.analysis.MultiLineToStringStyle
import de.fraunhofer.aisec.cpg.console.CpgConsole.configureREPL
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.query.Reporter
import de.fraunhofer.aisec.cpg.query.SarifReporter
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.system.exitProcess
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.CachedInstance
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import picocli.CommandLine

object CpgConsole {
    @JvmStatic
    fun main(args: Array<String>) {
        val exitCode = CommandLine(Cli()).execute(*args) // basically just runs [Cli.run]
        exitProcess(exitCode)
    }

    private fun configuration(): ReplConfiguration {
        val instance = CachedInstance<ReplConfiguration>()
        val klassName: String? = System.getProperty("config.class")

        return if (klassName != null) {
            instance.load(klassName, ReplConfiguration::class)
        } else {
            instance.get {
                object : ReplConfigurationBase() {
                    override fun plugins(): Iterator<Plugin> {
                        val list = super.plugins().asSequence().toList().toMutableList()
                        list += TranslatePlugin()
                        list += Neo4jPlugin()
                        list += ShowCodePlugin()
                        list += CompilationDatabase()
                        list += RunPlugin()
                        list += LoadReporterPlugin()
                        list += RunRulePlugin()
                        list += ReportPlugin()

                        return list.listIterator()
                    }
                }
            }
        }
    }

    internal fun configureREPL(): Shell {
        var repl =
            Shell(
                configuration(),
                defaultJvmScriptingHostConfiguration,
                ScriptCompilationConfiguration {
                    jvm {
                        dependenciesFromClassloader(
                            classLoader = CpgConsole::class.java.classLoader,
                            wholeClasspath = true
                        )
                    }
                    compilerOptions(
                        "-jvm-target=17"
                    ) // this needs to match the JVM toolchain target in the CPG
                },
                ScriptEvaluationConfiguration {
                    jvm { baseClassLoader(CpgConsole::class.java.classLoader) }
                }
            )

        Runtime.getRuntime()
            .addShutdownHook(
                Thread {
                    println("\nBye!")
                    repl.cleanUp()
                }
            )

        return repl
    }
}

// TODO: mby don't hardcode this here
enum class Language {
    C,
    CPP,
    JAVA,
    LLVMIR,
    PYTHON,
    GOLANG,
    TYPESCRIPT,
    RUBY,
    ALL;

    fun toClassName(): String {
        return when (this) {
            C -> "de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage"
            CPP -> "de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage"
            JAVA -> "de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage"
            LLVMIR -> "de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage"
            PYTHON -> "de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage"
            GOLANG -> "de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage"
            TYPESCRIPT -> "de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage"
            RUBY -> "de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage"
            ALL -> "" // optionalLanguage() doesnt crash
        }
    }
}

@CommandLine.Command(
    name = "cpg-analysis",
    description =
        [
            "Either specify no arguments to start an interactive shell or specify a compilation database and rules to run" +
                " like a normal CLI tool."
        ]
)
private class Cli : Runnable {
    @CommandLine.Option(
        names = ["-cdb", "--compilation-database"],
        description = ["Path to the JSON compilation database."],
        paramLabel = "FILE",
    )
    var compilationDatabase: File? = null

    private class LanguageConverter : CommandLine.ITypeConverter<Language?> {
        override fun convert(value: String?): Language {
            return try {
                Language.valueOf(value!!.uppercase())
            } catch (_: Exception) {
                throw CommandLine.ParameterException(CommandLine(this), "Invalid language:$value")
            } // TODO: maybe ignore or just warn
        }
    }

    // TODO: support this
    /*
    @CommandLine.Option(
        names = ["-l", "--languages"],
        description =
            [
                "Languages to analyze, any of {C, CPP, JAVA, LLVMIR, PYTHON, GOLANG, TYPESCRIPT, RUBY, ALL}. " +
                    "Case insensitive. Defaults to ALL." // TODO: whats actually available depends on build config
            ],
        split = ",",
        converter = [LanguageConverter::class],
        defaultValue = "ALL",
        paramLabel = "language",
    )
    val languages: List<Language> = emptyList()
    */

    @CommandLine.Option(names = ["-m", "--minify"], description = ["Minify the output."])
    var minify: Boolean = false

    @CommandLine.Option(
        names = ["-o", "--output"],
        description =
            [
                "Path to write the output to. If unspecified, a default path is used. Used to determine the " +
                    "report type (currently only SARIF). The default is SARIF."
            ],
        paramLabel = "FILE",
    )
    var outputPath: File =
        Path.of(
                "reports",
                "report-${
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern
                        ("yyyy-MM-dd-HH-mm-ss")
                )
            }.sarif"
            )
            .toFile()

    @CommandLine.Option(
        names = ["-r", "--rules"],
        description = ["Comma-separated list of rules to run. If unspecified, all rules are run."],
        split = ",",
        paramLabel = "rule",
    )
    var rules: List<String> = emptyList()

    // TODO: support this
    /*
    @CommandLine.Option(
        names = ["--load-includes"],
        description = ["Enable TranslationConfiguration option loadIncludes"],
    )
    private var loadIncludes: Boolean = false
    */

    @SuppressWarnings("unused") // used by picocli
    @CommandLine.Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["display this help and exit."],
    )
    var help: Boolean = false

    // TODO: add functionality
    //  -> Pass-related options from the neo4j app

    /**
     * Runs the rules on the given compilation database and reports the results.
     *
     * The rules are currently built into the application and cannot be changed without modifying
     * the source code. The output is a SARIF file because currently the only [Reporter] is the
     * [SarifReporter]. The report's path is determined by the [Reporter.getDefaultPath] method of
     * the respective [Reporter].
     */
    override fun run() {
        Node.TO_STRING_STYLE = MultiLineToStringStyle()

        val repl = configureREPL()

        Runtime.getRuntime()
            .addShutdownHook(
                Thread {
                    // println("\nBye!")
                    repl.cleanUp()
                }
            )

        val doInteractiveShell = compilationDatabase == null && rules.isEmpty()
        if (doInteractiveShell) {
            repl.doRun()
        } else {
            repl.initEngine()
            lateinit var translateCompilationDatabasePlugin: Command
            lateinit var loadReporterPlugin: Command
            lateinit var runRulePlugin: Command
            lateinit var reportPlugin: Command
            val results = mutableListOf<Command.Result>()
            // find the commands we need
            repl.commands.forEach {
                when (it.short) {
                    "trdb" -> translateCompilationDatabasePlugin = it
                    "lr" -> loadReporterPlugin = it
                    "rr" -> runRulePlugin = it
                    "rp" -> reportPlugin = it
                }
            }
            // load the specified reporter
            results.add(loadReporterPlugin.execute(":loadReporter ${outputPath.extension}"))
            // translate the compilation database
            results.add(
                translateCompilationDatabasePlugin.execute(
                    ":translateCompilationDatabase ${compilationDatabase?.absolutePath}"
                )
            )
            // run the rules
            for (rule in rules) results.add(runRulePlugin.execute(":runrule $rule"))
            // create the report
            results.add(reportPlugin.execute(":report ${outputPath.absolutePath} $minify"))

            // the following code is a copy of parts of the
            // org.jetbrains.kotlinx.ki.shell.Shell.doRun() method
            // of the kotlin-interactive-shell licensed under the Apache License 2.0.
            // This is necessary because we want to run the shell headless which isn't directly
            // supported.
            // COPY START
            var blankLines = 0
            fun evalSnippet(line: String) {
                if (line.isBlank() && repl.incompleteLines.isNotEmpty()) {
                    if (blankLines == repl.settings.blankLinesAllowed - 1) {
                        repl.incompleteLines.clear()
                        println(
                            "You typed ${repl.settings.blankLinesAllowed} blank lines. Starting a new command."
                        )
                    } else blankLines++
                } else {
                    val source = (repl.incompleteLines + line).joinToString(separator = "\n")
                    val time = System.nanoTime()
                    val result = repl.eval(source)
                    repl.evaluationTimeMillis = (System.nanoTime() - time) / 1_000_000
                    when (result.getStatus()) {
                        ResultWrapper.Status.INCOMPLETE -> repl.incompleteLines.add(line)
                        ResultWrapper.Status.ERROR -> {
                            repl.incompleteLines.clear()
                            repl.handleError(result.result, result.isCompiled)
                        }
                        ResultWrapper.Status.SUCCESS -> {
                            repl.incompleteLines.clear()
                            repl.handleSuccess(result.result as ResultWithDiagnostics.Success<*>)
                        }
                    }
                }
            }
            // COPY END

            results.forEach {
                (it as Command.Result.RunSnippets).snippetsToRun.forEach(::evalSnippet)
            }
        }
    }
}
