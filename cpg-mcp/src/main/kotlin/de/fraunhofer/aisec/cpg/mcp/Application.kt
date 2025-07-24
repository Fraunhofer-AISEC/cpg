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
package de.fraunhofer.aisec.cpg.mcp

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import java.io.File
import java.net.ConnectException
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val DEBUG_PARSER = true

/**
 * Checks if all elements in the parameter are a valid file and returns a list of files.
 *
 * @param filenames The filenames to check
 * @return List of files
 */
private fun getFilesOfList(filenames: Collection<String>): List<File> {
    val filePaths = filenames.map { Paths.get(it).toAbsolutePath().normalize().toFile() }
    filePaths.forEach {
        require(it.exists() && (!it.isHidden)) { "Please use a correct path. It was: ${it.path}" }
    }
    return filePaths
}

/**
 * Parse the file paths to analyze and set up the translationConfiguration with these paths.
 *
 * @throws IllegalArgumentException, if there were no arguments provided, or the path does not point
 *   to a file, is a directory or point to a hidden file or the paths does not have the same top
 *   level path.
 */
fun setupTranslationConfiguration(
    topLevel: File?,
    files: Collection<String>,
    includePaths: List<Path>,
    includesFile: File? = null,
    maxComplexity: Int = -1,
    loadIncludes: Boolean = true,
    exclusionPatterns: Collection<String> = listOf(),
    useUnityBuild: Boolean = false,
): TranslationConfiguration {
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
            .loadIncludes(loadIncludes)
            .exclusionPatterns(*exclusionPatterns.toTypedArray())
            .addIncludesToGraph(loadIncludes)
            .debugParser(DEBUG_PARSER)
            .useUnityBuild(useUnityBuild)
            .useParallelPasses(false)

    topLevel?.let { translationConfiguration.topLevel(it) }

    if (maxComplexity != -1) {
        translationConfiguration.configurePass<ControlFlowSensitiveDFGPass>(
            ControlFlowSensitiveDFGPass.Configuration(maxComplexity = maxComplexity)
        )
    }

    includePaths.forEach { translationConfiguration.includePath(it) }

    val filePaths = getFilesOfList(files)
    translationConfiguration.sourceLocations(filePaths)

    translationConfiguration.defaultPasses()
    translationConfiguration.registerPass<ControlDependenceGraphPass>()
    translationConfiguration.registerPass<ProgramDependenceGraphPass>()
    translationConfiguration.registerPass<PythonFileConceptPass>()

    translationConfiguration.registerPass(PrepareSerialization::class)

    includesFile?.let { theFile ->
        val baseDir = File(theFile.toString()).parentFile?.toString() ?: ""
        theFile
            .inputStream()
            .bufferedReader()
            .lines()
            .map(String::trim)
            .map { if (Paths.get(it).isAbsolute) it else Paths.get(baseDir, it).toString() }
            .forEach { translationConfiguration.includePath(it) }
    }

    translationConfiguration.inferenceConfiguration(
        InferenceConfiguration.builder().inferRecords(true).build()
    )
    return translationConfiguration.build()
}

/**
 * Starts a command line application of the cpg-vis-neo4j.
 *
 * @throws IllegalArgumentException, if there was no argument provided, or the path does not point
 *   to a file, is a directory or point to a hidden file or the paths does not have the same top
 *   level path
 * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the neo4j
 *   db.
 * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
 */
fun main() {
    runBlocking { runSseMcpServerUsingKtorPlugin(3001, configureServer()) }
}

/**
 * Starts an SSE (Server Sent Events) MCP server using the Ktor framework and the specified port.
 *
 * The url can be accessed in the MCP inspector at [http://localhost:$port]
 *
 * @param port The port number on which the SSE MCP server will listen for client connections.
 */
fun runSseMcpServerUsingKtorPlugin(port: Int, server: Server) {
    embeddedServer(CIO, host = "0.0.0.0", port = port) { mcp { server } }.start(wait = true)
}

fun configureServer(): Server {
    // Create the MCP Server instance with a basic implementation
    val server =
        Server(
            Implementation(
                name = "cpg", // Tool name is "cpg"
                version = "0.0.1", // Version of the implementation
            ),
            ServerOptions(
                capabilities =
                    ServerCapabilities(
                        prompts = ServerCapabilities.Prompts(listChanged = true),
                        resources =
                            ServerCapabilities.Resources(subscribe = true, listChanged = true),
                        tools = ServerCapabilities.Tools(listChanged = true),
                    )
            ),
        )

    // Add CPG Upload tool
    server.addTool(
        name = "cpg upload",
        description = "The CPG upload tool",
        inputSchema =
            Tool.Input(
                properties =
                    buildJsonObject {
                        putJsonObject("file") {
                            put("type", "string")
                            put("description", "Source code file to analyze")
                        }
                    },
                required = listOf("file"),
            ),
    ) { request ->
        val filePath = request.arguments["file"]?.jsonPrimitive?.content
        requireNotNull(filePath) { "No file uploaded" }
        // TODO: Replace this with a temporary file?
        // Or: Provide a list of "resources" = projects/files/directories which can be analyzed and
        // where we then run the queries.
        val file = File(filePath)
        // TODO: Dump the content of the file upload to the temp file.

        // Call setupTranslationConfiguration with the uploaded file
        val config =
            setupTranslationConfiguration(
                topLevel = file,
                files = listOf(file.absolutePath),
                includePaths = emptyList(),
            )
        CallToolResult(content = listOf(TextContent("Hello, world!")))
    }

    return server
}
