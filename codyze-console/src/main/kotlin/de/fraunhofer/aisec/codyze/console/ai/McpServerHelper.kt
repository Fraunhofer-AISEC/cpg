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
package de.fraunhofer.aisec.codyze.console.ai

import org.slf4j.LoggerFactory

/**
 * Helper object to make the features of the `cpg-mcp` module conditionally available. When the
 * module is available in the build, this will use the actual MCP functions directly.
 */
object McpServerHelper {
    private val log = LoggerFactory.getLogger(McpServerHelper::class.java)
    /** Check if mcp module is enabled */
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

    /** Set the global analysis result in the `cpg-mcp` module */
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
            log.warn("Failed to set globalAnalysisResult in cpg-mcp module: {}", e.message, e)
        }
    }
}
