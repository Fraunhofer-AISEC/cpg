/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.applyWithScope
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.problems
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.BaseTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class ExtensionsTest : BaseTest() {
    val problemDeclText = "This is a problem declaration."
    val problemExprText = "This is a problem expression."

    fun getTranslationResultWithProblems(
        config: TranslationConfiguration =
            TranslationConfiguration.builder()
                .defaultPasses()
                .registerLanguage<TestLanguage>()
                .build()
    ) =
        testFrontend(config).build {
            singleTranslationUnit("foo.bar") { tu ->
                newFunction("foo", holder = tu, enterScope = true) { func ->
                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newDeclarationStatement { declStmt ->
                                newProblemDeclaration(
                                    problem = problemDeclText,
                                    problemType = ProblemNode.ProblemType.TRANSLATION,
                                    holder = declStmt,
                                )
                            }
                        }
                    func.additionalProblems += ProblemExpression(problemExprText)
                }
            }
        }

    @Test
    fun testProblemsExtension() {
        val test = getTranslationResultWithProblems()
        assertNotNull(test)
        assertEquals(2, test.problems.size, "Expected two problems.")
        assertNotNull(
            test.problems.filter { it.problem == problemDeclText },
            "Failed to find the problem declaration.",
        )
        assertNotNull(
            test.problems.filter { it.problem == problemExprText },
            "Failed to find the problem expression.",
        )
    }

    @Test
    fun testApplyWithScope() {
        with(TestLanguageFrontend()) {
            val collectionComprehension =
                newCollectionComprehension().applyWithScope {
                    this.statement = newDeclarationStatement { it.addDeclaration(newVariable("a")) }
                }
            val varA = collectionComprehension.variables["a"]
            assertIs<Variable>(varA)
            assertIs<GlobalScope>(varA.scope)
        }
    }
}
