/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze.console

import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * Reflective bridge to the optional `cpg-ai` module (MCP server plus the `ChatService` chat/tool
 * API). The module may be absent from the build entirely (see `enableAIModule` in
 * `gradle.properties`), so `codyze-console` cannot hold a compile-time dependency on its classes
 * (neither `McpServer` nor `ChatService`) - all access happens through reflection instead.
 */
object McpServerHelper {
    private val log = LoggerFactory.getLogger(McpServerHelper::class.java)

    /** Check if the `cpg-ai` module is available on the classpath. */
    val isEnabled: Boolean by lazy {
        try {
            Class.forName("de.fraunhofer.aisec.cpg.mcp.ApplicationKt")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun startMcpServer(port: Int) {
        if (!isEnabled) {
            return
        }

        try {
            log.info("Starting MCP server with streamable HTTP on port {}...", port)
            val mcpServerKt = Class.forName("de.fraunhofer.aisec.cpg.mcp.mcpserver.McpServerKt")
            val server = mcpServerKt.getMethod("configureDefaultServer").invoke(null)

            val appKt = Class.forName("de.fraunhofer.aisec.cpg.mcp.ApplicationKt")
            val runServer = appKt.methods.first { it.name == "runHttpMcpServerUsingKtorPlugin" }
            runServer.invoke(null, port, "0.0.0.0", server, false)
        } catch (e: Exception) {
            log.error("Failed to start MCP server: {}", e.message, e)
        }
    }

    /** Set the global analysis result in the `cpg-ai` module. */
    fun setGlobalAnalysisResult(result: de.fraunhofer.aisec.cpg.TranslationResult) {
        if (!isEnabled) {
            return
        }

        try {
            val toolsClass =
                Class.forName("de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.CpgAnalyzeToolKt")
            val setResult = toolsClass.getMethod("setGlobalAnalysisResult", result.javaClass)
            setResult.invoke(null, result)
        } catch (e: Exception) {
            log.warn("Failed to set globalAnalysisResult in cpg-ai module: {}", e.message, e)
        }
    }

    /**
     * Create a `ChatService` instance (as an opaque [Any]) if an LLM provider is configured, or
     * `null` otherwise.
     */
    fun createChatService(): Any? {
        if (!isEnabled) {
            return null
        }

        return try {
            val chatServiceClass =
                Class.forName("de.fraunhofer.aisec.codyze.console.ai.ChatService")
            val companion = chatServiceClass.getField("Companion").get(null)
            companion.javaClass.getMethod("createIfConfigExist").invoke(companion)
        } catch (e: Exception) {
            log.warn("Failed to create ChatService: {}", e.message, e)
            null
        }
    }

    /**
     * Connect the given `ChatService` instance (as returned by [createChatService]) to its MCP
     * server.
     */
    suspend fun connectChatService(chatService: Any) {
        chatService.callSuspendFunction("connect")
    }

    /** Process a chat request and return the streamed response as a [Flow] of text chunks. */
    fun chat(chatService: Any, requestJson: JsonObject): Flow<String> {
        val method = chatService.javaClass.getMethod("chatJson", JsonObject::class.java)
        @Suppress("UNCHECKED_CAST")
        return method.invoke(chatService, requestJson) as Flow<String>
    }

    /** List the available LLM providers as a [JsonElement]. */
    suspend fun listAvailableProviders(chatService: Any): JsonElement =
        chatService.callSuspendFunction("listAvailableProvidersJson") as JsonElement

    /** Get the MCP capabilities (tools, prompts, resources) as a [JsonElement]. */
    fun getMcpCapabilities(chatService: Any): JsonElement {
        val method = chatService.javaClass.getMethod("getMcpCapabilitiesJson")
        return method.invoke(chatService) as JsonElement
    }

    /** Resolve an MCP prompt as a [JsonElement]. */
    suspend fun getPrompt(
        chatService: Any,
        name: String,
        arguments: Map<String, String>,
    ): JsonElement =
        chatService.callSuspendFunction("getPromptJson", name, arguments) as JsonElement

    /** Call an MCP tool directly and return the result as a [JsonElement]. */
    suspend fun callTool(chatService: Any, name: String, arguments: JsonObject): JsonElement =
        chatService.callSuspendFunction("callTool", name, arguments) as JsonElement

    /** Return the discovered skills as a [JsonElement]. */
    fun getSkills(chatService: Any): JsonElement {
        val method = chatService.javaClass.getMethod("getSkillsJson")
        return method.invoke(chatService) as JsonElement
    }

    /** Invoke a suspend member function of [this] by name via reflection. */
    private suspend fun Any.callSuspendFunction(name: String, vararg args: Any?): Any? {
        val function = this::class.memberFunctions.first { it.name == name }
        return function.callSuspend(this, *args)
    }
}
