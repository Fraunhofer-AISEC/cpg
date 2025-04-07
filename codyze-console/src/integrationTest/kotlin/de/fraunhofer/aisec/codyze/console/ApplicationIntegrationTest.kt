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

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationIntegrationTest {
    @Test
    fun testAnalyze() = testApplication {
        application { configureWebconsole(ConsoleService()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response =
            client.post("/api/analyze") {
                contentType(ContentType.Application.Json)
                setBody(
                    AnalyzeRequestJSON(
                        sourceDir =
                            "../codyze-compliance/src/integrationTest/resources/demo-app/components/auth/auth",
                        topLevel =
                            "../codyze-compliance/src/integrationTest/resources/demo-app/components/auth",
                    )
                )
            }
        assertEquals(HttpStatusCode.OK, response.status)

        var result = response.body<AnalysisResultJSON>()
        assertEquals(1, result.components.size)

        client.post("/api/reanalyze") {}
        assertEquals(HttpStatusCode.OK, response.status)

        result = response.body<AnalysisResultJSON>()
        assertEquals(1, result.components.size)
    }
}
