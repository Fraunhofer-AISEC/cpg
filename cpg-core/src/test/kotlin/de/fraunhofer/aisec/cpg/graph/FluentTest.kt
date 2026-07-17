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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.expressions.Comprehension
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

/**
 * Proof-of-concept migration of the former Fluent-DSL-based tests to the plain [NodeBuilder] API
 * (`enterScope`/`holder` parameters instead of context-receiver-resolved [Holder]s).
 *
 * Two things matter for correctness here, both because every [Node] already implements
 * [MetadataProvider] (via [ScopeProvider]/[LanguageProvider]):
 * 1. Every `init` callback takes its node as an explicit parameter (`(T) -> Unit`), never as a
 *    receiver (`T.() -> Unit`) -- and `.also { }` (callback-style) is used instead of `.apply { }`
 *    (receiver-style) whenever further node construction happens inside the block. A receiver
 *    lambda would introduce the node itself as an implicit receiver, and since every node also
 *    satisfies `MetadataProvider`/`ContextProvider`, a nested `newXYZ(...)` call could silently
 *    resolve to that inner node instead of the outer frontend -- no compile error, just quietly
 *    wrong `scope`.
 * 2. All node construction happens directly inside `.build { }`, where `this` is unambiguously the
 *    `LanguageFrontend` -- not inside `translationResult { }`'s own receiver lambda (that type is
 *    shared with the still-Fluent-based tests, so it can't be changed here). `translationResult {
 *    }` is only used for its trailing role: creating the `TranslationResult`/`Component` and
 *    running passes + pseudo-location inference over the already-fully-built tree.
 */
