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
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustMacrosTest : BaseTest() {
    @Test
    fun testDeriveAttribute() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("macros.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Point struct should exist and have derive annotations
        val point = tu.records["Point"]
        assertNotNull(
            point,
            "Point struct should exist, decls: ${tu.declarations.map { "${it::class.simpleName}(${it.name})" }}",
        )
        assertTrue(point.annotations.any { it.name.localName == "derive(Clone, Debug)" })

        // Config struct should exist with Default derive
        val config = tu.records["Config"]
        assertNotNull(config)
        assertTrue(config.annotations.any { it.name.localName == "derive(Default)" })
    }

    @Test
    fun testMacroInvocation() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("macros.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // The use_macros function should parse successfully
        val func = tu.functions["use_macros"]
        assertNotNull(func)

        // Macro invocations should be modeled as CallExpressions
        val calls = func.allChildren<CallExpression>()
        assertTrue(calls.any { it.name.localName == "my_macro" })
        assertTrue(calls.any { it.name.localName == "println" })
    }
}
