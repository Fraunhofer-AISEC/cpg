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

        // Selector should be a reference to the result expression
        assertIs<Reference>(selector)

        // The switch statement should have a block with cases
        val switchStatement = trySwitch.statement
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

        assertEquals(
            setOf(10),
            print1.memberExpressions
                .first()
                .collectAllPrevFullDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value }
                .toSet(),
        )

        assertEquals(
            setOf(20),
            print1.memberExpressions
                .last()
                .collectAllPrevFullDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value }
                .toSet(),
        )

        val print2 = printLines.getOrNull(1)

        assertEquals(
            setOf("Alice"),
            print2.memberExpressions
                .first()
                .collectAllPrevFullDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value.toString() }
                .toSet(),
        )

        assertEquals(
            setOf(30),
            print2.memberExpressions
                .last()
                .collectAllPrevFullDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value }
                .toSet(),
        )
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
        val floatLiterals = literals.filter { it.value is Float || it.value is Double }
        assertTrue(floatLiterals.isNotEmpty(), "Should have floating point literals")

        // Test floating point literal with `_` digit separators
        val underscoredFloatLit =
            floatLiterals.firstOrNull { (it.value as? Double) == 1_000_000_000.0 }
        assertNotNull(underscoredFloatLit, "Should parse '1_000_000_000.0' as 1e9")

        // Test boolean literal
        val boolLiterals = literals.filter { it.value is Boolean }
        assertTrue(boolLiterals.isNotEmpty(), "Should have boolean literals")

        val trueLit = boolLiterals.firstOrNull { it.value == true }
        assertNotNull(trueLit)

        val falseLit = boolLiterals.firstOrNull { it.value == false }
        assertNotNull(falseLit)

        val cstrings = stringLiterals.filter { (it.code ?: "").startsWith("c\"") }
        assertTrue(cstrings.isNotEmpty(), "Should have string literals")
        cstrings.forEach { it.value == (it.code ?: "").removePrefix("c\"").removeSuffix("\"") }

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

    @Test
    fun testEmptyExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("underscore.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val empties = main.allChildren<Empty>()
        assertTrue(empties.isNotEmpty(), "Expected at least one Empty node")
    }

    @Test
    fun testLetChainExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("let_chain.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val ifElse = main.allChildren<IfElse>().firstOrNull()
        assertNotNull(ifElse, "Expected an IfElse statement in main function")
        assertTrue(ifElse.condition.allChildren<DeclarationStatement>().size == 2)
    }

    @Test
    fun testIndexExpression() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("index_expression.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // ------------------------------------------------------------------
        // 1. array_index: show(numbers[2])
        // ------------------------------------------------------------------
        val arrayIndexFn = tu.functions["array_index"]
        assertNotNull(arrayIndexFn)

        val arraySubscriptions = arrayIndexFn.allChildren<Subscription>()
        assertEquals(1, arraySubscriptions.size)

        val arraySubscription = arraySubscriptions.first()
        val arrayRef = assertIs<Reference>(arraySubscription.arrayExpression)
        assertEquals("numbers", arrayRef.name.localName)

        val arrayIndexLit = assertIs<Literal<*>>(arraySubscription.subscriptExpression)
        assertEquals(2, arrayIndexLit.value)

        // A plain read access should keep the default access value ...
        assertEquals(AccessValues.READ, arraySubscription.access)
        // ... and the whole array should flow into the subscription.
        assertTrue(
            arraySubscription.prevDFGEdges.any { it.start == arraySubscription.arrayExpression },
            "Array expression should flow into the subscription for a read access",
        )

        // ------------------------------------------------------------------
        // 2. vector_index: show(numbers[1]) where numbers is a vec! macro
        // ------------------------------------------------------------------
        val vectorIndexFn = tu.functions["vector_index"]
        assertNotNull(vectorIndexFn)

        val vectorSubscriptions = vectorIndexFn.allChildren<Subscription>()
        assertEquals(1, vectorSubscriptions.size)

        val vectorSubscription = vectorSubscriptions.first()
        val vectorRef = assertIs<Reference>(vectorSubscription.arrayExpression)
        assertEquals("numbers", vectorRef.name.localName)

        val vectorIndexLit = assertIs<Literal<*>>(vectorSubscription.subscriptExpression)
        assertEquals(1, vectorIndexLit.value)

        // ------------------------------------------------------------------
        // 3. mutable_index: numbers[1] = 42
        // ------------------------------------------------------------------
        val mutableIndexFn = tu.functions["mutable_index"]
        assertNotNull(mutableIndexFn)

        val mutableSubscriptions = mutableIndexFn.allChildren<Subscription>()
        assertEquals(1, mutableSubscriptions.size)

        val mutableSubscription = mutableSubscriptions.first()
        assertIs<Reference>(mutableSubscription.arrayExpression)

        val mutableIndexLit = assertIs<Literal<*>>(mutableSubscription.subscriptExpression)
        assertEquals(1, mutableIndexLit.value)

        // Being the target of an assignment, the subscription itself is written to ...
        assertEquals(AccessValues.WRITE, mutableSubscription.access)
        // ... the assigned value (42) flows into the subscription ...
        assertTrue(
            mutableSubscription.prevDFGEdges.any {
                (it.start as? Literal<*>)?.value.toString() == "42"
            },
            "The assigned value should flow into the subscription",
        )
        // ... which in turn flows on into the array it indexes.
        assertTrue(
            mutableSubscription.nextDFGEdges.any { it.end == mutableSubscription.arrayExpression },
            "A write access should flow from the subscription back into the array expression",
        )

        // ------------------------------------------------------------------
        // 4. string_slice: let part = &s[1..4]
        // ------------------------------------------------------------------
        val stringSliceFn = tu.functions["string_slice"]
        assertNotNull(stringSliceFn)

        val part = stringSliceFn.variables["part"]
        assertNotNull(part)

        val partRefOp = assertIs<UnaryOperator>(part.assignments.first().value)
        assertEquals("&", partRefOp.operatorCode)

        val stringSliceSubscription = assertIs<Subscription>(partRefOp.input)
        val stringSliceArray = assertIs<Reference>(stringSliceSubscription.arrayExpression)
        assertEquals("s", stringSliceArray.name.localName)

        val stringSliceRange = assertIs<Range>(stringSliceSubscription.subscriptExpression)
        assertEquals("..", stringSliceRange.operatorCode)
        assertEquals(1, (stringSliceRange.floor as? Literal<*>)?.value)
        assertEquals(4, (stringSliceRange.ceiling as? Literal<*>)?.value)

        // ------------------------------------------------------------------
        // 5. slice_index: let middle = &data[1..4]
        // ------------------------------------------------------------------
        val sliceIndexFn = tu.functions["slice_index"]
        assertNotNull(sliceIndexFn)

        val middle = sliceIndexFn.variables["middle"]
        assertNotNull(middle)

        val middleRefOp = assertIs<UnaryOperator>(middle.assignments.first().value)
        assertEquals("&", middleRefOp.operatorCode)

        val sliceIndexSubscription = assertIs<Subscription>(middleRefOp.input)
        assertIs<Reference>(sliceIndexSubscription.arrayExpression)

        val sliceIndexRange = assertIs<Range>(sliceIndexSubscription.subscriptExpression)
        assertEquals("..", sliceIndexRange.operatorCode)
        assertEquals(1, (sliceIndexRange.floor as? Literal<*>)?.value)
        assertEquals(4, (sliceIndexRange.ceiling as? Literal<*>)?.value)

        // ------------------------------------------------------------------
        // 6. ranges: six differently-shaped ranges used as subscripts
        // ------------------------------------------------------------------
        val rangesFn = tu.functions["ranges"]
        assertNotNull(rangesFn)

        val rangeCalls = rangesFn.calls.filter { it.name.localName == "show_dbg" }
        assertEquals(6, rangeCalls.size)

        fun subscriptOf(call: Call): Subscription {
            val refOp = assertIs<UnaryOperator>(call.arguments.first())
            assertEquals("&", refOp.operatorCode)
            return assertIs<Subscription>(refOp.input)
        }

        // v[..] -> RangeFull: neither bound is set
        val rangeFull = assertIs<Range>(subscriptOf(rangeCalls[0]).subscriptExpression)
        assertEquals("..", rangeFull.operatorCode)
        assertNull(rangeFull.floor)
        assertNull(rangeFull.ceiling)

        // v[..3] -> RangeTo: exactly one bound is set, holding the value 3
        val rangeTo = assertIs<Range>(subscriptOf(rangeCalls[1]).subscriptExpression)
        assertEquals("..", rangeTo.operatorCode)
        val rangeToBound = rangeTo.floor ?: rangeTo.ceiling
        assertNotNull(rangeToBound, "RangeTo should carry its single bound")
        assertNull(if (rangeTo.floor != null) rangeTo.ceiling else rangeTo.floor)
        assertEquals(3, (rangeToBound as? Literal<*>)?.value)

        // v[2..] -> RangeFrom: exactly one bound is set, holding the value 2
        val rangeFrom = assertIs<Range>(subscriptOf(rangeCalls[2]).subscriptExpression)
        assertEquals("..", rangeFrom.operatorCode)
        val rangeFromBound = rangeFrom.floor ?: rangeFrom.ceiling
        assertNotNull(rangeFromBound, "RangeFrom should carry its single bound")
        assertNull(if (rangeFrom.floor != null) rangeFrom.ceiling else rangeFrom.floor)
        assertEquals(2, (rangeFromBound as? Literal<*>)?.value)

        // v[1..4] -> Range: both bounds are set
        val rangeBoth = assertIs<Range>(subscriptOf(rangeCalls[3]).subscriptExpression)
        assertEquals("..", rangeBoth.operatorCode)
        assertEquals(1, (rangeBoth.floor as? Literal<*>)?.value)
        assertEquals(4, (rangeBoth.ceiling as? Literal<*>)?.value)

        // v[..=2] -> RangeToInclusive: exactly one bound is set, holding the value 2
        val rangeToInclusive = assertIs<Range>(subscriptOf(rangeCalls[4]).subscriptExpression)
        assertEquals("..=", rangeToInclusive.operatorCode)
        val rangeToInclusiveBound = rangeToInclusive.floor ?: rangeToInclusive.ceiling
        assertNotNull(rangeToInclusiveBound, "RangeToInclusive should carry its single bound")
        assertNull(
            if (rangeToInclusive.floor != null) rangeToInclusive.ceiling else rangeToInclusive.floor
        )
        assertEquals(2, (rangeToInclusiveBound as? Literal<*>)?.value)

        // v[1..=3] -> RangeInclusive: both bounds are set
        val rangeInclusiveBoth = assertIs<Range>(subscriptOf(rangeCalls[5]).subscriptExpression)
        assertEquals("..=", rangeInclusiveBoth.operatorCode)
        assertEquals(1, (rangeInclusiveBoth.floor as? Literal<*>)?.value)
        assertEquals(3, (rangeInclusiveBoth.ceiling as? Literal<*>)?.value)
    }
}