class FluentTest {
    @Test
    fun test() {
        val result =
            testFrontend { it.registerLanguage<TestLanguageWithColon>() }
                .build {
                    val tu = newTranslationUnit("file.cpp")
                    scopeManager.resetToGlobal(tu)

                    val main =
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("int"))

                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    val a = newVariable("a", objectType("short"))
                                    a.initializer = newLiteral(1)
                                    val declStmt = newDeclarationStatement()
                                    declStmt.declarations += a
                                    scopeManager.addDeclaration(a)
                                    block += declStmt

                                    val ifElse = newIfElse { ifElse ->
                                        val lhs = newReference("argc")
                                        val rhs = newLiteral(1)
                                        ifElse.condition =
                                            newBinaryOperator("==").also {
                                                it.lhs = lhs
                                                it.rhs = rhs
                                            }
                                        ifElse.thenStatement =
                                            newBlock(enterScope = true) { thenBlock ->
                                                val printfCall = newCall(newReference("printf"))
                                                printfCall.arguments += newLiteral("then")
                                                thenBlock += printfCall
                                            }
                                        ifElse.elseStatement = newIfElse { elseIf ->
                                            val elseIfLhs = newReference("argc")
                                            val elseIfRhs = newLiteral(1)
                                            elseIf.condition =
                                                newBinaryOperator("==").also {
                                                    it.lhs = elseIfLhs
                                                    it.rhs = elseIfRhs
                                                }
                                            elseIf.thenStatement =
                                                newBlock(enterScope = true) { elseIfThenBlock ->
                                                    val printfCall = newCall(newReference("printf"))
                                                    printfCall.arguments += newLiteral("elseIf")
                                                    elseIfThenBlock += printfCall
                                                }
                                            elseIf.elseStatement =
                                                newBlock(enterScope = true) { elseIfElseBlock ->
                                                    val printfCall = newCall(newReference("printf"))
                                                    printfCall.arguments += newLiteral("else")
                                                    elseIfElseBlock += printfCall
                                                }
                                        }
                                    }
                                    block += ifElse

                                    val some = newVariable("some", objectType("SomeClass"))
                                    val someDeclStmt = newDeclarationStatement()
                                    someDeclStmt.declarations += some
                                    scopeManager.addDeclaration(some)
                                    block += someDeclStmt

                                    val doCall = newCall(newReference("do"))
                                    val memberBase = newReference("some")
                                    val memberCall =
                                        newMemberCall(newMemberAccess("func", memberBase), false)
                                    doCall.arguments += memberCall
                                    block += doCall

                                    val returnStmt = newReturn()
                                    val sumLhs = newReference("a")
                                    val sumRhs = newLiteral(2)
                                    returnStmt.returnValue =
                                        newBinaryOperator("+").also {
                                            it.lhs = sumLhs
                                            it.rhs = sumRhs
                                        }
                                    block += returnStmt
                                }
                        }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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

        val variable = declarationStatement.singleDeclaration as? Variable
        assertNotNull(variable)
        assertTrue(variable.scope is LocalScope)
        assertLocalName("a", variable)

        var lit1 = variable.initializer as? Literal<*>
        assertNotNull(lit1)
        assertTrue(lit1.scope is LocalScope)
        assertEquals(1, lit1.value)

        // Second line should be an If
        val ifStatement = main[1] as? IfElse
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
        val elseIf = ifStatement.elseStatement as? IfElse
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

        // Fourth line is the Call (containing another MemberCall as argument)
        val call = main[3] as? Call
        assertNotNull(call)
        assertLocalName("do", call)

        val mce = call.arguments[0] as? MemberCall
        assertNotNull(mce)
        assertFullName("UNKNOWN.func", mce)

        // Fifth line is the Return
        val returnStatement = main[4] as? Return
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
        app.allEOGStarters
            .filter { it.prevEOGEdges.isEmpty() }
            .forEach { eogStarter -> SymbolResolver(result.finalCtx).accept(eogStarter) }

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
                    val tu = newTranslationUnit("File")
                    scopeManager.resetToGlobal(tu)

                    val main =
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("list"))
                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    val some = newVariable("some")
                                    val statementRef = newReference("i")
                                    val variableRef = newReference("i")
                                    val iterableRef = newReference("someIterable")
                                    val predLhs = newReference("i")
                                    val predRhs = newLiteral(5, objectType("int"))
                                    val comprehension =
                                        newComprehension().also {
                                            it.variable = variableRef
                                            it.iterable = iterableRef
                                            it.predicate =
                                                newBinaryOperator(">").also { op ->
                                                    op.lhs = predLhs
                                                    op.rhs = predRhs
                                                }
                                        }
                                    some.initializer = newCollectionComprehension { cc ->
                                        cc.statement = statementRef
                                        cc.comprehensionExpressions += comprehension
                                    }
                                    val declStmt = newDeclarationStatement()
                                    declStmt.declarations += some
                                    scopeManager.addDeclaration(some)
                                    block += declStmt

                                    val returnStmt = newReturn()
                                    returnStmt.returnValue = newReference("some")
                                    block += returnStmt
                                }
                        }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
        assertIs<Comprehension>(compExpr)
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
                    val tu = newTranslationUnit("File")
                    scopeManager.resetToGlobal(tu)

                    val main =
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("list"))
                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    val some = newVariable("some")
                                    val statementRef = newReference("i")

                                    val i = newVariable("i")
                                    val iDeclStmt = newDeclarationStatement()
                                    iDeclStmt.declarations += i
                                    scopeManager.addDeclaration(i)

                                    val iterableRef = newReference("someIterable")
                                    val predLhs = newReference("i")
                                    val predRhs = newLiteral(5, objectType("int"))
                                    val comprehension =
                                        newComprehension().also {
                                            it.variable = iDeclStmt
                                            it.iterable = iterableRef
                                            it.predicate =
                                                newBinaryOperator(">").also { op ->
                                                    op.lhs = predLhs
                                                    op.rhs = predRhs
                                                }
                                        }
                                    some.initializer = newCollectionComprehension { cc ->
                                        cc.statement = statementRef
                                        cc.comprehensionExpressions += comprehension
                                    }
                                    val declStmt = newDeclarationStatement()
                                    declStmt.declarations += some
                                    scopeManager.addDeclaration(some)
                                    block += declStmt

                                    val returnStmt = newReturn()
                                    returnStmt.returnValue = newReference("some")
                                    block += returnStmt
                                }
                        }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
        assertIs<Comprehension>(compExpr)
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
                    val tu = newTranslationUnit("File")
                    scopeManager.resetToGlobal(tu)

                    val main =
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("list"))
                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    val some = newVariable("some")
                                    val statementRef = newReference("i")

                                    val i = newVariable("i")
                                    val y = newVariable("y")
                                    val iDeclStmt = newDeclarationStatement()
                                    iDeclStmt.declarations += i
                                    iDeclStmt.declarations += y
                                    scopeManager.addDeclaration(i)
                                    scopeManager.addDeclaration(y)

                                    val iterableRef = newReference("someIterable")
                                    val predLhs = newReference("i")
                                    val predRhs = newLiteral(5, objectType("int"))
                                    val comprehension =
                                        newComprehension().also {
                                            it.variable = iDeclStmt
                                            it.iterable = iterableRef
                                            it.predicate =
                                                newBinaryOperator(">").also { op ->
                                                    op.lhs = predLhs
                                                    op.rhs = predRhs
                                                }
                                        }
                                    some.initializer = newCollectionComprehension { cc ->
                                        cc.statement = statementRef
                                        cc.comprehensionExpressions += comprehension
                                    }
                                    val declStmt = newDeclarationStatement()
                                    declStmt.declarations += some
                                    scopeManager.addDeclaration(some)
                                    block += declStmt

                                    val returnStmt = newReturn()
                                    returnStmt.returnValue = newReference("some")
                                    block += returnStmt
                                }
                        }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
        assertIs<Comprehension>(compExpr)
        val variableDecl = compExpr.variable
        assertIs<DeclarationStatement>(variableDecl)
        assertLocalName("i", variableDecl.declarations[0])
        assertLocalName("y", variableDecl.declarations[1])
        assertIs<Reference>(compExpr.iterable)
        assertLocalName("someIterable", compExpr.iterable)
        assertNotNull(compExpr.predicate)
    }

    @Test
    fun testLocationIntegrity() {
        val results =
            listOf<TranslationResult>(
                GraphExamples.getWhileWithElseAndBreak(),
                GraphExamples.getDoWithElseAndBreak(),
                GraphExamples.getForWithElseAndBreak(),
                GraphExamples.getForEachWithElseAndBreak(),
                GraphExamples.getBasicSlice(),
            )
        results.forEach { result ->
            result.components
                .flatMap { it.translationUnits }
                .forEach { file ->
                    SubgraphWalker.flattenAST(file).forEach { parent ->
                        // Test that locations are correct with the end after the start
                        parent.location?.region?.let { pLoc ->
                            assertTrue(pLoc.startLine <= pLoc.endLine)
                            assertTrue(
                                pLoc.startLine != pLoc.endLine || pLoc.startColumn <= pLoc.endColumn
                            )

                            parent.astChildren.forEach { child ->
                                child.location?.region?.let {
                                    assertTrue(pLoc.startLine <= it.startLine)
                                    assertTrue(pLoc.endLine >= it.endLine)
                                    assertFalse(
                                        pLoc.startLine == it.startLine &&
                                            pLoc.startColumn > it.startColumn
                                    )
                                    assertFalse(
                                        pLoc.endLine == it.endLine && pLoc.endColumn < it.endColumn
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}
