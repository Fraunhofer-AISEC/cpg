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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgOverview
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.io.TempDir

/** `cpg_overview`'s content as JSON, analyzed from a tiny C file (no Python interpreter needed). */
class CpgOverviewTest {

    @Test
    fun overviewIsJsonWithDefinedAndExternalFunctions(@TempDir dir: Path) {
        val source = dir.resolve("main.c")
        source.writeText(
            """
            int ext(int x);
            int helper() { return 1; }
            int main() { helper(); ext(2); ext(3); return 0; }
            """
                .trimIndent()
        )
        val result =
            analyze(listOf(source.toFile()), dir, true) { it.registerLanguage<CLanguage>() }

        val json = Json.encodeToString(result.overview())
        val overview = Json.decodeFromString<CpgOverview>(json)

        assertTrue("\"externalFunctions\"" in json, json)
        assertFalse("declaredOnly" in json, json)
        assertEquals(2, overview.definedFunctions, json)
        assertTrue(overview.externalFunctions >= 1, json)
        assertEquals(
            listOf("ext" to 2),
            overview.externalFunctionsCalled.map { it.name to it.count },
        )
        assertEquals(
            listOf("helper" to 1),
            overview.mostCalledFunctions.map { it.name to it.count },
        )
        assertEquals(listOf("main"), overview.entryPointCandidates.map { it.name })
    }
}
