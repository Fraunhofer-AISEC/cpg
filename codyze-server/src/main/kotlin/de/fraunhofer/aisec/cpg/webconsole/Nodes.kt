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
package de.fraunhofer.aisec.cpg.webconsole

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TranslationResult(
    val components: List<ComponentJSON>,
    val totalNodes: Int,
    @Transient val cpgResult: de.fraunhofer.aisec.cpg.TranslationResult? = null,
)

@Serializable
data class ComponentJSON(val name: String, val translationUnits: List<TranslationUnit>)

@Serializable data class TranslationUnit(val name: String, val path: String, val code: String)

@Serializable
data class NodeInfo(
    val id: String,
    val type: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val code: String,
    val name: String,
)
