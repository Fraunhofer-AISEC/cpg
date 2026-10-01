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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConcept
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConceptList
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.runOnCpg
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction
import kotlin.concurrent.thread
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * Drives tool-style calls through [runOnCpg] on a real graph, the way parallel MCP requests would:
 * writers attach concept overlays to a few "hot" nodes while readers traverse exactly those nodes'
 * overlay and data-flow collections. Without
 * [de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgLock] this reliably produces
 * `ConcurrentModificationException`s (about a dozen per second here), which [runOnCpg] turns into
 * an "Error executing query" result - exactly what a model would be shown.
 */
class CpgGraphConcurrencyTest {

    private val previousResult = globalAnalysisResult

    @AfterTest
    fun restoreGlobalState() {
        globalAnalysisResult = previousResult
    }

    private fun isError(result: CallToolResult) =
        (result.content.firstOrNull() as? TextContent)?.text?.startsWith("Error") == true

    @Test
    fun noToolCallFailsWhileOthersMutateTheGraph(@TempDir dir: Path) {
        val source = StringBuilder()
        for (i in 0 until 20) {
            source.append("int helper$i(int a) { int b = a + $i; int c = b * 2; return c; }\n")
        }
        dir.resolve("hot.c").writeText(source.toString())
        val result: TranslationResult =
            analyze(listOf(dir.resolve("hot.c").toFile()), dir, true) {
                it.registerLanguage<CLanguage>()
            }
        globalAnalysisResult = result
        val hot = result.functions.filter { it.name.localName.startsWith("helper") }.take(5)
        val schemaFile = dir.resolve("concepts.yaml").toFile()

        val stop = AtomicBoolean(false)
        val failures = AtomicInteger()
        val reads = AtomicInteger()
        val writes = AtomicInteger()

        val writers =
            List(2) { w ->
                thread {
                    var i = 0
                    while (!stop.get()) {
                        val node = hot[(i + w) % hot.size]
                        val payload =
                            LLMConceptList(
                                listOf(
                                    LLMConcept(
                                        name = "C$w-$i",
                                        description = "d",
                                        nodeId = node.id.toString(),
                                        properties = emptyList(),
                                        operations = emptyList(),
                                    )
                                )
                            )
                        val outcome =
                            "write"
                                .runOnCpg(
                                    BiFunction<TranslationResult, String, CallToolResult> { r, _ ->
                                        applyLLMConcepts(r, payload, schemaFile)
                                        CallToolResult(content = emptyList())
                                    },
                                    mutating = true,
                                )
                        if (isError(outcome)) failures.incrementAndGet()
                        else writes.incrementAndGet()
                        i++
                    }
                }
            }
        val readers =
            List(6) {
                thread {
                    while (!stop.get()) {
                        val outcome =
                            "read"
                                .runOnCpg(
                                    BiFunction<TranslationResult, String, CallToolResult> { _, _ ->
                                        hot.forEach { f ->
                                            f.overlays.forEach { it.name }
                                            f.nextDFG.forEach { it.name }
                                            f.prevDFG.forEach { it.name }
                                            f.overlayEdges.toList()
                                        }
                                        CallToolResult(content = emptyList())
                                    }
                                )
                        if (isError(outcome)) failures.incrementAndGet()
                        else reads.incrementAndGet()
                    }
                }
            }

        Thread.sleep(4_000)
        stop.set(true)
        (writers + readers).forEach { it.join() }

        assertEquals(0, failures.get(), "tool calls failed while the graph was being mutated")
        assertTrue(reads.get() > 0 && writes.get() > 0, "both sides must have made progress")
    }

    @Test
    fun missingAnalysisResultIsReportedNotThrown() {
        globalAnalysisResult = null

        val outcome =
            "x"
                .runOnCpg(
                    BiFunction<TranslationResult, String, CallToolResult> { _, _ ->
                        error("the query must not run without an analysis result")
                    }
                )

        assertTrue(
            (outcome.content.single() as TextContent)
                .text
                .startsWith("No analysis result available")
        )
    }
}
