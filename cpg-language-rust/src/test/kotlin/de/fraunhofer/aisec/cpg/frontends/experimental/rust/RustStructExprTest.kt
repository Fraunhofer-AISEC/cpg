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

class RustStructExprTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testStructExpression() {
        val tu = parseTU("structs_and_methods.rs")
        assertNotNull(tu)

        val func = tu.functions["test_struct_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements
        assertNotNull(statements.getOrNull(0), "Should have first statement")

        // Point { x: 1, y: 2 } should be a ConstructExpression
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have struct construction")
    }

    @Test
    fun testMethodCall() {
        val tu = parseTU("structs_and_methods.rs")
        assertNotNull(tu)

        val func = tu.functions["test_struct_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements
        assertNotNull(statements.getOrNull(1), "Should have second statement")

        // p.sum() should be a MemberCallExpression
        val memberCalls = body.allChildren<MemberCallExpression>()
        assertTrue(memberCalls.isNotEmpty(), "Should have method calls")
    }

    @Test
    fun testShorthandStructInit() {
        val tu = parseTU("struct_init_shorthand.rs")
        assertNotNull(tu)
        val func = tu.functions["test_shorthand_init"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have struct construction")
        val config = constructs.first()
        assertTrue(config.arguments.isNotEmpty(), "Should have shorthand field arguments")
    }

    @Test
    fun testBranchStructFull() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_full"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.size >= 3, "Should have 3+ struct constructions")
    }

    @Test
    fun testDeepStructFieldInit() {
        val tu = parseTU("struct_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_field_init"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have struct construction")
    }

    @Test
    fun testDeepStructShorthand() {
        val tu = parseTU("struct_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_shorthand"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have shorthand struct construction")
    }

    @Test
    fun testDeepStructSpread() {
        val tu = parseTU("struct_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_spread"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(
            constructs.any { c ->
                c.arguments.any { arg -> arg is Reference && arg.name.localName == "p1" }
            },
            "Should have struct spread referencing p1",
        )
    }
}
