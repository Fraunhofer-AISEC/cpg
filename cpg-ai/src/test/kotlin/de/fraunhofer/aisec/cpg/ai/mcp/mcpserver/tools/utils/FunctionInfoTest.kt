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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.jupiter.api.io.TempDir

class FunctionInfoTest {
    @Test
    fun parametersCarryTheirNodeIds(@TempDir dir: Path) {
        val source = dir.resolve("main.c")
        source.writeText("int store(char *data, int size) { return size; }")
        val result =
            analyze(listOf(source.toFile()), dir, false) { it.registerLanguage<CLanguage>() }
        val session = CpgSession(result, result.ctx)
        val store = result.functions.singleOrNull { it.name.localName == "store" }
        assertNotNull(store)

        val parameters = FunctionInfo(store).parameters

        // The model can refer to a parameter, e.g. as the data a concept is about, by its id
        assertEquals(listOf("data", "size"), parameters.map { it.name })
        parameters.zip(store.parameters).forEach { (info, parameter) ->
            assertSame(parameter, session.findNodeById(info.nodeId))
        }
    }
}
