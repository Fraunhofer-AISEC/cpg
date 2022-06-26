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
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import java.io.File
import kotlin.test.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ScopeManagerTest : BaseTest() {
    private lateinit var config: TranslationConfiguration

    @BeforeTest
    fun setUp() {
        config = TranslationConfiguration.builder().defaultPasses().build()
    }

    @Test
    @Throws(TranslationException::class)
    fun testSetScope() {
        val frontend: LanguageFrontend = JavaLanguageFrontend(config, ScopeManager())
        assertEquals(frontend, frontend.scopeManager.lang)

        frontend.scopeManager = ScopeManager()
        assertEquals(frontend, frontend.scopeManager.lang)
    }

    @Test
    @Throws(TranslationException::class)
    fun testReplaceNode() {
        val scopeManager = ScopeManager()
        val frontend = CXXLanguageFrontend(config, scopeManager)
        val tu = frontend.parse(File("src/test/resources/recordstmt.cpp"))
        val methods =
            flattenListIsInstance<MethodDeclaration>(tu.declarations).filter {
                it !is ConstructorDeclaration
            }
        assertFalse(methods.isEmpty())

        methods.forEach {
            val scope = scopeManager.lookupScope(it)
            assertSame(it, scope!!.getAstNode())
        }

        val constructors = flattenListIsInstance<ConstructorDeclaration>(tu.declarations)
        assertFalse(constructors.isEmpty())

        // make sure that the scope of the constructor actually has the constructor as an ast node.
        // this is necessary, since the constructor was probably created as a function declaration
        // which later gets 'upgraded' to a constructor declaration.
        constructors.forEach {
            val scope = scopeManager.lookupScope(it)
            assertSame(it, scope!!.getAstNode())
        }
    }

    @Test
    fun testMerge() {
        val s1 = ScopeManager()
        CXXLanguageFrontend(TranslationConfiguration.builder().build(), s1)
        s1.resetToGlobal(NodeBuilder.newTranslationUnitDeclaration("f1.cpp", null))

        // build a namespace declaration in f1.cpp with the namespace A
        val namespaceA1 = NodeBuilder.newNamespaceDeclaration("A", null)
        s1.enterScope(namespaceA1)
        val func1 = NodeBuilder.newFunctionDeclaration("func1", null)
        s1.addDeclaration(func1)
        s1.leaveScope(namespaceA1)

        val s2 = ScopeManager()
        CXXLanguageFrontend(TranslationConfiguration.builder().build(), s2)
        s2.resetToGlobal(NodeBuilder.newTranslationUnitDeclaration("f1.cpp", null))

        // and do the same in the other file
        val namespaceA2 = NodeBuilder.newNamespaceDeclaration("A", null)
        s2.enterScope(namespaceA2)
        val func2 = NodeBuilder.newFunctionDeclaration("func2", null)
        s2.addDeclaration(func2)
        s2.leaveScope(namespaceA2)

        // merge the two scopes. this replicates the behaviour of parseParallel
        val final = ScopeManager()
        CXXLanguageFrontend(TranslationConfiguration.builder().build(), final)
        final.mergeFrom(listOf(s1, s2))

        // in the final scope manager, the should only be one NameScope "A"
        val scopes = final.filterScopes { it.scopedName == "A" }
        assertEquals(1, scopes.size)

        val scopeA = scopes.firstOrNull() as? NameScope
        assertNotNull(scopeA)

        // should also be able to look-up via the FQN
        assertEquals(scopeA, final.lookupScope("A"))

        // and it should contain both functions from the different file in the same namespace
        assertTrue(scopeA.valueDeclarations.contains(func1))
        assertTrue(scopeA.valueDeclarations.contains(func2))

        // finally, test whether our two namespace declarations are pointing to the same NameScope
        assertEquals(scopeA, final.lookupScope(namespaceA1))
        assertEquals(scopeA, final.lookupScope(namespaceA2))

        // resolve symbol
        val call = NodeBuilder.newCallExpression("func1", "A::func1", null, false)
        val func = final.resolveFunction(call).firstOrNull()

        assertEquals(func1, func)
    }

    @Test
    fun testScopeFQN() {
        val s = ScopeManager()
        CXXLanguageFrontend(TranslationConfiguration.builder().build(), s)
        s.resetToGlobal(NodeBuilder.newTranslationUnitDeclaration("file.cpp", null))

        assertEquals("", s.currentNamePrefix)

        val namespaceA = NodeBuilder.newNamespaceDeclaration("A", null)
        s.enterScope(namespaceA)

        assertEquals("A", s.currentNamePrefix)

        // nested namespace A::B. the name needs to be a FQN
        val namespaceB = NodeBuilder.newNamespaceDeclaration("A::B", null)
        s.enterScope(namespaceB)

        assertEquals("A::B", s.currentNamePrefix)

        val func = NodeBuilder.newFunctionDeclaration("func", null)
        s.addDeclaration(func)

        s.leaveScope(namespaceB)
        s.addDeclaration(namespaceB)
        s.leaveScope(namespaceA)

        val scope = s.lookupScope("A::B")
        assertNotNull(scope)
    }
}
