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

class RustExpressionsTest : BaseTest() {
    @Test
    fun testIndexExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_index"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val subscripts = body.allChildren<SubscriptExpression>()
        assertTrue(subscripts.isNotEmpty(), "Should have subscript expressions")

        val first = subscripts.first()
        assertNotNull(first.arrayExpression, "Should have array base")
        assertNotNull(first.subscriptExpression, "Should have index")
    }

    @Test
    fun testRangeExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_range"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val ranges = body.allChildren<RangeExpression>()
        assertTrue(ranges.isNotEmpty(), "Should have range expressions")
    }

    @Test
    fun testTypeCast() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_type_cast"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val casts = body.allChildren<CastExpression>()
        assertTrue(casts.isNotEmpty(), "Should have cast expressions")
        assertEquals("i64", casts.first().castType.name.localName)
    }

    @Test
    fun testClosure() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_closure"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have lambda/closure expressions")

        val lambda = lambdas.first()
        assertNotNull(lambda.function, "Lambda should have an inner function")
    }

    @Test
    fun testNegation() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_negation"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "-" }, "Should have negation operator")
        assertTrue(unaryOps.any { it.operatorCode == "!" }, "Should have NOT operator")
    }
}
