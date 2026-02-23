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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class StatementBuilderTest {
    @Test
    fun testNewLookupScopeStatement() {
        val frontend =
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        val result =
            frontend.build {
                translationResult {
                    var tu =
                        with(frontend) {
                            var tu = newTranslationUnit("main.file")
                            scopeManager.resetToGlobal(tu)

                            var globalA = newVariable("a")
                            scopeManager.addDeclaration(globalA)
                            tu.declarations += globalA

                            var func = newFunction("main")
                            scopeManager.enterScope(func)

                            var body = newBlock()
                            scopeManager.enterScope(body)

                            var localA = newVariable("a")
                            var stmt = newDeclarationStatement()
                            stmt.declarations += localA
                            scopeManager.addDeclaration(localA)
                            body += stmt

                            body += newLookupScopeStatement(listOf("a"), scopeManager.globalScope)
                            body += newReference("a")

                            scopeManager.leaveScope(body)
                            func.body = body
                            scopeManager.leaveScope(func)

                            scopeManager.addDeclaration(func)
                            tu.declarations += func

                            scopeManager.leaveScope(tu)
                            tu
                        }

                    components.firstOrNull()?.translationUnits?.add(tu)
                }
            }

        val globalA = result.variables["a"]
        assertNotNull(globalA)
        assertIs<GlobalScope>(globalA.scope)

        val a = result.refs["a"]
        assertRefersTo(a, globalA)
    }
}
