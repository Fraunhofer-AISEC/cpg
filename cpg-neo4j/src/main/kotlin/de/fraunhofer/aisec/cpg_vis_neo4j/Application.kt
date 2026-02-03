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

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase.Companion.fromFile
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.DFGConnectionPass
import de.fraunhofer.aisec.cpg.passes.QiskitPass
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumEOGPass
import de.fraunhofer.aisec.cpg.persistence.Neo4jConnectionDefaults
import de.fraunhofer.aisec.cpg.persistence.pushToNeo4j
import java.io.File
import java.net.ConnectException
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.system.exitProcess
import org.neo4j.ogm.context.EntityGraphMapper
import org.neo4j.ogm.context.MappingContext
import org.neo4j.ogm.cypher.compiler.MultiStatementCypherCompiler
import org.neo4j.ogm.cypher.compiler.builders.node.DefaultNodeBuilder
import org.neo4j.ogm.cypher.compiler.builders.node.DefaultRelationshipBuilder
import org.neo4j.ogm.metadata.MetaData
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

data class JsonNode(val id: Long, val labels: Set<String>, val properties: Map<String, Any>)

data class JsonEdge(
    val id: Long,
    val type: String,
    val startNode: Long,
    val endNode: Long,
    val properties: Map<String, Any>,
)

data class JsonGraph(val nodes: List<JsonNode>, val edges: List<JsonEdge>)

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
     * Create node and relationship builders to map the cpg via OGM. This method is not a public API
     * of the OGM, thus we use reflection to access the related methods.
     *
     * @param translationResult, translationResult to map
     */
    fun translateCPGToOGMBuilders(
        translationResult: TranslationResult
    ): Pair<List<DefaultNodeBuilder>?, List<DefaultRelationshipBuilder>?> {
        val meta = MetaData(*packages)
        val con = MappingContext(meta)
        val entityGraphMapper = EntityGraphMapper(meta, con)

        translationResult.components.map { entityGraphMapper.map(it, depth) }
        translationResult.additionalNodes.map { entityGraphMapper.map(it, depth) }

        val compiler = entityGraphMapper.compileContext().compiler

        // get private fields of `CypherCompiler` via reflection
        val getNewNodeBuilders =
            MultiStatementCypherCompiler::class.java.getDeclaredField("newNodeBuilders")
        val getNewRelationshipBuilders =
            MultiStatementCypherCompiler::class.java.getDeclaredField("newRelationshipBuilders")
        getNewNodeBuilders.isAccessible = true
        getNewRelationshipBuilders.isAccessible = true

        // We only need `newNodeBuilders` and `newRelationshipBuilders` as we are "importing" to an
        // empty "db" and all nodes and relations will be new
        val newNodeBuilders =
            (getNewNodeBuilders[compiler] as? ArrayList<*>)?.filterIsInstance<DefaultNodeBuilder>()
        val newRelationshipBuilders =
            (getNewRelationshipBuilders[compiler] as? ArrayList<*>)?.filterIsInstance<
                DefaultRelationshipBuilder
            >()
        return newNodeBuilders to newRelationshipBuilders
    }

    /**
     * Use the provided node and relationship builders to create list of nodes and edges
     *
     * @param newNodeBuilders, input node builders
     * @param newRelationshipBuilders, input relationship builders
     */
    fun buildJsonGraph(
        newNodeBuilders: List<DefaultNodeBuilder>?,
        newRelationshipBuilders: List<DefaultRelationshipBuilder>?,
    ): JsonGraph {
        // create simple json structure with flat list of nodes and edges
        val nodes =
            newNodeBuilders?.map {
                val node = it.node()
                JsonNode(
                    node.id,
                    node.labels.toSet(),
                    node.propertyList.associate { prop -> prop.key to prop.value },
                )
            } ?: emptyList()
        val edges =
            newRelationshipBuilders
                // For some reason, there are edges without start or end node??
                ?.filter { it.edge().startNode != null }
                ?.map {
                    val edge = it.edge()
                    JsonEdge(
                        edge.id,
                        edge.type,
                        edge.startNode,
                        edge.endNode,
                        edge.propertyList.associate { prop -> prop.key to prop.value },
                    )
                } ?: emptyList()

        return JsonGraph(nodes, edges)
    }

    /**
     * Exports the TranslationResult to json. Serialization is done via the Neo4j OGM.
     *
     * @param translationResult, input translationResult, not null
     * @param path, path to output json file
     */
    fun exportToJson(translationResult: TranslationResult, path: File) {
        val bench = Benchmark(this.javaClass, "Export cpg to json", false, translationResult)
        log.info("Export graph to json using import depth: $depth")

        val (nodes, edges) = translateCPGToOGMBuilders(translationResult)
        val graph = buildJsonGraph(nodes, edges)
        val objectMapper = ObjectMapper()
        objectMapper.writeValue(path, graph)

        log.info(
            "Exported ${graph.nodes.size} Nodes and ${graph.edges.size} Edges to json file ${path.absoluteFile}"
        )
        bench.addMeasurement()
    }

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
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.openqasm.OpenQasmLanguage")
                .loadIncludes(loadIncludes)
                .exclusionPatterns(*exclusionPatterns.toTypedArray())
                .addIncludesToGraph(loadIncludes)
                .debugParser(DEBUG_PARSER)
                .useUnityBuild(useUnityBuild)
                .registerPass<QiskitPass>()
                .registerPass<QuantumEOGPass>()
                .registerPass<QuantumDFGPass>()
                .registerPass<DFGConnectionPass>()
                .useParallelPasses(false)

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
            // translationConfiguration.registerPass<PythonEncryptionPass>()
        }
        if (customPasses != "DEFAULT") {
            val pieces = customPasses.split(",")
            for (pass in pieces) {
                if (pass.contains(".")) {
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

        exportJsonFile?.let { exportToJson(translationResult, it) }
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
