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
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgRunPassPayload
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.test.analyze
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** Records whether [Probe]'s static initializer ran, without being touched by it itself. */
private object InitFlag {
    @Volatile var initialized = false
}

/** Not a pass. Initializing this class sets [InitFlag.initialized]. */
internal class Probe {
    companion object {
        init {
            InitFlag.initialized = true
        }
    }
}

class CpgRunPassUnitTest {

    private val previousResult = globalAnalysisResult

    @AfterTest
    fun restoreGlobalState() {
        globalAnalysisResult = previousResult
        nodeToPass.clear()
    }

    private fun analyzeSnippet(dir: Path): TranslationResult {
        val source = dir.resolve("main.c")
        source.writeText("int main() { int x = 1; return x; }")
        return analyze(listOf(source.toFile()), dir, false) { it.registerLanguage<CLanguage>() }
    }

    @Test
    fun loadPassClassFindsARealPass() {
        assertEquals(
            EvaluationOrderGraphPass::class,
            loadPassClass(EvaluationOrderGraphPass::class.java.name),
        )
    }

    @Test
    fun loadPassClassRejectsClassesThatAreNotPasses() {
        assertNull(loadPassClass("java.lang.String"))
        assertNull(loadPassClass("does.not.Exist"))
    }

    @Test
    fun loadPassClassDoesNotInitializeAClassBeforeCheckingItIsAPass() {
        assertFalse(InitFlag.initialized)

        assertNull(loadPassClass(Probe::class.java.name))

        assertFalse(InitFlag.initialized, "a class that is not a pass must not be initialized")
    }

    @Test
    fun runPassUsesTheContextOfTheResultItRunsOn(@TempDir dir: Path) {
        // A host such as codyze-console hands the server a finished result; there is no other
        // context to fall back on, and none must be needed.
        val result = analyzeSnippet(dir)
        val main = result.functions.single { it.name.localName == "main" }

        val outcome =
            runPass(
                result,
                CpgRunPassPayload(EvaluationOrderGraphPass::class.java.name, main.id.toString()),
            )

        val text = outcome.content.filterIsInstance<TextContent>().joinToString("\n") { it.text }
        assertTrue(text.startsWith("Successfully ran"), text)
    }

    @Test
    fun runPassRejectsANameThatIsNotAPass(@TempDir dir: Path) {
        val result = analyzeSnippet(dir)
        val main = result.functions.single { it.name.localName == "main" }

        val outcome = runPass(result, CpgRunPassPayload("java.lang.String", main.id.toString()))

        assertEquals(
            "Could not find the pass java.lang.String.",
            (outcome.content.single() as TextContent).text,
        )
    }

    @Test
    fun reanalysisDropsThePassBookkeepingOfTheOldGraph() {
        val payload = CpgAnalyzePayload(content = "int main() { return 0; }", extension = "c")
        runCpgAnalyze(payload, runPasses = false, cleanup = false)
        val first = assertNotNull(globalAnalysisResult)
        nodeToPass[first.functions.first()] = mutableSetOf(EvaluationOrderGraphPass::class)

        runCpgAnalyze(payload, runPasses = false, cleanup = false)

        assertNotSame(first, globalAnalysisResult)
        assertTrue(nodeToPass.isEmpty(), "pass bookkeeping must not outlive the graph it describes")
    }
}
