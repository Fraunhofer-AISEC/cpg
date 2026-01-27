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
data class CpgAnalyzePayload(val content: String? = null, val extension: String? = null)

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

@Serializable data class CpgApplyConceptsPayload(val assignments: List<ConceptAssignment>)

@Serializable
data class CpgRunPassPayload(
    /** The FQN of the pass to run. */
    val passName: String,
    /** The ID of the node which should be analyzed by the pass. */
    val nodeId: String,
)

@Serializable
data class ConceptAssignment(
    val nodeId: String,
    /* FQN of concept or operation class */
    val overlay: String,
    /* "Concept" or "Operation" from LLM response */
    val overlayType: String? = null,
    /* NodeId of concept this operation references */
    val conceptNodeId: String? = null,
    val arguments: Map<String, String>? = null,
    val reasoning: String? = null,
    val securityImpact: String? = null,
)

@Serializable data class CpgDataflowPayload(val from: String, val to: String)

@Serializable data class CpgLlmAnalyzePayload(val description: String? = null)

/**
 * This class represents information about a pass, including its fully qualified name (FQN), a
 * description, required node type, dependencies, and soft dependencies.
 */
@Serializable
data class PassInfo(
    /** The fully qualified name of the pass. */
    val fqn: String,
    /** A brief description of the pass. */
    val description: String,
    /** The type of node required by the pass. */
    val requiredNodeType: String,
    /**
     * A list of passes whose results are required for this pass to run correctly. These are hard
     * requirements. Note that it may be sufficient to run these passes for the same nodes that this
     * pass should run on and may not require analyzing the whole CPG.
     */
    val dependsOn: List<String>,
    /**
     * A list of passes whose results can enhance the analysis of this pass but are not strictly
     * necessary. These are soft requirements. However, if the passes in this list may be run on the
     * node, this should happen before this pass.
     */
    val softDependencies: List<String>,
)
