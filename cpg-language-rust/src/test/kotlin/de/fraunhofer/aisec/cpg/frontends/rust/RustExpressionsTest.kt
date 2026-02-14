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
import de.fraunhofer.aisec.cpg.graph.statements.*
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

    @Test
    fun testTupleIndex() {
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

        val func = tu.functions["test_tuple_index"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val memberExprs = body.allChildren<MemberExpression>()
        assertTrue(memberExprs.any { it.name.localName == "0" }, "Should have tuple.0 access")
    }

    @Test
    fun testByteAndCStringLiterals() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("string_literals.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_byte_string"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val literals = body.allChildren<Literal<String>>()
        val byteLit = literals.firstOrNull { it.value == "hello bytes" }
        assertNotNull(byteLit, "Should have byte string literal")
        assertEquals("&[u8]", byteLit.type.name.localName)

        val cLit = literals.firstOrNull { it.value == "hello cstr" }
        assertNotNull(cLit, "Should have C string literal")
        assertEquals("&CStr", cLit.type.name.localName)
    }

    @Test
    fun testReturnInExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("return_expr.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["classify"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The return inside a match arm should produce a ReturnStatement, not a ProblemExpression
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("return_expression") },
            "return_expression should not produce a ProblemExpression",
        )

        val returns = body.allChildren<ReturnStatement>()
        assertTrue(returns.isNotEmpty(), "Should have a ReturnStatement from match arm")
    }

    @Test
    fun testIntegerLiterals() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("integer_literals.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_integer_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        @Suppress("UNCHECKED_CAST")
        val literals = body.allChildren<Literal<*>>().filter { it.value is Long }

        // decimal: 42
        val decimal =
            literals.first { (it.value as Long) == 42L && it.type.name.localName == "i32" }
        assertNotNull(decimal, "Should parse decimal 42")

        // hex: 0xFF = 255
        val hex = literals.first { (it.value as Long) == 255L }
        assertNotNull(hex, "Should parse hex 0xFF as 255")

        // octal: 0o77 = 63
        val octal = literals.first { (it.value as Long) == 63L }
        assertNotNull(octal, "Should parse octal 0o77 as 63")

        // binary: 0b1010 = 10
        val binary = literals.first { (it.value as Long) == 10L }
        assertNotNull(binary, "Should parse binary 0b1010 as 10")

        // with underscores: 1_000_000
        val underscored = literals.first { (it.value as Long) == 1_000_000L }
        assertNotNull(underscored, "Should parse 1_000_000")

        // typed u8: 42u8 -> type should be u8
        val typedU8 = literals.filter { (it.value as Long) == 42L }
        assertTrue(typedU8.any { it.type.name.localName == "u8" }, "Should have u8 typed literal")

        // typed i64: 100i64 -> type should be i64
        val typedI64 = literals.first { (it.value as Long) == 100L }
        assertEquals("i64", typedI64.type.name.localName, "Should infer i64 type from suffix")
    }

    @Test
    fun testTurbofish() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("turbofish.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_turbofish"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val calls = body.allChildren<CallExpression>()
        val turbofishCall = calls.firstOrNull { it.template }
        assertNotNull(turbofishCall, "Should have a template call (turbofish)")
        assertEquals(1, turbofishCall.templateArguments.size, "Should have one type argument")

        val typeArg = turbofishCall.templateArguments.first()
        assertIs<TypeExpression>(typeArg)
        assertEquals("i32", typeArg.name.localName)
    }
}
