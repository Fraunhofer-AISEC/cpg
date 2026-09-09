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
package de.fraunhofer.aisec.cpg.ai.mcp.tools

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.addLLMConceptAndOperations
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.globalAnalysisResult
import de.fraunhofer.aisec.cpg.ai.mcp.utils.withClient
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.graph.literals
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.io.File
import kotlin.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class CpgGenericConceptsPersistenceTest {

    private val conceptsFile = File("concepts.yaml")
    private val llmConceptsFile = File("llm-tagged-concepts.yaml")

    @BeforeEach
    fun setUp() {
        cleanFiles()
    }

    @AfterEach
    fun tearDown() {
        cleanFiles()
    }

    private fun cleanFiles() {
        listOf(
                File("concepts.yaml").absoluteFile,
                File("llm-tagged-concepts.yaml").absoluteFile,
                File("cpg-ai/concepts.yaml").absoluteFile,
                File("cpg-ai/llm-tagged-concepts.yaml").absoluteFile,
                File("../llm-tagged-concepts.yaml").absoluteFile,
                File("../cpg-ai/llm-tagged-concepts.yaml").absoluteFile,
            )
            .forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
                file.deleteOnExit()
            }
    }

    @Test
    fun testPersistAndReloadLLMConceptsAndOperations() =
        withClient(registerTools = { addLLMConceptAndOperations() }) { client ->
            try {
                val pyContent =
                    "class Foo:\n    secretKey = '0000'\ndef hello():\n    print('Hello World')"
                val tempPyFile = File.createTempFile("persistence_test_", ".py")
                tempPyFile.writeText(pyContent)
                tempPyFile.deleteOnExit()

                val initialResult =
                    analyze(
                        files = listOf(tempPyFile),
                        topLevel = tempPyFile.parentFile.toPath(),
                        usePasses = true,
                    ) {
                        it.registerLanguage<PythonLanguage>()
                        it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                    }
                globalAnalysisResult = initialResult
                assertNotNull(globalAnalysisResult, "Result should be set after analyze")

                val secretLiteral =
                    globalAnalysisResult?.literals?.singleOrNull { it.value == "0000" }
                assertNotNull(secretLiteral, "Expected '0000' literal")
                val nodeId = secretLiteral.id.toString()

                val applyResult =
                    client.callTool(
                        name = "cpg_add_llm_concept_and_operations",
                        arguments =
                            mapOf(
                                "concepts" to
                                    listOf(
                                        mapOf(
                                            "name" to "SecretKey",
                                            "description" to "A hardcoded API secret key",
                                            "nodeId" to nodeId,
                                            "properties" to
                                                listOf(
                                                    mapOf(
                                                        "name" to "severity",
                                                        "type" to "String",
                                                        "value" to "CRITICAL",
                                                    )
                                                ),
                                            "operations" to
                                                listOf(
                                                    mapOf(
                                                        "name" to "AccessSecret",
                                                        "description" to
                                                            "Accesses the secret value",
                                                        "nodeId" to nodeId,
                                                        "properties" to
                                                            listOf(
                                                                mapOf(
                                                                    "name" to "accessType",
                                                                    "type" to "String",
                                                                    "value" to "READ",
                                                                )
                                                            ),
                                                    )
                                                ),
                                        )
                                    )
                            ),
                    )

                assertNotNull(applyResult)
                val textContent = applyResult.content.filterIsInstance<TextContent>().firstOrNull()
                assertNotNull(textContent, "Expected TextContent in tool response")
                assertTrue(
                    textContent.text.contains("SecretKey"),
                    "Response should mention SecretKey",
                )
                assertTrue(
                    textContent.text.contains("AccessSecret"),
                    "Response should mention AccessSecret",
                )

                assertTrue(llmConceptsFile.exists(), "llm-tagged-concepts.yaml should be created")
                val yamlContent = llmConceptsFile.readText()
                assertTrue(yamlContent.contains("astId:"), "YAML output should contain astId block")
                assertTrue(
                    !yamlContent.contains("location:"),
                    "YAML output should not contain location block",
                )

                val reloadedResult =
                    analyze(
                        files = listOf(tempPyFile),
                        topLevel = tempPyFile.parentFile.toPath(),
                        usePasses = true,
                    ) {
                        it.registerLanguage<PythonLanguage>()
                        it.registerPass<LoadPersistedConcepts>()
                        it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                        it.configurePass<LoadPersistedConcepts>(
                            LoadPersistedConcepts.Configuration(
                                conceptFiles = listOf(llmConceptsFile)
                            )
                        )
                    }
                assertNotNull(reloadedResult)

                val reloadedConcept =
                    reloadedResult.nodes
                        .flatMap { it.overlays }
                        .filterIsInstance<GenericLLMConcept>()
                        .singleOrNull()
                assertIs<GenericLLMConcept>(reloadedConcept)
                assertEquals("SecretKey", reloadedConcept.conceptName)
                assertEquals("A hardcoded API secret key", reloadedConcept.description)
                assertEquals("CRITICAL", reloadedConcept.properties.properties["severity"])
                assertEquals(
                    (secretLiteral as AstNode).idAst,
                    (reloadedConcept.underlyingNode as AstNode).idAst,
                )

                val reloadedOp = reloadedConcept.ops.singleOrNull()
                assertIs<GenericLLMOperation>(reloadedOp)
                assertEquals("AccessSecret", reloadedOp.operationName)
                assertEquals("Accesses the secret value", reloadedOp.description)
                assertEquals("READ", reloadedOp.properties.properties["accessType"])
                assertSame(reloadedConcept, reloadedOp.genericLLMConcept)
            } finally {
                cleanFiles()
            }
        }
}
