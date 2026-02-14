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
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustLifetimesTest : BaseTest() {
    @Test
    fun testLifetimeParameters() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("lifetimes.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // The `longest` function should be a template with a lifetime parameter
        val longestTemplate =
            tu.declarations.filterIsInstance<FunctionTemplateDeclaration>().firstOrNull {
                it.name.localName == "longest"
            }
        assertNotNull(longestTemplate, "longest should be a template function")

        // Should have a lifetime type parameter 'a
        val lifetimeParam = longestTemplate.parameters.firstOrNull()
        assertNotNull(lifetimeParam)
        assertTrue(lifetimeParam.name.localName.contains("a"))

        // The Excerpt struct should also be a template with lifetime
        val excerptTemplate =
            tu.declarations.filterIsInstance<RecordTemplateDeclaration>().firstOrNull {
                it.name.localName == "Excerpt"
            }
        assertNotNull(excerptTemplate, "Excerpt should be a template struct")
    }

    @Test
    fun testMutability() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("lifetimes.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // The mutability_example function should have variables
        val func = tu.functions["mutability_example"]
        assertNotNull(func)

        // `counter` should be marked as mutable
        val vars = func.allChildren<VariableDeclaration>()
        val counter = vars.firstOrNull { it.name.localName == "counter" }
        assertNotNull(counter)
        assertTrue(counter.annotations.any { it.name.localName == "mut" })

        // `immutable` should NOT have a mut annotation
        val immutable = vars.firstOrNull { it.name.localName == "immutable" }
        assertNotNull(immutable)
        assertFalse(immutable.annotations.any { it.name.localName == "mut" })
    }

    @Test
    fun testBorrowParameter() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("lifetimes.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // borrow_example takes `&mut i32`
        val func = tu.functions["borrow_example"]
        assertNotNull(func)

        val param = func.parameters.firstOrNull { it.name.localName == "data" }
        assertNotNull(param)
        // The type should be a reference
        assertTrue(param.type is ReferenceType)
    }
}
