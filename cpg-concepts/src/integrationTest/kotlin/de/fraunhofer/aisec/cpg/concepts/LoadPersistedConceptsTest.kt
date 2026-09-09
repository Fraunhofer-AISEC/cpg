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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.graph.concepts.GenericProperties
import de.fraunhofer.aisec.cpg.graph.concepts.file.File
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts
import de.fraunhofer.aisec.cpg.passes.concepts.loadLLMConceptsFromFile
import de.fraunhofer.aisec.cpg.passes.concepts.persistLLMConcepts
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class LoadPersistedConceptsTest : BaseTest() {
    @Test
    fun testReadConceptByLocation() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoadPersistedConcepts>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                it.configurePass<LoadPersistedConcepts>(
                    LoadPersistedConcepts.Configuration(
                        conceptFiles =
                            listOf(topLevel.resolve("file-concept-location.yaml").toFile())
                    )
                )
            }
        assertNotNull(result)

        val fileConcept = result.conceptNodes.singleOrNull { it is File }
        assertIs<File>(fileConcept)

        val openCall = result.calls("open").singleOrNull()
        assertNotNull(openCall)

        assertTrue(
            fileConcept.nextDFG.contains(openCall),
            "NextDFG: `File` concept should contain `open` call.",
        )
        assertFalse(
            openCall.nextDFG.contains(fileConcept),
            "NextDFG: `open` call should not contain `File` concept.",
        )

        assertEquals("foo", fileConcept.fileName, "Expected name of the file concept to be `foo`.")
    }

    @Test
    fun testReadConceptBySignature() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoadPersistedConcepts>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                it.configurePass<LoadPersistedConcepts>(
                    LoadPersistedConcepts.Configuration(
                        conceptFiles =
                            listOf(topLevel.resolve("file-concept-signature.yaml").toFile())
                    )
                )
            }
        assertNotNull(result)

        val fileConcept = result.conceptNodes.singleOrNull { it is File }
        assertIs<File>(fileConcept)

        val openCall = result.calls("open").singleOrNull()
        assertNotNull(openCall)

        assertTrue(
            fileConcept.nextDFG.contains(openCall),
            "NextDFG: `File` concept should contain `open` call.",
        )
        assertFalse(
            openCall.nextDFG.contains(fileConcept),
            "NextDFG: `open` call should not contain `File` concept.",
        )

        assertEquals("foo", fileConcept.fileName, "Expected name of the file concept to be `foo`.")
    }

    @Test
    fun testReadGenericLLMConceptWithNestedOperationAndProperties() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoadPersistedConcepts>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                it.configurePass<LoadPersistedConcepts>(
                    LoadPersistedConcepts.Configuration(
                        conceptFiles = listOf(topLevel.resolve("generic-llm-concept.yaml").toFile())
                    )
                )
            }
        assertNotNull(result)

        val concept = result.conceptNodes.singleOrNull { it is GenericLLMConcept }
        assertIs<GenericLLMConcept>(concept)
        assertEquals("Authentication", concept.conceptName)
        assertEquals("Handles user authentication", concept.description)
        assertEquals("high", concept.properties.properties["tier"])

        val op = concept.ops.singleOrNull()
        assertIs<GenericLLMOperation>(op)
        assertEquals("Login", op.operationName)
        assertEquals("Performs user login", op.description)
        assertEquals("POST", op.properties.properties["method"])
        assertSame(concept, op.genericLLMConcept)
    }

    @Test
    fun testPersistAndLoadLLMConceptsRoundTripViaDirectApi() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")
        val outFile =
            Files.createTempFile("llm-concepts-", ".yaml").toFile().apply { deleteOnExit() }

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }

        val target = result.calls("open").single()
        val concept =
            GenericLLMConcept(
                    underlyingNode = target,
                    conceptName = "Authentication",
                    description = "Handles user authentication",
                    properties = GenericProperties(mapOf("tier" to "high")),
                )
                .apply {
                    this.codeAndLocationFrom(target)
                    this.name =
                        Name("${GenericLLMConcept::class.simpleName}[$conceptName]", target.name)
                    NodeBuilder.log(this)
                }
        val op =
            GenericLLMOperation(
                    underlyingNode = target,
                    operationName = "Login",
                    description = "Performs user login",
                    genericLLMConcept = concept,
                    properties = GenericProperties(mapOf("method" to "POST")),
                )
                .apply {
                    this.codeAndLocationFrom(target)
                    this.name =
                        Name(
                            "${GenericLLMOperation::class.simpleName}[$operationName]",
                            target.name,
                        )
                    NodeBuilder.log(this)
                }
        concept.ops += op

        result.persistLLMConcepts(outFile)
        assertTrue(outFile.exists() && outFile.length() > 0)
        val yamlContent = outFile.readText()
        assertTrue(yamlContent.contains("astId:"), "YAML output should contain astId block")
        assertTrue(
            !yamlContent.contains("location:"),
            "YAML output should not contain location block",
        )

        val reloaded =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        reloaded.loadLLMConceptsFromFile(outFile)

        val reloadedConcept = reloaded.conceptNodes.singleOrNull { it is GenericLLMConcept }
        assertIs<GenericLLMConcept>(reloadedConcept)
        assertEquals("Authentication", reloadedConcept.conceptName)
        assertEquals("Handles user authentication", reloadedConcept.description)
        assertEquals("high", reloadedConcept.properties.properties["tier"])
        assertEquals((target as AstNode).idAst, (reloadedConcept.underlyingNode as AstNode).idAst)

        val reloadedOp = reloadedConcept.ops.singleOrNull()
        assertIs<GenericLLMOperation>(reloadedOp)
        assertEquals("Login", reloadedOp.operationName)
        assertEquals("POST", reloadedOp.properties.properties["method"])
        assertSame(reloadedConcept, reloadedOp.genericLLMConcept)
        assertNull(reloadedConcept.notes)
        assertNull(reloadedOp.notes)
    }

    @Test
    fun testPersistAndLoadLLMConceptsRoundTripPreservesNotes() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")
        val outFile =
            Files.createTempFile("llm-concepts-notes-", ".yaml").toFile().apply { deleteOnExit() }

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }

        val target = result.calls("open").single()
        val concept =
            GenericLLMConcept(
                    underlyingNode = target,
                    conceptName = "Authentication",
                    description = "Handles user authentication",
                    properties = GenericProperties(mapOf("tier" to "high")),
                    notes = "Must run after the session init call.",
                )
                .apply {
                    this.codeAndLocationFrom(target)
                    this.name =
                        Name("${GenericLLMConcept::class.simpleName}[$conceptName]", target.name)
                    NodeBuilder.log(this)
                }
        val op =
            GenericLLMOperation(
                    underlyingNode = target,
                    operationName = "Login",
                    description = "Performs user login",
                    genericLLMConcept = concept,
                    properties = GenericProperties(mapOf("method" to "POST")),
                    notes = "Requires a valid session cookie.",
                )
                .apply {
                    this.codeAndLocationFrom(target)
                    this.name =
                        Name(
                            "${GenericLLMOperation::class.simpleName}[$operationName]",
                            target.name,
                        )
                    NodeBuilder.log(this)
                }
        concept.ops += op

        result.persistLLMConcepts(outFile)

        val reloaded =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        reloaded.loadLLMConceptsFromFile(outFile)

        val reloadedConcept = reloaded.conceptNodes.singleOrNull { it is GenericLLMConcept }
        assertIs<GenericLLMConcept>(reloadedConcept)
        assertEquals("Must run after the session init call.", reloadedConcept.notes)

        val reloadedOp = reloadedConcept.ops.singleOrNull()
        assertIs<GenericLLMOperation>(reloadedOp)
        assertEquals("Requires a valid session cookie.", reloadedOp.notes)
    }

    @Test
    fun testLoadLLMConceptsFromFileSkipsUnmatchedLocation() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")
        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }

        val badFile =
            Files.createTempFile("llm-concepts-missing-", ".yaml").toFile().apply { deleteOnExit() }
        badFile.writeText(
            """
            concepts:
              - concept:
                  name: "de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept"
                  constructorArguments:
                    - name: "conceptName"
                      value: "Ghost"
                    - name: "description"
                      value: "Should never match"
                  properties:
                    tier: "n/a"
                location:
                  file: "src/integrationTest/resources/python/file/file_read.py"
                  region: "999:1-999:5"
                  type: "de.fraunhofer.aisec.cpg.graph.expressions.Call"
            """
                .trimIndent()
        )

        // Must not throw - a non-matching location should be logged as a warning and skipped, not
        // abort the load.
        result.loadLLMConceptsFromFile(badFile)

        assertTrue(
            result.conceptNodes.none { it is GenericLLMConcept },
            "No GenericLLMConcept should have been attached for a non-matching location.",
        )
    }

    @Test
    fun testLoadLLMConceptsFromFileSkipsUnmatchedAstId() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")
        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }

        val badFile =
            Files.createTempFile("llm-concepts-missing-astid-", ".yaml").toFile().apply {
                deleteOnExit()
            }
        badFile.writeText(
            """
            concepts:
              - concept:
                  name: "de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept"
                  constructorArguments:
                    - name: "conceptName"
                      value: "Ghost"
                    - name: "description"
                      value: "Should never match"
                  properties:
                    tier: "n/a"
                astId:
                  idAst: "nonexistent/ast/node/id"
            """
                .trimIndent()
        )

        result.loadLLMConceptsFromFile(badFile)

        assertTrue(
            result.conceptNodes.none { it is GenericLLMConcept },
            "No GenericLLMConcept should have been attached for a non-matching astId.",
        )
    }

    @Test
    fun testPersistLLMConceptsUsesDefaultFileName() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")
        val defaultFile = java.io.File("llm-tagged-concepts.yaml")
        if (defaultFile.exists()) defaultFile.delete()

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }

        val target = result.calls("open").single()
        GenericLLMConcept(
                underlyingNode = target,
                conceptName = "DefaultFileCheck",
                description = "Regression guard for default persistLLMConcepts filename",
                properties = GenericProperties(emptyMap()),
            )
            .apply {
                this.codeAndLocationFrom(target)
                this.name =
                    Name("${GenericLLMConcept::class.simpleName}[$conceptName]", target.name)
                NodeBuilder.log(this)
            }

        try {
            result.persistLLMConcepts()
            assertTrue(
                defaultFile.exists(),
                "persistLLMConcepts() with no argument should write llm-tagged-concepts.yaml",
            )
        } finally {
            if (defaultFile.exists()) defaultFile.delete()
        }
    }
}
