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
import de.fraunhofer.aisec.codyze.console.AnalysisResultJSON
import de.fraunhofer.aisec.codyze.console.ConsoleService
import de.fraunhofer.aisec.codyze.console.configureWebconsole
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
                    .apply { components += Component().apply { name = Name("mock") } },
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
}
