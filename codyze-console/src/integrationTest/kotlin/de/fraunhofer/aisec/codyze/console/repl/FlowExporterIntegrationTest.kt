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
package de.fraunhofer.aisec.codyze.console.repl

import de.fraunhofer.aisec.codyze.console.AnalyzeRequestJSON
import de.fraunhofer.aisec.codyze.console.ConsoleService
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.query.dataFlow
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/** Exports the paths of a real [dataFlow] query, as `:flow` does. */
class FlowExporterIntegrationTest {

    private fun analyze(): TranslationResult {
        val consoleService = ConsoleService()
        runBlocking {
            consoleService.analyze(
                AnalyzeRequestJSON(
                    sourceDir =
                        "../codyze-compliance/src/integrationTest/resources/demo-app/components/webapp/webapp",
                    topLevel =
                        "../codyze-compliance/src/integrationTest/resources/demo-app/components/webapp",
                )
            )
        }
        return assertNotNull(ReplService(consoleService).translationResult)
    }

    @Test
    fun testExportDataFlow() {
        val result = analyze()
        val secret = assertNotNull(result.calls["get_secret_from_server"])
        val execute = assertNotNull(result.calls["execute"])
        val query = dataFlow(secret) { it in execute.arguments }
        assertTrue(query.value)

        val flows = FlowExporter.extractFlows(query)
        assertTrue(flows.isNotEmpty())
        val flow = flows.first()
        assertEquals(secret, flow.first())
        assertTrue(flow.last() in execute.arguments)

        val file = assertNotNull(FlowExporter.export(query))
        try {
            val sarif = file.readText()
            assertContains(sarif, "\"threadFlows\"")
            assertContains(sarif, "main.py")
        } finally {
            file.delete()
        }
    }

    @Test
    fun testExtractFlowsFromNodeList() {
        val result = analyze()
        val calls = result.calls.take(2)
        assertEquals(listOf(calls), FlowExporter.extractFlows(calls))
        assertEquals(listOf(calls), FlowExporter.extractFlows(listOf(calls, emptyList())))
        assertTrue(FlowExporter.extractFlows(42).isEmpty())
    }
}
