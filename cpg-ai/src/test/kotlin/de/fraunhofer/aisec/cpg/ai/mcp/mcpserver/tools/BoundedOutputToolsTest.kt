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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDataflowPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDfgBackwardPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgListCallsToPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgListPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgNamesPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DEFAULT_LIST_LIMIT
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DataflowResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConceptDescription
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodeInfo
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir

/** The tools that used to return everything they found, whatever the size. */
class BoundedOutputToolsTest {

    private fun texts(content: List<TextContent>) = content.map { it.text }

    private fun analyze(dir: Path, code: String, passes: Boolean): CpgSession {
        val source = dir.resolve("main.c")
        source.writeText(code)
        val result =
            analyze(listOf(source.toFile()), dir, passes) { it.registerLanguage<CLanguage>() }
        return CpgSession(result, result.ctx)
    }

    @Test
    fun listCallsToIsPaginated(@TempDir dir: Path) {
        val session =
            analyze(
                dir,
                "int f(int a) { return a; } int main() { f(1); f(2); f(3); return 0; }",
                passes = false,
            )

        val firstPage = listCallsTo(session, CpgListCallsToPayload("f", limit = 2)).content
        assertEquals(3, firstPage.size, "2 calls and the summary")
        assertTrue((firstPage.last() as TextContent).text.contains("offset=2"))

        val lastPage =
            listCallsTo(session, CpgListCallsToPayload("f", limit = 2, offset = 2)).content
        assertEquals(1, lastPage.size, "1 remaining call and no summary")

        assertEquals(3, listCallsTo(session, CpgListCallsToPayload("f")).content.size)
    }

    @Test
    fun dataflowIsPaginated(@TempDir dir: Path) {
        val session =
            analyze(
                dir,
                "int sink(int v) { return v; } " +
                    "int main() { int a = 1; int b = 2; int c = 3; return sink(a + b + c); }",
                passes = true,
            )
        val result = session.translationResult
        listOf("a", "b", "c").forEach { name ->
            val variable = result.variables.singleOrNull { it.name.localName == name }
            assertNotNull(variable)
            applyGenericConcept(variable, "Src")
        }
        val argument =
            result.calls.singleOrNull { it.name.localName == "sink" }?.arguments?.singleOrNull()
        assertNotNull(argument)
        applyGenericConcept(argument, "Dst")

        fun foundPaths(content: List<TextContent>) =
            Json.decodeFromString<DataflowResult>(content.first().text).foundPaths

        val firstPage =
            dataflow(session, CpgDataflowPayload("Src", "Dst", limit = 2))
                .content
                .filterIsInstance<TextContent>()
        assertEquals(2, foundPaths(firstPage).size)
        assertTrue(firstPage.last().text.contains("offset=2"), "the summary says how to go on")

        val lastPage =
            dataflow(session, CpgDataflowPayload("Src", "Dst", limit = 2, offset = 2))
                .content
                .filterIsInstance<TextContent>()
        assertEquals(1, lastPage.size, "one result, no summary")
        assertEquals(1, foundPaths(lastPage).size)
    }

    @Test
    fun functionsByNameIsPaginated(@TempDir dir: Path) {
        val names = (0 until 25).map { "f$it" }
        val session =
            analyze(dir, names.joinToString(" ") { "int $it() { return 0; }" }, passes = false)
        val requested = names + "missing"

        val firstPage =
            getFunctionsByName(session, CpgNamesPayload(requested, includeCode = false))
                .content
                .filterIsInstance<TextContent>()
        assertEquals(DEFAULT_LIST_LIMIT + 2, firstPage.size, "a page, the summary and the notice")
        assertTrue(firstPage[DEFAULT_LIST_LIMIT].text.contains("offset=$DEFAULT_LIST_LIMIT"))
        assertTrue(firstPage.last().text.contains("missing"))

        val lastPage =
            getFunctionsByName(
                    session,
                    CpgNamesPayload(requested, includeCode = false, offset = DEFAULT_LIST_LIMIT),
                )
                .content
                .filterIsInstance<TextContent>()
        assertEquals(5 + 1, lastPage.size, "the remaining functions and the notice, no summary")
        assertTrue(lastPage.last().text.contains("missing"))
    }

    @Test
    fun dfgBackwardKeepsOneJsonArrayAndCapsIt(@TempDir dir: Path) {
        val session = analyze(dir, "int main() { int a = 1; int b = a; return b; }", passes = true)
        val start = session.translationResult.refs.last { it.name.localName == "b" }.id.toString()

        val all = dfgBackward(session, CpgDfgBackwardPayload(start)).content.single() as TextContent
        val total = Json.decodeFromString<List<NodeInfo>>(all.text).size
        assertTrue(total > 1, "the slice of b must contain more than one node")

        val capped = dfgBackward(session, CpgDfgBackwardPayload(start, limit = 1)).content
        assertEquals(2, capped.size, "one JSON array and the summary")
        assertEquals(
            1,
            Json.decodeFromString<List<NodeInfo>>((capped.first() as TextContent).text).size,
        )
        assertTrue((capped.last() as TextContent).text.contains("of $total items"))
    }

    @Test
    fun dfgBackwardStillReportsAnUnknownNode(@TempDir dir: Path) {
        val session = analyze(dir, "int main() { return 0; }", passes = false)

        val outcome =
            dfgBackward(session, CpgDfgBackwardPayload("00000000-0000-0000-0000-000000000000"))

        assertTrue((outcome.content.single() as TextContent).text.startsWith("No node found"))
    }

    @Test
    fun conceptListIsPaginated(@TempDir dir: Path) {
        val file = dir.resolve("concepts.yaml").toFile()
        val schemas = (1..3).map { LLMConceptDescription("C$it", "d", emptyList(), emptyList()) }
        ObjectMapper(YAMLFactory()).registerKotlinModule().writeValue(file, schemas)

        val page =
            listConcepts(file, CpgListPayload(limit = 2)).content.filterIsInstance<TextContent>()

        assertEquals(3, page.size, "2 concepts and the summary")
        assertTrue(page.first().text.contains("\"C1\""))
        assertTrue(page.last().text.contains("offset=2"))
        assertEquals(3, listConcepts(file, CpgListPayload()).content.size)
    }
}
