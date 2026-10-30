/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** `notes` is part of the identity of a generic LLM concept/operation, like its other fields. */
class GenericLLMOverlayEqualityTest {
    private val noProperties = GenericProperties(emptyMap())

    private fun concept(notes: String? = null) =
        GenericLLMConcept(
            conceptName = "Decoding",
            description = "d",
            properties = noProperties,
            notes = notes,
        )

    private fun operation(notes: String? = null, concept: GenericLLMConcept = concept()) =
        GenericLLMOperation(
            operationName = "create",
            description = "d",
            genericLLMConcept = concept,
            properties = noProperties,
            notes = notes,
        )

    @Test
    fun conceptsDifferingOnlyInNotesAreNotEqual() {
        assertEquals(concept("before init"), concept("before init"))
        assertEquals(concept("before init").hashCode(), concept("before init").hashCode())
        assertNotEquals(concept("before init"), concept("after init"))
        assertNotEquals(concept("before init"), concept(null))
    }

    @Test
    fun operationsDifferingOnlyInNotesAreNotEqual() {
        assertEquals(operation("call init first"), operation("call init first"))
        assertEquals(
            operation("call init first").hashCode(),
            operation("call init first").hashCode(),
        )
        assertNotEquals(operation("call init first"), operation("no prerequisites"))
        assertNotEquals(operation("call init first"), operation(null))
    }

    @Test
    fun operationsOfDifferentConceptsAreNotEqual() {
        assertNotEquals(operation(concept = concept("a")), operation(concept = concept("b")))
    }
}
