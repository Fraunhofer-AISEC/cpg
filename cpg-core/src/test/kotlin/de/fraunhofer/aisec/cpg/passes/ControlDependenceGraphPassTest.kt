/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithShortCircuit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.ForEach
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertInstanceOf

class ControlDependenceGraphPassTest {
    @Test
    fun testIfStatements() {
        val result = getIfTest()
        assertNotNull(result)
        val main = result.functions["main"]
        assertNotNull(main)
        val if0 = (main.body as Block).statements[1]
        assertNotNull(if0)
        assertInstanceOf<IfElse>(if0)
        assertEquals(1, if0.prevCDG.size)
        assertTrue(main in if0.prevCDG)

        val assignment1 =
            result.assignments.firstOrNull { 1 == (it.value as? Literal<*>)?.value }?.start
        assertNotNull(assignment1)
        assertEquals(1, assignment1.prevCDG.size)
        val branchingNodes = listOfNotNull(if0.condition, if0.conditionDeclaration)
        branchingNodes.forEach { assertTrue(it in assignment1.prevCDG) }

        val print0 =
            result.calls("printf").first {
                "0\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(print0)
        assertEquals(1, print0.prevCDG.size)
        branchingNodes.forEach { assertTrue(it in print0.prevCDG) }

        val print1 =
            result.calls("printf").first {
                "1\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(print1)
        assertEquals(1, print1.prevCDG.size)
        assertTrue(main in print1.prevCDG)

        val print2 =
            result.calls("printf").first {
                "2\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(print2)
        assertEquals(1, print2.prevCDG.size)
        assertTrue(main in print2.prevCDG)
    }

    @Test
    fun testForEachLoop() {
        val result = getForEachTest()
        assertNotNull(result)
        val main = result.functions["main"]
        assertNotNull(main)
        val forEachStmt = (main.body as Block).statements[1]
        assertNotNull(forEachStmt)
        assertInstanceOf<ForEach>(forEachStmt)

        val variableDecl = forEachStmt.variable
        assertNotNull(variableDecl)

        assertEquals(1, forEachStmt.prevCDG.size)
        assertTrue(main in forEachStmt.prevCDG)

        val printInLoop =
            result.calls("printf").first {
                "loop: \${}\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(printInLoop)
        assertEquals(1, printInLoop.prevCDG.size)
        assertTrue(variableDecl in printInLoop.prevCDG)

        val printAfterLoop =
            result.calls("printf").first {
                "1\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(printAfterLoop)
        assertEquals(1, printAfterLoop.prevCDG.size)
        assertTrue(main in printAfterLoop.prevCDG)
        assertFalse(variableDecl in printAfterLoop.prevCDG)
    }

    @Test
    fun testTimeoutEffective() {
        val result = getTimeoutTest()
        assertTrue { result.allChildren<Node>().flatMap { it.nextCDG }.isEmpty() }
    }

    @Test
    fun testShortCircuit() {
        val result = getShortCircuitTest()
        assertNotNull(result)

        val barCall = result.calls("bar").singleOrNull()
        assertNotNull(barCall)
        val fooCall = result.calls("foo").singleOrNull()
        assertNotNull(fooCall)
        val bazCall = result.calls("baz").singleOrNull()
        assertNotNull(bazCall)
        val quuxCall = result.calls("quux").singleOrNull()
        assertNotNull(quuxCall)
        assertTrue(
            barCall.prevCDG.contains(fooCall),
            "Expected 'bar()' to be control dependent on 'foo()'",
        ) // TODO: Once we update the ShortCircuitOperator to a better EOG description, this should
        // test against the operator instead of foo().

        assertTrue(
            quuxCall.prevCDG.contains(bazCall),
            "Expected 'quux()' to be control dependent on 'baz()'",
        ) // TODO: Once we update the ShortCircuitOperator to a better EOG description, this should
        // test against the operator instead of baz().
    }

    companion object {

        /**
         * Test language with [HasShortCircuitOperators] to test short-circuit behavior in CDG.
         *
         * ```c
         * int main() {
         *   foo() && bar();
         *   baz() || quux();
         *   return 1;
         * }
         * ```
         */
        fun getShortCircuitTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage<TestLanguageWithShortCircuit>()
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .build()
                )
                .build {
                    val tu = newTranslationUnit("if.cpp")
                    scopeManager.resetToGlobal(tu)

                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                // Fluent's logicAnd/logicOr self-attach their operands (foo()/
                                // bar()) to the block as separate statements *and* the resulting
                                // operator itself -- there's no removal trick for these two
                                // operators (unlike +, -, *, etc.), so the block ends up with
                                // [foo(), bar(), foo() && bar()], not just the operator.
                                // Faithfully reproduced (confirmed via the original test).
                                val fooCall = newCall(newReference("foo"))
                                block += fooCall
                                val barCall = newCall(newReference("bar"))
                                block += barCall
                                block +=
                                    newBinaryOperator("&&").also {
                                        it.lhs = fooCall
                                        it.rhs = barCall
                                    }

                                val bazCall = newCall(newReference("baz"))
                                block += bazCall
                                val quuxCall = newCall(newReference("quux"))
                                block += quuxCall
                                block +=
                                    newBinaryOperator("||").also {
                                        it.lhs = bazCall
                                        it.rhs = quuxCall
                                    }

                                val returnStmt = newReturn()
                                returnStmt.returnValue = newLiteral(1, objectType("int"))
                                block += returnStmt
                            }
                    }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
                }

        fun getIfTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage<TestLanguageWithColon>()
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .build()
                )
                .build { buildIfTestBody("if.cpp") }

        private fun LanguageFrontend<*, *>.buildIfTestBody(tuName: String): TranslationResult {
            val tu = newTranslationUnit(tuName)
            scopeManager.resetToGlobal(tu)

            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("int"))
                func.type = computeType(func)

                func.body =
                    newBlock(enterScope = true) { block ->
                        val declStmt = newDeclarationStatement()
                        val i =
                            newVariable("i", objectType("int")).also {
                                it.initializer = newLiteral(0, objectType("int"))
                            }
                        declStmt.declarations += i
                        scopeManager.addDeclaration(i)
                        block += declStmt

                        val if0 = newIfElse { ifElse ->
                            // "lt" has no ArgumentHolder context (see note in
                            // ProgramDependenceGraphPassTest) -- condition ends up being just
                            // the literal, not the comparison. Faithfully reproduced.
                            ifElse.condition = newLiteral(1, objectType("int"))
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(newReference("i")),
                                            rhs = listOf(newLiteral(1, objectType("int"))),
                                        )

                                    val printfCall0 = newCall(newReference("printf"))
                                    printfCall0.addArgument(newLiteral("0\n", objectType("string")))
                                    thenBlock += printfCall0
                                }
                        }
                        block += if0

                        val printfCall1 = newCall(newReference("printf"))
                        printfCall1.addArgument(newLiteral("1\n", objectType("string")))
                        block += printfCall1

                        val if1 = newIfElse { ifElse ->
                            // "gt" DOES have ArgumentHolder context, so its own self-attach
                            // (which happens after the operands' self-attach) correctly ends
                            // up overwriting to become the condition.
                            ifElse.condition =
                                newBinaryOperator(">").also {
                                    it.lhs = newReference("i")
                                    it.rhs = newLiteral(0, objectType("int"))
                                }
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(newReference("i")),
                                            rhs = listOf(newLiteral(2, objectType("int"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(newReference("i")),
                                            rhs = listOf(newLiteral(3, objectType("int"))),
                                        )
                                }
                        }
                        block += if1

                        val printfCall2 = newCall(newReference("printf"))
                        printfCall2.addArgument(newLiteral("2\n", objectType("string")))
                        block += printfCall2

                        val returnStmt = newReturn()
                        returnStmt.returnValue = newReference("i")
                        block += returnStmt
                    }
            }

