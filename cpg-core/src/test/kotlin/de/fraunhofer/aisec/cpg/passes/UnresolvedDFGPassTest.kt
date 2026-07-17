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
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
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
        assertTrue(knownCall.prevDFG.firstOrNull() is Method)
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
        assertTrue(knownCall.prevDFG.firstOrNull() is Method)
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
        assertEquals(1, callWithParam.prevFullDFG.size)
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
        assertTrue(knownCall.prevDFG.firstOrNull() is Method)
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
                val tu = newTranslationUnit("DfgUnresolvedCalls.java")
                scopeManager.resetToGlobal(tu)

                newRecord("DfgUnresolvedCalls", "class", holder = tu, enterScope = true) { record ->
                    newField("i", objectType("int"), modifiers = setOf("private"), holder = record)

                    // Fluent's constructor() attaches via record.constructors +=, not the generic
                    // DeclarationHolder mechanism, so it's wired manually rather than via the
                    // `holder` builder parameter.
                    val ctor =
                        newConstructor(record.name, record, enterScope = true) { c ->
                            c.receiver = newVariable("this", objectType("DfgUnresolvedCalls"))
                            newParameter("i", objectType("int"), holder = c)
                            c.body =
                                newBlock(enterScope = true) { block ->
                                    val memberAccess = newMemberAccess("i", newReference("this"))
                                    block +=
                                        newAssign(
                                            operatorCode = "=",
                                            lhs = listOf(memberAccess),
                                            rhs = listOf(newReference("i")),
                                        )
                                    block += newReturn().also { it.isImplicit = true }
                                }
                        }
                    scopeManager.addDeclaration(ctor)
                    record.constructors += ctor

                    newMethod(
                        "knownFunction",
                        recordDeclaration = record,
                        holder = record,
                        enterScope = true,
                    ) { m ->
                        m.returnTypes = listOf(objectType("int"))
                        m.type = computeType(m)
                        m.receiver = newVariable("this", objectType("DfgUnresolvedCalls"))
                        newParameter("arg", objectType("int"), holder = m)
                        m.body =
                            newBlock(enterScope = true) { block ->
                                val returnStmt = newReturn()
                                returnStmt.returnValue =
                                    newBinaryOperator("+").also {
                                        it.lhs = newMemberAccess("i", newReference("this"))
                                        it.rhs = newReference("arg")
                                    }
                                block += returnStmt
                            }
                    }

                    // The main method
                    newMethod(
                        "main",
                        recordDeclaration = record,
                        holder = record,
                        enterScope = true,
                    ) { m ->
                        m.type = computeType(m)
                        m.isStatic = true
                        newParameter("args", objectType("String[]"), holder = m)
                        m.body =
                            newBlock(enterScope = true) { block ->
                                val osDeclStmt = newDeclarationStatement()
                                val os =
                                    newVariable(
                                            "os",
                                            objectType("Optional", listOf(objectType("String"))),
                                        )
                                        .also {
                                            it.initializer =
                                                newMemberCall(
                                                    newMemberAccess(
                                                        "getOptionalString",
                                                        newReference("RandomClass"),
                                                    ),
                                                    true,
                                                )
                                        }
                                osDeclStmt.declarations += os
                                scopeManager.addDeclaration(os)
                                block += osDeclStmt

                                val sDeclStmt = newDeclarationStatement()
                                val s =
                                    newVariable("s", objectType("String")).also {
                                        it.initializer =
                                            newMemberCall(
                                                newMemberAccess("get", newReference("os")),
                                                false,
                                            )
                                    }
                                sDeclStmt.declarations += s
                                scopeManager.addDeclaration(s)
                                block += sDeclStmt

                                val s2DeclStmt = newDeclarationStatement()
                                val s2 =
                                    newVariable("s2", objectType("String")).also {
                                        it.initializer =
                                            newMemberCall(
                                                    newMemberAccess("get", newReference("os")),
                                                    false,
                                                )
                                                .also { call ->
                                                    call.arguments +=
                                                        newLiteral(4, objectType("int"))
                                                }
                                    }
                                s2DeclStmt.declarations += s2
                                scopeManager.addDeclaration(s2)
                                block += s2DeclStmt

                                val ducDeclStmt = newDeclarationStatement()
                                val duc =
                                    newVariable("duc", objectType("DfgUnresolvedCalls")).also {
                                        it.initializer =
                                            newNew().also { newExpr ->
                                                newExpr.initializer =
                                                    newConstruction("DfgUnresolvedCalls").also {
                                                        construction ->
                                                        construction.type =
                                                            objectType("DfgUnresolvedCalls")
                                                        construction.arguments +=
                                                            newLiteral(3, objectType("int"))
                                                    }
                                            }
                                    }
                                ducDeclStmt.declarations += duc
                                scopeManager.addDeclaration(duc)
                                block += ducDeclStmt

                                val iDeclStmt = newDeclarationStatement()
                                val i =
                                    newVariable("i", objectType("int")).also {
                                        it.initializer =
                                            newMemberCall(
                                                    newMemberAccess(
                                                        "knownFunction",
                                                        newReference("duc"),
                                                    ),
                                                    false,
                                                )
                                                .also { call ->
                                                    call.arguments +=
                                                        newLiteral(2, objectType("int"))
                                                }
                                    }
                                iDeclStmt.declarations += i
                                scopeManager.addDeclaration(i)
                                block += iDeclStmt
                            }
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
        }
    }
}
