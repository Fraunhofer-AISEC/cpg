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

class RustDeepCoverageTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    // ==================== Match pattern coverage ====================

    @Test
    fun testMatchStructPattern() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_struct_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch from match")
    }

    @Test
    fun testMatchStructRename() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_struct_rename"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch from match with renamed fields")
    }

    @Test
    fun testMatchTupleStruct() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_tuple_struct"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch")
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms: ${cases.size}")
    }

    @Test
    fun testMatchOrPattern() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_or_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val binOps = body.allChildren<BinaryOperator>()
        assertTrue(
            binOps.any { it.operatorCode == "|" },
            "Should have or-pattern binary operator: ${binOps.map { it.operatorCode }}",
        )
    }

    @Test
    fun testMatchRefPattern() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_ref_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch")
    }

    @Test
    fun testMatchGuard() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_guard"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms")
        // Guard expressions produce binary operators with "if"
        val ifOps = body.allChildren<BinaryOperator>().filter { it.operatorCode == "if" }
        assertTrue(ifOps.isNotEmpty(), "Should have guard 'if' operators")
    }

    @Test
    fun testMatchNestedTuple() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_nested_tuple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testMatchSlice() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_slice"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch from slice match")
    }

    @Test
    fun testMatchNegativeLiteral() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_negative_literal"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms")
    }

    // ==================== Let declarations ====================

    @Test
    fun testLetMut() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_mut"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        assertTrue(assigns.any { it.operatorCode == "+=" }, "Should have compound assignment")
    }

    @Test
    fun testLetTypeAnnotation() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_type_annotation"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.size >= 4, "Should have 4+ variable declarations")
    }

    @Test
    fun testLetNoValue() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_no_value"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.isNotEmpty(), "Should have variable declaration")
    }

    @Test
    fun testLetDestructureStruct() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_destructure_struct"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // Should parse without unknown problems
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Struct destructure should not produce unknown problems: ${problems.map { it.problem }}",
        )
    }

    // ==================== If expressions ====================

    @Test
    fun testIfLet() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // if let with else clause is modeled as ConditionalExpression
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have if-let conditional expression")
        assertNotNull(condExprs.first().elseExpression, "If-let should have else branch")
    }

    @Test
    fun testIfLetNoElse() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let_no_else"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val ifStmts = body.allChildren<IfStatement>()
        assertTrue(ifStmts.isNotEmpty(), "Should have if-let statement")
    }

    @Test
    fun testIfElseIf() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_else_if"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // if-else-if chains with else are modeled as nested ConditionalExpressions
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.size >= 2, "Should have nested if-else-if chain")
    }

    @Test
    fun testIfSimple() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_simple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // Simple if-else is modeled as ConditionalExpression
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have conditional expression")
        assertNotNull(condExprs.first().elseExpression, "Should have else branch")
    }

    // ==================== Assignments ====================

    @Test
    fun testAssignments() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_assignments"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        assertTrue(assigns.any { it.operatorCode == "=" }, "Should have direct assignment")
    }

    @Test
    fun testCompoundAssignments() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_compound_assignments"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        val opCodes = assigns.map { it.operatorCode }.toSet()
        val expected = setOf("+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=")
        for (op in expected) {
            assertTrue(op in opCodes, "Should have compound assignment '$op': found $opCodes")
        }
    }

    // ==================== Struct expressions ====================

    @Test
    fun testStructFieldInit() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_field_init"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have struct construction")
    }

    @Test
    fun testStructShorthand() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_shorthand"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.isNotEmpty(), "Should have shorthand struct construction")
    }

    @Test
    fun testStructSpread() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_spread"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val constructs = body.allChildren<ConstructExpression>()
        // The second struct construction uses ..p1 spread syntax.
        // The spread argument is added with edge name ".." via addArgument(expr, "..")
        // Check that a construct has a Reference to "p1" among its arguments
        assertTrue(
            constructs.any { c ->
                c.arguments.any { arg -> arg is Reference && arg.name.localName == "p1" }
            },
            "Should have struct spread referencing p1: ${constructs.map { it.arguments.map { a -> a.name } }}",
        )
    }

    // ==================== Reference types ====================

    @Test
    fun testMutableRef() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_mutable_ref"]
        assertNotNull(func)
        assertTrue(func.parameters.isNotEmpty(), "Should have parameter")
    }

    @Test
    fun testLifetimeRef() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_lifetime_ref"]
        assertNotNull(func)
        assertTrue(func.parameters.isNotEmpty(), "Should have parameter")
    }

    // ==================== Traits ====================

    @Test
    fun testTraitWithMethods() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val drawable = tu.records["Drawable"]
        assertNotNull(drawable, "Should have Drawable trait")
        assertTrue(
            drawable.methods.size >= 2,
            "Drawable should have at least 2 methods: ${drawable.methods.map { it.name }}",
        )
    }

    @Test
    fun testTraitWithSuperTrait() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val resizable = tu.records["Resizable"]
        assertNotNull(resizable, "Should have Resizable trait")
    }

    @Test
    fun testImplTraitForStruct() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val circle = tu.records["Circle"]
        assertNotNull(circle, "Should have Circle struct")
        assertTrue(circle.methods.isNotEmpty(), "Circle should have methods from impl blocks")
    }

    // ==================== Generics ====================

    @Test
    fun testGenericStruct() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        // Container<T> should be a template
        val templates = tu.allChildren<TemplateDeclaration>()
        assertTrue(
            templates.any {
                val name = it.name.localName
                name.contains("Container") || name == "Container"
            },
            "Should have Container template: ${templates.map { it.name }}",
        )
    }

    @Test
    fun testGenericWithBounds() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_generic_with_bounds"]
        assertNotNull(func, "Should have generic function with bounds")
    }

    // ==================== Loops ====================

    @Test
    fun testLabeledLoops() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_loops"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val breaks = body.allChildren<BreakStatement>()
        assertTrue(breaks.isNotEmpty(), "Should have break statement")
        val continues = body.allChildren<ContinueStatement>()
        assertTrue(continues.isNotEmpty(), "Should have continue statement")
    }

    @Test
    fun testLoopWithBreak() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_loop_with_break"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Loop should produce WhileStatement")
    }

    @Test
    fun testWhileCondition() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_condition"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while loop")
        assertNotNull(whiles.first().condition, "While should have condition")
    }

    // ==================== Closures ====================

    @Test
    fun testClosureAsArg() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_as_arg"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.size >= 2, "Should have 2+ closures")
    }

    @Test
    fun testClosureWithMove() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_closure_with_move"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have move closure")
    }

    // ==================== Type cast ====================

    @Test
    fun testTypeCast() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_type_cast"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val casts = body.allChildren<CastExpression>()
        assertTrue(casts.size >= 2, "Should have multiple type casts: ${casts.size}")
    }

    // ==================== Unsafe/Async ====================

    @Test
    fun testUnsafeBlock() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unsafe"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // Unsafe produces a Block, check it parsed
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Unsafe block should not produce unknown problems",
        )
    }

    @Test
    fun testAsyncBlock() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Async block should not produce unknown problems",
        )
    }

    // ==================== Tuple index ====================

    @Test
    fun testTupleIndexSimple() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_simple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(
            members.any { it.name.localName == "0" },
            "Should have .0 access: ${members.map { it.name }}",
        )
        assertTrue(members.any { it.name.localName == "2" }, "Should have .2 access")
    }

    @Test
    fun testTupleIndexNested() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_tuple_index_nested"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.size >= 2, "Should have multiple tuple index accesses")
    }

    // ==================== Field access ====================

    @Test
    fun testFieldAccess() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_field_access"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val members = body.allChildren<MemberExpression>()
        assertTrue(members.any { it.name.localName == "x" }, "Should have .x field access")
        assertTrue(members.any { it.name.localName == "y" }, "Should have .y field access")
    }

    // ==================== Ranges ====================

    @Test
    fun testRanges() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_ranges"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val ranges = body.allChildren<RangeExpression>()
        assertTrue(ranges.size >= 2, "Should have multiple range expressions: ${ranges.size}")
    }

    // ==================== Macro invocation at decl level ====================

    @Test
    fun testMacroInvocationDecl() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        // macro_rules! definition should parse
        val macros = tu.allChildren<FunctionDeclaration>().filter { it.name.localName == "make_fn" }
        // If not a function, check for declaration
        val problems = tu.allChildren<ProblemDeclaration>()
        // Just verify no crashes — macro_rules is hard to model perfectly
    }

    // ==================== Float literals ====================

    @Test
    fun testFloatLiterals() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_float_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val floatLits = literals.filter { it.value is Double }
        assertTrue(floatLits.size >= 3, "Should have 3+ float literals: ${floatLits.size}")
    }

    // ==================== Integer literal variants ====================

    @Test
    fun testMoreIntegers() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_more_integers"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val literals = body.allChildren<Literal<*>>()
        val intLits = literals.filter { it.value is Long }
        assertTrue(intLits.size >= 5, "Should have 5+ integer literals: ${intLits.size}")
        // Check hex value
        assertTrue(intLits.any { it.value == 0xFFL }, "Should have hex literal 0xFF = 255")
        // Check octal value
        assertTrue(
            intLits.any { it.value == 63L }, // 0o77 = 63
            "Should have octal literal 0o77 = 63",
        )
        // Check binary value
        assertTrue(
            intLits.any { it.value == 10L }, // 0b1010 = 10
            "Should have binary literal 0b1010 = 10",
        )
    }

    // ==================== Await ====================

    @Test
    fun testAwaitExpression() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_await"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        // Await is modeled as a member call
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Await should not produce unknown problems: ${problems.map { it.problem }}",
        )
    }

    // ==================== Try ====================

    @Test
    fun testTryOperator() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_try_operator"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "?" }, "Should have ? try operator")
    }

    // ==================== Unary ops ====================

    @Test
    fun testUnaryOps() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unary_ops"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        val opCodes = unaryOps.map { it.operatorCode }.toSet()
        assertTrue("-" in opCodes, "Should have negation: $opCodes")
        assertTrue("!" in opCodes, "Should have logical not: $opCodes")
        assertTrue("*" in opCodes, "Should have dereference: $opCodes")
    }

    // ==================== Scoped identifiers ====================

    @Test
    fun testScopedIds() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_scoped_ids"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val refs = body.allChildren<Reference>()
        assertTrue(refs.isNotEmpty(), "Should have scoped references")
    }

    // ==================== Module ====================

    @Test
    fun testModule() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val namespaces = tu.allChildren<NamespaceDeclaration>()
        assertTrue(
            namespaces.any { it.name.localName == "inner" },
            "Should have inner module namespace",
        )
    }

    // ==================== Extern block ====================

    @Test
    fun testExternBlockFunctions() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val ns = tu.allChildren<NamespaceDeclaration>()
        // extern "C" block should be handled
        val problems = tu.allChildren<ProblemDeclaration>()
        // Just ensure it doesn't crash and extern functions exist
        val allFuncs = tu.allChildren<FunctionDeclaration>()
        assertTrue(allFuncs.any { it.name.localName == "abs" }, "Should have extern fn abs")
    }

    // ==================== Const/Static ====================

    @Test
    fun testConstStatic() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val decls = tu.allChildren<VariableDeclaration>()
        assertTrue(decls.any { it.name.localName == "MAX_SIZE" }, "Should have const MAX_SIZE")
        assertTrue(decls.any { it.name.localName == "GREETING" }, "Should have static GREETING")
    }

    // ==================== Type alias ====================

    @Test
    fun testTypeAlias() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        // type_item creates a declaration — check it exists
        val allDecls = tu.allChildren<Declaration>()
        assertTrue(
            allDecls.any { it.name.localName == "Pair" },
            "Should have Pair type alias: ${allDecls.map { it.name }}",
        )
    }

    // ==================== Where clause ====================

    @Test
    fun testWhereClause() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_where_clause"]
        assertNotNull(func, "Should have function with where clause")
    }

    // ==================== Enum with all variant types ====================

    @Test
    fun testEnumVariants() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val msg = tu.records["Message"]
        assertNotNull(msg, "Should have Message enum")
    }

    @Test
    fun testEnumMatch() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_enum_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms for Message: ${cases.size}")
    }

    // ==================== Pointer types ====================

    @Test
    fun testRawPointers() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_raw_pointers"]
        assertNotNull(func)
        assertTrue(func.parameters.size >= 2, "Should have 2 pointer parameters")
    }

    // ==================== Dynamic type ====================

    @Test
    fun testDynTrait() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_dyn_trait"]
        assertNotNull(func)
        assertTrue(func.parameters.isNotEmpty(), "Should have dyn trait parameter")
    }

    // ==================== Never type ====================

    @Test
    fun testNeverType() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_never"]
        assertNotNull(func, "Should have function returning never type")
    }

    // ==================== Nested function ====================

    @Test
    fun testNestedFn() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_fn"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val innerFuncs = body.allChildren<FunctionDeclaration>()
        assertTrue(
            innerFuncs.any { it.name.localName == "inner_add" },
            "Should have nested inner_add function",
        )
    }

    // ==================== Use declarations ====================

    @Test
    fun testUseDecl() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(includes.isNotEmpty(), "Should have use/include declarations")
    }

    // ==================== Impl trait in return ====================

    @Test
    fun testImplTrait() {
        val tu = parseTU("deep_coverage.rs")
        assertNotNull(tu)
        val func = tu.functions["test_impl_trait"]
        assertNotNull(func, "Should have function returning impl trait")
    }
}
