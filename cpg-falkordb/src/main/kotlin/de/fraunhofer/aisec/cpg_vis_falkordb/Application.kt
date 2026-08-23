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
package de.fraunhofer.aisec.cpg_vis_falkordb

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase.Companion.fromFile
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import de.fraunhofer.aisec.cpg.persistence.FalkorDBConnectionDefaults
import de.fraunhofer.aisec.cpg.persistence.persistJson
import de.fraunhofer.aisec.cpg.persistence.pushToFalkorDB
import de.fraunhofer.aisec.cpg.project.Project
import java.io.File
import java.net.ConnectException
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.system.exitProcess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.ArgGroup

private const val S_TO_MS_FACTOR = 1000
private const val EXIT_SUCCESS = 0
private const val DEBUG_PARSER = true

private const val DEFAULT_MAX_COMPLEXITY = -1

/**
 * An application to export the <a href="https://github.com/Fraunhofer-AISEC/cpg">cpg</a> to a <a
 * href="https://www.falkordb.com">FalkorDB</a> graph database.
 *
 * In contrast to the Neo4j exporter, no server-side plugin is required, since FalkorDB is addressed
 * with plain Cypher only.
 *
 * For example using docker:
 * ```
 * docker run -p 127.0.0.1:6379:6379 -d falkordb/falkordb:latest
 * ```
 */
class Application : Callable<Int> {

    private val log: Logger
        get() = LoggerFactory.getLogger(Application::class.java)

    // Either provide the files to evaluate or provide the path of compilation database with
    // --json-compilation-database flag
    @ArgGroup(exclusive = true, multiplicity = "1")
    lateinit var mutuallyExclusiveParameters: Exclusive

    class Exclusive {
        @CommandLine.Parameters(
            arity = "0..*",
            description =
                [
                    "The paths to analyze. If module support is enabled, the paths will be looked at if they contain modules"
                ],
        )
        var files: List<String> = mutableListOf()

        @CommandLine.Option(
            names = ["--softwareComponents", "-S"],
            description =
                [
                    "Maps the names of software components to their respective files. The files are separated by commas (No whitespace!).",
                    "Example: -S App1=./file1.c,./file2.c -S App2=./Main.java,./Class.java",
                ],
        )
        var softwareComponents: Map<String, String> = mutableMapOf()

        @CommandLine.Option(
            names = ["--json-compilation-database"],
            description =
                [
                    "The path to an optional a JSON compilation database. Please note, that the JSON compilation database always describes a single component."
                ],
        )
        var jsonCompilationDatabase: File? = null

        @CommandLine.Option(
            names = ["--list-passes"],
            description = ["Prints the list available passes"],
        )
        var listPasses: Boolean = false
    }

    @CommandLine.Option(
        names = ["--include-paths", "-IP"],
        description =
            ["Directories containing additional headers and implementations for imported code."],
    )
    var includePaths: List<String> = mutableListOf()

    @CommandLine.Option(
        names = ["--user"],
        description = ["FalkorDB user name (default: no authentication)"],
    )
    var falkorDbUsername: String? = null

    @CommandLine.Option(
        names = ["--password"],
        description = ["FalkorDB password (default: no authentication)"],
    )
    var falkorDbPassword: String? = null

    @CommandLine.Option(
        names = ["--host"],
        description =
            ["Set the host of the FalkorDB instance (default: ${FalkorDBConnectionDefaults.HOST})."],
    )
    private var host: String = FalkorDBConnectionDefaults.HOST

    @CommandLine.Option(
        names = ["--port"],
        description =
            ["Set the port of the FalkorDB instance (default: ${FalkorDBConnectionDefaults.PORT})."],
    )
    private var port: Int = FalkorDBConnectionDefaults.PORT

    @CommandLine.Option(
        names = ["--graph"],
        description =
            [
                "Set the name of the graph to store the cpg in (default: ${FalkorDBConnectionDefaults.GRAPH}).",
                "A single FalkorDB instance can hold several independent graphs.",
            ],
    )
    private var graphName: String = FalkorDBConnectionDefaults.GRAPH

    @CommandLine.Option(
        names = ["--max-complexity-cf-dfg"],
        description =
            [
                "Performance optimisation: " +
                    "Limit the ControlFlowSensitiveDFGPass to functions with a complexity less than what is specified here. " +
                    "$DEFAULT_MAX_COMPLEXITY (default) means no limit is used."
            ],
    )
    private var maxComplexity: Int = DEFAULT_MAX_COMPLEXITY

    @CommandLine.Option(
        names = ["--load-includes"],
        description = ["Enable TranslationConfig option"],
    )
    private var loadIncludes: Boolean = false

