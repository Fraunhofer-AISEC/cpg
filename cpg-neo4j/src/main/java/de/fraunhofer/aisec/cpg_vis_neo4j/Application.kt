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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import java.io.File
import java.net.ConnectException
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import org.neo4j.driver.exceptions.AuthenticationException
import org.neo4j.ogm.config.Configuration
import org.neo4j.ogm.exception.ConnectionException
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine

private const val S_TO_MS_FACTOR = 1000
private const val TIME_BETWEEN_CONNECTION_TRIES: Long = 2000
private const val MAX_COUNT_OF_FAILS = 10
private const val EXIT_SUCCESS = 0
private const val EXIT_FAILURE = 1
private const val VERIFY_CONNECTION = true
private const val PURGE_DB = true
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
 *
 * @author Andreas Hager, andreas.hager@aisec.fraunhofer.de
 */
class Application : Callable<Int> {
    private val log: Logger
        get() = LoggerFactory.getLogger(Application::class.java)

    @CommandLine.Parameters(
        arity = "1..*",
        description =
            [
                "The paths to analyze. If module support is enabled, the paths will be looked at if they contain modules"]
    )
    private var files: Array<String> = arrayOf(".")

    @CommandLine.Option(
        names = ["--user"],
        description = ["Neo4j user name (default: $DEFAULT_USER_NAME)"]
    )
    private var neo4jUsername: String = DEFAULT_USER_NAME

    @CommandLine.Option(
        names = ["--password"],
        description = ["Neo4j password (default: $DEFAULT_PASSWORD"]
    )
    private var neo4jPassword: String = DEFAULT_PASSWORD

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
                    "$DEFAULT_SAVE_DEPTH (default) means no limit is used."]
    )
    private var depth: Int = DEFAULT_SAVE_DEPTH

    @CommandLine.Option(
        names = ["--load-includes"],
        description = ["Enable TranslationConfiguration option loadIncludes"]
    )
    private var loadIncludes: Boolean = false

    @CommandLine.Option(names = ["--includes-file"], description = ["Load includes from file"])
    private var includesFile: File? = null

    @CommandLine.Option(
        names = ["--enable-experimental-python"],
        description =
            [
                "Enables the experimental language frontend for Python. Be aware, that further steps might be necessary to install native libraries such as jep"]
    )
    private var enableExperimentalPython: Boolean = false

    @CommandLine.Option(
        names = ["--enable-experimental-go"],
        description =
            [
                "Enables the experimental language frontend for Go. Be aware, that further steps might be necessary to install native libraries such as cpgo"]
    )
    private var enableExperimentalGo: Boolean = false

    /**
     * Pushes the whole translationResult to the neo4j db.
     *
     * @param translationResult, not null
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     * neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(InterruptedException::class, ConnectException::class)
    fun pushToNeo4j(translationResult: TranslationResult) {
        log.info("Using import depth: $depth")
        log.info("Count base nodes to save: " + translationResult.translationUnits.size)

        val sessionAndSessionFactoryPair = connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            if (PURGE_DB) session.purgeDatabase()
            session.save(translationResult.translationUnits, depth)
            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
    }

    /**
     * Connects to the neo4j db.
     *
     * @return a Pair of Optionals of the Session and the SessionFactory, if it is possible to
     * connect to neo4j. If it is not possible, the return value is a Pair of empty Optionals.
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     * neo4j db.
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
                sessionFactory = SessionFactory(configuration, "de.fraunhofer.aisec.cpg.graph")
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
     * Parse the file paths to analyze and set up the translationConfiguration with these paths.
     *
     * @throws IllegalArgumentException, if there was no arguments provided, or the path does not
     * point to a file, is a directory or point to a hidden file or the paths does not have the same
     * top level path.
     */
    @OptIn(ExperimentalPython::class, de.fraunhofer.aisec.cpg.ExperimentalGolang::class)
    private fun setupTranslationConfiguration(): TranslationConfiguration {
        assert(files.isNotEmpty())
        val filePaths = arrayOfNulls<File>(files.size)
        var topLevel: File? = null

        for (index in files.indices) {
            val path = Paths.get(files[index]).toAbsolutePath().normalize()
            val file = File(path.toString())
            require(file.exists() && (!file.isHidden)) {
                "Please use a correct path. It was: $path"
            }
            val currentTopLevel = if (file.isDirectory) file else file.parentFile
            if (topLevel == null) topLevel = currentTopLevel
            require(topLevel.toString() == currentTopLevel.toString()) {
                "All files should have the same top level path."
            }
            filePaths[index] = file
        }

        val translationConfiguration =
            TranslationConfiguration.builder()
                .sourceLocations(*filePaths)
                .topLevel(topLevel!!)
                .defaultPasses()
                .defaultLanguages()
                .loadIncludes(loadIncludes)
                .debugParser(DEBUG_PARSER)

        if (enableExperimentalPython) {
            translationConfiguration.registerLanguage(
                PythonLanguageFrontend::class.java,
                PythonLanguageFrontend.PY_EXTENSIONS
            )
        }

        if (enableExperimentalGo) {
            translationConfiguration.registerLanguage(
                GoLanguageFrontend::class.java,
                GoLanguageFrontend.GOLANG_EXTENSIONS
            )
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

        return translationConfiguration.build()
    }

    /**
     * A generic pair.
     *
     * @author Andreas Hager, andreas.hager@aisec.fraunhofer.de
     */
    class Pair<T, U>(val first: T, val second: U)

    /**
     * The entrypoint of the cpg-vis-neo4j.
     *
     * @throws IllegalArgumentException, if there was no arguments provided, or the path does not
     * point to a file, is a directory or point to a hidden file or the paths does not have the same
     * top level path
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     * neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(Exception::class, ConnectException::class, IllegalArgumentException::class)
    override fun call(): Int {
        val translationConfiguration = setupTranslationConfiguration()

        val startTime = System.currentTimeMillis()

        val translationResult =
            TranslationManager.builder().config(translationConfiguration).build().analyze().get()

        val analyzingTime = System.currentTimeMillis()
        log.info(
            "Benchmark: analyzing code in " + (analyzingTime - startTime) / S_TO_MS_FACTOR + " s."
        )

        pushToNeo4j(translationResult)

        val pushTime = System.currentTimeMillis()
        log.info("Benchmark: push code in " + (pushTime - analyzingTime) / S_TO_MS_FACTOR + " s.")

        return EXIT_SUCCESS
    }
}

/**
 * Starts a command line application of the cpg-vis-neo4j.
 *
 * @throws IllegalArgumentException, if there was no arguments provided, or the path does not point
 * to a file, is a directory or point to a hidden file or the paths does not have the same top level
 * path
 * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the neo4j
 * db.
 * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
 */
fun main(args: Array<String>) {
    val exitCode = CommandLine(Application()).execute(*args)
    exitProcess(exitCode)
}
