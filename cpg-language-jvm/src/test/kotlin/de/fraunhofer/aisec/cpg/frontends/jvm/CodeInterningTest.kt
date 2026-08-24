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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that enabling [de.fraunhofer.aisec.cpg.TranslationConfiguration.codeInterning] never
 * changes the `code` a node reports, when analyzing a `.jar`. This frontend derives `code` from
 * SootUp's re-serialization of the loaded bytecode (`JVMLanguageFrontend.codeOf`), never from the
 * original source text, so interning is expected to never engage here (see
 * CodeInterningBenchmarkTest) -- this test only guards correctness.
 */
class CodeInterningTest {
    @Test
    fun testInternedCodeMatchesLiteralCode() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val jar = topLevel.resolve("literals.jar").toFile()

        val withoutInterning =
            analyze(listOf(jar), topLevel, false) {
                it.registerLanguage<JVMLanguage>()
                it.codeInterning(false)
            }
        val withInterning =
            analyze(listOf(jar), topLevel, false) {
                it.registerLanguage<JVMLanguage>()
                it.codeInterning(true)
            }

        val withoutCodes = withoutInterning.nodes.map { it.code }
        val withCodes = withInterning.nodes.map { it.code }

        assertEquals(withoutCodes.size, withCodes.size)
        withoutCodes.zip(withCodes).forEach { (expected, actual) -> assertEquals(expected, actual) }
    }
}
