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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConcept
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConceptDescription
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMConceptList
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMProperty
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMPropertyDescription
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.concepts.GenericLLMConcept
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

/**
 * Unit tests for how the generic-concepts tool reads and caches its concept-schema file. Uses a
 * real (tiny, C) analysis instead of [CpgGenericConceptsToolTest]'s Python one, whose `@BeforeEach`
 * needs a native library that isn't available in every environment.
 */
class ConceptsSchemaFileTest {

    private val yaml = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private fun analyzeSnippet(dir: Path): TranslationResult {
        val source = dir.resolve("main.c")
        source.writeText("int main() { int x = 1; return x; }")
        return analyze(listOf(source.toFile()), dir, true) { it.registerLanguage<CLanguage>() }
    }

    private fun schema(name: String, fixedValue: String? = null) =
        LLMConceptDescription(
            name = name,
            description = "d",
            properties =
                listOf(
                    LLMPropertyDescription(
                        name = "algo",
                        type = "String",
                        description = "Algorithm",
                        fixedValue = fixedValue,
                    )
                ),
            operations = emptyList(),
        )

    @Test
    fun appliedConceptUsesFixedValueFromTheGivenSchemaFile(@TempDir dir: Path) {
        val result = analyzeSnippet(dir)
        val main = result.functions.single { it.name.localName == "main" }
        val schemaFile = dir.resolve("custom-concepts.yaml").toFile()
        yaml.writeValue(schemaFile, listOf(schema("Crypto", fixedValue = "AES-256")))

        val response =
            applyLLMConcepts(
                result,
                LLMConceptList(
                    listOf(
                        LLMConcept(
                            name = "Crypto",
                            description = "d",
                            nodeId = main.id.toString(),
                            properties = listOf(LLMProperty("algo", "String", "", "wrong")),
                            operations = emptyList(),
                        )
                    )
                ),
                schemaFile,
            )

        assertEquals(emptyList(), response.failed)
        val concept = result.allChildrenWithOverlays<GenericLLMConcept>().single()
        assertEquals("AES-256", concept.properties.properties["algo"]?.rawValue)
    }

    @Test
    fun schemaCacheDoesNotServeOneFilesContentForAnotherWithTheSameTimestamp(@TempDir dir: Path) {
        val a = dir.resolve("a.yaml").toFile()
        val b = dir.resolve("b.yaml").toFile()
        yaml.writeValue(a, listOf(schema("ConceptA")))
        yaml.writeValue(b, listOf(schema("ConceptB")))
        val sameTimestamp = System.currentTimeMillis() / 1000 * 1000
        a.setLastModified(sameTimestamp)
        b.setLastModified(sameTimestamp)

        assertEquals("ConceptA", loadPersistedConceptsAndOperations(a).single().name)
        assertEquals("ConceptB", loadPersistedConceptsAndOperations(b).single().name)
    }
}
