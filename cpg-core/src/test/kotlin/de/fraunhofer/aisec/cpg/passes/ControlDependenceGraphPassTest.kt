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
                                block.statements +=
                                    newBinaryOperator("&&") {
                                        it.lhs = newCall(newReference("foo"))
                                        it.rhs = newCall(newReference("bar"))
                                    }

                                block.statements +=
                                    newBinaryOperator("||") {
                                        it.lhs = newCall(newReference("baz"))
                                        it.rhs = newCall(newReference("quux"))
                                    }

                                block.statements += newReturn {
                                    it.returnValue = newLiteral(1, objectType("int"))
                                }
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
                        block.statements += newDeclarationStatement { declStmt ->
                            newVariable("i", objectType("int"), holder = declStmt) {
                                it.initializer = newLiteral(0, objectType("int"))
                            }
                        }

                        block.statements += newIfElse { ifElse ->
                            ifElse.condition =
                                newBinaryOperator("<") {
                                    it.lhs = newReference("i")
                                    it.rhs = newLiteral(1, objectType("int"))
                                }
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock.statements +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(newReference("i")),
                                            rhs = listOf(newLiteral(1, objectType("int"))),
                                        )

                                    thenBlock.statements +=
                                        newCall(newReference("printf")) {
                                            it.arguments += newLiteral("0\n", objectType("string"))
                                        }
                                }
                        }

                        block.statements +=
                            newCall(newReference("printf")) {
                                it.arguments += newLiteral("1\n", objectType("string"))
                            }

                        block.statements += newIfElse { ifElse ->
                            ifElse.condition =
                                newBinaryOperator(">") {
                                    it.lhs = newReference("i")
                                    it.rhs = newLiteral(0, objectType("int"))
                                }
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock.statements +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(newReference("i")),
                                            rhs = listOf(newLiteral(2, objectType("int"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock.statements +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(newReference("i")),
                                            rhs = listOf(newLiteral(3, objectType("int"))),
                                        )
                                }
                        }

                        block.statements +=
                            newCall(newReference("printf")) {
                                it.arguments += newLiteral("2\n", objectType("string"))
                            }

                        block.statements += newReturn { it.returnValue = newReference("i") }
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
                                block.statements += newDeclarationStatement { declStmt ->
                                    newVariable("i", objectType("int"), holder = declStmt) {
                                        it.initializer = newLiteral(0, objectType("int"))
                                    }
                                }

                                block.statements += newForEach { forEach ->
                                    forEach.variable = newDeclarationStatement { loopVarDeclStmt ->
                                        newVariable(
                                            "loopVar",
                                            objectType("string"),
                                            holder = loopVarDeclStmt,
                                        )
                                    }
                                    val magicCall = newCall(newReference("magicFunction"))
                                    forEach.iterable = magicCall
                                    forEach.statement =
                                        newBlock(enterScope = true) { loopBody ->
                                            loopBody.statements +=
                                                newCall(newReference("printf")) {
                                                    it.arguments +=
                                                        newLiteral(
                                                            "loop: \${}\n",
                                                            objectType("string"),
                                                        )
                                                    it.arguments += newReference("loopVar")
                                                }
                                        }
                                }

                                block.statements +=
                                    newCall(newReference("printf")) {
                                        it.arguments += newLiteral("1\n", objectType("string"))
                                    }

                                block.statements += newReturn { it.returnValue = newReference("i") }
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
