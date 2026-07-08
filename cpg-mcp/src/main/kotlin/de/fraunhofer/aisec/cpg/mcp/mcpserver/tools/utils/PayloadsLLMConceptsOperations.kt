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
data class LLMPropertyDescription(
    @Description("The name of the property. It should be short and precises, preferably one word.")
    val name: String,
    @Description(
        "The type of the property. It should be a simple Kotlin data type (e.g. String, Integer, Boolean)."
    )
    val type: String,
    @Description("A short description of the property.") val description: String?,
) {
    constructor(
        property: LLMProperty
    ) : this(name = property.name, type = property.type, description = property.description)
}

@Serializable
data class LLMOperationDescription(
    @Description("The name of the operation. It should be short and precises, preferably one word.")
    val name: String,
    @Description(
        "The description of the operation. It should explain what the operation does and provide guidance on when to apply it."
    )
    val description: String,
    @Description("The parameters of the operation.") val properties: List<LLMPropertyDescription>,
) {
    constructor(
        operation: LLMOperation
    ) : this(
        name = operation.name,
        description = operation.description,
        properties = operation.properties.map { LLMPropertyDescription(it) },
    )
}

@Serializable
data class LLMConceptDescription(
    @Description("The name of the concept. It should be short and precises, preferably one word.")
    val name: String,
    @Description(
        "The description of the concept. It should explain the concept in more detail and provide guidance on when to apply it."
    )
    val description: String,
    @Description("The properties of the concept.") val properties: List<LLMPropertyDescription>,
    @Description(
        "The operations that can be applied to this concept. Each operation should have a name, a description, and a list of parameters."
    )
    val operations: List<LLMOperationDescription>,
) {
    constructor(
        concept: LLMConcept
    ) : this(
        name = concept.name,
        description = concept.description,
        properties = concept.properties.map { LLMPropertyDescription(it) },
        operations = concept.operations.map { LLMOperationDescription(it) },
    )
}

@Serializable
data class LLMProperty(
    @Description("The name of the property. It should be short and precises, preferably one word.")
    val name: String,
    @Description(
        "The type of the property. It should be a simple Kotlin data type (e.g. string, integer, boolean)."
    )
    val type: String,
    @Description("A short description of the property.") val description: String? = null,
    @Description(
        "The value to set for the property (as string representation). The type of the value should match the `type` field."
    )
    val value: String,
)

@Serializable
data class LLMOperation(
    @Description(
        "The name of the operation to apply. It must match the name of an operation defined in the concept."
    )
    val name: String,
    @Description(
        "The description of the operation. It should explain what the operation does and provide guidance on when to apply it."
    )
    val description: String,
    @Description("The CPG id of the node to which the operation should be applied.")
    val nodeId: String,
    @Description(
        "The properties to set for the operation. Each property should have a name and a value. The name should match the name of a parameter defined in the operation description, and the value should be the corresponding value for this specific application of the operation."
    )
    val properties: List<LLMProperty>,
)

@Serializable
data class LLMConcept(
    @Description("The name of the concept to apply. It must match the name of a concept.")
    val name: String,
    @Description(
        "The description of the concept. It should explain the concept in more detail and provide guidance on when to apply it."
    )
    val description: String,
    @Description("The CPG id of the node to which the concept should be applied.")
    val nodeId: String,
    @Description(
        "The properties to set for the concept. Each property should have a name and a value. The name should match the name of a parameter defined in the concept description, and the value should be the corresponding value for this specific application of the concept."
    )
    val properties: List<LLMProperty>,
    @Description("A list of operations to apply to this concept.")
    val operations: List<LLMOperation>,
)

@Serializable
data class LLMConceptList(
    @Description("A list of concepts with their operations to apply to the graph.")
    val concepts: List<LLMConcept>
)

@Serializable data class AppliedOperation(val operation: LLMOperation, val overlayNodeId: String)

@Serializable data class FailedOperation(val operation: LLMOperation, val reason: String)

@Serializable
data class AppliedConcept(
    val concept: LLMConcept,
    val overlayNodeId: String,
    val appliedOperations: List<AppliedOperation>,
    val failedOperations: List<FailedOperation>,
)

@Serializable data class FailedConcept(val concept: LLMConcept, val reason: String)

@Serializable
data class AddConceptsResult(val applied: List<AppliedConcept>, val failed: List<FailedConcept>)
