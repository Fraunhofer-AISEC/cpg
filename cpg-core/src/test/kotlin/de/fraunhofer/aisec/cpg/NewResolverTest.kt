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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodParameterIn
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.NewResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Test

class NewResolverTest {
    @Test
    fun newResolve() {
        val file = File("src/test/resources/cxx/simple.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerPass(TypeResolver())
                it.registerPass(NewResolver())
            }

        assertNotNull(result)

        val nodes = result.allChildren<Node>()

        val refLocal =
            nodes.filterIsInstance<DeclaredReferenceExpression>().firstOrNull { it.name == "local" }
        assertNotNull(refLocal)

        val local = refLocal.refersTo as? VariableDeclaration
        assertNotNull(local)
        assertEquals("local", local.name)

        val callFoo = nodes.filterIsInstance<CallExpression>().firstOrNull { it.name == "foo" }
        assertNotNull(callFoo)

        val foo = callFoo.invokes.firstOrNull()
        assertNotNull(foo)
        assertEquals("foo", foo.name)
        assertFalse(foo.isInferred)

        val memberCallFoo =
            nodes.filterIsInstance<MemberCallExpression>().firstOrNull { it.name == "foo" }
        assertNotNull(callFoo)

        val base = memberCallFoo?.base
        assertNotNull(base)

        val tFoo = memberCallFoo.invokes.firstOrNull() as? MethodDeclaration
        assertNotNull(tFoo)
        assertEquals("foo", tFoo.name)
        assertFalse(tFoo.isInferred)
    }

    @Test
    fun resolveFunctionParameter() {
        val (sm, resolver) = prepareTest()

        sm.withScope(newFunctionDeclaration("someFunction")) {
            val param =
                newMethodParameterIn("arg0", UnknownType.getUnknownType(), false, lang = sm.lang)
            sm.addDeclaration(param)

            val ref = newDeclaredReferenceExpression("arg0", lang = sm.lang)
            val symbol = resolver.resolve(ref, TranslationUnitDeclaration())

            assertNotNull(symbol)
            assertSame(param, symbol)
        }
    }

    @Test
    fun resolveField() {
        val (sm, resolver) = prepareTest()

        var field: FieldDeclaration
        var superField: Declaration? = null

        sm.resetToGlobal(newTranslationUnitDeclaration("file.cpp"))

        val parentClass =
            sm.declareWithScope(newRecordDeclaration("MySuperClass", "class")) {
                superField = newFieldDeclaration("mySuperField", lang = sm.lang)
                sm.addDeclaration(superField)
            }

        // Normally, the type (hierarchy) resolver will populate this, but as we are in a unit test,
        // we need to do this manually
        val parentType = TypeParser.createFrom("MySuperClass", false) as? ObjectType
        parentType?.recordDeclaration = parentClass

        sm.declareWithScope(newRecordDeclaration("MyClass", "class")) {
            // Establish a small type hierarchy
            this.superClasses = listOf(parentType)
            this.superTypeDeclarations = setOf(parentClass)

            field = newFieldDeclaration("myField", lang = sm.lang)
            sm.addDeclaration(field)

            // Normally, the type (hierarchy) resolver will populate this, but as we are in a unit
            // test, we need to do this manually
            val type = TypeParser.createFrom("MyClass", false) as? ObjectType
            type?.recordDeclaration = this

            // Resolve a field directly in MyClass
            var ref =
                newMemberExpression(
                    newDeclaredReferenceExpression("this", type, lang = sm.lang),
                    UnknownType.getUnknownType(),
                    "myField",
                    operatorCode = "->",
                    lang = sm.lang
                )
            var symbol = resolver.resolve(ref, TranslationUnitDeclaration())

            assertNotNull(symbol)
            assertSame(field, symbol)

            // Resolve a field in MyClass's super class
            ref =
                newMemberExpression(
                    newDeclaredReferenceExpression("this", type, lang = sm.lang),
                    UnknownType.getUnknownType(),
                    "mySuperField",
                    operatorCode = "->",
                    lang = sm.lang
                )
            symbol = resolver.resolve(ref, TranslationUnitDeclaration())

            assertNotNull(symbol)
            assertSame(superField, symbol)
        }
    }

    /**
     * Prepares a resolver test by initializing all the necessary classes that we need, such as a
     * scope manager, a language frontend and the resolver itself.
     */
    private fun prepareTest(): Pair<ScopeManager, NewResolver> {
        // Build a new scope manager and resolver
        val sm = ScopeManager()
        val resolver = NewResolver()

        // We need a language frontend. Ideally one that has a lot of scoping, such as C++
        sm.lang = CXXLanguageFrontend(TranslationConfiguration.builder().build(), sm)
        resolver.lang = sm.lang

        // Type manager NEEDS to be informed
        TypeManager.getInstance().setLanguageFrontend(sm.lang as CXXLanguageFrontend)

        // Return the prepared pair
        return Pair(sm, resolver)
    }
}
