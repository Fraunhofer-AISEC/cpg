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
package de.fraunhofer.aisec.cpg.persistence

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCommon {
    @Test
    fun testSchemaProperties() {
        val properties = FunctionDeclaration::class.schemaProperties
        assertEquals(
            setOf(
                "complexity",
                "isDefinition",
                "signature",
                "argumentIndex",
                "code",
                "comment",
                "id",
                "isImplicit",
                "isInferred",
                "isStatic",
                "location",
                "name",
            ),
            properties.keys,
        )
    }

    @Test
    fun testSchemaRelationships() {
        var relationships = FunctionDeclaration::class.schemaRelationships
        assertEquals(
            listOf(
                "ANNOTATIONS",
                "ASSIGNED_TYPES",
                "AST",
                "BB",
                "BODY",
                "CDG",
                "DECLARING_SCOPE",
                "DEFINES",
                "DFG",
                "EOG",
                "FIRST_BASIC_BLOCK",
                "LANGUAGE",
                "OVERLAY",
                "OVERRIDES",
                "PARAMETERS",
                "PDG",
                "RETURN_TYPES",
                "SCOPE",
                "SECONDARY_TYPES",
                "SIGNATURE_TYPES",
                "THROWS_TYPES",
                "TYPE",
                "USAGE",
            ),
            relationships.keys.sorted(),
        )

        relationships = TranslationResult::class.schemaRelationships
        assertEquals(
            listOf(
                "ADDITIONAL_NODES",
                "ANNOTATIONS",
                "AST",
                "BB",
                "CDG",
                "COMPONENTS",
                "DFG",
                "EOG",
                "LANGUAGE",
                "OVERLAY",
                "PDG",
                "SCOPE",
                "USED_LANGUAGES",
            ),
            relationships.keys.sorted(),
        )
    }
}
