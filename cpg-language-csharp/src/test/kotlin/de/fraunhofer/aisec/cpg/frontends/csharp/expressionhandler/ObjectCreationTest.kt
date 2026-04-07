/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.ExpressionList
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ObjectCreationTest : BaseTest() {

    @Test
    fun objectCreationTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ObjectCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val bar = tu.records["Bar"]
        assertNotNull(bar)

        val createFooMethod = bar.methods["createFoo"]
        assertNotNull(createFooMethod)
        val body = createFooMethod.body
        assertIs<Block>(body)

        // Foo f = new Foo(1);
        val fVariable = body.variables["f"]
        assertNotNull(fVariable)
        assertEquals("Foo", fVariable.type.name.localName)

        val initializer = fVariable.initializer
        assertNotNull(initializer)
        assertIs<New>(initializer)
        assertEquals("Foo", initializer.type.name.localName)

        val constructCall = initializer.initializer
        assertNotNull(constructCall)
        assertIs<Construction>(constructCall)
        val constructor = constructCall.constructor
        assertNotNull(constructor)
        assertEquals("Foo", constructor.name.localName)

        val args = constructCall.arguments
        assertEquals(1, args.size)
        val firstArg = args.firstOrNull()
        assertNotNull(firstArg)
        assertIs<IntegerType>(firstArg.type)
        assertIs<Literal<*>>(firstArg)
        assertEquals(1, firstArg.value)
    }

    @Test
    fun implicitObjectCreationTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ObjectCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        // class Baz { Foo foo = new(1); }
        val baz = tu.records["Baz"]
        assertNotNull(baz)

        val fooClass = tu.records["Foo"]
        assertNotNull(fooClass)

        val fooField = baz.fields["foo"]
        assertNotNull(fooField)

        val newNode = fooField.initializer
        assertNotNull(newNode)
        assertIs<New>(newNode)
        val constructCall = newNode.initializer
        assertNotNull(constructCall)
        assertIs<Construction>(constructCall)
        val constructor = constructCall.constructor
        assertNotNull(constructor)
        assertEquals("Foo", constructor.name.localName)
        val instantiates = constructCall.instantiates
        assertNotNull(instantiates)
        assertEquals(fooClass, instantiates)

        val args = constructCall.arguments
        assertEquals(1, args.size)
        val firstArg = args.firstOrNull()
        assertNotNull(firstArg)
        assertIs<Literal<*>>(firstArg)
        assertEquals(1, firstArg.value)
    }

    @Test
    fun objectCreationWithInitializerTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ObjectCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        // Foo f = new Foo(1) { x = 2 };
        val fooBarClass = tu.records["FooBar"]
        assertNotNull(fooBarClass)

        val fVariable = fooBarClass.fields["f"]
        assertNotNull(fVariable)

        // The initializer as an ExpressionList
        val exprList = fVariable.initializer
        assertNotNull(exprList)
        assertIs<ExpressionList>(exprList)
        assertEquals(3, exprList.expressions.size)

        // 1. DeclarationStatement with implicit tmp variable: __tmp = new Foo(1)
        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        assertTrue(declStmt.isImplicit)

        val tmpVar = declStmt.declarations.firstOrNull()
        assertNotNull(tmpVar)
        assertTrue(tmpVar.isImplicit)
        assertIs<Variable>(tmpVar)

        val newNode = tmpVar.initializer
        assertNotNull(newNode)
        assertIs<New>(newNode)

        val constructCall = newNode.initializer
        assertNotNull(constructCall)
        assertIs<Construction>(constructCall)
        assertEquals(1, constructCall.arguments.size)
        val constructArg = constructCall.arguments.firstOrNull()
        assertNotNull(constructArg)
        assertIs<Literal<*>>(constructArg)
        assertEquals(1, constructArg.value)

        // 2. Implicit assign: __tmp.x = 2
        val assign = exprList.expressions[1]
        assertIs<Assign>(assign)
        assertTrue(assign.isImplicit)

        val memberAccess = assign.lhs.firstOrNull()
        assertIs<MemberAccess>(memberAccess)
        assertTrue(memberAccess.isImplicit)
        assertEquals("x", memberAccess.name.localName)

        val base = memberAccess.base
        assertIs<Reference>(base)
        assertTrue(base.isImplicit)
        assertRefersTo(base, tmpVar)

        val rhs = assign.rhs.firstOrNull()
        assertIs<Literal<*>>(rhs)
        assertEquals(2, rhs.value)

        // 3. Reference to the tmp variable
        val finalRef = exprList.expressions[2]
        assertIs<Reference>(finalRef)
        assertTrue(finalRef.isImplicit)
        assertRefersTo(finalRef, tmpVar)
    }
}
