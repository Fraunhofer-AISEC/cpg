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
package de.fraunhofer.aisec.cpg.graph.concepts.simple

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * A simple data class concept that demonstrates the improved data class support. This concept
 * inherits from Concept and benefits from all data class features.
 */
data class SimpleDataConcept(
    val conceptId: String,
    val description: String = "Default description",
) : Concept()

/** A simple data class operation that works with SimpleDataConcept. */
data class SimpleDataOperation(val operationId: String, override val concept: SimpleDataConcept) :
    Operation(concept = concept)

/** Example user-specific concept using data class functionality. */
data class UserConcept(
    val userId: String,
    val username: String,
    val roles: Set<String> = emptySet(),
) : Concept()

/** Example configuration concept using data class functionality. */
data class ConfigConcept(val configName: String, val settings: Map<String, Any> = emptyMap()) :
    Concept()
