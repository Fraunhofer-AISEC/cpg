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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.cpp.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import java.io.File
import kotlin.test.*

// TODO(oxisto): Use TestLanguage instead of CPPLanguage/JavaLanguage
internal class ScopeManagerTest : BaseTest() {
    private lateinit var config: TranslationConfiguration

    @BeforeTest
    fun setUp() {
        config = TranslationConfiguration.builder().defaultPasses().build()
    }

    @Test
    @Throws(TranslationException::class)
    fun testSetScope() {
        val frontend: LanguageFrontend = CXXLanguageFrontend(CPPLanguage(), config, ScopeManager())
        assertEquals(frontend, frontend.scopeManager.lang)

        frontend.scopeManager = ScopeManager()
        assertEquals(frontend, frontend.scopeManager.lang)
    }

    @Test
    @Throws(TranslationException::class)
    fun testReplaceNode() {
        val scopeManager = ScopeManager()
        val frontend = CXXLanguageFrontend(CPPLanguage(), config, scopeManager)
        val tu = frontend.parse(File("src/test/resources/cxx/recordstmt.cpp"))
        val methods = tu.allChildren<MethodDeclaration>().filter { it !is ConstructorDeclaration }
        assertFalse(methods.isEmpty())

        methods.forEach {
            val scope = scopeManager.lookupScope(it)
            assertSame(it, scope!!.astNode)
        }

        val constructors = tu.allChildren<ConstructorDeclaration>()
        assertFalse(constructors.isEmpty())

        // make sure that the scope of the constructor actually has the constructor as an ast node.
        // this is necessary, since the constructor was probably created as a function declaration
        // which later gets 'upgraded' to a constructor declaration.
        constructors.forEach {
            val scope = scopeManager.lookupScope(it)
            assertSame(it, scope!!.astNode)
        }
    }

    @Test
    fun testMerge() {
        val s1 = ScopeManager()
        val frontend1 =
            CXXLanguageFrontend(
                CPPLanguage(),
                TranslationConfiguration.builder().build(),
                s1,
            )
        s1.resetToGlobal(frontend1.newTranslationUnitDeclaration("f1.cpp", null))

        // build a namespace declaration in f1.cpp with the namespace A
        val namespaceA1 = frontend1.newNamespaceDeclaration("A", null)
        s1.enterScope(namespaceA1)
        val func1 = frontend1.newFunctionDeclaration("func1", null)
        s1.addDeclaration(func1)
        s1.leaveScope(namespaceA1)

        val s2 = ScopeManager()
        val frontend2 =
            CXXLanguageFrontend(
                CPPLanguage(),
                TranslationConfiguration.builder().build(),
                s2,
            )
        s2.resetToGlobal(frontend2.newTranslationUnitDeclaration("f1.cpp", null))

        // and do the same in the other file
        val namespaceA2 = frontend2.newNamespaceDeclaration("A", null)
        s2.enterScope(namespaceA2)
        val func2 = frontend2.newFunctionDeclaration("func2", null)
        s2.addDeclaration(func2)
        s2.leaveScope(namespaceA2)

        // merge the two scopes. this replicates the behaviour of parseParallel
        val final = ScopeManager()
        val frontend =
            CXXLanguageFrontend(
                CPPLanguage(),
                TranslationConfiguration.builder().build(),
                final,
            )
        final.mergeFrom(listOf(s1, s2))

        // in the final scope manager, there should only be one NameScope "A"
        val scopes = final.filterScopes { it.name.toString() == "A" }
        assertEquals(1, scopes.size)

        val scopeA = scopes.firstOrNull() as? NameScope
        assertNotNull(scopeA)

        // should also be able to look up via the FQN
        assertEquals(scopeA, final.lookupScope("A"))

        // and it should contain both functions from the different file in the same namespace
        assertTrue(scopeA.valueDeclarations.contains(func1))
        assertTrue(scopeA.valueDeclarations.contains(func2))

        // finally, test whether our two namespace declarations are pointing to the same NameScope
        assertEquals(scopeA, final.lookupScope(namespaceA1))
        assertEquals(scopeA, final.lookupScope(namespaceA2))

        // resolve symbol
        val call =
            frontend.newCallExpression(
                frontend.newDeclaredReferenceExpression("A::func1"),
                "A::func1",
                null,
                false
            )
        val func = final.resolveFunction(call).firstOrNull()

        assertEquals(func1, func)
    }

    @Test
    fun testScopeFQN() {
        val s = ScopeManager()
        val frontend =
            CXXLanguageFrontend(
                CPPLanguage(),
                TranslationConfiguration.builder().build(),
                s,
            )
        s.resetToGlobal(frontend.newTranslationUnitDeclaration("file.cpp", null))

        assertNull(s.currentNamespace)

        val namespaceA = frontend.newNamespaceDeclaration("A", null)
        s.enterScope(namespaceA)

        assertEquals("A", s.currentNamespace.toString())

        // nested namespace A::B
        val namespaceB = frontend.newNamespaceDeclaration("B", null)
        s.enterScope(namespaceB)

        assertEquals("A::B", s.currentNamespace.toString())

        val func = frontend.newFunctionDeclaration("func", null)
        s.addDeclaration(func)

        s.leaveScope(namespaceB)
        s.addDeclaration(namespaceB)
        s.leaveScope(namespaceA)

        val scope = s.lookupScope("A::B")
        assertNotNull(scope)
    }
}