    @CommandLine.Option(
        names = ["--use-unity-build"],
        description = ["Enable unity build mode for C++ (requires --load-includes)"],
    )
    private var useUnityBuild: Boolean = false

    @CommandLine.Option(names = ["--includes-file"], description = ["Load includes from file"])
    private var includesFile: File? = null

    @CommandLine.Option(
        names = ["--print-benchmark"],
        description = ["Print benchmark result as markdown table"],
    )
    private var printBenchmark: Boolean = false

    @CommandLine.Option(
        names = ["--no-default-passes"],
        description = ["Do not register default passes [used for debugging]"],
    )
    private var noDefaultPasses: Boolean = false

    @CommandLine.Option(
        names = ["--custom-pass-list"],
        description =
            [
                "Add custom list of passes (might be used additional to --no-default-passes) which is" +
                    " passed as a comma-separated list; give either pass name if pass is in list," +
                    " or its FQDN" +
                    " (e.g. --custom-pass-list=DFGPass,CallResolver)"
            ],
    )
    private var customPasses: String = "DEFAULT"

    @CommandLine.Option(
        names = ["--no-falkordb"],
        description = ["Do not push cpg into FalkorDB [used for debugging]"],
    )
    private var noFalkorDb: Boolean = false

    @CommandLine.Option(
        names = ["--no-purge-db"],
        description = ["Do not purge the graph before pushing the cpg"],
    )
    private var noPurgeDb: Boolean = false

    @CommandLine.Option(
        names = ["--infer-nodes"],
        description = ["Create inferred nodes for missing declarations"],
    )
    private var inferNodes: Boolean = false

    @CommandLine.Option(
        names = ["--top-level"],
        description =
            [
                "Set top level directory of project structure. Default: Largest common path of all source files"
            ],
    )
    private var topLevel: File? = null

    @CommandLine.Option(
        names = ["--exclusion-patterns"],
        description =
            ["Configures an exclusion pattern for files or directories that should not be parsed"],
    )
    private var exclusionPatterns: List<String> = listOf()

    @CommandLine.Option(
        names = ["--benchmark-json"],
        description = ["Save benchmark results to json file"],
    )
    private var benchmarkJson: File? = null

    @CommandLine.Option(names = ["--export-json"], description = ["Export cpg as json"])
    private var exportJsonFile: File? = null

    private var passClassList =
        listOf(
            TypeHierarchyResolver::class,
            SymbolResolver::class,
            DFGPass::class,
            EvaluationOrderGraphPass::class,
            TypeResolver::class,
            ControlFlowSensitiveDFGPass::class,
            ControlDependenceGraphPass::class,
            ProgramDependenceGraphPass::class,
        )
    private var passClassMap = passClassList.associateBy { it.simpleName }

    /** The list of available passes that can be registered. */
    private val passList: List<String>
        get() = passClassList.mapNotNull { it.simpleName }

    /**
     * Checks if all elements in the parameter are a valid file and returns a list of files.
     *
     * @param filenames The filenames to check
     * @return List of files
     */
    private fun getFilesOfList(filenames: Collection<String>): List<File> {
        val filePaths = filenames.map { Paths.get(it).toAbsolutePath().normalize().toFile() }
        filePaths.forEach {
            require(it.exists() && (!it.isHidden)) {
                "Please use a correct path. It was: ${it.path}"
            }
        }
        return filePaths
    }

