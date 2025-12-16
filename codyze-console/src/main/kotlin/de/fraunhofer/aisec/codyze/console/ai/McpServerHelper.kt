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

/**
 * Helper object to make the features of the `cpg-mcp` module conditionally available. When the
 * module is available in the build, this will use the actual MCP functions directly.
 */
object McpServerHelper {
    /** Check if mcp module is enabled */
    val isEnabled: Boolean by lazy {
        try {
            Class.forName("de.fraunhofer.aisec.cpg.mcp.ApplicationKt")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    suspend fun startMcpServer(port: Int) {
        if (!isEnabled) {
            return
        }

        try {
            println("Starting MCP server on port $port...")
            val server = de.fraunhofer.aisec.cpg.mcp.mcpserver.configureServer()
            de.fraunhofer.aisec.cpg.mcp.runSseMcpServerUsingKtorPlugin(port, server)
        } catch (e: Exception) {
            println("Failed to start MCP server: ${e.message}")
            e.printStackTrace()
        }
    }

    /** Set the global analysis result in the `cpg-mcp` module */
    fun setGlobalAnalysisResult(result: de.fraunhofer.aisec.cpg.TranslationResult) {
        if (!isEnabled) {
            return
        }

        try {
            de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult = result
        } catch (e: Exception) {
            println("Warning: Failed to set globalAnalysisResult in cpg-mcp module: ${e.message}")
            e.printStackTrace()
        }
    }
}
