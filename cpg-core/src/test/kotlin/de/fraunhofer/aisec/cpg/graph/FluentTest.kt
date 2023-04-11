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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.BlockScope
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import kotlin.test.*

class FluentTest {
    @Test
    fun test() {
        val scopeManager = ScopeManager()
        val result =
            TestLanguageFrontend(scopeManager).build {
                translationResult(TranslationConfiguration.builder().build()) {
                    translationUnit("file.cpp") {
                        function("main", t("int")) {
                            param("argc", t("int"))
                            body {
                                declare { variable("a", t("short")) { literal(1) } }
                                ifStmt {
                                    condition { ref("argc") eq literal(1) }
                                    thenStmt { call("printf") { literal("then") } }
                                    elseIf {
                                        condition { ref("argc") eq literal(1) }
                                        thenStmt { call("printf") { literal("elseIf") } }
                                        elseStmt { call("printf") { literal("else") } }
                                    }
                                }
                                call("do") { call("some::func") }

                                returnStmt { ref("a") + literal(2) }
                            }
                        }
                    }
                }
            }
        val tu = result.translationUnits.firstOrNull()

        // Let's assert that we did this correctly
        val main = result.functions["main"]
        assertNotNull(main)
        assertNotNull(main.scope)
        assertTrue(main.scope is GlobalScope)

        val argc = main.parameters["argc"]
        assertNotNull(argc)
        assertLocalName("argc", argc)
        assertLocalName("int", argc.type)

        val body = main.body as? CompoundStatement
        assertNotNull(body)
        assertTrue {
            body.scope is FunctionScope
            body.scope?.astNode == main
        }

        // First line should be a DeclarationStatement
        val declarationStatement = main[0] as? DeclarationStatement
        assertNotNull(declarationStatement)
        assertTrue(declarationStatement.scope is BlockScope)

        val variable = declarationStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(variable)
        assertTrue(variable.scope is BlockScope)
        assertLocalName("a", variable)

        var lit1 = variable.initializer as? Literal<*>
        assertNotNull(lit1)
        assertTrue(lit1.scope is BlockScope)
        assertEquals(1, lit1.value)

        // Second line should be an IfStatement
        val ifStatement = main[1] as? IfStatement
        assertNotNull(ifStatement)
        assertTrue(ifStatement.scope is BlockScope)

        val condition = ifStatement.condition as? BinaryOperator
        assertNotNull(condition)
        assertEquals("==", condition.operatorCode)

        // The "then" should have a call to "printf" with argument "then"
        var printf = ifStatement.thenStatement.calls["printf"]
        assertNotNull(printf)
        assertEquals("then", printf.arguments[0]<Literal<*>>()?.value)

        // The "else" contains another if (else-if) and a call to "printf" with argument "elseIf"
        val elseIf = ifStatement.elseStatement as? IfStatement
        assertNotNull(elseIf)

        printf = elseIf.thenStatement.calls["printf"]
        assertNotNull(printf)
        assertEquals("elseIf", printf.arguments[0]<Literal<*>>()?.value)

        printf = elseIf.elseStatement.calls["printf"]
        assertNotNull(printf)
        assertEquals("else", printf.arguments[0]<Literal<*>>()?.value)

        var ref = condition.lhs<DeclaredReferenceExpression>()
        assertNotNull(ref)
        assertLocalName("argc", ref)

        lit1 = condition.rhs()
        assertNotNull(lit1)
        assertEquals(1, lit1.value)

        // Third line is th
        // e CallExpression (containing another MemberCallExpression as argument)
        val call = main[2] as? CallExpression
        assertNotNull(call)
        assertLocalName("do", call)

        val mce = call.arguments[0] as? MemberCallExpression
        assertNotNull(mce)
        assertFullName("some::func", mce)

        // Fourth line is the ReturnStatement
        val returnStatement = main[3] as? ReturnStatement
        assertNotNull(returnStatement)
        assertNotNull(returnStatement.scope)

        val binOp = returnStatement.returnValue as? BinaryOperator
        assertNotNull(binOp)
        assertNotNull(binOp.scope)
        assertEquals("+", binOp.operatorCode)

        ref = binOp.lhs as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertNotNull(ref.scope)
        assertNull(ref.refersTo)
        assertLocalName("a", ref)

        val lit2 = binOp.rhs as? Literal<*>
        assertNotNull(lit2)
        assertNotNull(lit2.scope)
        assertEquals(2, lit2.value)

        VariableUsageResolver().accept(result)

        // Now the reference should be resolved
        assertRefersTo(ref, variable)
    }
}
