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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgCallArgumentByNameOrIndexPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgIdPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class CallArgumentToolsTest {

    private fun texts(result: CallToolResult) = result.content.map { (it as TextContent).text }

    private fun analyzeCall(dir: Path): Pair<CpgSession, String> {
        val source = dir.resolve("main.c")
        source.writeText("int f(int a, int b) { return a + b; } int main() { return f(1, 2); }")
        val result =
            analyze(listOf(source.toFile()), dir, false) { it.registerLanguage<CLanguage>() }
        return CpgSession(result, result.ctx) to
            result.calls.single { it.name.localName == "f" }.id.toString()
    }

    @Test
    fun listCallArgsReturnsOneEntryPerArgument(@TempDir dir: Path) {
        val (session, callId) = analyzeCall(dir)

        assertEquals(2, listCallArgs(session, CpgIdPayload(callId)).content.size)
    }

    @Test
    fun listCallArgsReportsAnUnknownCallInsteadOfThrowing(@TempDir dir: Path) {
        val (session, _) = analyzeCall(dir)

        assertEquals(
            listOf("No call found with id nope."),
            texts(listCallArgs(session, CpgIdPayload("nope"))),
        )
    }

    @Test
    fun theIdOfANodeThatIsNoCallIsNoCall(@TempDir dir: Path) {
        val (session, _) = analyzeCall(dir)
        val functionId =
            session.translationResult.functions.single { it.name.localName == "f" }.id.toString()

        assertEquals(
            listOf("No call found with id $functionId."),
            texts(listCallArgs(session, CpgIdPayload(functionId))),
        )
    }

    @Test
    fun listCallArgByIndexReturnsThatArgument(@TempDir dir: Path) {
        val (session, callId) = analyzeCall(dir)

        val outcome =
            listCallArgByNameOrIndex(
                session,
                CpgCallArgumentByNameOrIndexPayload(callId, index = 1),
            )

        assertTrue(texts(outcome).single().contains("2"), texts(outcome).toString())
    }

    @Test
    fun listCallArgByIndexReportsAnUnknownCallAndAnUnknownArgument(@TempDir dir: Path) {
        val (session, callId) = analyzeCall(dir)

        assertEquals(
            listOf("No call found with id nope."),
            texts(
                listCallArgByNameOrIndex(
                    session,
                    CpgCallArgumentByNameOrIndexPayload("nope", index = 0),
                )
            ),
        )
        assertEquals(
            listOf("No argument found with the given name or index."),
            texts(
                listCallArgByNameOrIndex(
                    session,
                    CpgCallArgumentByNameOrIndexPayload(callId, index = 9),
                )
            ),
        )
    }
}
