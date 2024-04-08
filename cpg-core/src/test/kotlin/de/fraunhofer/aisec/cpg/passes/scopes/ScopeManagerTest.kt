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
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
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
        val s1 = ScopeManager()
        val frontend1 =
            TestLanguageFrontend("::", TestLanguage(), TranslationContext(config, s1, tm))
        s1.resetToGlobal(frontend1.newTranslationUnitDeclaration("f1.cpp", null))
        with(frontend1) {
            // build a namespace declaration in f1.cpp with the namespace A
            val namespaceA1 = frontend1.newNamespaceDeclaration("A")
            s1.enterScope(namespaceA1)
            val func1 = frontend1.newFunctionDeclaration("func1")
            s1.addDeclaration(func1)
            s1.leaveScope(namespaceA1)

            val s2 = ScopeManager()
            val frontend2 =
                TestLanguageFrontend("::", TestLanguage(), TranslationContext(config, s2, tm))
            s2.resetToGlobal(frontend2.newTranslationUnitDeclaration("f1.cpp", null))

            // and do the same in the other file
            val namespaceA2 = frontend2.newNamespaceDeclaration("A")
            s2.enterScope(namespaceA2)
            val func2 = frontend2.newFunctionDeclaration("func2")
            s2.addDeclaration(func2)
            s2.leaveScope(namespaceA2)

            // merge the two scopes. this replicates the behaviour of parseParallel
            val final = ScopeManager()
            val frontend =
                TestLanguageFrontend("::", TestLanguage(), TranslationContext(config, final, tm))
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

            // finally, test whether our two namespace declarations are pointing to the same
            // NameScope
            assertEquals(scopeA, final.lookupScope(namespaceA1))
            assertEquals(scopeA, final.lookupScope(namespaceA2))

            // in the final scope manager, the global scope should not be any of the merged scope
            // managers' original global scopes
            assertFalse(listOf(s1, s2).map { it.globalScope }.contains(final.globalScope))

            // resolve symbol
            val call =
                frontend.newCallExpression(frontend.newReference("A::func1"), "A::func1", false)
            val func = final.resolveFunctionLegacy(call).firstOrNull()

            assertEquals(func1, func)
        }
    }

    @Test
    fun testSymbolsFunction() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnitDeclaration("file.cpp")
            ctx.scopeManager.resetToGlobal(tu)

            val func = newFunctionDeclaration("main")
            ctx.scopeManager.addDeclaration(func)
            ctx.scopeManager.enterScope(func)

            // parameter "argc" is scoped to the function
            val argc = newParameterDeclaration("argc")
            ctx.scopeManager.addDeclaration(argc)

            val body = newBlock()
            ctx.scopeManager.enterScope(body)

            val stmt = newDeclarationStatement()
            // variable "a" is scoped to the block scope
            val a = newVariableDeclaration("a")
            stmt.addToPropertyEdgeDeclaration(a)
            ctx.scopeManager.addDeclaration(a)
            body += stmt

            val assign = newAssignExpression()
            assign.lhs = listOf(newReference("a"))
            assign.rhs = listOf(newReference("argc"))
            body += assign

            func.body = body

            ctx.scopeManager.leaveScope(body)
            ctx.scopeManager.leaveScope(func)

            val aRef = tu.refs["a"]
            assertNotNull(aRef)

            var candidates = aRef.scope?.resolveSymbol(aRef.name.localName)
            assertEquals(listOf(a), candidates)

            val argcRef = tu.refs["argc"]
            assertNotNull(argcRef)

            candidates = argcRef.scope?.resolveSymbol(argcRef.name.localName)
            assertEquals(listOf(argc), candidates)
        }
    }

    @Test
    fun testSymbolsRecord() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnitDeclaration("file.cpp")
            ctx.scopeManager.resetToGlobal(tu)

            val myClass = newRecordDeclaration("MyClass", "class")
            ctx.scopeManager.addDeclaration(myClass)
            ctx.scopeManager.enterScope(myClass)

            val doSomething = newMethodDeclaration("doSomething")
            ctx.scopeManager.addDeclaration(doSomething)
            ctx.scopeManager.enterScope(doSomething)
            ctx.scopeManager.leaveScope(doSomething)

            ctx.scopeManager.leaveScope(myClass)

            val func = newFunctionDeclaration("main")
            ctx.scopeManager.addDeclaration(func)
            ctx.scopeManager.enterScope(func)

            // parameter "argc" is scoped to the function
            val argc = newParameterDeclaration("argc")
            ctx.scopeManager.addDeclaration(argc)

            val body = newBlock()
            ctx.scopeManager.enterScope(body)

            val stmt = newDeclarationStatement()
            // variable "a" is scoped to the block scope
            val my = newVariableDeclaration("my", objectType("MyClass"))
            stmt.addToPropertyEdgeDeclaration(my)
            ctx.scopeManager.addDeclaration(my)
            body += stmt

            val memberCall =
                newMemberCallExpression(
                    newMemberExpression("doSomething", newReference("my")),
                )
            body += memberCall

            func.body = body

            ctx.scopeManager.leaveScope(body)
            ctx.scopeManager.leaveScope(func)

            val myRef = tu.refs["my"]
            assertNotNull(myRef)

            var candidates = myRef.scope?.resolveSymbol(myRef.name.localName)
            assertEquals(listOf(my), candidates)

            val doSomethingRef = tu.refs["doSomething"]
            assertNotNull(doSomethingRef)

            candidates =
                ctx.scopeManager.lookupScope(myClass)?.resolveSymbol(doSomethingRef.name.localName)
            assertEquals(listOf(doSomething), candidates)
        }
    }

    @Test
    fun testScopeFQN() {
        val s = ScopeManager()
        val frontend =
            TestLanguageFrontend("::", TestLanguage(), TranslationContext(config, s, TypeManager()))
        s.resetToGlobal(frontend.newTranslationUnitDeclaration("file.cpp", null))

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

        s.leaveScope(namespaceB)
        s.addDeclaration(namespaceB)
        s.leaveScope(namespaceA)

        val scope = s.lookupScope("A::B")
        assertNotNull(scope)
    }
}
