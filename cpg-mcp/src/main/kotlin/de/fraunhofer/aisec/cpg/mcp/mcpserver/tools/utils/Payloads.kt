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

@Serializable data class CpgAnalyzePayload(val file: String? = null, val filePath: String? = null)

@Serializable data class CpgNamePayload(val name: String)

@Serializable data class CpgApplyConceptsPayload(val assignments: List<ConceptAssignment>)

@Serializable
data class ConceptAssignment(
    val nodeId: String,
    val overlay: String, // FQN of concept or operation class
)

@Serializable data class CpgDataflowPayload(val from: String, val to: String)

@Serializable data class CpgLlmAnalyzePayload(val description: String? = null)
