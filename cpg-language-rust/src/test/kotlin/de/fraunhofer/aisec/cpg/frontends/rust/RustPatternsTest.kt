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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustPatternsTest : BaseTest() {
    @Test
    fun testStructPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("patterns.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_struct_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Struct pattern should not produce ProblemExpressions
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Struct pattern should not produce ProblemExpression: ${problems.map { it.problem }}",
        )
    }

    @Test
    fun testOrPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("patterns.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_or_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Or pattern should not produce ProblemExpressions
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Or pattern should not produce ProblemExpression: ${problems.map { it.problem }}",
        )
    }
}
