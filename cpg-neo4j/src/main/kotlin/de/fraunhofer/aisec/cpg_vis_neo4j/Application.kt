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
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase.Companion.fromFile
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.order.*
import java.io.File
import java.lang.Class
import java.net.ConnectException
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.system.exitProcess
import org.neo4j.driver.exceptions.AuthenticationException
import org.neo4j.ogm.config.Configuration
import org.neo4j.ogm.exception.ConnectionException
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.neo4j.ogm.session.event.Event
import org.neo4j.ogm.session.event.EventListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.ArgGroup

private const val S_TO_MS_FACTOR = 1000
private const val TIME_BETWEEN_CONNECTION_TRIES: Long = 2000
private const val MAX_COUNT_OF_FAILS = 10
private const val EXIT_SUCCESS = 0
private const val EXIT_FAILURE = 1
private const val VERIFY_CONNECTION = true
private const val DEBUG_PARSER = true
private const val AUTO_INDEX = "none"
private const val PROTOCOL = "bolt://"

private const val DEFAULT_HOST = "localhost"
private const val DEFAULT_PORT = 7687
private const val DEFAULT_USER_NAME = "neo4j"
private const val DEFAULT_PASSWORD = "password"
private const val DEFAULT_SAVE_DEPTH = -1

/**
 * An application to export the <a href="https://github.com/Fraunhofer-AISEC/cpg">cpg</a> to a <a
 * href="https://github.com/Fraunhofer-AISEC/cpg">neo4j</a> database.
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
                ]
        )
        var files: List<String> = mutableListOf()

        @CommandLine.Option(
            names = ["--softwareComponents", "-S"],
            description =
                [
                    "Maps the names of software components to their respective files. The files are separated by commas (No whitespace!).",
                    "Example: -S App1=./file1.c,./file2.c -S App2=./Main.java,./Class.java"
                ]
        )
        var softwareComponents: Map<String, String> = mutableMapOf()

        @CommandLine.Option(
            names = ["--json-compilation-database"],
            description = ["The path to an optional a JSON compilation database"]
        )
        var jsonCompilationDatabase: File? = null

        @CommandLine.Option(
            names = ["--list-passes"],
            description = ["Prints the list available passes"]
        )
        var listPasses: Boolean = false
    }

    @CommandLine.Option(
        names = ["--user"],
        description = ["Neo4j user name (default: $DEFAULT_USER_NAME)"]
    )
    var neo4jUsername: String = DEFAULT_USER_NAME

    @CommandLine.Option(
        names = ["--password"],
        description = ["Neo4j password (default: $DEFAULT_PASSWORD"]
    )
    var neo4jPassword: String = DEFAULT_PASSWORD

    @CommandLine.Option(
        names = ["--host"],
        description = ["Set the host of the neo4j Database (default: $DEFAULT_HOST)."]
    )
    private var host: String = DEFAULT_HOST

    @CommandLine.Option(
        names = ["--port"],
        description = ["Set the port of the neo4j Database (default: $DEFAULT_PORT)."]
    )
    private var port: Int = DEFAULT_PORT

    @CommandLine.Option(
        names = ["--save-depth"],
        description =
            [
                "Performance optimisation: " +
                    "Limit recursion depth form neo4j OGM when leaving the AST. " +
                    "$DEFAULT_SAVE_DEPTH (default) means no limit is used."
            ]
    )
    private var depth: Int = DEFAULT_SAVE_DEPTH

    @CommandLine.Option(
        names = ["--load-includes"],
        description = ["Enable TranslationConfiguration option loadIncludes"]
    )
    private var loadIncludes: Boolean = false

    @CommandLine.Option(
        names = ["--use-unity-build"],
        description = ["Enable unity build mode for C++ (requires --load-includes)"]
    )
    private var useUnityBuild: Boolean = false

    @CommandLine.Option(names = ["--includes-file"], description = ["Load includes from file"])
    private var includesFile: File? = null

    @CommandLine.Option(
        names = ["--print-benchmark"],
        description = ["Print benchmark result as markdown table"]
    )
    private var printBenchmark: Boolean = false

    @CommandLine.Option(
        names = ["--no-default-passes"],
        description = ["Do not register default passes [used for debugging]"]
    )
    private var noDefaultPasses: Boolean = false

    @CommandLine.Option(
        names = ["--custom-pass-list"],
        description =
            [
                "Add custom list of passes (includes --no-default-passes) which is" +
                    " passed as a comma-separated list; give either pass name if pass is in list," +
                    " or its FQDN" +
                    " (e.g. --custom-pass-list=DFGPass,CallResolver)"
            ]
    )
    private var customPasses: String = "DEFAULT"

    @CommandLine.Option(
        names = ["--no-neo4j"],
        description = ["Do not push cpg into neo4j [used for debugging]"]
    )
    private var noNeo4j: Boolean = false

    @CommandLine.Option(
        names = ["--no-purge-db"],
        description = ["Do no purge neo4j database before pushing the cpg"]
    )
    private var noPurgeDb: Boolean = false

    @CommandLine.Option(
        names = ["--infer-nodes"],
        description = ["Create inferred nodes for missing declarations"]
    )
    private var inferNodes: Boolean = false

    @CommandLine.Option(
        names = ["--top-level"],
        description =
            [
                "Set top level directory of project structure. Default: Largest common path of all source files"
            ]
    )
    private var topLevel: File? = null

    @CommandLine.Option(
        names = ["--benchmark-json"],
        description = ["Save benchmark results to json file"]
    )
    private var benchmarkJson: File? = null

    private var passClassList =
        listOf(
            TypeHierarchyResolver::class,
            ImportResolver::class,
            VariableUsageResolver::class,
            CallResolver::class,
            DFGPass::class,
            EvaluationOrderGraphPass::class,
            TypeResolver::class,
            ControlFlowSensitiveDFGPass::class,
            FilenameMapper::class
        )
    private var passClassMap = passClassList.associateBy { it.simpleName }

    /** The list of available passes that can be registered. */
    private val passList: List<String>
        get() = passClassList.mapNotNull { it.simpleName }

    /**
     * Pushes the whole translationResult to the neo4j db.
     *
     * @param translationResult, not null
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     *   neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(InterruptedException::class, ConnectException::class)
    fun pushToNeo4j(translationResult: TranslationResult) {
        val bench = Benchmark(this.javaClass, "Push cpg to neo4j", false, translationResult)
        log.info("Using import depth: $depth")
        log.info(
            "Count base nodes to save: " +
                translationResult.components.size +
                translationResult.additionalNodes.size
        )

        val sessionAndSessionFactoryPair = connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            if (!noPurgeDb) session.purgeDatabase()
            session.save(translationResult.components, depth)
            session.save(translationResult.additionalNodes, depth)
            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
        bench.addMeasurement()
    }

    /**
     * Connects to the neo4j db.
     *
     * @return a Pair of Optionals of the Session and the SessionFactory, if it is possible to
     *   connect to neo4j. If it is not possible, the return value is a Pair of empty Optionals.
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     *   neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(InterruptedException::class, ConnectException::class)
    fun connect(): Pair<Session, SessionFactory> {
        var fails = 0
        var sessionFactory: SessionFactory? = null
        var session: Session? = null
        while (session == null && fails < MAX_COUNT_OF_FAILS) {
            try {
                val configuration =
                    Configuration.Builder()
                        .uri("$PROTOCOL$host:$port")
                        .autoIndex(AUTO_INDEX)
                        .credentials(neo4jUsername, neo4jPassword)
                        .verifyConnection(VERIFY_CONNECTION)
                        .build()
                sessionFactory =
                    SessionFactory(
                        configuration,
                        "de.fraunhofer.aisec.cpg.graph",
                        "de.fraunhofer.aisec.cpg.frontends"
                    )
                sessionFactory.register(AstChildrenEventListener())
                sessionFactory.register(PDGEventListener())

                session = sessionFactory.openSession()
            } catch (ex: ConnectionException) {
                sessionFactory = null
                fails++
                log.error(
                    "Unable to connect to localhost:7687, " +
                        "ensure the database is running and that " +
                        "there is a working network connection to it."
                )
                Thread.sleep(TIME_BETWEEN_CONNECTION_TRIES)
            } catch (ex: AuthenticationException) {
                log.error("Unable to connect to localhost:7687, wrong username/password!")
                exitProcess(EXIT_FAILURE)
            }
        }
        if (session == null || sessionFactory == null) {
            log.error("Unable to connect to localhost:7687")
            exitProcess(EXIT_FAILURE)
        }
        assert(fails <= MAX_COUNT_OF_FAILS)
        return Pair(session, sessionFactory)
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
    private fun setupTranslationConfiguration(): TranslationConfiguration {
        val translationConfiguration =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .defaultLanguages()
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
                .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage")
                .loadIncludes(loadIncludes)
                .addIncludesToGraph(loadIncludes)
                .debugParser(DEBUG_PARSER)
                .useUnityBuild(useUnityBuild)

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

        if (!noDefaultPasses && customPasses == "DEFAULT") {
            translationConfiguration.defaultPasses()
        } else if (!noDefaultPasses && customPasses != "DEFAULT") {
            val pieces = customPasses.split(",")
            for (pass in pieces) {
                if (pass.contains(".")) {
                    translationConfiguration.registerPass(
                        Class.forName(pass).kotlin as KClass<out Pass<*>>
                    )
                } else {
                    if (pass !in passClassMap) {
                        throw ConfigurationException("Asked to produce unknown pass")
                    }
                    passClassMap[pass]?.let { translationConfiguration.registerPass(it) }
                }
            }
        }

        mutuallyExclusiveParameters.jsonCompilationDatabase?.let {
            val db = fromFile(it)
            if (db.isNotEmpty()) {
                translationConfiguration.useCompilationDatabase(db)
                translationConfiguration.sourceLocations(db.sourceFiles)
            }
        }

        includesFile?.let { theFile ->
            log.info("Load includes form file: $theFile")
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

        if (!noNeo4j) {
            pushToNeo4j(translationResult)
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

class AstChildrenEventListener : EventListenerAdapter() {
    override fun onPreSave(event: Event?) {
        val node = event?.`object` as? Node ?: return
        node.astChildren = SubgraphWalker.getAstChildren(node)
    }
}

class PDGEventListener : EventListenerAdapter() {
    override fun onPreSave(event: Event?) {
        val node = event?.`object` as? Node ?: return
        val prevPDGEdges = node.prevCDGEdges.map { PropertyEdge(it) } +
                PropertyEdge.transformIntoOutgoingPropertyEdgeList(node.prevDFG.toList(), node)
        node.prevPDGEdges = prevPDGEdges
        val nextPDGEdges = node.nextCDGEdges.map { PropertyEdge(it) } +
                PropertyEdge.transformIntoOutgoingPropertyEdgeList(node.nextDFG.toList(), node)
        node.nextPDGEdges = nextPDGEdges
    }
}

/**
 * Starts a command line application of the cpg-vis-neo4j.
 *
 * @throws IllegalArgumentException, if there was no arguments provided, or the path does not point
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
