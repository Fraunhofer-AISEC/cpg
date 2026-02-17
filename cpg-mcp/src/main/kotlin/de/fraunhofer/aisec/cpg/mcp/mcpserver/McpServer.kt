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

import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions

fun configureServer(
    configure: Server.() -> Server = {
        //        this.addCpgTranslate()
        //        this.addListPasses()
        //        this.addRunPass()
        //        this.addCpgAnalyzeTool()
        this.addCpgLlmAnalyzeTool()
        this.addCpgApplyConceptsTool()
        this.addCpgDataflowTool()
        this.listFunctions()
        this.listRecords()
        this.listCalls()
        this.listCallsTo()
        //        this.listAvailableConcepts()
        //        this.listAvailableOperations()
        //        this.getAllArgs()
        //        this.getArgByIndexOrName()
        //        this.listConceptsAndOperations()
        this.getNode()
        this
    }
): Server {
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

    return Server(info, options).configure()
}

const val cpgDescription =
    """
This server provides tools to analyze the Fraunhofer AISEC CPG (Code Property Graph) and
allows to perform various operations on it. A CPG is a supergraph which consists of several subgraphs:
The AST (Abstract Syntax Tree) represents the code's structure and allows to navigate between the children and parents of individual nodes.
The DFG (Data Flow Graph) indicates that there are dataflows between two nodes.
The EOG (Evaluation Order Graph) represents the order in which nodes are evaluated on runtime of the program.
The CDG (Control Dependence Graph) indicates conditional executions of nodes, i.e., an edge in the CDG indicates that the target node is executed only if the source node is executed.
The PDG (Program Dependence Graph) combines the DFG and CDG to represent both data and control dependencies.
Each edge in the DFG, EOG, CDG and PDG works as follows: There is a source node and a target node and the source flows into target during a forward analysis, which is indicated by nextDFG, nextEOG, nextCDG or nextPDG.
Each edge is mirrored in the reverse direction, i.e., there is a prevDFG, prevEOG, prevCDG or prevPDG edge. For the edges in the AST, the children are the target node of the ast edge, the opposite direction is called astParent.

Further edges are the invokes edges which represent (potential) function and method calls between a CallExpression and a FunctionDeclaration or MethodDeclaration.
The DFG is inter-procedural, meaning that it can also represent data flows between different functions or methods.

Each node in the CPG has a unique ID, a name, a location in the file, and potentially one or multiple OverlayNodes which associate a node with additional information.
Examples of an OverlayNode are Concept and Operation, which are connected to their underlyingNode via the overlay/underlyingNode edge and are also connected with DFG and EOG edges.
"""
