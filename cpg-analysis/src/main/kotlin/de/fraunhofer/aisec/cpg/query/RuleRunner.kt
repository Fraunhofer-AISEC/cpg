/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.query

// import de.fraunhofer.aisec.cpg.rules.*
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import java.io.File
import java.nio.file.Path
import kotlin.system.exitProcess
import picocli.CommandLine

// TODO don't hardcode this here
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

/**
 * A class that runs a set of rules on a given [CompilationDatabase] and reports the results
 *
 * @param rules List of [Rule]s to run
 * @param languages List of [Language]s to use
 * @param reporter the [Reporter] to use for reporting
 * @param compilationDatabase the compilation database to use
 */
class RuleRunner(
    private val rules: List<Rule>,
    private var languages: List<Language>,
    private val reporter: Reporter,
    compilationDatabase: CompilationDatabase,
    loadIncludes: Boolean
) {
    private val config: TranslationConfiguration =
        TranslationConfiguration.builder()
            .useCompilationDatabase(compilationDatabase)
            .sourceLocations(compilationDatabase.sourceFiles)
            .let { it ->
                if (languages.contains(Language.ALL)) {
                    languages = Language.entries.filter { inner -> inner.name != "ALL" }
                }
                for (language in languages) {
                    try {
                        it.registerLanguage(language.toClassName())
                    } catch (_: Exception) {
                        // TODO: log
                        println(
                            "Failed to register language \"$language\", maybe it's not configured in gradle.properties?"
                        )
                    }
                }
                it
            }
            .loadIncludes(loadIncludes)
            .defaultPasses()
            .build()

    private val result: TranslationResult =
        TranslationManager.builder().config(config).build().analyze().get()

    /** Runs the [rules] on the given [TranslationResult] */
    fun runRules() {
        for (rule in rules) {
            rule.run(result)
        }
    }

    /**
     * Reports the results of the rules to a file. Uses the [reporter] to generate the report.
     *
     * @param minify if false, the output will not be minified and will be more human-readable
     * @param path the [Path] to write the report to. If unspecified, the [reporter]'s default path
     *   is used
     */
    fun report(minify: Boolean = true, path: Path = reporter.getDefaultPath()) {
        reporter.toFile(reporter.report(rules, minify), path)
    }
}

@CommandLine.Command(
    name = "cpg-analysis",
    description =
        [
            "Runs a set of rules on a given compilation database and reports the results. The rules are hard-coded in " +
                "the source code. The output is a SARIF file as no other reporters are implemented."
        ]
)
private class Cli : Runnable {
    @CommandLine.Option(
        names = ["-cdb", "--compilation-database"],
        description = ["Path to the JSON compilation database."],
        paramLabel = "FILE",
        required = true
    )
    lateinit var compilationDatabase: File

    private class LanguageConverter : CommandLine.ITypeConverter<Language?> {
        override fun convert(value: String?): Language {
            return try {
                Language.valueOf(value!!.uppercase())
            } catch (_: Exception) {
                throw CommandLine.ParameterException(CommandLine(this), "Invalid language: $value")
            } // TODO: maybe ignore or just warn
        }
    }

    @CommandLine.Option(
        names = ["-l", "--languages"],
        description =
            [
                "Languages to analyze, any of {C, CPP, JAVA, LLVMIR, PYTHON, GOLANG, TYPESCRIPT, RUBY, ALL}. " +
                    "Case insensitive. Defaults to ALL."
            ],
        split = ",",
        converter = [LanguageConverter::class],
        defaultValue = "ALL"
    )
    val languages: List<Language> = emptyList()

    @CommandLine.Option(names = ["-m", "--minify"], description = ["Minify the output."])
    var minify: Boolean = false

    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["Path to write the output to. If unspecified, a default path is used."],
        paramLabel = "FILE"
    )
    var outputPath: File? = null

    @CommandLine.Option(
        names = ["--load-includes"],
        description = ["Enable TranslationConfiguration option loadIncludes"]
    )
    private var loadIncludes: Boolean = false

    @SuppressWarnings("unused") // used by picocli
    @CommandLine.Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["display this help and exit."]
    )
    var help: Boolean = false

    // TODO: add functionality
    //  -> Pass-related options from the neo4j app
    //  -> don't hardcode rules but load them dynamically (may be similar to the pass system in the
    // neo4j app)

    /**
     * Runs the rules on the given compilation database and reports the results.
     *
     * The rules are currently built into the application and cannot be changed without modifying
     * the source code. The output is a SARIF file because currently the only [Reporter] is the
     * [SarifReporter]. The report's path is determined by the [Reporter.getDefaultPath] method of
     * the respective [Reporter].
     */
    override fun run() {
        val runner =
            RuleRunner(
                rules =
                    listOf(
                        // BufferOverreadMemcpy()
                    ),
                languages = languages,
                reporter = SarifReporter(),
                compilationDatabase = CompilationDatabase.fromFile(compilationDatabase),
                loadIncludes = loadIncludes
            )
        runner.runRules()
        if (outputPath != null) runner.report(minify = minify, path = outputPath!!.toPath())
        else runner.report(minify = minify)
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(Cli()).execute(*args)
    exitProcess(exitCode)
}
