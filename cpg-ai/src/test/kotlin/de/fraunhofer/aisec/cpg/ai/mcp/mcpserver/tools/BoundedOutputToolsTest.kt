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
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDfgBackwardPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgListCallsToPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgListPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConceptDescription
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.NodeInfo
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir

/** The tools that used to return everything they found, whatever the size. */
class BoundedOutputToolsTest {

    private fun texts(content: List<TextContent>) = content.map { it.text }

    private fun analyze(dir: Path, code: String, passes: Boolean): TranslationResult {
        val source = dir.resolve("main.c")
        source.writeText(code)
        return analyze(listOf(source.toFile()), dir, passes) { it.registerLanguage<CLanguage>() }
    }

    @Test
    fun listCallsToIsPaginated(@TempDir dir: Path) {
        val result =
            analyze(
                dir,
                "int f(int a) { return a; } int main() { f(1); f(2); f(3); return 0; }",
                passes = false,
            )

        val firstPage = listCallsTo(result, CpgListCallsToPayload("f", limit = 2)).content
        assertEquals(3, firstPage.size, "2 calls and the summary")
        assertTrue((firstPage.last() as TextContent).text.contains("offset=2"))

        val lastPage =
            listCallsTo(result, CpgListCallsToPayload("f", limit = 2, offset = 2)).content
        assertEquals(1, lastPage.size, "1 remaining call and no summary")

        assertEquals(3, listCallsTo(result, CpgListCallsToPayload("f")).content.size)
    }

    @Test
    fun dfgBackwardKeepsOneJsonArrayAndCapsIt(@TempDir dir: Path) {
        val result = analyze(dir, "int main() { int a = 1; int b = a; return b; }", passes = true)
        val start = result.refs.last { it.name.localName == "b" }.id.toString()

        val all = dfgBackward(result, CpgDfgBackwardPayload(start)).content.single() as TextContent
        val total = Json.decodeFromString<List<NodeInfo>>(all.text).size
        assertTrue(total > 1, "the slice of b must contain more than one node")

        val capped = dfgBackward(result, CpgDfgBackwardPayload(start, limit = 1)).content
        assertEquals(2, capped.size, "one JSON array and the summary")
        assertEquals(
            1,
            Json.decodeFromString<List<NodeInfo>>((capped.first() as TextContent).text).size,
        )
        assertTrue((capped.last() as TextContent).text.contains("of $total items"))
    }

    @Test
    fun dfgBackwardStillReportsAnUnknownNode(@TempDir dir: Path) {
        val result = analyze(dir, "int main() { return 0; }", passes = false)

        val outcome =
            dfgBackward(result, CpgDfgBackwardPayload("00000000-0000-0000-0000-000000000000"))

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
