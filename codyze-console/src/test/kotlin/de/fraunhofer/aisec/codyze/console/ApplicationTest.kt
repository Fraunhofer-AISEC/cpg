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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.arch.Agnostic
import de.fraunhofer.aisec.cpg.graph.concepts.file.File
import de.fraunhofer.aisec.cpg.graph.concepts.flows.Main
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import io.github.detekt.sarif4k.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.*

/** A mock configuration for the translation manager. */
val mockConfig =
    TranslationConfiguration.builder().sourceLocations(java.io.File("some/path")).build()

/** A mock translation unit. */
val mockTu =
    TranslationUnitDeclaration().apply {
        name = Name("tu1")
        var func =
            FunctionDeclaration().apply {
                name = Name("main")
                Main(this, os = Agnostic(this))
                body =
                    Block().apply {
                        statements +=
                            CallExpression().apply {
                                callee = Reference().apply { name = Name("open") }
                            }
                    }
            }
        declarations += func
        statements +=
            CallExpression().apply {
                name = Name("main")
                prevDFG += func
            }
    }

/**
 * A mock version of the [ConsoleService] that returns a mock analysis result containing of a few
 * nodes.
 */
val mockService =
    ConsoleService.fromAnalysisResult(
        AnalysisResult(
            translationResult =
                TranslationResult(
                        translationManager =
                            TranslationManager.builder().config(mockConfig).build(),
                        finalCtx = TranslationContext(config = mockConfig),
                    )
                    .apply {
                        components +=
                            Component().apply {
                                name = Name("mock")
                                translationUnits += mockTu
                            }
                    },
            project = AnalysisProject(name = "mock", projectDir = null, config = mockConfig),
            sarif =
                SarifSchema210(
                    version = Version.The210,
                    runs =
                        listOf(
                            Run(
                                tool = Tool(driver = ToolComponent(name = "mock")),
                                results =
                                    listOf(
                                        Result(
                                            ruleID = "mock",
                                            message = Message(text = "mock"),
                                            locations =
                                                listOf(
                                                    Location(
                                                        physicalLocation =
                                                            PhysicalLocation(
                                                                artifactLocation =
                                                                    ArtifactLocation(
                                                                        uri = "file:/mock.cpp"
                                                                    ),
                                                                region =
                                                                    Region(
                                                                        startLine = 1,
                                                                        startColumn = 1,
                                                                        endLine = 2,
                                                                        endColumn = 2,
                                                                    ),
                                                            )
                                                    )
                                                ),
                                        )
                                    ),
                            )
                        ),
                ),
        )
    )

/** A mock version of the [ConsoleService] that returns an empty analysis result. */
val emptyService = ConsoleService()

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application { configureWebconsole() }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "<title>Codyze Console</title>")
    }

    @Test
    fun testGetResult() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/result")
        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<AnalysisResultJSON>()
        assertEquals(1, result.components.size)

        val component = result.components.firstOrNull()
        assertNotNull(component)
        assertEquals("mock", component.name)

        val findings = result.findings
        assertEquals(1, findings.size)
    }

    @Test
    fun testGetResultNotFound() = testApplication {
        application { configureWebconsole(emptyService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/result")
        assertEquals(
            HttpStatusCode.NotFound,
            response.status,
            "Expected 404 Not Found because no result is available",
        )
    }

    @Test
    fun testReAnalyzeFailed() = testApplication {
        application { configureWebconsole(emptyService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/api/reanalyze")
        assertEquals(
            HttpStatusCode.BadRequest,
            response.status,
            "Expected 400 Bad Request because to previous analysis is there",
        )
    }

    @Test
    fun testGetComponent() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/component/mock")
        assertEquals(HttpStatusCode.OK, response.status)

        val component = response.body<ComponentJSON>()
        assertNotNull(component)
        assertEquals("mock", component.name)
    }

    @Test
    fun testGetComponentNotFound() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/component/unknown")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testGetTranslationUnit() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/component/mock/translation-unit/${mockTu.id}")
        assertEquals(HttpStatusCode.OK, response.status)

        val translationUnit = response.body<TranslationUnitJSON>()
        assertEquals("tu1", translationUnit.name)
    }

    @Test
    fun testGetTranslationUnitNotFound() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response =
            client.get("/api/component/mock/translation-unit/00000000-0000-0000-0000-000000000002")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testGetAstNodes() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/component/mock/translation-unit/${mockTu.id}/ast-nodes")
        assertEquals(HttpStatusCode.OK, response.status)

        val nodes = response.body<List<NodeJSON>>()
        assertEquals(2, nodes.size)
    }

    @Test
    fun testGetOverlayNodes() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/component/mock/translation-unit/${mockTu.id}/overlay-nodes")
        assertEquals(HttpStatusCode.OK, response.status)

        val nodes = response.body<List<NodeJSON>>()
        assertEquals(2, nodes.size)
    }

    @Test
    fun testAddConcept() = testApplication {
        val open = assertNotNull(mockTu.calls["open"])

        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        var response =
            client.post("/api/concepts") {
                contentType(ContentType.Application.Json)
                setBody(
                    ConceptRequestJSON(
                        nodeId = open.id,
                        conceptName = File::class.java.name,
                        addDFGToConcept = false,
                        addDFGFromConcept = false,
                        constructorArgs =
                            listOf(
                                ConstructorArguments(
                                    argumentName = "fileName",
                                    argumentValue = "path/to/file",
                                )
                            ),
                    )
                )
            }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(open.conceptNodes.any { it is File })

        response = client.get("/api/export-concepts")
        assertEquals(HttpStatusCode.OK, response.status)

        val yaml = response.bodyAsText()
        assertNotNull(yaml)
    }

    @Test
    fun testAddConceptWrongConstructorKey() = testApplication {
        val open = assertNotNull(mockTu.calls["open"])

        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        var response =
            client.post("/api/concepts") {
                contentType(ContentType.Application.Json)
                setBody(
                    ConceptRequestJSON(
                        nodeId = open.id,
                        conceptName = File::class.java.name,
                        addDFGToConcept = false,
                        addDFGFromConcept = false,
                        constructorArgs =
                            listOf(
                                ConstructorArguments(
                                    argumentName = "path",
                                    argumentValue = "path/to/file",
                                )
                            ),
                    )
                )
            }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertEquals(
            "{\n" +
                "    \"error\": \"There is no argument with name \\\"path\\\" which is specified to generate the concept File\"\n" +
                "}",
            body,
        )
    }
}
