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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that enabling [de.fraunhofer.aisec.cpg.TranslationConfiguration.codeInterning] never
 * changes the `code` a node reports. Unlike the CXX/Python equivalents, this frontend's `codeOf`
 * reconstructs code from JavaParser's token stream/pretty-printer rather than slicing the raw file
 * (see CodeInterningBenchmarkTest), so interning is expected to rarely, if ever, engage here --
 * this test only guards correctness, not the memory benefit.
 */
class CodeInterningTest {
    @Test
    fun testInternedCodeMatchesLiteralCode() {
        val file = File("src/test/resources/bouncycastle/AES_CBC.java")

        val withoutInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<JavaLanguage>()
                it.codeInterning(false)
            }
        val withInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<JavaLanguage>()
                it.codeInterning(true)
            }

        val withoutCodes = withoutInterning.nodes.map { it.code }
        val withCodes = withInterning.nodes.map { it.code }

        assertEquals(withoutCodes.size, withCodes.size)
        withoutCodes.zip(withCodes).forEach { (expected, actual) -> assertEquals(expected, actual) }
    }
}
