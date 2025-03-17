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
import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.codyze.console.*
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.concepts.arch.Agnostic
import de.fraunhofer.aisec.cpg.graph.concepts.flows.Main
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

/** A mock configuration for the translation manager. */
val mockConfig = TranslationConfiguration.builder().sourceLocations(File("some/path")).build()

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
                                translationUnits +=
                                    TranslationUnitDeclaration().apply {
                                        name = Name("tu1")
                                        id = Uuid.parse("00000000-0000-0000-0000-000000000001")
                                        var func =
                                            FunctionDeclaration().apply {
                                                name = Name("main")
                                                Main(this, os = Agnostic(this))
                                            }
                                        declarations += func
                                        statements +=
                                            CallExpression().apply {
                                                name = Name("main")
                                                prevDFG += func
                                            }
                                    }
                            }
                    },
            project = AnalysisProject(name = "mock", projectDir = null, config = mockConfig),
        )
    )

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
    fun testGetTranslationUnit() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response =
            client.get("/api/component/mock/translation-unit/00000000-0000-0000-0000-000000000001")
        assertEquals(HttpStatusCode.OK, response.status)

        val translationUnit = response.body<TranslationUnitJSON>()
        assertEquals("tu1", translationUnit.name)
    }

    @Test
    fun testGetAstNodes() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response =
            client.get(
                "/api/component/mock/translation-unit/00000000-0000-0000-0000-000000000001/ast-nodes"
            )
        assertEquals(HttpStatusCode.OK, response.status)

        val nodes = response.body<List<NodeJSON>>()
        assertEquals(2, nodes.size)
    }

    @Test
    fun testGetOverlayNodes() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response =
            client.get(
                "/api/component/mock/translation-unit/00000000-0000-0000-0000-000000000001/overlay-nodes"
            )
        assertEquals(HttpStatusCode.OK, response.status)

        val nodes = response.body<List<NodeJSON>>()
        assertEquals(2, nodes.size)
    }
}
