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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgRunPassPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodeIndex
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.findNodeById
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.jupiter.api.io.TempDir

class NodeIndexTest {

    @BeforeTest @AfterTest fun resetIndex() = NodeIndex.invalidate()

    private fun analyzeSnippet(dir: Path, code: String): TranslationResult {
        val source = dir.resolve("main.c")
        source.writeText(code)
        return analyze(listOf(source.toFile()), dir, false) { it.registerLanguage<CLanguage>() }
    }

    private val code = "int f(int a) { return a; } int main() { int x = f(1); return x; }"

    @Test
    fun findsEveryNodeTheSameAsALinearSearch(@TempDir dir: Path) {
        val result = analyzeSnippet(dir, code)

        result.nodes.forEach { assertSame(it, result.findNodeById(it.id.toString())) }
    }

    @Test
    fun anUnknownIdIsNull(@TempDir dir: Path) {
        val result = analyzeSnippet(dir, code)

        assertNull(result.findNodeById("00000000-0000-0000-0000-000000000000"))
        assertNull(result.findNodeById("not an id"))
    }

    @Test
    fun theIndexIsBuiltOncePerResult(@TempDir dir: Path) {
        val result = analyzeSnippet(dir, code)
        val before = NodeIndex.buildCount

        repeat(3) { result.findNodeById(result.nodes.first().id.toString()) }

        assertEquals(before + 1, NodeIndex.buildCount)
    }

    @Test
    fun invalidatingForcesARebuild(@TempDir dir: Path) {
        val result = analyzeSnippet(dir, code)
        val id = result.nodes.first().id.toString()
        result.findNodeById(id)
        val before = NodeIndex.buildCount

        NodeIndex.invalidate()
        result.findNodeById(id)

        assertEquals(before + 1, NodeIndex.buildCount)
    }

    @Test
    fun aDifferentResultNeverSeesTheOldOnesNodes(@TempDir first: Path, @TempDir second: Path) {
        val oldResult = analyzeSnippet(first, "int onlyInFirst() { return 1; }")
        val newResult = analyzeSnippet(second, "int onlyInSecond() { return 2; }")
        val oldFunction = oldResult.functions.single { it.name.localName == "onlyInFirst" }
        val newFunction = newResult.functions.single { it.name.localName == "onlyInSecond" }
        val oldId = oldFunction.id.toString()
        val newId = newFunction.id.toString()

        assertSame(oldFunction, oldResult.findNodeById(oldId))
        assertSame(newFunction, newResult.findNodeById(newId))
        assertNull(newResult.findNodeById(oldId), "the index of the old graph must not be reused")
        assertNull(oldResult.findNodeById(newId))
    }

    @Test
    fun concurrentFirstLookupsBuildTheIndexOnce(@TempDir dir: Path) {
        val result = analyzeSnippet(dir, code)
        val id = result.nodes.first().id.toString()
        val before = NodeIndex.buildCount
        val threads = 8
        val start = CyclicBarrier(threads)
        val pool = Executors.newFixedThreadPool(threads)
        try {
            val found =
                List(threads) {
                    pool.submit<Any?> {
                        start.await(5, TimeUnit.SECONDS)
                        result.findNodeById(id)
                    }
                }
            found.forEach { assertNotNull(it.get(10, TimeUnit.SECONDS)) }
        } finally {
            pool.shutdownNow()
        }

        assertEquals(before + 1, NodeIndex.buildCount)
    }

    @Test
    fun runningAPassRefreshesTheIndex(@TempDir dir: Path) {
        val result = analyzeSnippet(dir, code)
        val main = result.functions.single { it.name.localName == "main" }
        result.findNodeById(main.id.toString())
        val before = NodeIndex.buildCount

        runPass(
            result,
            CpgRunPassPayload(EvaluationOrderGraphPass::class.java.name, main.id.toString()),
        )
        result.findNodeById(main.id.toString())

        assertEquals(
            before + 1,
            NodeIndex.buildCount,
            "a pass can add AST nodes, so the index must be rebuilt",
        )
    }
}
