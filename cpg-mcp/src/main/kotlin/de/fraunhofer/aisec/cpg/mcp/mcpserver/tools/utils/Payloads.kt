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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.passes.Description
import kotlinx.serialization.Serializable

@Serializable
data class CpgAnalyzePayload(
    @Description("The contents of the file which should be analyzed.") val content: String? = null,
    @Description(
        "The file extension. This is required to identify the programming language and should resemble the typical file ending (e.g. '.py' for python, '.c' for C code)."
    )
    val extension: String? = null,
)

@Serializable
@Description("The payload to identify a node by its name.")
data class CpgNamePayload(@Description("The local name of the node to consider.") val name: String)

@Serializable
@Description("The payload to identify a node by its id.")
data class CpgIdPayload(@Description("The id of the node to consider.") val id: String)

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
)

@Serializable
data class CpgApplyConceptsPayload(
    @Description("List of concept assignments to perform") val assignments: List<ConceptAssignment>
)

@Serializable
data class CpgRunPassPayload(
    @Description("The FQN of the pass to run.") val passName: String,
    @Description("The ID of the node which should be analyzed by the pass.") val nodeId: String,
)

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
)

/**
 * This class represents the payload for a CPG data flow analysis request, containing the source and
 * target concept types.
 */
@Serializable
data class CpgDataflowPayload(
    @Description("Source concept type (e.g., 'ReadData', 'Data', 'Authentication')")
    val from: String,
    @Description("Target concept type (e.g., 'HttpRequest', 'CallExpression')") val to: String,
)

@Serializable
data class CpgLlmAnalyzePayload(
    @Description("A special description of what to take care of while analyzing the target")
    val description: String? = null
)

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
