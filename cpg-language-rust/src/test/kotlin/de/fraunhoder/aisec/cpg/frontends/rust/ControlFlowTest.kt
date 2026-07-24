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
import de.fraunhofer.aisec.cpg.graph.collectAllPrevDFGPaths
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Break
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Case
import de.fraunhofer.aisec.cpg.graph.expressions.Deconstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Empty
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.ObjectDeconstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Range
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.forEachLoops
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.switches
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.graph.whileLoops
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNull

class ControlFlowTest {

    @Test
    fun testWhile() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["while_examples"]
        assertNotNull(function)

        val whiles = function.whileLoops

        assertEquals(whiles.size, 2)

        val while1 = whiles[0]

        assertInstanceOf<BinaryOperator>(while1.condition)

        var body = while1.statement

        assertNotNull(body)

        assertInstanceOf<Block>(body)

        assertEquals(body.statements.size, 2)

        val while2 = whiles[1]

        assertInstanceOf<BinaryOperator>(while2.condition)

        body = while2.statement

        assertNotNull(body)

        assertInstanceOf<Block>(body)

        assertEquals(body.statements.size, 3)
    }

    @Test
    fun testWhileLet() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["while_let_examples"]
        assertNotNull(function)

        val whiles = function.whileLoops

        // 2 while let statements in while_let_examples
        assertEquals(whiles.size, 2)

        val whileLet1 = whiles[0]

        assertInstanceOf<Assign>(whileLet1.condition)

        var body = whileLet1.statement
        assertNotNull(body)
        assertInstanceOf<Block>(body)
        assertEquals(body.statements.size, 1)

        var variable = whileLet1.variables.firstOrNull()
        assertNotNull(variable)
        assertEquals(variable.name.toString(), "v")

        val whileLet2 = whiles[1]
        assertInstanceOf<Assign>(whileLet2.condition)
        body = whileLet2.statement
        assertNotNull(body)
        assertInstanceOf<Block>(body)
        assertEquals(body.statements.size, 1)
        assertNotNull(whileLet2.variables["item"])
    }

    @Test
    fun testLoop() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["loop_examples"]
        assertNotNull(function)

        val loops = function.whileLoops

        // 2 loop statements (loop is translated to while with true condition) in loop_examples
        assertEquals(loops.size, 2)

        val loop1 = loops[0]
        assertInstanceOf<Literal<Boolean>>(loop1.condition)
        var body = loop1.statement
        assertNotNull(body)
        assertInstanceOf<Block>(body)
        assertEquals(body.statements.size, 3)

        val loop2 = loops[1]
        body = loop2.statement
        assertNotNull(body)
        assertInstanceOf<Block>(body)
        assertEquals(body.statements.size, 3)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["if_examples"]
        assertNotNull(function)

        val ifs = SubgraphWalker.flattenAST(function).filterIsInstance<IfElse>()

        // 5 if statements in if_examples: if without else, if with else, if/else if/else, nested
        // if, if as expression
        assertEquals(ifs.size, 7)

        val if1 = ifs[0]
        assertInstanceOf<BinaryOperator>(if1.condition)
        var then = if1.thenStatement
        assertNotNull(then)
        assertInstanceOf<Block>(then)
        assertEquals(then.statements.size, 1)

        var elseStmt = if1.elseStatement
        assertNull(elseStmt)

        val if2 = ifs[1]

        assertInstanceOf<BinaryOperator>(if2.condition)
        then = if2.thenStatement
        assertNotNull(then)
        assertInstanceOf<Block>(then)
        assertEquals(then.statements.size, 1)

        elseStmt = if2.elseStatement
        assertNotNull(elseStmt)
        assertInstanceOf<Block>(elseStmt)
        assertEquals(elseStmt.statements.size, 1)

        val if3 = ifs[2]

        assertInstanceOf<BinaryOperator>(if3.condition)
        then = if3.thenStatement
        assertNotNull(then)
        assertInstanceOf<Block>(then)
        assertEquals(then.statements.size, 1)

        elseStmt = if3.elseStatement
        assertNotNull(elseStmt)
        assertEquals(elseStmt, ifs[3])

        val if5 = ifs[4]

        assertInstanceOf<BinaryOperator>(if5.condition)
        then = if5.thenStatement
        assertNotNull(then)
        assertInstanceOf<Block>(then)
        assertEquals(then.statements.size, 2)

        assertTrue(then.statements[1] is IfElse)

        elseStmt = if5.elseStatement
        assertNotNull(elseStmt)
        assertInstanceOf<Block>(elseStmt)
        assertEquals(elseStmt.statements.size, 1)

        val if7 = ifs[6]

        assertInstanceOf<BinaryOperator>(if7.condition)
        then = if7.thenStatement
        assertNotNull(then)
        assertInstanceOf<Block>(then)
        assertEquals(then.statements.size, 2)

        elseStmt = if7.elseStatement
        assertNotNull(elseStmt)
        assertInstanceOf<Block>(elseStmt)
        assertEquals(elseStmt.statements.size, 2)

        assertTrue(if7.prevDFG.flatMap { it.prevDFG }.contains(then.statements.last()))
        assertTrue(if7.prevDFG.flatMap { it.prevDFG }.contains(elseStmt.statements.last()))
    }

    @Test
    fun testIfLet() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["if_let_examples"]
        assertNotNull(function)

        val ifLets = SubgraphWalker.flattenAST(function).filterIsInstance<IfElse>()

        // 3 if let statements in if_let_examples: if let without else, if let with else, chained if
        // let
        assertEquals(ifLets.size, 1)

        val ifLet = ifLets[0]
        assertInstanceOf<Assign>(ifLet.condition)
        var then = ifLet.thenStatement
        assertNotNull(then)
        assertInstanceOf<Block>(then)
        assertEquals(then.statements.size, 1)
    }

    @Test
    @Ignore
    fun testLetElse() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["let_else_example"]
        assertNotNull(function)

        val switches = function.switches

        // 1 let else statement in let_else_example
        assertEquals(switches.size, 1)

        val letElse1 = function.switches[0]
        assertNotNull(letElse1)
        assertIs<Reference>(letElse1.selector)
        var ref = letElse1.selector as Reference
        assertEquals(ref.name.toString(), "opt")

        val block = letElse1.statement as Block
        assertNotNull(block)

        val statements = block.statements

        assertEquals(4, statements.size)

        val case = statements.first() as Case
        assertInstanceOf<Deconstruction>(case.caseExpression)

        assertInstanceOf<Break>(statements[1])

        val breakStmt = statements[1] as Break
        assertNotNull(breakStmt.expr)

        breakStmt.expr.refs.forEach { assertTrue(case.variables.contains(it.refersTo)) }

        val oDec = case.caseExpression

        assertEquals(oDec.variables.size, 1)
        assertEquals(oDec.variables.first().name.toString(), "value")

        // Collect all literals reachable from the reference after the let else
        val reference =
            function.body
                ?.astChildren
                ?.filterIsInstance<Call>()
                ?.firstOrNull()
                ?.astChildren
                ?.filterIsInstance<Reference>()
                ?.firstOrNull()
        assertNotNull(reference)
        val reachableDFG =
            reference
                .collectAllPrevDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Parameter>()
                .toSet()
        assertNotEquals(0, reachableDFG.size)
    }

    @Test
    fun testMatch() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["match_examples"]
        assertNotNull(function)

        val switches = function.switches

        // 4 match statements in match_examples: simple match, match with ranges, match with guard,
        // match on enum-like
        assertEquals(switches.size, 4)

        val match1 = function.switches[0]
        assertNotNull(match1)
        assertIs<Reference>(match1.selector)
        var ref = match1.selector as Reference
        assertEquals(ref.name.toString(), "value")

        var children = match1.statement?.astChildren ?: emptyList()
        var cases = children.filterIsInstance<Case>()
        var others = children.filter { !cases.contains(it) }

        others.forEach {
            assertInstanceOf<Break>(it)
            assertInstanceOf<Call>(it.astChildren.first())
        }

        assertInstanceOf<Literal<Int>>(cases[0].caseExpression)
        var literal = cases[0].caseExpression as Literal<Int>
        assertEquals("0", literal.value.toString())

        assertInstanceOf<Literal<Int>>(cases[1].caseExpression)
        literal = cases[1].caseExpression as Literal<Int>
        assertEquals("1", literal.value.toString())

        assertInstanceOf<Empty>(cases[2].caseExpression)

        val match2 = function.switches[1]
        assertNotNull(match2)
        assertIs<Reference>(match2.selector)
        ref = match2.selector as Reference
        assertEquals(ref.name.toString(), "value")

        children = match2.statement?.astChildren ?: emptyList()
        cases = children.filterIsInstance<Case>()
        others = children.filter { !cases.contains(it) }

        others.forEach {
            assertInstanceOf<Break>(it)
            assertInstanceOf<Call>(it.astChildren.first())
        }

        assertInstanceOf<Range>(cases[0].caseExpression)
        var range = cases[0].caseExpression as Range
        assertEquals("0", (range.floor as Literal<*>).value.toString())
        assertEquals("10", (range.ceiling as Literal<*>).value.toString())

        assertInstanceOf<Range>(cases[1].caseExpression)
        range = cases[1].caseExpression as Range
        assertEquals("11", (range.floor as Literal<*>).value.toString())
        assertEquals("100", (range.ceiling as Literal<*>).value.toString())

        assertInstanceOf<Empty>(cases[2].caseExpression)

        val match3 = function.switches[2]

        assertNotNull(match3)
        assertIs<Reference>(match3.selector)
        ref = match3.selector as Reference
        assertEquals(ref.name.toString(), "value")

        children = match3.statement?.astChildren ?: emptyList()
        cases = children.filterIsInstance<Case>()
        others = children.filter { !cases.contains(it) }

        others.dropLast(1).forEach {
            assertInstanceOf<IfElse>(it)
            assertInstanceOf<Break>(it.thenStatement?.astChildren?.first())
        }

        val match4 = function.switches[3]

        assertNotNull(match4)
        assertIs<Reference>(match4.selector)
        ref = match4.selector as Reference
        assertEquals(ref.name.toString(), "opt")

        children = match4.statement?.astChildren ?: emptyList()
        cases = children.filterIsInstance<Case>()

        assertInstanceOf<ObjectDeconstruction>(cases[0].caseExpression)
        // assertInstanceOf<ObjectDeconstruction>(cases[0].caseExpression)

    }

    @Test
    fun testFor() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("cfstructures.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val function = tu.functions["for_examples"]
        assertNotNull(function)

        val forLoops = function.forEachLoops

        // 3 for loops in for_examples: for over range, for over iterator, for with enumerate
        assertEquals(forLoops.size, 3)

        val for1 = forLoops[0]

        assertEquals("i", for1.variable.variables.first().name.toString())

        assertInstanceOf<Range>(for1.iterable)

        assertInstanceOf<Block>(for1.statement)

        assertInstanceOf<Call>(for1.statement?.astChildren?.first())

        val for2 = forLoops[1]
        assertEquals("item", for2.variable.variables.first().name.toString())
        assertInstanceOf<Reference>(for2.iterable)
        assertInstanceOf<Block>(for2.statement)
        assertInstanceOf<Call>(for2.statement?.astChildren?.first())

        val for3 = forLoops[2]

        assertInstanceOf<ObjectDeconstruction>(for3.variable)

        assertEquals("index", for3.variable.variables.first().name.toString())
        assertEquals("value", for3.variable.variables.last().name.toString())

        assertInstanceOf<MemberCall>(for3.iterable)

        assertInstanceOf<Block>(for3.statement)

        assertInstanceOf<Call>(for3.statement?.astChildren?.first())
    }
}
