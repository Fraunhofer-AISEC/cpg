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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import kotlin.test.*

class TypePropagationTest {
    @Test
    fun testBinopTypePropagation() {
        val result =
            TestLanguageFrontend().build {
                translationResult {
                    translationUnit("test") {
                        function("main", t("int")) {
                            body {
                                declare { variable("intVar", t("int")) {} }
                                declare { variable("intVar2", t("int")) { literal(5) } }
                                declare {
                                    variable("addResult", t("int")) {
                                        ref("intVar") + ref("intVar2")
                                    }
                                }
                                returnStmt { literal(0) }
                            }
                        }
                    }
                }
            }

        VariableUsageResolver(result.finalCtx).accept(result.components.first())

        val intVar = result.variables["intVar"]
        assertNotNull(intVar)
        assertLocalName("int", intVar.type)
        assertLocalName("int", intVar.declaredType)

        val intVarRef = result.refs["intVar"]
        assertNotNull(intVarRef)
        assertLocalName("int", intVarRef.type)
        assertLocalName("int", intVar.declaredType)

        val addResult = result.variables["addResult"]
        assertNotNull(addResult)

        val binaryOp = addResult.initializer
        assertNotNull(binaryOp)

        assertTrue(binaryOp.type is IntegerType)
        assertEquals("int", (binaryOp.type as IntegerType).name.toString())
        assertEquals(32, (binaryOp.type as IntegerType).bitWidth)
    }

    @Test
    fun testAssignTypePropagation() {
        val result =
            TestLanguageFrontend().build {
                translationResult {
                    translationUnit("test") {
                        function("main", t("int")) {
                            body {
                                declare { variable("intVar", t("int")) {} }
                                declare { variable("shortVar", t("short")) {} }
                                ref("shortVar") assign ref("intVar")
                                returnStmt { literal(0) }
                            }
                        }
                    }
                }
            }

        VariableUsageResolver(result.finalCtx).accept(result.components.first())

        val assign =
            (result.functions["main"]?.body as? CompoundStatement)?.statements?.get(2)
                as? AssignExpression
        assertNotNull(assign)

        val rhs = assign.rhs.firstOrNull() as? DeclaredReferenceExpression
        assertNotNull(rhs)
        assertIs<IntegerType>(rhs.type)
        assertLocalName("int", rhs.type)
        assertEquals(32, (rhs.type as IntegerType).bitWidth)

        val lhs = assign.lhs.firstOrNull() as? DeclaredReferenceExpression
        assertNotNull(lhs)
        assertIs<IntegerType>(lhs.type)
        assertLocalName("short", lhs.type)
        assertEquals(16, (lhs.type as IntegerType).bitWidth)

        assertIs<IntegerType>(lhs.assignedType)
        assertLocalName("int", lhs.assignedType)

        val refersTo = lhs.refersTo as? VariableDeclaration
        assertNotNull(refersTo)
        assertIs<IntegerType>(refersTo.type)
        assertLocalName("short", refersTo.type)
        assertEquals(16, (refersTo.type as IntegerType).bitWidth)
    }
}
