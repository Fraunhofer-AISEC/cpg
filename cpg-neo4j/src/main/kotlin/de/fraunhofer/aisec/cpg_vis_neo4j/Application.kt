/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase.Companion.fromFile
import de.fraunhofer.aisec.cpg.helpers.CommonPath
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import de.fraunhofer.aisec.cpg.persistence.Neo4jConnectionDefaults
import de.fraunhofer.aisec.cpg.persistence.persistJson
import de.fraunhofer.aisec.cpg.persistence.pushToNeo4j
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
private const val EXIT_FAILURE = 1
private const val DEBUG_PARSER = true

private const val DEFAULT_SAVE_DEPTH = -1
private const val DEFAULT_MAX_COMPLEXITY = -1

/**
 * An application to export the <a href="https://github.com/Fraunhofer-AISEC/cpg">cpg</a> to a <a
 * href="https://github.com/Fraunhofer-AISEC/cpg">neo4j</a> database.
 *
 * Please make sure, that the [APOC](https://neo4j.com/labs/apoc/) plugin is enabled on your neo4j
 * server. It is used in mass-creating nodes and relationships.
 *
 * For example using docker:
 * ```
 * docker run -p 127.0.0.1:7474:7474 -p 127.0.0.1:7687:7687 -d -e NEO4J_AUTH=neo4j/password -e NEO4JLABS_PLUGINS='["apoc"]' neo4j:5
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
            description = ["The path to an optional a JSON compilation database"],
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
        description = ["Neo4j user name (default: ${Neo4jConnectionDefaults.USERNAME})"],
    )
    var neo4jUsername: String = Neo4jConnectionDefaults.USERNAME

    @CommandLine.Option(
        names = ["--password"],
        description = ["Neo4j password (default: ${Neo4jConnectionDefaults.USERNAME})"],
    )
    var neo4jPassword: String = Neo4jConnectionDefaults.PASSWORD

    @CommandLine.Option(
        names = ["--host"],
        description =
            ["Set the host of the neo4j Database (default: ${Neo4jConnectionDefaults.HOST})."],
    )
    private var host: String = Neo4jConnectionDefaults.HOST

    @CommandLine.Option(
        names = ["--port"],
        description =
            ["Set the port of the neo4j Database (default: ${Neo4jConnectionDefaults.PORT})."],
    )
    private var port: Int = Neo4jConnectionDefaults.PORT

    @CommandLine.Option(
        names = ["--save-depth"],
        description =
            [
                "Performance optimisation: " +
                    "Limit recursion depth form neo4j OGM when leaving the AST. " +
                    "$DEFAULT_SAVE_DEPTH (default) means no limit is used."
            ],
    )
    private var depth: Int = DEFAULT_SAVE_DEPTH

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
        description = ["Enable TranslationConfiguration option loadIncludes"],
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
        names = ["--no-neo4j"],
        description = ["Do not push cpg into neo4j [used for debugging]"],
    )
    private var noNeo4j: Boolean = false

    @CommandLine.Option(
        names = ["--no-purge-db"],
        description = ["Do no purge neo4j database before pushing the cpg"],
    )
    private var noPurgeDb: Boolean = false

    @CommandLine.Option(
        names = ["--infer-nodes"],
        description = ["Create inferred nodes for missing declarations"],
    )
    private var inferNodes: Boolean = false

    @CommandLine.Option(
        names = ["--schema-markdown"],
        description = ["Print the CPGs nodes and edges that they can have."],
    )
    private var schemaMarkdown: Boolean = false

    @CommandLine.Option(
        names = ["--schema-json"],
        description = ["Print the CPGs nodes and edges that they can have."],
    )
    private var schemaJson: Boolean = false

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

    private val packages: Array<String> =
        arrayOf("de.fraunhofer.aisec.cpg.graph", "de.fraunhofer.aisec.cpg.frontends")

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
     * Parse the file paths to analyze and set up the [Project] for these paths.
     *
     * @throws IllegalArgumentException, if there were no arguments provided, no source files could
     *   be resolved, or a path points to a non-existent or hidden file. A bare directory argument
     *   is supported and triggers [Project] auto-detection instead of an error.
     */
    fun setupProject(): Project {
        val db = mutuallyExclusiveParameters.jsonCompilationDatabase?.let { fromFile(it) }

        // The named components to analyze, e.g. {"application": [file1, file2]} for a plain file
        // list, or one entry per --softwareComponents value, or the compilation database's files.
        val namedComponents: Map<String, List<File>> =
            when {
                db != null && db.isNotEmpty() -> mapOf(DEFAULT_APPLICATION_NAME to db.sourceFiles)
                db != null -> emptyMap()
                mutuallyExclusiveParameters.softwareComponents.isNotEmpty() ->
                    mutuallyExclusiveParameters.softwareComponents.mapValues { (_, files) ->
                        getFilesOfList(files.split(","))
                    }

                else ->
                    mapOf(
                        DEFAULT_APPLICATION_NAME to
                            getFilesOfList(mutuallyExclusiveParameters.files)
                    )
            }

        val allFiles = namedComponents.values.flatten()
        require(allFiles.isNotEmpty()) {
            "No source files to analyze. Either no paths were given, or " +
                "--json-compilation-database resolved to an empty compilation database."
        }

        // If the user just points us at a single directory (and did not ask for an explicit
        // top-level, software components or a compilation database), let Project auto-detect its
        // structure (e.g. Go modules, a compilation database in a build/ folder) instead of
        // treating it as one flat component.
        val singleDirectory =
            mutuallyExclusiveParameters.files.singleOrNull()?.let {
                Paths.get(it).toAbsolutePath().normalize().toFile()
            }
        val autoDetect =
            db == null &&
                mutuallyExclusiveParameters.softwareComponents.isEmpty() &&
                topLevel == null &&
                singleDirectory?.isDirectory == true

        val projectPath =
            (topLevel
                    ?: (if (autoDetect) singleDirectory
                    else CommonPath.commonPath(allFiles) ?: allFiles.firstOrNull() ?: File(".")))
                .toPath()

        return Project.from(projectPath) {
            if (!autoDetect) {
                components {
                    namedComponents.forEach { (name, files) ->
                        val root =
                            (topLevel ?: CommonPath.commonPath(files) ?: files.first()).toPath()
                        component(name, root = root, sources = files.map(File::toPath))
                    }
                }
            }

            passes {
                if (!noDefaultPasses) {
                    default()
                    use<ControlDependenceGraphPass>()
                    use<ProgramDependenceGraphPass>()
                    use<PythonFileConceptPass>()
                }
                if (customPasses != "DEFAULT") {
                    for (pass in customPasses.split(",")) {
                        if (pass.contains(".")) {
                            @Suppress("UNCHECKED_CAST")
                            use(Class.forName(pass).kotlin as KClass<out Pass<*>>)
                        } else {
                            val clazz =
                                passClassMap[pass]
                                    ?: throw ConfigurationException(
                                        "Asked to produce unknown pass: $pass"
                                    )
                            use(clazz)
                        }
                    }
                }
                use<PrepareSerialization>()
            }

            exclude(*exclusionPatterns.toTypedArray())

            translation { builder ->
                builder.debugParser(DEBUG_PARSER)
                builder.loadIncludes(loadIncludes)
                builder.addIncludesToGraph(loadIncludes)
                builder.useUnityBuild(useUnityBuild)

                includePaths.forEach { builder.includePath(it) }

                if (maxComplexity != -1) {
                    builder.configurePass<ControlFlowSensitiveDFGPass>(
                        ControlFlowSensitiveDFGPass.Configuration(maxComplexity = maxComplexity)
                    )
                }

                db?.let { builder.useCompilationDatabase(it) }

                includesFile?.let { theFile ->
                    log.info("Load includes from file: $theFile")
                    val baseDir = theFile.parentFile?.toString() ?: ""
                    theFile.bufferedReader().useLines { lines ->
                        lines
                            .map(String::trim)
                            .map {
                                if (Paths.get(it).isAbsolute) it
                                else Paths.get(baseDir, it).toString()
                            }
                            .forEach { builder.includePath(it) }
                    }
                }

                if (inferNodes) {
                    builder.inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                }
            }
        }
    }

    /** Builds the [TranslationConfiguration] derived from [setupProject]. */
    fun setupTranslationConfiguration(): TranslationConfiguration = setupProject().config

    fun printSchema(filenames: Collection<String>, format: Schema.Format) {
        val schema = Schema()
        schema.extractSchema()
        filenames.forEach { schema.printToFile(it, format) }
    }

    /**
     * The entrypoint of the cpg-vis-neo4j.
     *
     * @throws IllegalArgumentException, if there were no arguments provided, or the path does not
     *   point to a file, is a directory or point to a hidden file or the paths does not have the
     *   same top level path
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     *   neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(Exception::class, ConnectException::class, IllegalArgumentException::class)
    override fun call(): Int {

        if (schemaMarkdown || schemaJson) {
            if (schemaMarkdown) {
                printSchema(mutuallyExclusiveParameters.files, Schema.Format.MARKDOWN)
            }
            if (schemaJson) {
                printSchema(mutuallyExclusiveParameters.files, Schema.Format.JSON)
            }
            return EXIT_SUCCESS
        }

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
        if (!noNeo4j) {
            translationResult.pushToNeo4j(
                noPurgeDb = noPurgeDb,
                host = host,
                port = port,
                neo4jUsername = neo4jUsername,
                neo4jPassword = neo4jPassword,
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
 * Starts a command line application of the cpg-vis-neo4j.
 *
 * @throws IllegalArgumentException, if there were no arguments provided, or the path does not point
 *   to a file, is a directory or point to a hidden file or the paths does not have the same top
 *   level path
 * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the neo4j
 *   db.
 * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
 */
fun main(args: Array<String>) {
    val exitCode = CommandLine(Application()).execute(*args)
    exitProcess(exitCode)
}
