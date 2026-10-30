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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.passes.Description
import kotlinx.serialization.Serializable

/** The description of the `projectName` that every [HasProjectNamePayload] exposes to the LLM. */
const val PROJECT_NAME_DESCRIPTION =
    "The name of the analyzed project this tool should operate on, as returned by cpg_analyze or cpg_translate. Can be omitted if only one project is analyzed."

/** The payload of tools that need nothing but the project to operate on. */
@Serializable
@Description("The payload to identify an analyzed project by its name.")
data class CpgProjectPayload(
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null
) : HasProjectNamePayload

@Serializable
data class CpgAnalyzePayload(
    @Description(
        "The contents of the file which should be analyzed. Alternatively, 'path' can be used to analyze files or whole projects on the local filesystem."
    )
    val content: String? = null,
    @Description(
        "The file extension. This is required to identify the programming language when providing 'content' and should resemble the typical file ending (e.g. '.py' for python, '.c' for C code)."
    )
    val extension: String? = null,
    @Description(
        "The path to a source file or a project directory (e.g., a repository checkout) on the local filesystem. For directories, the project structure is detected automatically, e.g., components based on Go modules or a C/C++ compilation database (compile_commands.json). Either 'path' or 'content' must be provided."
    )
    // NOTE: This is the one MCP tool that breaks the "the model only reasons via the CPG"
    // invariant the rest of the tool set upholds: the model names a host path and the server reads
    // it. There is no path allowlist or jail; the model can name any path the process can read,
    // and the parsed source becomes reachable through cpg_get_node / cpg_list_functions. The MCP
    // server itself has no authentication and defaults to loopback only (see Application.kt), but
    // any non-loopback bind (allowed with a warning via --host) plus a prompt-injected or
    // untrusted model turns this into arbitrary host-source exfiltration. Hosts that expose the
    // server beyond loopback must treat cpg_analyze as an explicit, auditable trust decision.
    val path: String? = null,
)

