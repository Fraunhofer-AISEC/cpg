/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

/**
 * This file demonstrates true data classes for concepts and operations by removing the
 * underlyingNode parameter completely. This enables the use of Kotlin data classes with automatic
 * equals/hashCode/toString/copy implementations.
 */

// Example of a simple data class concept - no underlyingNode, just pure data
data class SimpleDataConcept(val name: String, val value: Int)

// Example of a data class concept with multiple properties
data class UserConcept(val userId: String, val username: String, val email: String)

// Example of a data class concept for configuration
data class ConfigConcept(val key: String, val value: String, val environment: String)

// Example of a data class operation that works on concepts
data class ReadOperation(val concept: SimpleDataConcept, val timestamp: Long)

// Example of a data class operation with additional metadata
data class WriteOperation(val concept: UserConcept, val data: String, val operationType: String)

// Example of a data class operation for database interactions
data class DatabaseOperation(
    val concept: ConfigConcept,
    val query: String,
    val parameters: Map<String, Any>,
)
