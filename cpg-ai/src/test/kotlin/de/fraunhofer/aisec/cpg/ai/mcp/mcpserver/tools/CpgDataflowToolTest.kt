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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgDataflowPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgSession
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.DataflowResult
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMOperation
import de.fraunhofer.aisec.cpg.graph.concepts.GenericProperties
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

/**
 * Applies a generic concept called [conceptName] to [node], named the way
 * cpg_add_llm_concept_and_operations names it.
 */
internal fun applyGenericConcept(node: Node, conceptName: String): GenericLLMConcept =
    GenericLLMConcept(
            underlyingNode = node,
            conceptName = conceptName,
            description = conceptName,
            properties = GenericProperties(emptyMap()),
        )
        .apply { name = Name("${GenericLLMConcept::class.simpleName}[$conceptName]", node.name) }

/**
 * Applies a generic operation called [operationName] of [concept] to [node], named the way
 * cpg_add_llm_concept_and_operations names it.
 */
internal fun applyGenericOperation(
    node: Node,
    operationName: String,
    concept: GenericLLMConcept,
): GenericLLMOperation =
    GenericLLMOperation(
            underlyingNode = node,
            operationName = operationName,
            description = operationName,
            genericLLMConcept = concept,
            properties = GenericProperties(emptyMap()),
        )
        .apply {
            name = Name("${GenericLLMOperation::class.simpleName}[$operationName]", node.name)
        }

class CpgDataflowToolTest {
    private fun analyze(dir: Path, code: String): CpgSession {
        val source = dir.resolve("main.c")
        source.writeText(code)
        val result =
            analyze(listOf(source.toFile()), dir, true) { it.registerLanguage<CLanguage>() }
        return CpgSession(result, result.ctx)
    }

    @Test
    fun findsGenericConceptsAndOperationsByTheirNames(@TempDir dir: Path) {
        val session =
            analyze(dir, "int sink(int v) { return v; } int main() { int a = 1; return sink(a); }")
        val result = session.translationResult
        val variable = result.variables.singleOrNull { it.name.localName == "a" }
        assertNotNull(variable)
        val argument =
            result.calls.singleOrNull { it.name.localName == "sink" }?.arguments?.singleOrNull()
        assertNotNull(argument)

        val secret = applyGenericConcept(variable, "Secret")
        applyGenericOperation(argument, "leak", secret)

        val content = dataflow(session, CpgDataflowPayload("Secret", "leak")).content
        val text = content.filterIsInstance<TextContent>().single().text
        assertEquals(1, Json.decodeFromString<DataflowResult>(text).foundPaths.size, text)

        val unknown = dataflow(session, CpgDataflowPayload("Unknown", "leak")).content
        assertTrue(unknown.filterIsInstance<TextContent>().single().text.startsWith("No concept"))
    }
}
