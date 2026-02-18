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

    @Test
    fun testReferenceAndDereference() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("references.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_references"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements

        // Statement 0: let x = 5;
        assertNotNull(statements.getOrNull(0), "Should have statement for let x = 5")

        // Statement 1: let r = &x; -> should have UnaryOperator with operatorCode "&"
        val refOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "&" }
        assertTrue(refOps.isNotEmpty(), "Should have & reference operator")

        // Statement 2: let mr = &mut x; -> should have UnaryOperator with operatorCode "&mut"
        val mutRefOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "&mut" }
        assertTrue(mutRefOps.isNotEmpty(), "Should have &mut reference operator")

        // Statement 3: let v = *r; -> should have UnaryOperator with operatorCode "*"
        val derefOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "*" }
        assertTrue(derefOps.isNotEmpty(), "Should have * dereference operator")
    }

    @Test
    fun testTupleIndexExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("tuple_index.rs").toFile()),
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

        // t.0 and t.1 should produce MemberExpressions
        val memberExprs = body.allChildren<MemberExpression>()
        assertTrue(memberExprs.any { it.name.localName == "0" }, "Should have t.0 access")
        assertTrue(memberExprs.any { it.name.localName == "1" }, "Should have t.1 access")
    }

    @Test
    fun testNegativeLiteral() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("control_flow_advanced.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_negative_literal"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The match should parse without unknown problems
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Negative literals in match should not produce unknown problems: ${problems.map { it.problem }}",
        )
    }

    @Test
    fun testMethodCallOnField() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("method_calls.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_method_on_field"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val memberCalls = body.allChildren<MemberCallExpression>()
        assertTrue(
            memberCalls.any { it.name.localName == "len" },
            "Should have len() call: ${memberCalls.map { it.name }}",
        )
    }

    @Test
    fun testChainedMethods() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("method_calls.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_chained_methods"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val memberCalls = body.allChildren<MemberCallExpression>()
        assertTrue(memberCalls.size >= 2, "Should have chained method calls")
    }

    @Test
    fun testComplexClosures() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("closures_complex.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_complex_closure"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.size >= 3, "Should have 3 closures: ${lambdas.size}")

        // Closure with params
        val withParams = lambdas.first { it.function?.parameters?.isNotEmpty() == true }
        assertNotNull(withParams, "Should have closure with parameters")

        // Closure with no params
        val noParams = lambdas.first { it.function?.parameters?.isEmpty() == true }
        assertNotNull(noParams, "Should have closure with no parameters")
    }

    @Test
    fun testAllBinaryOperators() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("binary_operators.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_all_binary_ops"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val binOps = body.allChildren<BinaryOperator>()
        val opCodes = binOps.map { it.operatorCode }.toSet()
        val expected =
            setOf(
                "+",
                "-",
                "*",
                "/",
                "%",
                "&",
                "|",
                "^",
                "<<",
                ">>",
                "==",
                "!=",
                "<",
                "<=",
                ">",
                ">=",
                "&&",
                "||",
            )
        for (op in expected) {
            assertTrue(op in opCodes, "Should have binary operator '$op': found $opCodes")
        }
    }

    @Test
    fun testArrayIndex() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("array_index.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_array_index"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val subscripts = body.allChildren<SubscriptExpression>()
        assertTrue(subscripts.size >= 2, "Should have array subscript expressions")
    }

    @Test
    fun testGenericFunctionReference() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("method_calls.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_generic_ref"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // identity::<i32> without call should produce a Reference
        val refs = body.allChildren<Reference>()
        assertTrue(
            refs.any { it.name.localName == "identity" },
            "Should have reference to identity function: ${refs.map { it.name }}",
        )
    }

    @Test
    fun testBranchReturnExpressionInClosure() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["expr_return_in_closure"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with return")
        val returns = lambdas.flatMap { it.allChildren<ReturnStatement>() }
        assertTrue(returns.isNotEmpty(), "Closure should contain return statement")
    }

    @Test
    fun testBranchWhileExpressionInBlock() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["expr_while_in_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while loop in block expression")
    }

    @Test
    fun testBranchForExpressionInBlock() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["expr_for_in_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val forEachs = body.allChildren<ForEachStatement>()
        assertTrue(forEachs.isNotEmpty(), "Should have for loop in block expression")
    }

    @Test
    fun testBranchTupleIndexExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.any { it.name.localName == "0" }, "Should have .0 tuple access")
        assertTrue(members.any { it.name.localName == "1" }, "Should have .1 tuple access")
        assertTrue(members.any { it.name.localName == "2" }, "Should have .2 tuple access")
    }

    @Test
    fun testBranchTupleIndexNested() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_nested"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.size >= 2, "Should have nested tuple index accesses")
    }

    @Test
    fun testBranchNegativeFloatLiteral() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_negative_float_literal"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val hasFloats =
            body.allChildren<Literal<*>>().any { it.value is Double } ||
                body.allChildren<UnaryOperator>().any { it.operatorCode == "-" }
        assertTrue(hasFloats, "Should have negative float handling")
    }

    @Test
    fun testBranchIntegerSuffixesExtended() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_integer_suffixes_extended"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>().filter { it.value is Long }
        assertTrue(literals.size >= 7, "Should have 7+ integer literals with suffixes")
    }

    @Test
    fun testBranchUppercaseLiteralPrefixes() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_uppercase_literal_prefixes"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>().filter { it.value is Long }
        assertTrue(literals.isNotEmpty(), "Should have integer literals for prefixes")
    }

    @Test
    fun testBranchParenthesizedExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_parenthesized"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val binOps = body.allChildren<BinaryOperator>()
        assertTrue(binOps.isNotEmpty(), "Should have binary operators from parenthesized expr")
    }

    @Test
    fun testBranchUnitExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_unit_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        assertTrue(literals.any { it.value == null }, "Should have unit literal (null value)")
    }

    @Test
    fun testBranchRawStrings() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_raw_strings"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val strLiterals = literals.filter { it.value is String }
        assertTrue(strLiterals.size >= 3, "Should have 3+ raw string literals")
    }

    @Test
    fun testBranchSpecialStrings() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_special_strings"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Special strings should not produce unknown problems",
        )
    }

    @Test
    fun testBranchCharLiterals() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_char_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        assertTrue(
            literals.any { it.value is String && (it.value as String).length <= 2 },
            "Should have char literals",
        )
    }

    @Test
    fun testBranchBoolLiterals() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_bool_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        assertTrue(literals.any { it.value == true }, "Should have true literal")
        assertTrue(literals.any { it.value == false }, "Should have false literal")
    }

    @Test
    fun testBranchScopedIds() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_scoped_ids"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val refs = body.allChildren<Reference>()
        assertTrue(refs.isNotEmpty(), "Should have references including scoped identifiers")
    }

    @Test
    fun testBranchIndexExpr() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_index_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val subscripts = body.allChildren<SubscriptExpression>()
        assertTrue(subscripts.size >= 2, "Should have 2+ subscript expressions")
    }

    @Test
    fun testBranchAllRanges() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_all_ranges"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val ranges = body.allChildren<RangeExpression>()
        assertTrue(ranges.size >= 4, "Should have 4+ range expressions: ${ranges.size}")
    }

    @Test
    fun testBranchCasts() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_casts"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val casts = body.allChildren<CastExpression>()
        assertTrue(casts.size >= 2, "Should have 2+ cast expressions: ${casts.size}")
    }

    @Test
    fun testBranchClosureTypedBlock() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_closure_typed_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with typed params")
    }

    @Test
    fun testBranchClosureReturnType() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_closure_return_type"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with return type")
    }

    @Test
    fun testBranchNegativeIntLiteral() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_negative_int_literal"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val allExprs = body.allChildren<Expression>()
        assertTrue(allExprs.isNotEmpty(), "Should have negative integer expressions")
    }

    @Test
    fun testBranchFloatExponent() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_float_exponent"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val floatLiterals = literals.filter { it.value is Double }
        assertTrue(floatLiterals.size >= 3, "Should have 3+ float literals")
    }

    @Test
    fun testBranchTryExpr() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_try_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "?" }, "Should have ? try operator")
    }

    @Test
    fun testBranchBlockAsValue() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_block_as_value"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val blocks = body.allChildren<Block>()
        assertTrue(blocks.size >= 2, "Should have block expressions as values")
    }

    @Test
    fun testBranchFnReturningClosure() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_fn_returning_closure"]
        assertNotNull(func)
    }

    @Test
    fun testDeepAssignments() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("assignments_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_assignments"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        assertTrue(assigns.any { it.operatorCode == "=" }, "Should have direct assignment")
    }

    @Test
    fun testDeepCompoundAssignments() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("assignments_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_compound_assignments"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        val opCodes = assigns.map { it.operatorCode }.toSet()
        val expected = setOf("+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=")
        for (op in expected) {
            assertTrue(op in opCodes, "Should have compound assignment '$op'")
        }
    }

    @Test
    fun testDeepClosureAsArg() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("closures_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_closure_as_arg"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.size >= 2, "Should have 2+ closures")
    }

    @Test
    fun testDeepClosureWithMove() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("closures_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_closure_with_move"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have move closure")
    }

    @Test
    fun testDeepTypeCast() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("type_operations.rs").toFile()),
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
        assertTrue(casts.size >= 2, "Should have multiple type casts")
    }

    @Test
    fun testDeepTupleIndexSimple() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("tuple_field_access.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_simple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.any { it.name.localName == "0" }, "Should have .0 access")
        assertTrue(members.any { it.name.localName == "2" }, "Should have .2 access")
    }

    @Test
    fun testDeepTupleIndexNested() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("tuple_field_access.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_nested"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.size >= 2, "Should have multiple tuple index accesses")
    }

    @Test
    fun testDeepFieldAccess() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("field_access.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_field_access"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.any { it.name.localName == "x" }, "Should have .x field access")
        assertTrue(members.any { it.name.localName == "y" }, "Should have .y field access")
    }

    @Test
    fun testDeepRanges() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("range_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_ranges"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val ranges = body.allChildren<RangeExpression>()
        assertTrue(ranges.size >= 2, "Should have multiple range expressions")
    }

    @Test
    fun testDeepFloatLiterals() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("comprehensive.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_float_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val floatLits = literals.filter { it.value is Double }
        assertTrue(floatLits.isNotEmpty(), "Should have float literals")
    }

    @Test
    fun testDeepMoreIntegers() {
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
        val intLits = body.allChildren<Literal<*>>().filter { it.value is Long }
        assertTrue(intLits.size >= 5, "Should have 5+ integer literals")
    }

    @Test
    fun testDeepAwaitExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("comprehensive.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_await"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(problems.none { it.problem.contains("Unknown") }, "Await should not error")
    }

    @Test
    fun testDeepTryOperator() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("comprehensive.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_try_operator"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "?" }, "Should have ? try operator")
    }

    @Test
    fun testDeepUnaryOps() {
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
        val opCodes = body.allChildren<UnaryOperator>().map { it.operatorCode }.toSet()
        assertTrue("-" in opCodes, "Should have negation")
        assertTrue("!" in opCodes, "Should have logical not")
    }

    @Test
    fun testDeepScopedIds() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("comprehensive.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_scoped_ids"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val refs = body.allChildren<Reference>()
        assertTrue(refs.isNotEmpty(), "Should have scoped references")
    }
}
