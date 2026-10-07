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

import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/**
 * Analyzes the workbench demo, a small C project that is also used to try the console by hand (see
 * its README), and checks what the analysis finds and what the endpoints of the agent page return
 * for it.
 */
class WorkbenchDemoIntegrationTest {
    private val demoDir = "src/integrationTest/resources/workbench-demo/components/wifi_demo/main"
    private val mainLines = File("$demoDir/main.c").readLines()

    /** The 1-based line and column of the first occurrence of [text] in main.c. */
    private fun positionOf(text: String): Pair<Int, Int> {
        val index = mainLines.indexOfFirst { text in it }
        assertTrue(index >= 0, "main.c has no '$text'")
        return index + 1 to mainLines[index].indexOf(text) + 1
    }

    private suspend fun HttpClient.nodeAt(base: String, text: String): NodeDetailsJSON {
        val (line, column) = positionOf(text)
        val response = get("$base/node-at?line=$line&column=$column")
        assertEquals(HttpStatusCode.OK, response.status, "no node at '$text'")
        return response.body()
    }

    @Test
    fun testWorkbenchDemo() = testApplication {
        application { configureWebconsole(ConsoleService()) }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response =
            client.post("/api/analyze") {
                contentType(ContentType.Application.Json)
                setBody(AnalyzeRequestJSON(sourceDir = demoDir, topLevel = demoDir))
            }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = response.body<AnalysisResultJSON>()
        val component = result.components.single()
        val main = component.translationUnits.first { it.name.endsWith("main.c") }
        val base = "/api/component/${component.name}/translation-unit/${main.id}"

        // The key figures of app_main: get_key is external, the call through the function pointer
        // is unresolved
        val annotations = client.get("$base/annotations").body<FileAnnotationsJSON>()
        val appMain = annotations.functions.first { it.function.name.endsWith("app_main") }
        assertTrue(appMain.externalCalls >= 1, "external calls: ${appMain.externalCalls}")
        assertTrue(appMain.unresolvedCalls >= 1, "unresolved calls: ${appMain.unresolvedCalls}")

        // What affects the call of encrypt: the key (data) and the condition (control)
        val encrypt = client.nodeAt(base, "encrypt(key, buf, len)")
        val backward =
            client.get("/api/node/${encrypt.node.id}/pdg?direction=backward&hops=2").body<PdgSliceJSON>()
        val root = backward.nodes.first { it.id == backward.root }
        assertTrue("encrypt" in root.code, "root: ${root.code}")
        val getKey = backward.nodes.firstOrNull { "get_key" in it.code }
        assertNotNull(getKey, "nodes: ${backward.nodes.map { it.code }}")
        assertTrue(
            backward.edges.any {
                it.kind == PdgEdgeKind.DATA && it.from == getKey.id && it.label == "key"
            },
            "edges: ${backward.edges}",
        )
        val condition = backward.nodes.firstOrNull { "cfg.secure" in it.code }
        assertNotNull(condition, "nodes: ${backward.nodes.map { it.code }}")
        assertEquals(PdgNodeKind.BRANCH, condition.kind)
        assertTrue(
            backward.edges.any {
                it.kind == PdgEdgeKind.CONTROL && it.from == condition.id && it.label == "true"
            },
            "edges: ${backward.edges}",
        )
        assertTrue(getKey.unresolved || getKey.external, "get_key should be marked as uncertain")

        // What the key affects: the encryption, the plain copy and the log
        val forward =
            client
                .get("/api/node/${getKey.id}/pdg?direction=forward&hops=3")
                .body<PdgSliceJSON>()
        val affected = forward.nodes.map { it.code }
        assertTrue(affected.any { "encrypt" in it }, "affected: $affected")
        assertTrue(affected.any { "copy_plain" in it }, "affected: $affected")
        assertTrue(affected.any { "report_key" in it }, "affected: $affected")

        // The counts of the context menu
        val counts = client.get("/api/node/${encrypt.node.id}/pdg-counts?hops=2").body<PdgCountsJSON>()
        assertTrue(counts.backward > 0 && counts.forward >= 0, "counts: $counts")

        // The evidence of an answer relies on the unresolved call in the function
        val issues =
            client
                .post("/api/trust") {
                    contentType(ContentType.Application.Json)
                    setBody(listOf(encrypt.node.id.toString()))
                }
                .body<List<TrustIssueJSON>>()
        assertTrue(
            issues.any { it.kind == TrustIssueKind.UNRESOLVED_CALL },
            "issues: ${issues.map { "${it.kind} ${it.reason}" }}",
        )

        // A selection is resolved to the innermost node that contains all of it
        val (line, column) = positionOf("encrypt(key, buf, len)")
        val selection =
            client
                .get("$base/node-at?line=$line&column=$column&endLine=$line&endColumn=${column + 7}")
                .body<NodeDetailsJSON>()
        assertTrue("encrypt" in selection.node.code, "selection: ${selection.node.code}")
    }
}
