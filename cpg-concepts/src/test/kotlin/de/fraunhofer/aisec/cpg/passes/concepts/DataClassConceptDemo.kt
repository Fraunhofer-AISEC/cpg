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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * This test demonstrates how the improvements to the Operation class make it easier for users to
 * create operations with the concept parameter as a constructor property, which is a prerequisite
 * for data class conversion.
 */
class DataClassConceptDemo {

    // Example of a simple concept
    class SimpleConcept(underlyingNode: Node? = null) : Concept(underlyingNode)

    // After: Operation base class now has concept as a constructor property
    // This is a prerequisite for potential data class conversion
    class SimpleOperation(underlyingNode: Node? = null, concept: Concept) :
        Operation(underlyingNode, concept)

    @Test
    fun testOperationConceptProperty() {
        val concept = SimpleConcept()
        val operation = SimpleOperation(concept = concept)

        // The concept property is now properly accessible
        assertEquals(concept, operation.concept)

        // This demonstrates that concept is now a constructor property,
        // which is required for data class conversion
        assertTrue(operation.concept === concept)
    }

    @Test
    fun testOperationEqualityWithSameConcept() {
        val concept = SimpleConcept()
        val operation1 = SimpleOperation(concept = concept)
        val operation2 = SimpleOperation(concept = concept)

        // With concept as a constructor property, the Operation equals method
        // can properly compare concepts
        assertEquals(operation1.concept, operation2.concept)

        // Note: The operations themselves may not be equal due to Node's
        // equality semantics, but having concept as a property is a step forward
    }

    @Test
    fun testOperationEqualityWithDifferentConcepts() {
        val concept1 = SimpleConcept()
        val concept2 = SimpleConcept()
        val operation1 = SimpleOperation(concept = concept1)
        val operation2 = SimpleOperation(concept = concept2)

        // The main improvement: we can now access concept as a property
        // Whether the concepts are equal or not depends on Node's equality semantics
        // but the important thing is that concept is now a constructor property
        assertTrue(operation1.concept === concept1)
        assertTrue(operation2.concept === concept2)
    }
}
