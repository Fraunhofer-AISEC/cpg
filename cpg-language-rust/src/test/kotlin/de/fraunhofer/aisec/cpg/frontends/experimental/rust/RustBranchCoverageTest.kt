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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

/**
 * Tests targeting specifically uncovered branches in the Rust CPG frontend handlers. Each test
 * exercises a code path that was previously not covered according to JaCoCo reports.
 */
class RustBranchCoverageTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    // =========================================================================
    // ExpressionHandler: return/while/for dispatched as expressions
    // =========================================================================

    @Test
    fun testReturnExpressionInClosure() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["expr_return_in_closure"]
        assertNotNull(func, "Should have expr_return_in_closure function")
        val body = func.body as? Block
        assertNotNull(body)
        // The closure should contain a return statement
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with return")
        val returns = lambdas.flatMap { it.allChildren<ReturnStatement>() }
        assertTrue(returns.isNotEmpty(), "Closure should contain return statement")
    }

    @Test
    fun testWhileExpressionInBlock() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["expr_while_in_block"]
        assertNotNull(func, "Should have expr_while_in_block function")
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while loop in block expression")
    }

    @Test
    fun testForExpressionInBlock() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["expr_for_in_block"]
        assertNotNull(func, "Should have expr_for_in_block function")
        val body = func.body as? Block
        assertNotNull(body)
        val forEachs = body.allChildren<ForEachStatement>()
        assertTrue(forEachs.isNotEmpty(), "Should have for loop in block expression")
    }

    // =========================================================================
    // ExpressionHandler: tuple_index_expression (was 0% covered)
    // =========================================================================

    @Test
    fun testTupleIndexExpression() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_expr"]
        assertNotNull(func, "Should have test_tuple_index_expr function")
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(
            members.any { it.name.localName == "0" },
            "Should have .0 tuple access: ${members.map { it.name }}",
        )
        assertTrue(members.any { it.name.localName == "1" }, "Should have .1 tuple access")
        assertTrue(members.any { it.name.localName == "2" }, "Should have .2 tuple access")
    }

    @Test
    fun testTupleIndexNested() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_nested"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.size >= 2, "Should have nested tuple index accesses")
    }

    // =========================================================================
    // ExpressionHandler: negative float literal
    // =========================================================================

    @Test
    fun testNegativeFloatLiteral() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_negative_float_literal"]
        assertNotNull(func, "Should have test_negative_float_literal function")
        val body = func.body as? Block
        assertNotNull(body)
        // Negative float may be parsed as negative_literal (with decimal) or as
        // unary_expression(-) + float_literal. Either way the function should parse fine.
        val allExprs = body.allChildren<Expression>()
        assertTrue(allExprs.isNotEmpty(), "Should have expressions for negative float literals")
        // Check that we have either float Literals or UnaryOperators with float input
        val hasFloats =
            body.allChildren<Literal<*>>().any { it.value is Double } ||
                body.allChildren<UnaryOperator>().any { it.operatorCode == "-" }
        assertTrue(hasFloats, "Should have negative float handling")
    }

    // =========================================================================
    // ExpressionHandler: integer suffix variants
    // =========================================================================

    @Test
    fun testIntegerSuffixesExtended() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_integer_suffixes_extended"]
        assertNotNull(func, "Should have test_integer_suffixes_extended function")
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val intLiterals = literals.filter { it.value is Long }
        assertTrue(
            intLiterals.size >= 7,
            "Should have 7+ integer literals with suffixes: ${intLiterals.size}",
        )
    }

    // =========================================================================
    // StatementHandler: let with mutable pattern
    // =========================================================================

    @Test
    fun testLetMutPattern() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_mut_pattern"]
        assertNotNull(func, "Should have test_let_mut_pattern function")
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.any { it.name.localName == "x" }, "Should have variable x")
    }

    // =========================================================================
    // StatementHandler: ref_pattern and mut_pattern in extractBindings
    // =========================================================================

    @Test
    fun testRefPatternBinding() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_ref_pattern_binding"]
        assertNotNull(func, "Should have test_ref_pattern_binding function")
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch with ref pattern")
    }

    @Test
    fun testMutPatternBinding() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_mut_pattern_binding"]
        assertNotNull(func, "Should have test_mut_pattern_binding function")
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch with mut pattern")
    }

    // =========================================================================
    // StatementHandler: while let
    // =========================================================================

    @Test
    fun testWhileLet() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_let"]
        assertNotNull(func, "Should have test_while_let function")
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while let loop")
    }

    @Test
    fun testWhileLetOption() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_let_option"]
        assertNotNull(func, "Should have test_while_let_option function")
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while-let loop")
    }

    // =========================================================================
    // DeclarationHandler: tuple struct
    // =========================================================================

    @Test
    fun testTupleStructs() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val pair = tu.records["Pair"]
        assertNotNull(pair, "Should have Pair tuple struct")
        val triple = tu.records["Triple"]
        assertNotNull(triple, "Should have Triple tuple struct")
        val wrapper = tu.records["Wrapper"]
        assertNotNull(wrapper, "Should have Wrapper tuple struct")
    }

    // =========================================================================
    // DeclarationHandler: trait with generic type parameters
    // =========================================================================

    @Test
    fun testTraitWithTypeParams() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        // Generic trait should be wrapped in a template
        val templates = tu.allChildren<TemplateDeclaration>()
        val converterTemplate = templates.find { it.name.localName == "Converter" }
        assertNotNull(
            converterTemplate,
            "Should have Converter template: ${templates.map { it.name }}",
        )
    }

    // =========================================================================
    // DeclarationHandler: mut parameter pattern
    // =========================================================================

    @Test
    fun testMutParam() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_mut_param"]
        assertNotNull(func, "Should have test_mut_param function")
        assertTrue(func.parameters.isNotEmpty(), "Should have parameters")
    }

    // =========================================================================
    // DeclarationHandler: attribute propagation in trait
    // =========================================================================

    @Test
    fun testAnnotatedTrait() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["Annotated"]
        assertNotNull(record, "Should have Annotated trait")
        assertTrue(
            record.methods.any { it.name.localName == "compute" },
            "Trait should have compute method",
        )
    }

    // =========================================================================
    // DeclarationHandler: attribute propagation in impl
    // =========================================================================

    @Test
    fun testAnnotatedImpl() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["MyType"]
        assertNotNull(record, "Should have MyType record")
        val methods = record.methods
        assertTrue(
            methods.any { it.name.localName == "get_value" },
            "MyType should have get_value method",
        )
    }

    // =========================================================================
    // DeclarationHandler: attribute propagation in module
    // =========================================================================

    @Test
    fun testAnnotatedModule() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val namespaces = tu.allChildren<NamespaceDeclaration>()
        assertTrue(
            namespaces.any { it.name.localName == "annotated_mod" },
            "Should have annotated_mod namespace",
        )
    }

    // =========================================================================
    // DeclarationHandler: extern "C" with ABI string
    // =========================================================================

    @Test
    fun testExternCBlock() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val funcs = tu.allChildren<FunctionDeclaration>()
        assertTrue(
            funcs.any { it.name.localName == "c_abs" },
            "Should have extern C function c_abs",
        )
    }

    // =========================================================================
    // DeclarationHandler: empty_statement and macro_invocation at decl level
    // =========================================================================

    @Test
    fun testEmptyStatementAndMacroDecl() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        // Just verify no crash -- these produce problem declarations or are handled
        val allDecls = tu.allChildren<Declaration>()
        assertTrue(allDecls.isNotEmpty(), "Should have declarations")
    }

    // =========================================================================
    // TypeHandler: bounded_type
    // =========================================================================

    @Test
    fun testBoundedType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_bounded_type"]
        assertNotNull(func, "Should have test_bounded_type function")
        assertTrue(func.parameters.isNotEmpty(), "Should have parameter with bounded type")
    }

    // =========================================================================
    // TypeHandler: fn type without return
    // =========================================================================

    @Test
    fun testFnTypeNoReturn() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_fn_type_no_return"]
        assertNotNull(func, "Should have test_fn_type_no_return function")
        assertTrue(func.parameters.isNotEmpty(), "Should have fn type parameter")
    }

    // =========================================================================
    // Additional coverage: closures with typed params and block body
    // =========================================================================

    @Test
    fun testClosureTypedBlock() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_typed_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with typed params")
        val innerFunc = lambdas.first().function
        assertNotNull(innerFunc, "Lambda should have function declaration")
        assertTrue(
            innerFunc.parameters.size >= 2,
            "Closure should have 2 typed parameters: ${innerFunc.parameters.size}",
        )
    }

    @Test
    fun testClosureReturnType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_return_type"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with return type")
    }

    // =========================================================================
    // Parenthesized expression
    // =========================================================================

    @Test
    fun testParenthesizedExpression() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_parenthesized"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val binOps = body.allChildren<BinaryOperator>()
        assertTrue(binOps.isNotEmpty(), "Should have binary operators from parenthesized expr")
    }

    // =========================================================================
    // Unit expression
    // =========================================================================

    @Test
    fun testUnitExpression() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unit_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        assertTrue(literals.any { it.value == null }, "Should have unit literal (null value)")
    }

    // =========================================================================
    // Self reference, method calls
    // =========================================================================

    @Test
    fun testMethodCalls() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_method_calls"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val memberCalls = body.allChildren<MemberCallExpression>()
        assertTrue(memberCalls.isNotEmpty(), "Should have member call expressions")
    }

    @Test
    fun testCounterMethods() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val counter = tu.records["Counter"]
        assertNotNull(counter, "Should have Counter struct")
        assertTrue(
            counter.methods.any { it.name.localName == "increment" },
            "Counter should have increment method",
        )
        assertTrue(
            counter.methods.any { it.name.localName == "get" },
            "Counter should have get method",
        )
        // Check self parameter
        val incrementMethod = counter.methods.first { it.name.localName == "increment" }
        assertNotNull(incrementMethod.receiver, "increment should have self receiver")
    }

    // =========================================================================
    // Nested if statements
    // =========================================================================

    @Test
    fun testNestedIf() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_if"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // All if branches in test_nested_if have else clauses, so they are modeled
        // as ConditionalExpression (Rust if-else is an expression).
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.size >= 3, "Should have nested conditional chain: ${condExprs.size}")
    }

    // =========================================================================
    // Match with wildcards
    // =========================================================================

    @Test
    fun testMatchWildcard() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_wildcard"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms: ${cases.size}")
    }

    // =========================================================================
    // Labeled loops
    // =========================================================================

    @Test
    fun testLabeledWhile() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_while"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val labels = body.allChildren<LabelStatement>()
        assertTrue(labels.isNotEmpty(), "Should have labeled while loop")
        val breaks = body.allChildren<BreakStatement>()
        assertTrue(
            breaks.any { it.label == "outer" },
            "Should have break 'outer: ${breaks.map { it.label }}",
        )
    }

    @Test
    fun testLabeledFor() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_for"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val labels = body.allChildren<LabelStatement>()
        assertTrue(labels.isNotEmpty(), "Should have labeled for loop")
    }

    // =========================================================================
    // Raw string literals
    // =========================================================================

    @Test
    fun testRawStrings() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_raw_strings"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val strLiterals = literals.filter { it.value is String }
        assertTrue(strLiterals.size >= 3, "Should have 3+ raw string literals")
    }

    // =========================================================================
    // Special string types (byte, C string)
    // =========================================================================

    @Test
    fun testSpecialStrings() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_special_strings"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // byte string and c string should be parsed
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Special strings should not produce unknown problems",
        )
    }

    // =========================================================================
    // Char and boolean literals
    // =========================================================================

    @Test
    fun testCharLiterals() {
        val tu = parseTU("branch_coverage.rs")
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
    fun testBoolLiterals() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_bool_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        assertTrue(literals.any { it.value == true }, "Should have true literal")
        assertTrue(literals.any { it.value == false }, "Should have false literal")
    }

    // =========================================================================
    // Where clause
    // =========================================================================

    @Test
    fun testWhereClause() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_where_clause"]
        assertNotNull(func, "Should have function with where clause")
    }

    // =========================================================================
    // Trait impl
    // =========================================================================

    @Test
    fun testTraitImpl() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val counter = tu.records["Counter"]
        assertNotNull(counter, "Should have Counter")
        // Check that Summary trait is implemented
        assertTrue(
            counter.methods.any { it.name.localName == "summarize" },
            "Counter should have summarize from Summary trait impl",
        )
    }

    // =========================================================================
    // Empty match arm
    // =========================================================================

    @Test
    fun testEmptyMatchArm() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_empty_match_arm"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms: ${cases.size}")
    }

    // =========================================================================
    // Nested tuple match patterns
    // =========================================================================

    @Test
    fun testNestedTupleMatch() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_tuple_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    // =========================================================================
    // Enum patterns
    // =========================================================================

    @Test
    fun testEnumPatterns() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_enum_patterns"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3+ match arms for enum: ${cases.size}")
    }

    // =========================================================================
    // Function type parameters
    // =========================================================================

    @Test
    fun testFnTypeWithReturn() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_fn_type_with_return"]
        assertNotNull(func, "Should have test_fn_type_with_return function")
        assertTrue(func.parameters.isNotEmpty(), "Should have fn type parameter")
    }

    // =========================================================================
    // Async function
    // =========================================================================

    @Test
    fun testAsyncFunction() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["async_helper"]
        assertNotNull(func, "Should have async_helper function")
    }

    @Test
    fun testAsyncAwait() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_await"]
        assertNotNull(func, "Should have test_async_await function")
        val body = func.body as? Block
        assertNotNull(body)
        // Await produces a UnaryOperator with "await"
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(
            unaryOps.any { it.operatorCode == "await" },
            "Should have await operator: ${unaryOps.map { it.operatorCode }}",
        )
    }

    // =========================================================================
    // Try expression
    // =========================================================================

    @Test
    fun testTryExpr() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_try_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "?" }, "Should have ? try operator")
    }

    // =========================================================================
    // Index expression
    // =========================================================================

    @Test
    fun testIndexExpr() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_index_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val subscripts = body.allChildren<SubscriptExpression>()
        assertTrue(subscripts.size >= 2, "Should have 2+ subscript expressions")
    }

    // =========================================================================
    // Range expressions
    // =========================================================================

    @Test
    fun testAllRanges() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_all_ranges"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val ranges = body.allChildren<RangeExpression>()
        assertTrue(ranges.size >= 4, "Should have 4+ range expressions: ${ranges.size}")
    }

    // =========================================================================
    // Cast expressions
    // =========================================================================

    @Test
    fun testCasts() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_casts"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val casts = body.allChildren<CastExpression>()
        assertTrue(casts.size >= 2, "Should have 2+ cast expressions: ${casts.size}")
    }

    // =========================================================================
    // Unsafe block
    // =========================================================================

    @Test
    fun testUnsafeBlock() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unsafe_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // Should produce Block with "unsafe" annotation
        val blocks = body.allChildren<Block>()
        assertTrue(
            blocks.any { block -> block.annotations.any { it.name.localName == "unsafe" } },
            "Should have unsafe block",
        )
    }

    // =========================================================================
    // Async block
    // =========================================================================

    @Test
    fun testAsyncBlock() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val blocks = body.allChildren<Block>()
        assertTrue(
            blocks.any { block -> block.annotations.any { it.name.localName == "async" } },
            "Should have async block",
        )
    }

    // =========================================================================
    // Struct expression with all field init types
    // =========================================================================

    @Test
    fun testStructFull() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_full"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.size >= 3, "Should have 3+ struct constructions: ${constructs.size}")
    }

    // =========================================================================
    // Macro invocations
    // =========================================================================

    @Test
    fun testMacros() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_macros"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val calls = body.allChildren<CallExpression>()
        assertTrue(
            calls.any { it.name.localName == "println" },
            "Should have println macro call: ${calls.map { it.name }}",
        )
    }

    // =========================================================================
    // Pointer types
    // =========================================================================

    @Test
    fun testPointerTypes() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_pointer_types"]
        assertNotNull(func, "Should have test_pointer_types function")
        assertTrue(func.parameters.size >= 2, "Should have 2 pointer parameters")
    }

    // =========================================================================
    // Array type in signature
    // =========================================================================

    @Test
    fun testArrayType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_array_type"]
        assertNotNull(func, "Should have test_array_type function")
        assertTrue(func.parameters.isNotEmpty(), "Should have array parameter")
    }

    // =========================================================================
    // Tuple type in signature
    // =========================================================================

    @Test
    fun testTupleType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_type"]
        assertNotNull(func, "Should have test_tuple_type function")
        assertTrue(func.parameters.isNotEmpty(), "Should have tuple parameter")
    }

    // =========================================================================
    // Reference type with lifetime in struct
    // =========================================================================

    @Test
    fun testRefHolderStruct() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["RefHolder"]
        assertNotNull(record, "Should have RefHolder struct with lifetime")
        assertTrue(record.fields.size >= 2, "RefHolder should have 2 fields")
    }

    // =========================================================================
    // Const, static, type alias
    // =========================================================================

    @Test
    fun testConstStaticDecls() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val decls = tu.allChildren<VariableDeclaration>()
        assertTrue(decls.any { it.name.localName == "PI" }, "Should have const PI")
        assertTrue(decls.any { it.name.localName == "COUNTER" }, "Should have static mut COUNTER")
    }

    // =========================================================================
    // Union
    // =========================================================================

    @Test
    fun testUnion() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["IntOrFloat"]
        assertNotNull(record, "Should have IntOrFloat union")
        assertTrue(record.fields.size >= 2, "Union should have 2 fields")
    }

    // =========================================================================
    // Associated type in trait
    // =========================================================================

    @Test
    fun testAssociatedType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["Container2"]
        assertNotNull(record, "Should have Container2 trait")
        assertTrue(
            record.methods.any { it.name.localName == "first" },
            "Container2 should have first method",
        )
    }

    // =========================================================================
    // Abstract type (impl Trait) in parameter
    // =========================================================================

    @Test
    fun testAbstractParam() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_abstract_param"]
        assertNotNull(func, "Should have test_abstract_param function")
        assertTrue(func.parameters.isNotEmpty(), "Should have impl Trait parameter")
    }

    // =========================================================================
    // Nested function
    // =========================================================================

    @Test
    fun testNestedFunction() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_fn"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val innerFuncs = body.allChildren<FunctionDeclaration>()
        assertTrue(
            innerFuncs.any { it.name.localName == "helper" },
            "Should have nested helper function",
        )
    }

    // =========================================================================
    // Use and extern crate declarations
    // =========================================================================

    @Test
    fun testUseDeclarations() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(includes.size >= 2, "Should have use declarations: ${includes.size}")
    }

    @Test
    fun testExternCrate() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(
            includes.any { it.name.localName == "std" },
            "Should have extern crate std: ${includes.map { it.name }}",
        )
    }

    // =========================================================================
    // Inner attribute
    // =========================================================================

    @Test
    fun testInnerAttribute() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val decls = tu.allChildren<VariableDeclaration>()
        // Inner attribute creates a variable declaration with annotation
        val annotatedDecls =
            decls.filter { d -> d.annotations.any { it.name.localName.contains("#!") } }
        assertTrue(annotatedDecls.isNotEmpty(), "Should have inner attribute declaration")
    }

    // =========================================================================
    // Never type and impl return
    // =========================================================================

    @Test
    fun testNeverType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_never_type"]
        assertNotNull(func, "Should have function returning never type")
    }

    @Test
    fun testImplReturn() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_impl_return"]
        assertNotNull(func, "Should have function returning impl trait")
    }

    // =========================================================================
    // Dynamic trait
    // =========================================================================

    @Test
    fun testDynTrait() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_dyn_trait"]
        assertNotNull(func, "Should have test_dyn_trait function")
        assertTrue(func.parameters.isNotEmpty(), "Should have dyn trait parameter")
    }

    // =========================================================================
    // Scoped identifiers
    // =========================================================================

    @Test
    fun testScopedIds() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_scoped_ids"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val refs = body.allChildren<Reference>()
        assertTrue(refs.isNotEmpty(), "Should have references including scoped identifiers")
    }

    // =========================================================================
    // Section 6: Targeted constructs for remaining uncovered branches
    // =========================================================================

    @Test
    fun testClosureReturnExpr() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_return_expr"]
        assertNotNull(func, "Should have test_closure_return_expr function")
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure")
        // The closure contains a return statement
        val returns = lambdas.flatMap { it.allChildren<ReturnStatement>() }
        assertTrue(returns.isNotEmpty(), "Closure should contain return statement")
    }

    @Test
    fun testLetWhileValue() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_while_value"]
        assertNotNull(func, "Should have test_let_while_value function")
    }

    @Test
    fun testLetForValue() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_for_value"]
        assertNotNull(func, "Should have test_let_for_value function")
    }

    @Test
    fun testClosureWithReturnBody() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_with_return_body"]
        assertNotNull(func, "Should have test_closure_with_return_body function")
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with return body")
    }

    @Test
    fun testMatchWithGuard() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_with_guard"]
        assertNotNull(func, "Should have test_match_with_guard function")
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch")
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms: ${cases.size}")
    }

    @Test
    fun testSingleAlternativePattern() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_single_alternative_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 2, "Should have match arms: ${cases.size}")
    }

    @Test
    fun testNegativeIntLiteral() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_negative_int_literal"]
        assertNotNull(func, "Should have test_negative_int_literal function")
        val body = func.body as? Block
        assertNotNull(body)
        // Negative integer literals should be handled (either as negative_literal or unary -)
        val allExprs = body.allChildren<Expression>()
        assertTrue(allExprs.isNotEmpty(), "Should have negative integer expressions")
    }

    @Test
    fun testFloatExponent() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_float_exponent"]
        assertNotNull(func, "Should have test_float_exponent function")
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val floatLiterals = literals.filter { it.value is Double }
        assertTrue(floatLiterals.size >= 3, "Should have 3+ float literals: ${floatLiterals.size}")
    }

    @Test
    fun testQualifiedPath() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_qualified_path"]
        assertNotNull(func, "Should have test_qualified_path function")
    }

    @Test
    fun testLoopWithBreakValue() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_loop_with_break_value"]
        assertNotNull(func, "Should have test_loop_with_break_value function")
        val body = func.body as? Block
        assertNotNull(body)
        // loop { break 42; } is modeled as while(true) - it may appear as
        // a separate statement or as initializer depending on handler dispatch
        val allStmts = body.allChildren<Statement>()
        assertTrue(allStmts.isNotEmpty(), "Should have statements from loop with break value")
    }

    @Test
    fun testLabeledLoopBreak() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_loop_break"]
        assertNotNull(func, "Should have test_labeled_loop_break function")
        val body = func.body as? Block
        assertNotNull(body)
        // Labeled loop may produce a LabelStatement or just a WhileStatement
        val allStmts = body.allChildren<Statement>()
        assertTrue(allStmts.isNotEmpty(), "Should have statements from labeled loop")
    }

    @Test
    fun testIfLet() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let"]
        assertNotNull(func, "Should have test_if_let function")
        val body = func.body as? Block
        assertNotNull(body)
        // if let with else clause is modeled as ConditionalExpression
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have if-let conditional expression")
    }

    @Test
    fun testIfLetChain() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let_chain"]
        assertNotNull(func, "Should have test_if_let_chain function")
        val body = func.body as? Block
        assertNotNull(body)
        // Both if-let expressions have else clauses, so they become ConditionalExpressions
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.size >= 2, "Should have nested if-let conditionals: ${condExprs.size}")
    }

    @Test
    fun testStructPatternMatch() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_pattern_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 struct pattern match arms: ${cases.size}")
    }

    @Test
    fun testSlicePattern() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_slice_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 slice pattern match arms: ${cases.size}")
    }

    @Test
    fun testFieldPatternBinding() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_field_pattern_binding"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.isNotEmpty(), "Should have field pattern match arm")
    }

    @Test
    fun testContinueWithLabel() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_continue_with_label"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val continues = body.allChildren<ContinueStatement>()
        assertTrue(
            continues.any { it.label == "outer" },
            "Should have continue 'outer: ${continues.map { it.label }}",
        )
    }

    @Test
    fun testSelfMethodCalls() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_self_method_calls"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val memberCalls = body.allChildren<MemberCallExpression>()
        assertTrue(memberCalls.size >= 3, "Should have 3+ member calls: ${memberCalls.size}")
    }

    @Test
    fun testRefAndMutRef() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_ref_and_mut_ref"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "&" }, "Should have & reference")
        assertTrue(unaryOps.any { it.operatorCode == "&mut" }, "Should have &mut reference")
    }

    @Test
    fun testConstrainedGeneric() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_constrained_generic"]
        assertNotNull(func, "Should have test_constrained_generic function")
    }

    @Test
    fun testPubFields() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["PubFields"]
        assertNotNull(record, "Should have PubFields struct")
        assertTrue(record.fields.size >= 2, "Should have 2 fields")
    }

    @Test
    fun testTupleDestructure() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_destructure"]
        assertNotNull(func, "Should have test_tuple_destructure function")
    }

    @Test
    fun testMatchEmptyArms() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_empty_arms"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms: ${cases.size}")
    }

    @Test
    fun testLetWithMutDestructure() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_with_mut_destructure"]
        assertNotNull(func, "Should have test_let_with_mut_destructure function")
        val body = func.body as? Block
        assertNotNull(body)
        val ifStmts = body.allChildren<IfStatement>()
        assertTrue(ifStmts.isNotEmpty(), "Should have if let with mut destructure")
    }

    @Test
    fun testFnWithMutParam() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_fn_with_mut_param"]
        assertNotNull(func, "Should have test_fn_with_mut_param function")
        assertTrue(func.parameters.isNotEmpty(), "Should have mut parameter")
    }

    @Test
    fun testExpressionStatementVariants() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_expression_statement_variants"]
        assertNotNull(func, "Should have test_expression_statement_variants function")
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        assertTrue(assigns.size >= 2, "Should have assignment statements: ${assigns.size}")
    }

    @Test
    fun testComplexEnumMatch() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_complex_enum_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 enum match arms: ${cases.size}")
    }

    @Test
    fun testEnumConstruction() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_enum_construction"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have struct/enum constructions")
    }

    // =========================================================================
    // Section 6bb+: Additional coverage tests
    // =========================================================================

    @Test
    fun testLetWithoutType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_without_type"]
        assertNotNull(func, "Should have test_let_without_type function")
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.any { it.name.localName == "x" }, "Should have declaration of x")
    }

    @Test
    fun testLetTypedNoValue() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_typed_no_value"]
        assertNotNull(func, "Should have test_let_typed_no_value function")
    }

    @Test
    fun testEmptyStatements() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_empty_statements"]
        assertNotNull(func, "Should have test_empty_statements function")
    }

    @Test
    fun testClosureSimpleParams() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_simple_params"]
        assertNotNull(func, "Should have test_closure_simple_params function")
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closure with simple params")
    }

    @Test
    fun testOrPatternComplex() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_or_pattern_complex"]
        assertNotNull(func, "Should have test_or_pattern_complex function")
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3+ match arms: ${cases.size}")
    }

    @Test
    fun testNestedMatch() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_match"]
        assertNotNull(func, "Should have test_nested_match function")
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.size >= 2, "Should have nested match: ${switches.size}")
    }

    @Test
    fun testMultipleReturns() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_multiple_returns"]
        assertNotNull(func, "Should have test_multiple_returns function")
        val body = func.body as? Block
        assertNotNull(body)
        val returns = body.allChildren<ReturnStatement>()
        assertTrue(returns.size >= 2, "Should have 2+ return statements: ${returns.size}")
    }

    @Test
    fun testBlockAsValue() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_block_as_value"]
        assertNotNull(func, "Should have test_block_as_value function")
        val body = func.body as? Block
        assertNotNull(body)
        val blocks = body.allChildren<Block>()
        assertTrue(blocks.size >= 2, "Should have block expressions as values")
    }

    @Test
    fun testGenericImpl() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_generic_impl"]
        assertNotNull(func, "Should have test_generic_impl function")
    }

    @Test
    fun testTraitDefaultMethod() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_trait_default_method"]
        assertNotNull(func, "Should have test_trait_default_method function")
    }

    @Test
    fun testLetUnderscore() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_underscore"]
        assertNotNull(func, "Should have test_let_underscore function")
    }

    @Test
    fun testComplexGeneric() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_complex_generic"]
        assertNotNull(func, "Should have test_complex_generic function")
    }

    @Test
    fun testTraitObject() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_trait_object"]
        assertNotNull(func, "Should have test_trait_object function")
    }

    @Test
    fun testScopedType() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_scoped_type"]
        assertNotNull(func, "Should have test_scoped_type function")
    }

    @Test
    fun testFnReturningClosure() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_fn_returning_closure"]
        assertNotNull(func, "Should have test_fn_returning_closure function")
    }

    @Test
    fun testAsyncExpr() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_expr"]
        assertNotNull(func, "Should have test_async_expr function")
    }

    // =========================================================================
    // Section 7: Final coverage push - reachable but uncovered paths
    // =========================================================================

    @Test
    fun testBlockMacroInvocationAtTopLevel() {
        // Block-style macro invocation (without semicolon) at top level triggers
        // DeclarationHandler.handleMacroInvocationDecl (L64, L788-797)
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        // The macro invocation should produce a declaration
        val allDecls = tu.allChildren<Declaration>()
        assertTrue(allDecls.isNotEmpty(), "Should have declarations including macro invocation")
    }

    @Test
    fun testTupleLetNoInit() {
        // Tuple let without initializer covers StatementHandler L224-227
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_let_no_init"]
        assertNotNull(func, "Should have test_tuple_let_no_init function")
        val body = func.body as? Block
        assertNotNull(body)
        // Should have tuple declaration without initializer
        val decls = body.allChildren<TupleDeclaration>()
        assertTrue(decls.isNotEmpty(), "Should have tuple declaration")
    }

    @Test
    fun testTraitWithConst() {
        // Const item in trait body triggers DeclarationHandler L426 (else -> null)
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu)
        val record = tu.records["TraitWithConst"]
        assertNotNull(record, "Should have TraitWithConst trait")
        assertTrue(
            record.methods.any { it.name.localName == "value" },
            "TraitWithConst should have value method",
        )
    }

    // =========================================================================
    // Verify whole file parses without crashes
    // =========================================================================

    @Test
    fun testWholeFileParsesSuccessfully() {
        val tu = parseTU("branch_coverage.rs")
        assertNotNull(tu, "Translation unit should parse successfully")
        // Verify we have a substantial number of declarations
        val allFuncs = tu.allChildren<FunctionDeclaration>()
        assertTrue(allFuncs.size >= 30, "Should have many function declarations: ${allFuncs.size}")
    }
}
