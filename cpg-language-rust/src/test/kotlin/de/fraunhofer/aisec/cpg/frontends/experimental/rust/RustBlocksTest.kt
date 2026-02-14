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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustBlocksTest : BaseTest() {
    @Test
    fun testUnsafeBlock() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("blocks.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_unsafe"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements
        assertNotNull(statements.getOrNull(0), "Should have statement for let x = unsafe { 42 }")

        // The unsafe block should not produce a ProblemExpression
        val problems =
            func.allChildren<ProblemExpression>().filter { it.problem.contains("unsafe") }
        assertTrue(
            problems.isEmpty(),
            "unsafe block should be handled, not produce ProblemExpression",
        )

        // The unsafe block should produce a Block with @unsafe annotation
        val unsafeBlocks =
            func.allChildren<Block>().filter { block ->
                block.annotations.any { it.name.localName == "unsafe" }
            }
        assertTrue(unsafeBlocks.isNotEmpty(), "Should have a block with @unsafe annotation")
    }

    @Test
    fun testAsyncBlock() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("blocks.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_async_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements
        assertNotNull(statements.getOrNull(0), "Should have statement for let fut = async { 42 }")

        // The async block should not produce a ProblemExpression
        val problems = func.allChildren<ProblemExpression>().filter { it.problem.contains("async") }
        assertTrue(
            problems.isEmpty(),
            "async block should be handled, not produce ProblemExpression",
        )

        // The async block should produce a Block with @async annotation
        val asyncBlocks =
            func.allChildren<Block>().filter { block ->
                block.annotations.any { it.name.localName == "async" }
            }
        assertTrue(asyncBlocks.isNotEmpty(), "Should have a block with @async annotation")
    }

    @Test
    fun testImplicitReturn() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("blocks.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["implicit_return"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The function body should contain a statement (the implicit return value 42)
        assertTrue(body.statements.isNotEmpty(), "Function body should have statements")
    }
}
