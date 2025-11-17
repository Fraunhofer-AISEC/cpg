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
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class QueryExecutionTest {

    @Test
    fun testExecuteQueryEndpoint() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(ExecuteQueryRequestJSON(scriptCode = "functions.size"))
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        // Should return the number of functions in the mock service (1 function: "main")
        assertEquals("1", result["result"])
    }

    @Test
    fun testExecuteQueryWithShortcutAPI() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(ExecuteQueryRequestJSON(scriptCode = "nodes.size"))
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        // Should return the total number of nodes in the mock translation unit
        assertTrue(result["result"]!!.toInt() > 0)
    }

    @Test
    fun testExecuteQueryWithResultVariable() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(ExecuteQueryRequestJSON(scriptCode = "result.functions.size"))
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        assertEquals("1", result["result"])
    }

    @Test
    fun testExecuteQueryWithCallExpressions() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(ExecuteQueryRequestJSON(scriptCode = "calls.size"))
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        // Should return the number of call expressions in the mock service (2 calls: "open" and
        // "main")
        assertEquals("2", result["result"])
    }

    @Test
    fun testExecuteQueryNoAnalysisResult() = testApplication {
        application { configureWebconsole(emptyService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(ExecuteQueryRequestJSON(scriptCode = "functions.size"))
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        assertEquals(
            "No analysis result available. Please run an analysis first.",
            result["result"],
        )
    }

    @Test
    fun testExecuteQueryInvalidScript() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(ExecuteQueryRequestJSON(scriptCode = "invalid.syntax.here"))
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        assertTrue(result["result"]!!.startsWith("Compilation error:"))
    }

    @Test
    fun testExecuteQueryComplexExpression() = testApplication {
        application { configureWebconsole(mockService) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response =
            client.post("/api/execute-query") {
                contentType(ContentType.Application.Json)
                setBody(
                    ExecuteQueryRequestJSON(
                        scriptCode = "calls.filter { it.name.localName == \"open\" }.size"
                    )
                )
            }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<Map<String, String>>()
        assertNotNull(result["result"])
        // Should find one "open" call in the mock service
        assertEquals("1", result["result"])
    }

    @Test
    fun testExecuteQueryDirectService() {
        val service = mockService

        runBlocking {
            val result = service.executeQuery("functions.size")
            assertEquals("1", result)
        }
    }

    @Test
    fun testExecuteQueryDirectServiceNoResult() {
        val service = emptyService

        runBlocking {
            val result = service.executeQuery("functions.size")
            assertEquals("No analysis result available. Please run an analysis first.", result)
        }
    }

    @Test
    fun testExecuteQueryDirectServiceInvalidScript() {
        val service = mockService

        runBlocking {
            val result = service.executeQuery("invalidScript()")
            assertTrue(result.startsWith("Compilation error:"))
        }
    }
}
