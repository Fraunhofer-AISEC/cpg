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
package de.fraunhofer.aisec.cpg.graph.concepts.api

import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.builder.body
import de.fraunhofer.aisec.cpg.graph.builder.memberCall
import de.fraunhofer.aisec.cpg.graph.builder.method
import de.fraunhofer.aisec.cpg.graph.builder.param
import de.fraunhofer.aisec.cpg.graph.builder.record
import de.fraunhofer.aisec.cpg.graph.builder.ref
import de.fraunhofer.aisec.cpg.graph.builder.t
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.operationNodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RestApiConceptTest {
    @Test
    fun testRestApiConcept() {
        val result = getRestApiTranslationResult()
        assertNotNull(result)

        val concept = result.conceptNodes.filterIsInstance<RestApiConcept>().firstOrNull()
        val operation = result.operationNodes.filterIsInstance<RestApiOperation>().firstOrNull()
        assertNotNull(concept)
        assertNotNull(operation)

        assertEquals(operation.concept, concept)
        assertEquals("POST", operation.httpMethod.name)
        val memberCall = operation.underlyingNode
        assertNotNull(memberCall)
    }
}

fun getRestApiTranslationResult() =
    testFrontend { builder -> builder.registerLanguage(TestLanguage(".")) }
        .build {
            translationResult {
                translationUnit() {
                    val controller =
                        record("Controller") {
                            method("create") {
                                param("id", t("String"))
                                body { memberCall("_create", ref("call")) { ref("id") } }
                            }
                        }

                    val apiConcept =
                        this@translationResult.newRestApiConcept(
                            underlyingNode = controller,
                            apiUrl = "/example/api/{id}",
                            role = ApiRole.CONSUMER,
                        )

                    val memberCall = controller.calls.first()
                    this@translationResult.newRestApiOperation(
                        underlyingNode = memberCall,
                        httpMethod = memberCall.name.localName,
                        arguments = memberCall.arguments,
                        concept = apiConcept,
                    )
                }
            }
        }
