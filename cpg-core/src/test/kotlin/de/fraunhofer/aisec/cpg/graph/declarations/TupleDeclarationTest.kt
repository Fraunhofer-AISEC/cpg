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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.TestUtils.assertInvokes
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TupleDeclarationTest {
    @Test
    fun testTupleDeclaration() {
        with(
            TestLanguageFrontend(
                ctx =
                    TranslationContext(
                        TranslationConfiguration.builder().defaultPasses().build(),
                        ScopeManager(),
                        TypeManager()
                    )
            )
        ) {
            val result = build {
                translationResult {
                    translationUnit {
                        function(
                            "func",
                            returnTypes = listOf(objectType("MyClass"), objectType("error"))
                        )

                        // I fear this is too complex for the fluent DSL; so we just use the node
                        // builder here
                        val tuple =
                            newTupleDeclaration(
                                listOf(newVariableDeclaration("a"), newVariableDeclaration("b")),
                                newCallExpression(newDeclaredReferenceExpression("func"))
                            )
                        scopeManager.addDeclaration(tuple)
                    }
                }
            }

            val tuple = result.tuples["(a,b)"]
            assertNotNull(tuple)
            assertIs<TupleType>(tuple.type)
            assertInvokes(tuple.initializer as? CallExpression, result.functions["func"])
            assertContains(tuple.prevDFG, tuple.initializer!!)

            val a = tuple.elements["a"]
            assertNotNull(a)
            assertLocalName("MyClass", a.type)

            val b = tuple.elements["b"]
            assertNotNull(b)
            assertLocalName("error", b.type)
        }
    }
}
