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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.persistence.Convert
import de.fraunhofer.aisec.cpg.persistence.Relationship
import java.util.*

/**
 * A generic [Operation] for use when operation details are not yet known at compile time. This can
 * be used to dynamically load and persist operations from/to a file.
 *
 * @param underlyingNode The CPG node this operation is attached to.
 * @param operationName The name of the operation. This should be unique across all operations.
 * @param description A human-readable description of the operation.
 * @param genericLLMConcept The concept this operation belongs to.
 * @param properties The properties of the operation.
 */
class GenericLLMOperation(
    underlyingNode: Node? = null,
    val operationName: String,
    val description: String,
    val genericLLMConcept: GenericLLMConcept,
    @Convert(GenericPropertiesConverter::class) override val properties: GenericProperties,
) : Operation(concept = genericLLMConcept, underlyingNode = underlyingNode), HasGenericProperties {

    /** Lazy backing field for [propertyReferenceEdges]. */
    private var _propertyReferenceEdges: GenericPropertyReferences? = null

    /**
     * The nodes referenced by [properties], as real relationships. Derived from [properties], which
     * stays the single source of truth.
     *
     * The backing container is allocated lazily on first access, since most generic operations do
     * not reference any other node.
     */
    @Relationship(value = GenericPropertyReferenceEdge.RELATIONSHIP_NAME)
    override val propertyReferenceEdges: GenericPropertyReferences
        get() =
            _propertyReferenceEdges
                ?: buildGenericPropertyReferences(properties).also { _propertyReferenceEdges = it }

    override fun equals(other: Any?): Boolean {
        return other is GenericLLMOperation &&
            super.equals(other) &&
            other.operationName == this.operationName &&
            other.description == this.description &&
            other.genericLLMConcept == this.genericLLMConcept &&
            other.properties == this.properties
    }

    override fun hashCode() =
        Objects.hash(super.hashCode(), operationName, description, genericLLMConcept, properties)
}