            return translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
        }

        fun getForEachTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage<TestLanguageWithColon>()
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .build()
                )
                .build {
                    val tu = newTranslationUnit("forEach.cpp")
                    scopeManager.resetToGlobal(tu)

                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                val declStmt = newDeclarationStatement()
                                val i =
                                    newVariable("i", objectType("int")).also {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                declStmt.declarations += i
                                scopeManager.addDeclaration(i)
                                block += declStmt

                                val forEach = newForEach()
                                val loopVarDeclStmt = newDeclarationStatement()
                                val loopVar = newVariable("loopVar", objectType("string"))
                                loopVarDeclStmt.declarations += loopVar
                                scopeManager.addDeclaration(loopVar)
                                // Fluent's declare{}/call(...) self-attach to ForEach's generic
                                // StatementHolder.statements *in addition to* the explicit
                                // `variable =`/`iterable =` property assignment done in the
                                // original test -- both effects are reproduced faithfully.
                                forEach.statements += loopVarDeclStmt
                                forEach.variable = loopVarDeclStmt
                                val magicCall = newCall(newReference("magicFunction"))
                                forEach.statements += magicCall
                                forEach.iterable = magicCall
                                forEach.statement =
                                    newBlock(enterScope = true) { loopBody ->
                                        val printfCall = newCall(newReference("printf"))
                                        printfCall.addArgument(
                                            newLiteral("loop: \${}\n", objectType("string"))
                                        )
                                        printfCall.addArgument(newReference("loopVar"))
                                        loopBody += printfCall
                                    }
                                block += forEach

                                val printfCall1 = newCall(newReference("printf"))
                                printfCall1.addArgument(newLiteral("1\n", objectType("string")))
                                block += printfCall1

                                val returnStmt = newReturn()
                                returnStmt.returnValue = newReference("i")
                                block += returnStmt
                            }
                    }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
                }

        fun getTimeoutTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage<TestLanguageWithColon>()
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .configurePass<ControlDependenceGraphPass>(
                            ControlDependenceGraphPass.Configuration(timeout = 0L)
                        )
                        .build()
                )
                .build { buildIfTestBody("if.cpp") }
    }
}
