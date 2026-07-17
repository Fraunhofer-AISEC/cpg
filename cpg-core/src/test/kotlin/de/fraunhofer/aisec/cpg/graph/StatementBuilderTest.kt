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
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class StatementBuilderTest {
    @Test
    fun testNewLookupScope() {
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

                            newVariable("a", holder = tu)

                            newFunction("main", holder = tu, enterScope = true) { func ->
                                func.body =
                                    newBlock(enterScope = true) { body ->
                                        val stmt = newDeclarationStatement()
                                        newVariable("a", holder = stmt)
                                        body.statements += stmt

                                        body.statements +=
                                            newLookupScope(listOf("a"), scopeManager.globalScope)
                                        body.statements += newReference("a")
                                    }
                            }

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
