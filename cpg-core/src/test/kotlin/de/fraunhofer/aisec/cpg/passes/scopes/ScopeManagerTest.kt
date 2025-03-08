/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

internal class ScopeManagerTest : BaseTest() {
    private lateinit var config: TranslationConfiguration

    @BeforeTest
    fun setUp() {
        config = TranslationConfiguration.builder().defaultPasses().build()
    }

    @Test
    fun testMerge() {
        val tm = TypeManager()

        val rootCtx = TranslationContext(config, tm)
        val language = TestLanguageWithColon(rootCtx)

        val ctx1 = TranslationContext(config, tm)
        val s1 = ctx1.scopeManager

        val frontend1 = TestLanguageFrontend(ctx1, language)
        val (func1, namespaceA1) =
            with(frontend1) {
                val tu1 = frontend1.newTranslationUnitDeclaration("f1.cpp", null)
                s1.resetToGlobal(tu1)

                // build a namespace declaration in f1.cpp with the namespace A
                val namespaceA1 = frontend1.newNamespaceDeclaration("A")
                s1.enterScope(namespaceA1)

                val func1 = frontend1.newFunctionDeclaration("func1")
                s1.addDeclaration(func1)
                namespaceA1.declarations += func1

                s1.leaveScope(namespaceA1)
                s1.addDeclaration(namespaceA1)
                tu1.declarations += namespaceA1
                Pair(func1, namespaceA1)
            }

        val ctx2 = TranslationContext(config, tm)
        val s2 = ctx2.scopeManager
        val frontend2 = TestLanguageFrontend(ctx2, language)
        val (func2, namespaceA2) =
            with(frontend2) {
                val tu2 = frontend2.newTranslationUnitDeclaration("f1.cpp", null)
                s2.resetToGlobal(tu2)

                // and do the same in the other file
                val namespaceA2 = frontend2.newNamespaceDeclaration("A")
                s2.enterScope(namespaceA2)

                val func2 = frontend2.newFunctionDeclaration("func2")
                s2.addDeclaration(func2)
                namespaceA2.declarations += func2

                s2.leaveScope(namespaceA2)
                s2.addDeclaration(namespaceA2)
                tu2.declarations += namespaceA2
                Pair(func2, namespaceA2)
            }

        // merge the two scopes. this replicates the behaviour of parseParallel
        val final = rootCtx.scopeManager
        final.mergeFrom(listOf(s1, s2))

        // in the final scope manager, there should only be one NameScope "A"
        val scopes = final.filterScopes { it.name.toString() == "A" }
        assertEquals(1, scopes.size)

        val scopeA = scopes.firstOrNull() as? NameScope
        assertNotNull(scopeA)

        // should also be able to look up via the FQN
        assertEquals(scopeA, final.lookupScope(parseName("A", delimiter = "::")))

        // and it should contain both functions from the different file in the same namespace
        assertContains(scopeA.symbols["func1"] ?: listOf(), func1)
        assertContains(scopeA.symbols["func2"] ?: listOf(), func2)

        // finally, test whether our two namespace declarations are pointing to the same
        // NameScope
        assertEquals(scopeA, final.lookupScope(namespaceA1))
        assertEquals(scopeA, final.lookupScope(namespaceA2))

        // in the final scope manager, the global scope should not be any of the merged scope
        // managers' original global scopes
        assertNotSame(s1.globalScope, final.globalScope)
        assertNotSame(s2.globalScope, final.globalScope)

        // resolve symbol
        val func =
            final
                .lookupSymbolByName(
                    name = parseName("A::func1", delimiter = "::"),
                    language = language,
                    startScope = final.globalScope,
                )
                .firstOrNull()

        assertEquals(func1, func)
    }

    @Test
    fun testScopeFQN() {
        val ctx = TranslationContext(config)
        val s = ctx.scopeManager
        val frontend = TestLanguageFrontend(ctx, TestLanguageWithColon(ctx))
        with(frontend) {
            val tu = frontend.newTranslationUnitDeclaration("file.cpp", null)
            s.resetToGlobal(tu)

            assertNull(s.currentNamespace)

            val namespaceA = frontend.newNamespaceDeclaration("A", null)
            s.enterScope(namespaceA)

            assertEquals("A", s.currentNamespace.toString())

            // nested namespace A::B
            val namespaceB = frontend.newNamespaceDeclaration("B", null)
            s.enterScope(namespaceB)

            assertEquals("A::B", s.currentNamespace.toString())

            val func = frontend.newFunctionDeclaration("func")
            s.addDeclaration(func)
            tu.declarations += func

            s.leaveScope(namespaceB)
            s.addDeclaration(namespaceB)
            namespaceA.declarations += namespaceB

            s.leaveScope(namespaceA)

            val scope = s.lookupScope(parseName("A::B"))
            assertNotNull(scope)
        }
    }

    @Test
    fun testMatchesSignature() {
        val frontend = TestLanguageFrontend(TranslationContext(config))
        with(frontend) {
            val method =
                newMethodDeclaration("testMethod").apply {
                    parameters =
                        mutableListOf(
                            newParameterDeclaration("x", primitiveType("string")),
                            newParameterDeclaration("y", primitiveType("boolean")).apply {
                                default = newLiteral(true, primitiveType("boolean"))
                            },
                            newParameterDeclaration("kwargs", primitiveType("string")).apply {
                                isVariadic = true
                            },
                        )
                }

            // First test: Matching with a single argument and default param
            val arguments =
                listOf(
                    newLiteral("test", primitiveType("string")),
                    newLiteral(false, primitiveType("boolean")),
                )

            val matchingSignature = listOf(primitiveType("string"), primitiveType("boolean"))
            val result =
                method.matchesSignature(matchingSignature, arguments, useDefaultArguments = true)
            assertIs<SignatureMatches>(result, "Function should match with default and kwargs")

            // Second test: Matching with multiple kwargs
            val arguments2 =
                listOf(
                    newLiteral("test", primitiveType("string")),
                    newLiteral(false, primitiveType("boolean")),
                    newLiteral("kwargs[0]", primitiveType("string")),
                    newLiteral("kwargs[1]", primitiveType("string")),
                )

            val matchingSignature2 =
                listOf(
                    primitiveType("string"),
                    primitiveType("boolean"),
                    primitiveType("string"),
                    primitiveType("string"),
                )

            val result2 =
                method.matchesSignature(matchingSignature2, arguments2, useDefaultArguments = true)

            assertIs<SignatureMatches>(result2, "Function should match with multiple kwargs")
        }
    }
}
