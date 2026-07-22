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
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.expressions.Comprehension
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

/**
 * Tests that build small ASTs directly via the [NodeBuilder] API and assert
 * collection-comprehension structure and AST location integrity.
 */
class NodeBuilderTest {
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
                    singleTranslationUnit("File") { tu ->
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("list"))
                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("some", holder = declStmt) {
                                            it.initializer = newCollectionComprehension { cc ->
                                                cc.statement = newReference("i")
                                                cc.comprehensionExpressions += newComprehension {
                                                    it.variable = newReference("i")
                                                    it.iterable = newReference("someIterable")
                                                    it.predicate =
                                                        newBinaryOperator(">") { op ->
                                                            op.lhs = newReference("i")
                                                            op.rhs =
                                                                newLiteral(5, objectType("int"))
                                                        }
                                                }
                                            }
                                        }
                                    }

                                    block.statements += newReturn {
                                        it.returnValue = newReference("some")
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
                    singleTranslationUnit("File") { tu ->
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("list"))
                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    val iDeclStmt = newDeclarationStatement()
                                    newVariable("i", holder = iDeclStmt)

                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("some", holder = declStmt) {
                                            it.initializer = newCollectionComprehension { cc ->
                                                cc.statement = newReference("i")
                                                cc.comprehensionExpressions += newComprehension {
                                                    it.variable = iDeclStmt
                                                    it.iterable = newReference("someIterable")
                                                    it.predicate =
                                                        newBinaryOperator(">") { op ->
                                                            op.lhs = newReference("i")
                                                            op.rhs =
                                                                newLiteral(5, objectType("int"))
                                                        }
                                                }
                                            }
                                        }
                                    }

                                    block.statements += newReturn {
                                        it.returnValue = newReference("some")
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
                    singleTranslationUnit("File") { tu ->
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("list"))
                            newParameter("argc", objectType("int"), holder = func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    val iDeclStmt = newDeclarationStatement()
                                    newVariable("i", holder = iDeclStmt)
                                    newVariable("y", holder = iDeclStmt)

                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("some", holder = declStmt) {
                                            it.initializer = newCollectionComprehension { cc ->
                                                cc.statement = newReference("i")
                                                cc.comprehensionExpressions += newComprehension {
                                                    it.variable = iDeclStmt
                                                    it.iterable = newReference("someIterable")
                                                    it.predicate =
                                                        newBinaryOperator(">") { op ->
                                                            op.lhs = newReference("i")
                                                            op.rhs =
                                                                newLiteral(5, objectType("int"))
                                                        }
                                                }
                                            }
                                        }
                                    }

                                    block.statements += newReturn {
                                        it.returnValue = newReference("some")
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