@Serializable
@Description("The payload to identify a node by its id.")
data class CpgIdPayload(
    @Description("The id of the node to consider.") val id: String,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
@Description("The payload to identify a batch of functions by their names.")
data class CpgNamesPayload(
    @Description(
        "The local names of the functions to look up. Keep this batch reasonably small (e.g. around 10 names) to avoid overly large responses."
    )
    val names: List<String>,
    @Description(
        "Whether to include each function's source code in the response. Defaults to true. Set to false if you only need to confirm a function exists or inspect its signature/parameters - use cpg_get_node afterwards to fetch the full source for a specific one once picked."
    )
    val includeCode: Boolean = true,
    @Description(LIMIT_DESCRIPTION) val limit: Int? = null,
    @Description(OFFSET_DESCRIPTION) val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
data class CpgCallArgumentByNameOrIndexPayload(
    @Description("ID of the method/function call whose arguments should be listed.")
    val nodeId: String,
    @Description("The name of the argument (if arguments can be passed by name).")
    val argumentName: String? = null,
    @Description(
        "The index/position of the argument. The first argument is at index 0. We do not support the base/receiver of a method call here."
    )
    val index: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

/** Shared parameter descriptions of the paginated tools' `limit`/`offset`. */
private const val LIMIT_DESCRIPTION =
    "Maximum number of items to return. Defaults to 20 if not specified."
private const val OFFSET_DESCRIPTION =
    "Number of items to skip before returning results, for pagination. Defaults to 0."

@Serializable
@Description("The payload to list the calls to a function, paginated.")
data class CpgListCallsToPayload(
    @Description("The local name of the function or method whose calls should be listed.")
    val name: String,
    @Description(LIMIT_DESCRIPTION) val limit: Int? = null,
    @Description(OFFSET_DESCRIPTION) val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
@Description("The payload to trace a node's data back to its sources, paginated.")
data class CpgDfgBackwardPayload(
    @Description("The id of the node to start from.") val id: String,
    @Description(LIMIT_DESCRIPTION) val limit: Int? = null,
    @Description(OFFSET_DESCRIPTION) val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

/** Shared parameter descriptions of the filters of the listing tools. */
private const val PATTERN_DESCRIPTION =
    "Only include nodes whose name matches this regular expression (case-insensitive, partial match)."
private const val FILE_DESCRIPTION =
    "Only include nodes located in a file whose path contains this string, e.g. 'main.c'."
private const val DETAIL_DESCRIPTION =
    "If true, return each node's full summary (without source code); by default only its nodeId and a one-line signature."

@Serializable
@Description("The payload to filter and page through a listing of nodes.")
data class CpgListNodesPayload(
    @Description(PATTERN_DESCRIPTION) val pattern: String? = null,
    @Description(FILE_DESCRIPTION) val file: String? = null,
    @Description(DETAIL_DESCRIPTION) val detail: Boolean = false,
    @Description(LIMIT_DESCRIPTION) val limit: Int? = null,
    @Description(OFFSET_DESCRIPTION) val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
@Description("The payload to filter and page through a listing of functions.")
data class CpgListFunctionsPayload(
    @Description(PATTERN_DESCRIPTION) val pattern: String? = null,
    @Description(FILE_DESCRIPTION) val file: String? = null,
    @Description("Only include functions that call a function with this local name, e.g. 'memcpy'.")
    val calls: String? = null,
    @Description(DETAIL_DESCRIPTION) val detail: Boolean = false,
    @Description(LIMIT_DESCRIPTION) val limit: Int? = null,
    @Description(OFFSET_DESCRIPTION) val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
@Description("The payload to page through a listing.")
data class CpgListPayload(
    @Description("Maximum number of items to return. Defaults to 20 if not specified.")
    val limit: Int? = null,
    @Description("Number of items to skip before returning results, for pagination. Defaults to 0.")
    val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
data class CpgApplyConceptsPayload(
    @Description("List of concept assignments to perform") val assignments: List<ConceptAssignment>,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
data class CpgRunPassPayload(
    @Description("The FQN of the pass to run.") val passName: String,
    @Description("The ID of the node which should be analyzed by the pass.") val nodeId: String,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

@Serializable
data class KeyValuePair<K, V>(
    @Description("The key of the key-value pair") val key: K,
    @Description("The value of the key-value pair") val value: V,
)

@Serializable
data class ConceptAssignment(
    @Description("ID of the node to apply overlay to") val nodeId: String,
    @Description("Fully qualified name of concept or operation class") val overlay: String,
    @Description("Type of overlay: 'Concept' or 'Operation'") val overlayType: String? = null,
    @Description("NodeId of the concept this operation references (only for operations)")
    val conceptNodeId: String? = null,
    @Description("Additional constructor arguments (optional)")
    val arguments: List<KeyValuePair<String, String>>? = null,
    @Description("Reasoning for applying this concept/operation (optional)")
    val reasoning: String? = null,
    @Description("A description if this concept could have security implications (optional)")
    val securityImpact: String? = null,
    val name: String? = null,
    val type: String? = null,
    val code: String? = null,
    val fileName: String? = null,
    val startLine: Int? = null,
    val endLine: Int? = null,
)

/**
 * This class represents the payload for a CPG data flow analysis request, containing the source and
 * target concept types.
 */
@Serializable
data class CpgDataflowPayload(
    @Description(
        "Source concept or operation: a catalog type (e.g., 'ReadData', 'Data', 'Authentication') or the name of one defined with cpg_add_llm_concept_and_operations."
    )
    val from: String,
    @Description(
        "Target concept or operation: a catalog type (e.g., 'HttpRequest', 'Call') or the name of one defined with cpg_add_llm_concept_and_operations."
    )
    val to: String,
    @Description(LIMIT_DESCRIPTION) val limit: Int? = null,
    @Description(OFFSET_DESCRIPTION) val offset: Int? = null,
    @Description(PROJECT_NAME_DESCRIPTION) override val projectName: String? = null,
) : HasProjectNamePayload

/**
 * This class represents information about a pass, including its fully qualified name (FQN), a
 * description, required node type, dependencies, and soft dependencies.
 */
@Serializable
data class PassInfo(
    @Description("The fully qualified name of the pass.") val fqn: String,
    @Description("A brief description of the pass.") val description: String,
    @Description("The type of node required by the pass.") val requiredNodeType: String,
    @Description(
        "A list of passes whose results are required for this pass to run correctly. These are hard requirements. Note that it may be sufficient to run these passes for the same nodes that this  pass should run on and may not require analyzing the whole CPG."
    )
    val dependsOn: List<String>,
    @Description(
        "A list of passes whose results can enhance the analysis of this pass but are not strictly necessary. These are soft requirements. However, if the passes in this list may be run on the node, this should happen before this pass."
    )
    val softDependencies: List<String>,
)
