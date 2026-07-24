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
package de.fraunhoder.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.frontends.rust.RustLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class ExpressionsTest {

    @Test
    fun testTryExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("try.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Test: AST structure - try expression should be modeled as a Switch statement
        val main = tu.functions["main"]
        assertNotNull(main)

        // Look for Switch statements (try expressions are modeled as switch)
        val switches = main.allChildren<Switch>()
        assertTrue(
            switches.isNotEmpty(),
            "Expected at least one Switch statement from try expression",
        )

        val trySwitch = switches.first()
        assertNotNull(trySwitch)

        // The switch should have a selector (the result being checked)
        val selector = trySwitch.selector
        assertNotNull(selector, "Try expression switch should have a selector")

        // Selector should be a reference to the result expression
        assertIs<Reference>(selector)

        // The switch statement should have a block with cases
        val switchStatement = trySwitch.statement
        assertNotNull(switchStatement, "Try expression switch should have a statement")
        assertIs<Block>(switchStatement)

        val block = switchStatement
        assertTrue(block.statements.isNotEmpty(), "Expected cases in try expression switch")

        // Should have exactly 2 cases: Ok and Err
        val cases = block.statements.filterIsInstance<Case>()
        assertTrue(cases.size >= 2, "Try expression should have at least Ok and Err cases")

        // Verify first case (Ok case)
        val okCase = cases.getOrNull(0)
        assertNotNull(okCase, "Try expression should have Ok case")
        assertNotNull(okCase.caseExpression, "Ok case should have a case expression")

        // Verify second case (Err case)
        val errCase = cases.getOrNull(1)
        assertNotNull(errCase, "Try expression should have Err case")
        assertNotNull(errCase.caseExpression, "Err case should have a case expression")

        // Control flow: Try expressions should have proper EOG edges
        val selectorEOGSuccessors = selector.nextEOGEdges
        assertTrue(selectorEOGSuccessors.isNotEmpty(), "Selector should have EOG edges to cases")

        // Data flow: The selector should have incoming DFG edges from the result
        assertTrue(selector.prevDFGEdges.isNotEmpty(), "Selector should have incoming DFG edges")

        // Verify the switch is marked as used as expression
        assertTrue(
            trySwitch.usedAsExpression,
            "Try expression switch should be marked as expression",
        )

        // Verify block is marked as used as expression
        assertTrue(block.usedAsExpression, "Try expression block should be marked as expression")

        val z = tu.variables["z"]
        assertNotNull(z)
        val z_values =
            z.collectAllPrevDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value.toString() }
                .toSet()
        assertEquals(1, z_values.size)
        assertTrue { z_values.contains("5") }
    }

    @Test
    fun testRecordExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("record_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Test: AST structure - record expression should be a Construction
        val main = tu.functions["main"]
        assertNotNull(main)

        // Find construction nodes (record expressions)
        val constructions = main.allChildren<Construction>()
        assertTrue(
            constructions.isNotEmpty(),
            "Expected at least one Construction from record expression",
        )

        val pointConstruction = constructions.first()
        assertNotNull(pointConstruction)
        assertEquals("Point", pointConstruction.type.name.localName)

        // Check that the construction has arguments (field assignments)
        assertTrue(
            pointConstruction.arguments.isNotEmpty(),
            "Construction should have field assignments",
        )

        // Verify field assignment structure
        val firstArg = pointConstruction.arguments.first()
        // Field assignments should be represented as assignments
        val fieldAssigns = pointConstruction.arguments.filterIsInstance<Assign>()
        assertTrue(fieldAssigns.isNotEmpty(), "Construction should contain field assignments")

        // Test struct update syntax (spread operator)
        val spreadConstructions = constructions.filter { it.arguments.size > 1 }
        assertTrue(
            spreadConstructions.isNotEmpty(),
            "Should find construction with spread operator",
        )

        // Data flow: Variables used in construction should have proper DFG edges
        val p1Var = main.variables["p1"]
        assertNotNull(p1Var)
        assertTrue(p1Var.assignments.isNotEmpty(), "p1 should have assignments")

        // Control flow: Construction should properly be part of control flow
        val p1Assignment = p1Var.assignments.first()
        assertTrue(p1Assignment.value is Construction, "Assignment should be a Construction")

        // Test construction with multiple fields
        val personVariable = main.variables["person"]
        assertNotNull(personVariable)

        val printLines = main.calls.filter { it.name.toString().startsWith("print_proxy") }
        assertTrue(2 == printLines.size)

        val print1 = printLines.getOrNull(0)

        print1.memberExpressions
            .first()
            .collectAllPrevDFGPaths()
            .flatMap { it.nodes }
            .filterIsInstance<Literal<*>>()
            .map { it.value.toString() }
            .toSet()
            .equals(setOf(10))

        print1.memberExpressions
            .last()
            .collectAllPrevDFGPaths()
            .flatMap { it.nodes }
            .filterIsInstance<Literal<*>>()
            .map { it.value.toString() }
            .toSet()
            .equals(setOf(20))

        val print2 = printLines.getOrNull(1)

        print2.memberExpressions
            .first()
            .collectAllPrevDFGPaths()
            .flatMap { it.nodes }
            .filterIsInstance<Literal<*>>()
            .map { it.value.toString() }
            .toSet()
            .equals(setOf("Alice"))

        print2.memberExpressions
            .last()
            .collectAllPrevDFGPaths()
            .flatMap { it.nodes }
            .filterIsInstance<Literal<*>>()
            .map { it.value.toString() }
            .toSet()
            .equals(setOf(30))
    }

    @Test
    fun testClosureExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("closure_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Test: AST structure - closure should be a Lambda node
        val main = tu.functions["main"]
        assertNotNull(main)

        val lambdas = main.allChildren<Lambda>()
        assertTrue(lambdas.isNotEmpty(), "Expected at least one Lambda from closure expression")

        val firstLambda = lambdas.first()
        assertNotNull(firstLambda)

        // Lambda should have an enclosed function
        assertNotNull(firstLambda.function, "Lambda should contain an enclosed function")

        val enclosedFunction = firstLambda.function
        assertNotNull(enclosedFunction)

        // Test: Closure with parameters
        // The first closure: |x: i32| x + 1
        assertTrue(enclosedFunction.parameters.isNotEmpty(), "Closure should have parameters")

        val firstParam = enclosedFunction.parameters.first()
        assertNotNull(firstParam)
        assertEquals("x", firstParam.name.localName)

        // Enclosed function should have a body
        assertNotNull(enclosedFunction.body, "Closure should have a function body")

        // Control flow: Closures should have proper EOG
        val bodyEOGSuccessors = enclosedFunction.body?.nextEOGEdges
        assertNotNull(bodyEOGSuccessors)

        // Data flow: Parameter should have DFG edges to where it's used
        val paramUsages =
            enclosedFunction.body?.allChildren<Reference>()?.filter { it.refersTo == firstParam }
                ?: listOf()
        assertTrue(paramUsages.isNotEmpty(), "Parameter should be referenced in body")

        // Test: Closure with multiple parameters
        val multiParamClosures = lambdas.filter { it.function?.parameters?.size ?: 0 > 1 }
        assertTrue(multiParamClosures.isNotEmpty(), "Should have closure with multiple parameters")

        // Test: Closure with block body
        val blockBodyClosures = lambdas.filter { it.function?.body is Block }
        assertTrue(blockBodyClosures.isNotEmpty(), "Should have closure with block body")
    }

    @Test
    fun testLiterals() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("literals.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Test: AST structure - literals should be Literal nodes
        val main = tu.functions["main"]
        assertNotNull(main)

        val literals = main.allChildren<Literal<*>>()
        assertTrue(literals.isNotEmpty(), "Expected multiple Literal nodes")

        // Test character literal
        val charLiterals = literals.filter { it.value is Char }
        assertTrue(charLiterals.isNotEmpty(), "Should have character literals")

        val firstCharLit = charLiterals.first()
        assertEquals('a', firstCharLit.value)

        // Test string literal
        val stringLiterals = literals.filter { it.value is String }
        assertTrue(stringLiterals.isNotEmpty(), "Should have string literals")

        val helloLit = stringLiterals.firstOrNull { (it.value as String).contains("Hello") }
        assertNotNull(helloLit)
        assertEquals("Hello, World!", helloLit.value)

        // Test integer literal
        val intLiterals = literals.filter { it.value is Int }
        assertTrue(intLiterals.isNotEmpty(), "Should have integer literals")

        val intLit = intLiterals.firstOrNull { (it.value as Int) == 42 }
        assertNotNull(intLit)

        // Test floating point literal
        val floatLiterals = literals.filter { it.value is Float }
        assertTrue(floatLiterals.isNotEmpty(), "Should have floating point literals")

        // Test boolean literal
        val boolLiterals = literals.filter { it.value is Boolean }
        assertTrue(boolLiterals.isNotEmpty(), "Should have boolean literals")

        val trueLit = boolLiterals.firstOrNull { it.value == true }
        assertNotNull(trueLit)

        // Test: Variables assigned with literals should have proper types
        val ch1 = main.variables["ch1"]
        assertNotNull(ch1)
        assertTrue(ch1.assignments.isNotEmpty(), "ch1 should have an assignment")

        val ch1Lit = ch1.assignments.first().value
        assertIs<Literal<*>>(ch1Lit)

        val literal_in_expression = tu.functions["literal_in_expression"]
        assertNotNull(literal_in_expression)

        // Test: Literals in expressions should properly type
        val result = literal_in_expression.variables["result"]
        assertNotNull(result)
        assertTrue(result.assignments.isNotEmpty(), "result should have an assignment")
        assertTrue { result.assignments.flatMap { it.value.literals }.size == 3 }

        // Data flow: Literals should have no incoming DFG edges (they are sources)
        val intLit42 = intLiterals.firstOrNull { (it.value as Int) == 42 }
        assertNotNull(intLit42)

        assertTrue(
            tu.literals.all { it.prevDFG.isEmpty() },
            "Literal should not have incoming DFG edges",
        )
    }

    @Test
    fun testCastExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cast_expressions.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Test: AST structure - cast should be a Cast node
        val main = tu.functions["main"]
        assertNotNull(main)

        val casts = main.allChildren<Cast>()
        assertTrue(casts.isNotEmpty(), "Expected at least one Cast node")

        val firstCast = casts.first()
        assertNotNull(firstCast)

        // Cast should have an expression (the input)
        assertNotNull(firstCast.expression, "Cast should have an input expression")

        // Cast should have a castType
        assertNotNull(firstCast.castType, "Cast should have a castType")

        // Test: Cast from u32 to i64
        val xVar = main.variables["x"]
        assertNotNull(xVar)
        assertTrue(xVar.assignments.isNotEmpty(), "x should have an assignment")

        val yVar = main.variables["y"]
        assertNotNull(yVar)
        assertTrue(yVar.assignments.isNotEmpty(), "y should have an assignment")

        // y should be assigned from a cast
        val yAssignment = yVar.assignments.first().value
        val yCast = assertIs<Cast>(yAssignment)
        // The input should be a reference to x
        val yCastInput = yCast.expression
        assertIs<Reference>(yCastInput)

        // Control flow: Cast should be part of control flow
        val castEOG = yCast.nextEOGEdges
        assertNotNull(castEOG)

        // Data flow: Cast should have DFG edge from source expression to itself
        assertTrue(
            yCast.prevDFGEdges.isNotEmpty(),
            "Cast should have incoming DFG edges from input",
        )

        // Test: Cast in expressions
        val result = main.variables["result"]
        assertNotNull(result)

        // Test: Multiple casts
        val multiCasts = casts.filter { it.expression is Cast }
        assertTrue(multiCasts.isNotEmpty(), "Should have nested casts")

        // Test: Cast in function
        val castFunction = tu.functions["cast_in_function"]
        assertNotNull(castFunction)

        val functionCasts = castFunction.allChildren<Cast>()
        assertTrue(functionCasts.isNotEmpty(), "Function should have cast expression")

        // Test: Cast in conditional
        val conditionFunction = tu.functions["cast_in_condition"]
        assertNotNull(conditionFunction)

        val conditionCasts = conditionFunction.allChildren<Cast>()
        assertTrue(conditionCasts.isNotEmpty(), "Conditional should have cast expression")
    }
}
