/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UnresolvedDFGPassTest {
    @Test
    fun testUnresolvedCalls() {
        val result = getDfgUnresolvedCalls(true, false)

        // Flow from base to return value
        val firstCall = result.calls { it.name.localName == "get" }[0]
        val osDecl = result.variables["os"]
        assertEquals(1, firstCall.prevDFG.size)
        assertEquals(osDecl, (firstCall.prevDFG.firstOrNull() as? Reference)?.refersTo)

        // Flow from base and argument to return value
        val callWithParam = result.calls { it.name.localName == "get" }[1]
        assertEquals(2, callWithParam.prevDFG.size)
        assertEquals(
            osDecl,
            callWithParam.prevDFG.filterIsInstance<Reference>().firstOrNull()?.refersTo,
        )
        assertEquals(4, callWithParam.prevDFG.filterIsInstance<Literal<*>>().firstOrNull()?.value)

        // No specific flows for resolved functions
        // => Goes through the method declaration and then follows the instructions in the method's
        // implementation
        val knownCall = result.calls { it.name.localName == "knownFunction" }[0]
        assertEquals(1, knownCall.prevDFG.size)
        assertTrue(knownCall.prevDFG.firstOrNull() is MethodDeclaration)
    }

    @Test
    fun testUnresolvedCallsNoInference() {
        val result = getDfgUnresolvedCalls(false, false)

        // No flow from base to return value
        val firstCall = result.calls { it.name.localName == "get" }[0]
        assertEquals(0, firstCall.prevDFG.size)

        // No flow from base or argument to return value
        val callWithParam = result.calls { it.name.localName == "get" }[1]
        assertEquals(0, callWithParam.prevDFG.size)

        // No specific flows for resolved functions
        // => Goes through the method declaration and then follows the instructions in the method's
        // implementation
        val knownCall = result.calls { it.name.localName == "knownFunction" }[0]
        assertEquals(1, knownCall.prevDFG.size)
        assertTrue(knownCall.prevDFG.firstOrNull() is MethodDeclaration)
    }

    @Test
    fun testUnresolvedCallsWithInference() {
        // For calls with an inferred function declaration, we connect the arguments with the
        // parameter declaration.
        // The parameter declaration is connected to the function declaration which then flows to
        // the lhs of the assignment.
        val result = getDfgUnresolvedCalls(false, true)

        val osDecl = result.variables["os"]
        assertNotNull(osDecl)

        // Flow from base to method declaration which then flows to the call
        val firstCall = result.calls { it.name.localName == "get" }[0]
        assertEquals(1, firstCall.prevDFG.size)
        // Check if it's the "get" method.
        val getMethod1 = firstCall.prevDFG.singleOrNull { it.name.localName == "get" }
        assertNotNull(getMethod1)
        assertEquals(1, getMethod1.prevDFG.size)
        assertEquals(
            osDecl,
            (getMethod1.prevDFG.singleOrNull()?.prevDFG?.singleOrNull() as? Reference)?.refersTo,
        )

        // Flow from base and argument to return value
        val callWithParam = result.calls { it.name.localName == "get" }[1]
        assertEquals(1, callWithParam.prevDFG.size)
        // Check if it's the "get" method.
        val getMethod2 = callWithParam.prevDFG.singleOrNull { it.name.localName == "get" }
        assertNotNull(getMethod2)
        assertEquals(2, getMethod2.prevDFG.size)
        val callWithParamArgs = getMethod2.prevDFG.flatMap { it.prevDFG }
        assertEquals(
            osDecl,
            callWithParamArgs.filterIsInstance<Reference>().firstOrNull()?.refersTo,
        )
        assertEquals(4, callWithParamArgs.filterIsInstance<Literal<*>>().firstOrNull()?.value)

        // No specific flows for resolved functions
        // => Goes through the method declaration and then follows the instructions in the method's
        // implementation
        val knownCall = result.calls { it.name.localName == "knownFunction" }[0]
        assertEquals(1, knownCall.prevDFG.size)
        assertTrue(knownCall.prevDFG.firstOrNull() is MethodDeclaration)
    }

    companion object {

        fun getDfgUnresolvedCalls(
            inferUnresolved: Boolean,
            inferFunctions: Boolean,
        ): TranslationResult {
            val config =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferDfgForUnresolvedCalls(inferUnresolved)
                            .inferFunctions(inferFunctions)
                            .build()
                    )
                    .build()
            return testFrontend(config).build {
                translationResult {
                    translationUnit("DfgUnresolvedCalls.java") {
                        record("DfgUnresolvedCalls") {
                            field("i", t("int")) { modifiers = setOf("private") }
                            constructor {
                                receiver = newVariableDeclaration("this", t("DfgUnresolvedCalls"))
                                param("i", t("int"))
                                body {
                                    member("i", ref("this")) assign { ref("i") }
                                    returnStmt { isImplicit = true }
                                }
                            }
                            method("knownFunction", t("int")) {
                                receiver = newVariableDeclaration("this", t("DfgUnresolvedCalls"))
                                param("arg", t("int"))
                                body { returnStmt { member("i", ref("this")) + ref("arg") } }
                            }

                            // The main method
                            method("main") {
                                this.isStatic = true
                                param("args", t("String[]"))
                                body {
                                    declare {
                                        variable("os", t("Optional", listOf(t("String")))) {
                                            memberCall("getOptionalString", ref("RandomClass")) {
                                                isStatic = true
                                            }
                                        }
                                    }
                                    declare {
                                        variable("s", t("String")) { memberCall("get", ref("os")) }
                                    }
                                    declare {
                                        variable("s2", t("String")) {
                                            memberCall("get", ref("os")) { literal(4, t("int")) }
                                        }
                                    }
                                    declare {
                                        variable("duc", t("DfgUnresolvedCalls")) {
                                            new {
                                                construct("DfgUnresolvedCalls") {
                                                    literal(3, t("int"))
                                                }
                                            }
                                        }
                                    }
                                    declare {
                                        variable("i", t("int")) {
                                            memberCall("knownFunction", ref("duc")) {
                                                literal(2, t("int"))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
