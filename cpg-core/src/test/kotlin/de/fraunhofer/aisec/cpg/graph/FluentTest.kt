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

import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

class FluentTest {
    @Test
    fun test() {
        val result =
            testFrontend { it.registerLanguage<TestLanguageWithColon>() }
                .build {
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

        val body = main.body as? Block
        assertNotNull(body)
        assertTrue {
            body.scope is FunctionScope
            body.scope?.astNode == main
        }

        // First line should be a DeclarationStatement
        val declarationStatement = main[0] as? DeclarationStatement
        assertNotNull(declarationStatement)
        assertTrue(declarationStatement.scope is LocalScope)

        val variable = declarationStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(variable)
        assertTrue(variable.scope is LocalScope)
        assertLocalName("a", variable)

        var lit1 = variable.initializer as? Literal<*>
        assertNotNull(lit1)
        assertTrue(lit1.scope is LocalScope)
        assertEquals(1, lit1.value)

        // Second line should be an IfStatement
        val ifStatement = main[1] as? IfStatement
        assertNotNull(ifStatement)
        assertTrue(ifStatement.scope is LocalScope)

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

        var ref = condition.lhs<Reference>()
        assertNotNull(ref)
        assertLocalName("argc", ref)

        lit1 = condition.rhs()
        assertNotNull(lit1)
        assertEquals(1, lit1.value)

        // Fourth line is the CallExpression (containing another MemberCallExpression as argument)
        val call = main[3] as? CallExpression
        assertNotNull(call)
        assertLocalName("do", call)

        val mce = call.arguments[0] as? MemberCallExpression
        assertNotNull(mce)
        assertFullName("UNKNOWN.func", mce)

        // Fifth line is the ReturnStatement
        val returnStatement = main[4] as? ReturnStatement
        assertNotNull(returnStatement)
        assertNotNull(returnStatement.scope)

        val binOp = returnStatement.returnValue as? BinaryOperator
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

        var app = result.components.firstOrNull()
        assertNotNull(app)

        ImportResolver(result.finalCtx).accept(result)
        EvaluationOrderGraphPass(result.finalCtx).accept(app.translationUnits.first())
        SymbolResolver(result.finalCtx).accept(app)

        // Now the reference should be resolved and the MCE name set
        assertRefersTo(ref, variable)
        assertFullName("SomeClass::func", mce)
    }

    @Test
    fun testCollectionComprehensions() {
        val result =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    translationResult {
                        translationUnit("File") {
                            function("main", t("list")) {
                                param("argc", t("int"))
                                body {
                                    declare {
                                        variable("some") {
                                            listComp {
                                                ref("i")
                                                compExpr {
                                                    ref("i")
                                                    ref("someIterable")
                                                    ref("i") gt literal(5, t("int"))
                                                }
                                            }
                                        }
                                    }

                                    returnStmt { ref("some") }
                                }
                            }
                        }
                    }
                }

        val listComp = result.variables["some"]?.initializer
        assertIs<CollectionComprehension>(listComp)
        print(listComp.toString()) // This is only here to get a better test coverage
        print(
            listComp.comprehensionExpressions.firstOrNull()?.toString()
        ) // This is only here to get a better test coverage
        assertIs<Reference>(listComp.statement)
        assertLocalName("i", listComp.statement)
        assertEquals(1, listComp.comprehensionExpressions.size)
        val compExpr = listComp.comprehensionExpressions.single()
        assertIs<ComprehensionExpression>(compExpr)
        assertIs<Reference>(compExpr.variable)
        assertLocalName("i", compExpr.variable)
        assertIs<Reference>(compExpr.iterable)
        assertLocalName("someIterable", compExpr.iterable)
        assertNotNull(compExpr.predicate)
    }

    @Test
    fun testCollectionComprehensionsWithDeclaration() {
        val result =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    translationResult {
                        translationUnit("File") {
                            function("main", t("list")) {
                                param("argc", t("int"))
                                body {
                                    declare {
                                        variable("some") {
                                            listComp {
                                                ref("i")
                                                compExpr {
                                                    this.variable = declare { variable("i") }
                                                    ref("someIterable")
                                                    ref("i") gt literal(5, t("int"))
                                                }
                                            }
                                        }
                                    }

                                    returnStmt { ref("some") }
                                }
                            }
                        }
                    }
                }

        val listComp = result.variables["some"]?.initializer
        assertIs<CollectionComprehension>(listComp)
        print(listComp.toString()) // This is only here to get a better test coverage
        print(
            listComp.comprehensionExpressions.firstOrNull()?.toString()
        ) // This is only here to get a better test coverage
        assertIs<Reference>(listComp.statement)
        assertLocalName("i", listComp.statement)
        assertEquals(1, listComp.comprehensionExpressions.size)
        val compExpr = listComp.comprehensionExpressions.single()
        assertIs<ComprehensionExpression>(compExpr)
        val variableDecl = compExpr.variable
        assertIs<DeclarationStatement>(variableDecl)
        assertLocalName("i", variableDecl.singleDeclaration)
        assertIs<Reference>(compExpr.iterable)
        assertLocalName("someIterable", compExpr.iterable)
        assertNotNull(compExpr.predicate)
    }

    @Test
    fun testCollectionComprehensionsWithTwoDeclarations() {
        val result =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    translationResult {
                        translationUnit("File") {
                            function("main", t("list")) {
                                param("argc", t("int"))
                                body {
                                    declare {
                                        variable("some") {
                                            listComp {
                                                ref("i")
                                                compExpr {
                                                    this.variable = declare {
                                                        variable("i")
                                                        variable("y")
                                                    }
                                                    ref("someIterable")
                                                    ref("i") gt literal(5, t("int"))
                                                }
                                            }
                                        }
                                    }

                                    returnStmt { ref("some") }
                                }
                            }
                        }
                    }
                }

        val listComp = result.variables["some"]?.initializer
        assertIs<CollectionComprehension>(listComp)
        print(listComp.toString()) // This is only here to get a better test coverage
        print(
            listComp.comprehensionExpressions.firstOrNull()?.toString()
        ) // This is only here to get a better test coverage
        assertIs<Reference>(listComp.statement)
        assertLocalName("i", listComp.statement)
        assertEquals(1, listComp.comprehensionExpressions.size)
        val compExpr = listComp.comprehensionExpressions.single()
        assertIs<ComprehensionExpression>(compExpr)
        val variableDecl = compExpr.variable
        assertIs<DeclarationStatement>(variableDecl)
        assertLocalName("i", variableDecl.declarations[0])
        assertLocalName("y", variableDecl.declarations[1])
        assertIs<Reference>(compExpr.iterable)
        assertLocalName("someIterable", compExpr.iterable)
        assertNotNull(compExpr.predicate)
    }
}
