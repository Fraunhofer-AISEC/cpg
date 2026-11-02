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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgRunPassPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.jupiter.api.io.TempDir

class NodeIndexTest {

    private fun analyzeSnippet(dir: Path, code: String): CpgSession {
        val source = dir.resolve("main.c")
        source.writeText(code)
        val result =
            analyze(listOf(source.toFile()), dir, false) { it.registerLanguage<CLanguage>() }
        return CpgSession(result, result.ctx)
    }

    private val code = "int f(int a) { return a; } int main() { int x = f(1); return x; }"

    @Test
    fun findsEveryNodeTheSameAsALinearSearch(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, code)

        session.translationResult.nodes.forEach {
            assertSame(it, session.findNodeById(it.id.toString()))
        }
    }

    @Test
    fun anUnknownIdIsNull(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, code)

        assertNull(session.findNodeById("00000000-0000-0000-0000-000000000000"))
        assertNull(session.findNodeById("not an id"))
    }

    @Test
    fun theIdIsNormalizedBeforeTheLookup(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, "int main() { return 0; }")
        val node = session.translationResult.nodes.first()
        val id = node.id.toString()

        assertSame(node, session.findNodeById(id.uppercase()), "upper case")
        assertSame(node, session.findNodeById("  $id\n"), "surrounding whitespace")
        assertNull(session.findNodeById(id.drop(1)), "not a UUID")
    }

    @Test
    fun theIndexIsBuiltOncePerSession(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, code)
        val before = session.nodeIndex.buildCount

        repeat(3) { session.findNodeById(session.translationResult.nodes.first().id.toString()) }

        assertEquals(before + 1, session.nodeIndex.buildCount)
    }

    @Test
    fun invalidatingForcesARebuild(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, code)
        val id = session.translationResult.nodes.first().id.toString()
        session.findNodeById(id)
        val before = session.nodeIndex.buildCount

        session.nodeIndex.invalidate()
        session.findNodeById(id)

        assertEquals(before + 1, session.nodeIndex.buildCount)
    }

    @Test
    fun aDifferentSessionNeverSeesTheOtherOnesNodes(@TempDir first: Path, @TempDir second: Path) {
        val oldSession = analyzeSnippet(first, "int onlyInFirst() { return 1; }")
        val newSession = analyzeSnippet(second, "int onlyInSecond() { return 2; }")
        val oldFunction =
            oldSession.translationResult.functions.single { it.name.localName == "onlyInFirst" }
        val newFunction =
            newSession.translationResult.functions.single { it.name.localName == "onlyInSecond" }
        val oldId = oldFunction.id.toString()
        val newId = newFunction.id.toString()

        assertSame(oldFunction, oldSession.findNodeById(oldId))
        assertSame(newFunction, newSession.findNodeById(newId))
        assertNull(
            newSession.findNodeById(oldId),
            "the index of the other session must not be used",
        )
        assertNull(oldSession.findNodeById(newId))
    }

    @Test
    fun replacingTheResultOfASessionNeverServesTheOldNodes(
        @TempDir first: Path,
        @TempDir second: Path,
    ) {
        val session = analyzeSnippet(first, "int onlyInFirst() { return 1; }")
        val oldId =
            session.translationResult.functions
                .single { it.name.localName == "onlyInFirst" }
                .id
                .toString()
        assertNotNull(session.findNodeById(oldId))

        session.translationResult =
            analyzeSnippet(second, "int onlyInSecond() { return 2; }").translationResult

        assertNull(session.findNodeById(oldId), "the index of the old graph must not be reused")
    }

    @Test
    fun concurrentFirstLookupsBuildTheIndexOnce(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, code)
        val id = session.translationResult.nodes.first().id.toString()
        val before = session.nodeIndex.buildCount
        val threads = 8
        val start = CyclicBarrier(threads)
        val pool = Executors.newFixedThreadPool(threads)
        try {
            val found =
                List(threads) {
                    pool.submit<Any?> {
                        start.await(5, TimeUnit.SECONDS)
                        session.findNodeById(id)
                    }
                }
            found.forEach { assertNotNull(it.get(10, TimeUnit.SECONDS)) }
        } finally {
            pool.shutdownNow()
        }

        assertEquals(before + 1, session.nodeIndex.buildCount)
    }

    @Test
    fun runningAPassRefreshesTheIndex(@TempDir dir: Path) {
        val session = analyzeSnippet(dir, code)
        val main = session.translationResult.functions.single { it.name.localName == "main" }
        session.findNodeById(main.id.toString())
        val before = session.nodeIndex.buildCount

        runPass(
            session,
            CpgRunPassPayload(EvaluationOrderGraphPass::class.java.name, main.id.toString()),
        )
        session.findNodeById(main.id.toString())

        assertEquals(
            before + 1,
            session.nodeIndex.buildCount,
            "a pass can add AST nodes, so the index must be rebuilt",
        )
    }
}
