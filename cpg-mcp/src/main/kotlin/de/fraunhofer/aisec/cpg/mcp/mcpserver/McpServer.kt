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
package de.fraunhofer.aisec.cpg.mcp.mcpserver

import de.fraunhofer.aisec.codyze.console.listCalls
import de.fraunhofer.aisec.codyze.console.listCallsTo
import de.fraunhofer.aisec.codyze.console.listFunctions
import de.fraunhofer.aisec.codyze.console.listRecords
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgAnalyzeTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgApplyConceptsTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgDataflowTool
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.addCpgLlmAnalyzeTool
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions

fun configureServer(): Server {
    val info = Implementation(name = "cpg-mcp-server", version = "1.0.0")

    val options =
        ServerOptions(
            capabilities =
                ServerCapabilities(
                    prompts = ServerCapabilities.Prompts(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true),
                )
        )

    val server = Server(info, options)
    server.addCpgAnalyzeTool()
    server.addCpgLlmAnalyzeTool()
    server.addCpgApplyConceptsTool()
    server.addCpgDataflowTool()
    server.listFunctions()
    server.listRecords()
    server.listCalls()
    server.listCallsTo()

    return server
}
