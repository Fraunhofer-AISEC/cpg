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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import kotlin.test.*

class TypePropagationTest {
    @Test
    fun testBinopTypePropagation() {
        val result =
            TestLanguageFrontend().build {
                translationResult(TranslationConfiguration.builder().build()) {
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

        val binaryOp =
            (((result.functions["main"]?.body as? CompoundStatement)?.statements?.get(2)
                        as? DeclarationStatement)
                    ?.singleDeclaration as? VariableDeclaration)
                ?.initializer

        assertNotNull(binaryOp)
        assertTrue(binaryOp.type is IntegerType)
        assertEquals("int", (binaryOp.type as IntegerType).name.toString())
        assertEquals(32, (binaryOp.type as IntegerType).bitWidth)
    }

    @Test
    fun testAssignTypePropagation() {
        // TODO: This test is related to issue 1071 (it models case 2).
        val scopeManager = ScopeManager()
        val result =
            TestLanguageFrontend(scopeManager).build {
                translationResult(TranslationConfiguration.builder().build()) {
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
        VariableUsageResolver().accept(result)

        val binaryOp =
            (result.functions["main"]?.body as? CompoundStatement)?.statements?.get(2)
                as? BinaryOperator
        assertNotNull(binaryOp)

        val rhs = binaryOp.rhs as? DeclaredReferenceExpression
        assertNotNull(rhs)
        assertTrue(rhs.type is IntegerType)
        assertEquals("int", (rhs.type as IntegerType).name.toString())
        assertEquals(32, (rhs.type as IntegerType).bitWidth)

        assertTrue(binaryOp.type is IntegerType)
        assertEquals("short", (binaryOp.type as IntegerType).name.toString())
        assertEquals(16, (binaryOp.type as IntegerType).bitWidth)

        val lhs = binaryOp.lhs as? DeclaredReferenceExpression
        assertNotNull(lhs)
        assertTrue(lhs.type is IntegerType)
        assertEquals("short", (lhs.type as IntegerType).name.toString())
        assertEquals(16, (lhs.type as IntegerType).bitWidth)

        val refersTo = lhs.refersTo as? VariableDeclaration
        assertNotNull(refersTo)
        assertTrue(refersTo.type is IntegerType)
        assertEquals("short", (refersTo.type as IntegerType).name.toString())
        assertEquals(16, (refersTo.type as IntegerType).bitWidth)
    }
}
