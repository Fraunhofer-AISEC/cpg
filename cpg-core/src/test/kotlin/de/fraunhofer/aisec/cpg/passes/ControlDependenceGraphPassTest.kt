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
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ControlDependenceGraphPassTest {

    @Test
    fun testIfStatements() {
        val result = getIfTest()
        assertNotNull(result)
        val main = result.functions["main"]
        assertNotNull(main)
        val if0 = (main.body as Block).statements[1]
        assertNotNull(if0)
        assertEquals(1, if0.prevCDG.size)
        assertTrue(main in if0.prevCDG)

        val assignment1 =
            result.assignments.firstOrNull { 1 == (it.value as? Literal<*>)?.value }?.start
                as AstNode
        assertNotNull(assignment1)
        assertEquals(1, assignment1.prevCDG.size)
        assertTrue(if0 in assignment1.prevCDG)

        val print0 =
            result.calls("printf").first {
                "0\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(print0)
        assertEquals(1, print0.prevCDG.size)
        assertTrue(if0 in print0.prevCDG)

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
        assertEquals(1, forEachStmt.prevCDG.size)
        assertTrue(main in forEachStmt.prevCDG)

        val printInLoop =
            result.calls("printf").first {
                "loop: \${}\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(printInLoop)
        assertEquals(1, printInLoop.prevCDG.size)
        assertTrue(forEachStmt in printInLoop.prevCDG)

        val printAfterLoop =
            result.calls("printf").first {
                "1\n" == (it.arguments.firstOrNull() as? Literal<*>)?.value
            }
        assertNotNull(printAfterLoop)
        assertEquals(2, printAfterLoop.prevCDG.size)
        assertTrue(main in printAfterLoop.prevCDG)
        assertTrue(
            forEachStmt in printAfterLoop.prevCDG
        ) // TODO: Is this really correct or should it be filtered out in the pass?
    }

    companion object {
        fun getIfTest() =
            testFrontend(
                    TranslationConfiguration.builder()
                        .registerLanguage<TestLanguageWithColon>()
                        .defaultPasses()
                        .registerPass<ControlDependenceGraphPass>()
                        .build()
                )
                .build {
                    translationResult {
                        translationUnit("if.cpp") {
                            // The main method
                            function("main", t("int")) {
                                body {
                                    declare { variable("i", t("int")) { literal(0, t("int")) } }
                                    ifStmt {
                                        condition { ref("i") lt literal(1, t("int")) }
                                        thenStmt {
                                            ref("i") assign literal(1, t("int"))
                                            call("printf") { literal("0\n", t("string")) }
                                        }
                                    }
                                    call("printf") { literal("1\n", t("string")) }
                                    ifStmt {
                                        condition { ref("i") gt literal(0, t("int")) }
                                        thenStmt { ref("i") assign literal(2, t("int")) }
                                        elseStmt { ref("i") assign literal(3, t("int")) }
                                    }
                                    call("printf") { literal("2\n", t("string")) }
                                    returnStmt { ref("i") }
                                }
                            }
                        }
                    }
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
                    translationResult {
                        translationUnit("forEach.cpp") {
                            // The main method
                            function("main", t("int")) {
                                body {
                                    declare { variable("i", t("int")) { literal(0, t("int")) } }
                                    forEachStmt {
                                        declare { variable("loopVar", t("string")) }
                                        call("magicFunction")
                                        loopBody {
                                            call("printf") {
                                                literal("loop: \${}\n", t("string"))
                                                ref("loopVar")
                                            }
                                        }
                                    }
                                    call("printf") { literal("1\n", t("string")) }

                                    returnStmt { ref("i") }
                                }
                            }
                        }
                    }
                }
    }
}
