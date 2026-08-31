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

/**
 * Special [LLMPropertyDescription.type] / [LLMProperty.type] value indicating that a property's
 * `value` is not a plain scalar but the CPG id of another node in the graph. When applying a
 * concept/operation, such properties are resolved against the current translation result and stored
 * as an actual reference to that node instead of a stringified id.
 */
const val NODE_REFERENCE_TYPE = "NodeReference"

@Serializable
data class LLMPropertyDescription(
    @Description("The name of the property. It should be short and precises, preferably one word.")
    val name: String,
    @Description(
        "The type of the property. It should be a simple Kotlin data type (e.g. String, Int, Long, Float, " +
            "Double, Boolean), which is parsed into a value of that type when the concept/operation is applied, " +
            "or the special value \"NodeReference\" to indicate that the property's value must be the CPG id " +
            "of another node in the graph (e.g. to relate this concept to a different node than the one it is " +
            "attached to). Any unrecognized type name is treated as text."
    )
    val type: String,
    @Description("A short description of the property.") val description: String?,
    @Description(
        "If set, this property has a value that is intrinsic to the concept/operation definition itself " +
            "(e.g. a specific ID from a taxonomy) and must not vary between applications of this concept/operation. " +
            "Callers do not need to supply a value for this property; it is applied automatically, and any value " +
            "a caller does supply for it is overridden with this fixed value."
    )
    val fixedValue: String? = null,
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
        "The type of the property. It should be a simple Kotlin data type (e.g. String, Int, Long, Float, " +
            "Double, Boolean), or the special value \"NodeReference\" if the value refers to another node in " +
            "the graph. It should match the type declared for this property in the concept/operation schema."
    )
    val type: String,
    @Description("A short description of the property.") val description: String? = null,
    @Description(
        "The value to set for the property (as string representation). The value must be parsable as the type " +
            "given in the `type` field, otherwise applying the concept/operation fails: use e.g. \"42\" for an " +
            "Int, \"1.5\" for a Double, and \"true\" or \"false\" for a Boolean. " +
            "If `type` is \"NodeReference\", this must be the CPG id of an existing node from the current translation " +
            "result (not a placeholder or invented id)."
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
