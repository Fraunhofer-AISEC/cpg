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

import kotlinx.serialization.Serializable

@Serializable
data class CpgAnalyzePayload(val content: String? = null, val extension: String? = null)

@Serializable data class CpgNamePayload(val name: String)

@Serializable data class CpgIdPayload(val id: String)

@Serializable
data class CpgCallArgumentByNameOrIndexPayload(
    val id: String,
    val argumentName: String? = null,
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

@Serializable
data class PassInfo(
    val fqn: String,
    val description: String,
    val requiredNodeType: String,
    val dependsOn: List<String>,
)
