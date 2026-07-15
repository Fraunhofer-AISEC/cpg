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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.mcp.mcpserver.cpgDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableConcepts
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.getAvailableOperations
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

fun Server.addSuggestConceptsPrompt() {
    this.addPrompt(
        name = "suggest_concepts",
        description =
            "Generates a prompt that guides the LLM to suggest CPG concepts and operations for the analyzed code. Use this before applying concepts with cpg_apply_concepts.",
        arguments =
            listOf(
                PromptArgument(
                    name = "description",
                    description =
                        "Optional additional context or focus area for the analysis, e.g. 'focus on authentication handling' or 'look for encryption handling'.",
                    required = false,
                )
            ),
    ) { request ->
        val description = request.arguments?.get("description")
        val availableConcepts = getAvailableConcepts()
        val availableOperations = getAvailableOperations()

        GetPromptResult(
            description =
                "Guides the LLM to explore the CPG and suggest concept/operation overlays for security-relevant nodes.",
            messages =
                listOf(
                    PromptMessage(
                        role = Role.User,
                        content =
                            TextContent(
                                text =
                                    buildString {
                                        appendLine("## About CPG")
                                        appendLine(cpgDescription)
                                        appendLine()

                                        appendLine("## Goal")
                                        appendLine(
                                            "Identify nodes in the CPG that represent security-relevant data or operations, and suggest appropriate concept/operation overlays for them."
                                        )
                                        appendLine(
                                            "This allows us to analyze how sensitive data flows through the code and discover security-relevant patterns."
                                        )
                                        appendLine()

                                        appendLine("## Understanding Concepts and Operations")
                                        appendLine()
                                        appendLine("**Concepts** mark 'what something IS':")
                                        appendLine(
                                            "- Applied to data-holding nodes (variables, fields)"
                                        )
                                        appendLine(
                                            "- Examples: user_email → Data, api_token → Secret"
                                        )
                                        appendLine(
                                            "- Purpose: Track where important data is stored"
                                        )
                                        appendLine()
                                        appendLine("**Operations** mark 'what something DOES':")
                                        appendLine(
                                            "- Applied to nodes that perform actions (function calls, method calls)"
                                        )
                                        appendLine(
                                            "- Examples: requests.post() → HttpRequest, file.write() → FileWrite, encrypt() → Encryption"
                                        )
                                        appendLine(
                                            "- Purpose: Track what happens to important data"
                                        )
                                        appendLine()

                                        appendLine("## Rules")
                                        appendLine(
                                            "1. **Domain consistency**: Operations must match their Concept's domain (e.g., GetSecret needs Secret, HttpRequest needs HttpClient)"
                                        )
                                        appendLine(
                                            "2. **Operations always need Concepts**: Every Operation MUST have a conceptNodeId pointing to an existing Concept node. Concepts can stand alone."
                                        )
                                        appendLine(
                                            "3. **Use only existing overlays**: Only use the concepts and operations listed below."
                                        )
                                        appendLine()

                                        appendLine("## Available Concepts")
                                        availableConcepts.forEach { appendLine("- ${it.name}") }
                                        appendLine()
                                        appendLine("## Available Operations")
                                        availableOperations.forEach { appendLine("- ${it.name}") }
                                        appendLine()

                                        if (description != null) {
                                            appendLine("## Additional Context")
                                            appendLine(description)
                                            appendLine()
                                        }

                                        appendLine("## How to Explore the Code")
                                        appendLine(
                                            "You can use the tools to explore the code before making suggestions."
                                        )
                                        appendLine(
                                            "The listing tools e.g., `cpg_list_functions` or `cpg_list_records`, provide a summary of functions or classes with the name, parameters and code."
                                        )
                                        appendLine(
                                            "To inspect interesting nodes in more detail use `cpg_get_node`. This retrieves the complete details of a specific node filtered by its ID."
                                        )

                                        appendLine("## Expected Response Format")
                                        appendLine(
                                            "After exploring the code, respond with a JSON object in this exact format:"
                                        )
                                        appendLine(
                                            """
                    {
                      "conceptAssignments": [
                        {
                          "nodeId": "1234",
                          "overlay": "fully.qualified.class.Name",
                          "overlayType": "Concept",
                          "reasoning": "Reason for this classification",
                          "securityImpact": "Potential security implications"
                        },
                        {
                          "nodeId": "5678",
                          "overlay": "fully.qualified.class.Name",
                          "overlayType": "Operation",
                          "conceptNodeId": "1234",
                          "reasoning": "Reason for this classification",
                          "securityImpact": "Potential security implications"
                        }
                      ]
                    }
                    """
                                                .trimIndent()
                                        )
                                        appendLine()
                                        appendLine(
                                            "**IMPORTANT**: After providing your suggestions, WAIT for user approval before applying anything. Do not automatically call `cpg_apply_concepts`."
                                        )
                                    }
                            ),
                    )
                ),
        )
    }
}