    /**
     * Parse the file paths to analyze and set up the translationConfiguration with these paths.
     *
     * @throws IllegalArgumentException, if there were no arguments provided, or the path does not
     *   point to a file, is a directory or point to a hidden file or the paths does not have the
     *   same top level path.
     */
    fun setupTranslationConfiguration(): TranslationConfiguration {
        val translationConfiguration =
            TranslationConfiguration.builder()
                .also { builder ->
                    Project.defaultLanguages.forEach { builder.optionalLanguage(it) }
                }
                .loadIncludes(loadIncludes)
                .exclusionPatterns(*exclusionPatterns.toTypedArray())
                .addIncludesToGraph(loadIncludes)
                .debugParser(DEBUG_PARSER)
                .useUnityBuild(useUnityBuild)

        topLevel?.let { translationConfiguration.topLevel(it) }

        if (maxComplexity != -1) {
            translationConfiguration.configurePass<ControlFlowSensitiveDFGPass>(
                ControlFlowSensitiveDFGPass.Configuration(maxComplexity = maxComplexity)
            )
        }

        includePaths.forEach { translationConfiguration.includePath(it) }

        if (mutuallyExclusiveParameters.softwareComponents.isNotEmpty()) {
            val components = mutableMapOf<String, List<File>>()
            for (sc in mutuallyExclusiveParameters.softwareComponents) {
                components[sc.key] = getFilesOfList(sc.value.split(","))
            }
            translationConfiguration.softwareComponents(components)
        } else {
            val filePaths = getFilesOfList(mutuallyExclusiveParameters.files)
            translationConfiguration.sourceLocations(filePaths)
        }

        if (!noDefaultPasses) {
            translationConfiguration.defaultPasses()
            translationConfiguration.registerPass<ControlDependenceGraphPass>()
            translationConfiguration.registerPass<ProgramDependenceGraphPass>()
            translationConfiguration.registerPass<PythonFileConceptPass>()
        }
        if (customPasses != "DEFAULT") {
            val pieces = customPasses.split(",")
            for (pass in pieces) {
                if (pass.contains(".")) {
                    @Suppress("UNCHECKED_CAST")
                    translationConfiguration.registerPass(
                        Class.forName(pass).kotlin as KClass<out Pass<*>>
                    )
                } else {
                    if (pass !in passClassMap) {
                        throw ConfigurationException("Asked to produce unknown pass: $pass")
                    }
                    passClassMap[pass]?.let { translationConfiguration.registerPass(it) }
                }
            }
        }
        translationConfiguration.registerPass(PrepareSerialization::class)

        mutuallyExclusiveParameters.jsonCompilationDatabase?.let {
            val db = fromFile(it)
            if (db.isNotEmpty()) {
                translationConfiguration.useCompilationDatabase(db)
                translationConfiguration.sourceLocations(db.sourceFiles)
            }
        }

        includesFile?.let { theFile ->
            log.info("Load includes from file: $theFile")
            val baseDir = File(theFile.toString()).parentFile?.toString() ?: ""
            theFile
                .inputStream()
                .bufferedReader()
                .lines()
                .map(String::trim)
                .map { if (Paths.get(it).isAbsolute) it else Paths.get(baseDir, it).toString() }
                .forEach { translationConfiguration.includePath(it) }
        }

        if (inferNodes) {
            translationConfiguration.inferenceConfiguration(
                InferenceConfiguration.builder().inferRecords(true).build()
            )
        }
        return translationConfiguration.build()
    }

    /**
     * The entrypoint of the cpg-vis-falkordb.
     *
     * @throws IllegalArgumentException, if there were no arguments provided, or the path does not
     *   point to a file, is a directory or point to a hidden file or the paths does not have the
     *   same top level path
     * @throws ConnectException, if no connection to the FalkorDB instance is possible
     */
    @Throws(Exception::class, ConnectException::class, IllegalArgumentException::class)
    override fun call(): Int {
        if (mutuallyExclusiveParameters.listPasses) {
            log.info("List of passes:")
            passList.iterator().forEach { log.info("- $it") }
            log.info("--")
            log.info("End of list. Stopping.")
            return EXIT_SUCCESS
        }

        val translationConfiguration = setupTranslationConfiguration()

        val startTime = System.currentTimeMillis()

        val translationResult =
            TranslationManager.builder().config(translationConfiguration).build().analyze().get()

        val analyzingTime = System.currentTimeMillis()
        log.info(
            "Benchmark: analyzing code in " + (analyzingTime - startTime) / S_TO_MS_FACTOR + " s."
        )

        exportJsonFile?.let { translationResult.persistJson(it) }
        if (!noFalkorDb) {
            translationResult.pushToFalkorDB(
                noPurgeDb = noPurgeDb,
                host = host,
                port = port,
                username = falkorDbUsername,
                password = falkorDbPassword,
                graphName = graphName,
            )
        }

        val pushTime = System.currentTimeMillis()
        log.info("Benchmark: push code in " + (pushTime - analyzingTime) / S_TO_MS_FACTOR + " s.")

        val benchmarkResult = translationResult.benchmarkResults

        if (printBenchmark) {
            benchmarkResult.print()
        }

        benchmarkJson?.let { theFile ->
            log.info("Save benchmark results to file: $theFile")
            theFile.writeText(benchmarkResult.json)
        }

        return EXIT_SUCCESS
    }
}

/**
 * Starts a command line application of the cpg-vis-falkordb.
 *
 * @throws IllegalArgumentException, if there were no arguments provided, or the path does not point
 *   to a file, is a directory or point to a hidden file or the paths does not have the same top
 *   level path
 * @throws ConnectException, if no connection to the FalkorDB instance is possible
 */
fun main(args: Array<String>) {
    val exitCode = CommandLine(Application()).execute(*args)
    exitProcess(exitCode)
}
