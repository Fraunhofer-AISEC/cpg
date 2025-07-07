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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * This file demonstrates how simpler concepts and operations could be created with the improvements
 * to enable easier data class conversion in specific cases.
 *
 * While full inheritance from OverlayNode prevents true data classes, the improvements make it
 * easier to work with concepts and operations that have simple additional fields.
 */

// Example of a concept with additional properties that could benefit from data class patterns
// Before: Would need manual equals/hashCode implementation
// After: With concept properties as constructor params, simpler patterns become possible
class SimpleConceptWithData(underlyingNode: Node? = null, val additionalData: String) :
    Concept(underlyingNode) {
    // Before the improvement: Manual equals/hashCode would be needed here
    // After: The base class handles the basic equality, and for simple cases
    // like this, users might be able to avoid manual implementations in some scenarios
}

// Example of an operation with additional properties
// With the concept parameter now being a constructor property, this becomes cleaner
class SimpleOperationWithData(
    underlyingNode: Node? = null,
    concept: Concept,
    val operationData: String,
) : Operation(underlyingNode, concept) {
    // Before: Manual equals/hashCode implementation would be required
    // After: With concept as a constructor property, the base implementation is more robust
    // Users still need manual equals/hashCode for additional fields, but the foundation is better
}

// For very simple cases with no additional fields, the base implementations may suffice:
class VerySimpleConcept(underlyingNode: Node? = null) : Concept(underlyingNode)

class VerySimpleOperation(underlyingNode: Node? = null, concept: Concept) :
    Operation(underlyingNode, concept)
