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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDecl
import de.fraunhofer.aisec.cpg.graph.scopes.BlockScope
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStmt
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStmt
import de.fraunhofer.aisec.cpg.graph.statements.IfStmt
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import kotlin.test.*

class FluentTest {
    @Test
    fun test() {
        val result =
            TestLanguageFrontend().build {
                translationResult {
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
                                declare { variable("some", t("SomeClass")) }
                                call("do") { call("some.func") }

                                returnStmt { ref("a") + literal(2) }
                            }
                        }
                    }
                }
            }

        // Let's assert that we did this correctly
        val main = result.functions["main"]
        assertNotNull(main)
        assertNotNull(main.scope)
        assertTrue(main.scope is GlobalScope)

        val argc = main.parameters["argc"]
        assertNotNull(argc)
        assertLocalName("argc", argc)
        assertLocalName("int", argc.type)

        val body = main.body as? CompoundStmt
        assertNotNull(body)
        assertTrue {
            body.scope is FunctionScope
            body.scope?.astNode == main
        }

        // First line should be a DeclarationStatement
        val declarationStmt = main[0] as? DeclarationStmt
        assertNotNull(declarationStmt)
        assertTrue(declarationStmt.scope is BlockScope)

        val variable = declarationStmt.singleDeclaration as? VariableDecl
        assertNotNull(variable)
        assertTrue(variable.scope is BlockScope)
        assertLocalName("a", variable)

        var lit1 = variable.initializer as? Literal<*>
        assertNotNull(lit1)
        assertTrue(lit1.scope is BlockScope)
        assertEquals(1, lit1.value)

        // Second line should be an IfStatement
        val ifStmt = main[1] as? IfStmt
        assertNotNull(ifStmt)
        assertTrue(ifStmt.scope is BlockScope)

        val condition = ifStmt.condition as? BinaryOp
        assertNotNull(condition)
        assertEquals("==", condition.operatorCode)

        // The "then" should have a call to "printf" with argument "then"
        var printf = ifStmt.thenStatement.calls["printf"]
        assertNotNull(printf)
        assertEquals("then", printf.arguments[0]<Literal<*>>()?.value)

        // The "else" contains another if (else-if) and a call to "printf" with argument "elseIf"
        val elseIf = ifStmt.elseStatement as? IfStmt
        assertNotNull(elseIf)

        printf = elseIf.thenStatement.calls["printf"]
        assertNotNull(printf)
        assertEquals("elseIf", printf.arguments[0]<Literal<*>>()?.value)

        printf = elseIf.elseStatement.calls["printf"]
        assertNotNull(printf)
        assertEquals("else", printf.arguments[0]<Literal<*>>()?.value)

        var ref = condition.lhs<Reference>()
        assertNotNull(ref)
        assertLocalName("argc", ref)

        lit1 = condition.rhs()
        assertNotNull(lit1)
        assertEquals(1, lit1.value)

        // Fourth line is the CallExpression (containing another MemberCallExpression as argument)
        val call = main[3] as? CallExpr
        assertNotNull(call)
        assertLocalName("do", call)

        val mce = call.arguments[0] as? MemberCallExpr
        assertNotNull(mce)
        assertFullName("UNKNOWN.func", mce)

        // Fifth line is the ReturnStatement
        val returnStmt = main[4] as? ReturnStmt
        assertNotNull(returnStmt)
        assertNotNull(returnStmt.scope)

        val binOp = returnStmt.returnValue as? BinaryOp
        assertNotNull(binOp)
        assertNotNull(binOp.scope)
        assertEquals("+", binOp.operatorCode)

        ref = binOp.lhs as? Reference
        assertNotNull(ref)
        assertNotNull(ref.scope)
        assertNull(ref.refersTo)
        assertLocalName("a", ref)

        val lit2 = binOp.rhs as? Literal<*>
        assertNotNull(lit2)
        assertNotNull(lit2.scope)
        assertEquals(2, lit2.value)

        VariableUsageResolver(result.finalCtx).accept(result.components.first())

        // Now the reference should be resolved and the MCE name set
        assertRefersTo(ref, variable)
        assertFullName("SomeClass::func", mce)
    }
}
