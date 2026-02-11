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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustCoverageGapsTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testTupleIndexExpression() {
        val tu = parseTU("coverage_gaps.rs")
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
        val tu = parseTU("coverage_gaps.rs")
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
    fun testContinueWithLabel() {
        val tu = parseTU("coverage_gaps.rs")
        assertNotNull(tu)
        val func = tu.functions["test_continue_with_label"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val continueStmts = body.allChildren<ContinueStatement>()
        assertTrue(continueStmts.isNotEmpty(), "Should have continue statement")
        assertEquals("outer", continueStmts.first().label, "Continue should have label 'outer'")
    }

    @Test
    fun testContinueSimple() {
        val tu = parseTU("coverage_gaps.rs")
        assertNotNull(tu)
        val func = tu.functions["test_continue_simple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val continueStmts = body.allChildren<ContinueStatement>()
        assertTrue(continueStmts.isNotEmpty(), "Should have continue statement")
    }

    @Test
    fun testGenericFunctionReference() {
        val tu = parseTU("coverage_gaps.rs")
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
    fun testBreakWithValue() {
        val tu = parseTU("coverage_gaps.rs")
        assertNotNull(tu)
        val func = tu.functions["test_break_with_value"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // loop { break 42; } is used as expression — verify it parses without unknown problems
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Break with value should not produce unknown problems: ${problems.map { it.problem }}",
        )
    }

    @Test
    fun testMethodCallOnField() {
        val tu = parseTU("coverage_gaps.rs")
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
        val tu = parseTU("coverage_gaps.rs")
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
        val tu = parseTU("coverage_gaps.rs")
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
        val tu = parseTU("coverage_gaps.rs")
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
    fun testShorthandStructInit() {
        val tu = parseTU("coverage_gaps.rs")
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
    fun testArrayIndex() {
        val tu = parseTU("coverage_gaps.rs")
        assertNotNull(tu)
        val func = tu.functions["test_array_index"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val subscripts = body.allChildren<SubscriptExpression>()
        assertTrue(subscripts.size >= 2, "Should have array subscript expressions")
    }

    @Test
    fun testIfElseChain() {
        val tu = parseTU("coverage_gaps.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_else_chain"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val ifStmts = body.allChildren<IfStatement>()
        assertTrue(ifStmts.isNotEmpty(), "Should have if statements")
        // The outer if should have an else branch
        val outerIf = ifStmts.first()
        assertNotNull(outerIf.elseStatement, "First if should have else branch")
    }

    @Test
    fun testWhileLet() {
        val tu = parseTU("coverage_gaps.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val whileStmts = body.allChildren<WhileStatement>()
        assertTrue(whileStmts.isNotEmpty(), "Should have while-let loop")
    }
}
