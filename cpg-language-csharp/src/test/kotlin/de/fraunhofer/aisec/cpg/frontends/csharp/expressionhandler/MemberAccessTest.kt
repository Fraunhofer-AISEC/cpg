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
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MemberAccessTest : BaseTest() {

    @Test
    fun simpleMemberAccessTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("MemberAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val method = foo.methods["simpleMemberAccess"]
        assertNotNull(method)
        val objParam = method.parameters["obj"]
        assertNotNull(objParam)
        val body = method.body
        assertIs<Block>(body)

        // int a = obj.b;
        val declStmt = body.statements.firstOrNull()
        assertIs<DeclarationStatement>(declStmt)
        val decl = declStmt.singleDeclaration
        assertIs<Variable>(decl)

        val memberAccess = decl.initializer
        assertIs<MemberAccess>(memberAccess)
        assertEquals("b", memberAccess.name.localName)
        assertEquals(".", memberAccess.operatorCode)

        val bar = tu.records["Bar"]
        assertNotNull(bar)
        val fieldB = bar.fields["b"]
        assertNotNull(fieldB)
        assertEquals(fieldB, memberAccess.refersTo)

        val base = memberAccess.base
        assertIs<Reference>(base)
        assertEquals(objParam, base.refersTo)
    }

    @Test
    fun thisMemberAccessTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("MemberAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val method = foo.methods["thisMemberAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // int a = this.x;
        val declStmt = body.statements.firstOrNull()
        assertIs<DeclarationStatement>(declStmt)
        val decl = declStmt.singleDeclaration
        assertIs<Variable>(decl)

        val memberAccess = decl.initializer
        assertIs<MemberAccess>(memberAccess)
        assertEquals("x", memberAccess.name.localName)
        assertEquals(".", memberAccess.operatorCode)

        val base = memberAccess.base
        assertIs<Reference>(base)
        assertEquals("this", base.name.localName)
        assertEquals(foo.toType(), base.type)
        assertNotNull(method.receiver)
        assertEquals(method.receiver, base.refersTo)
    }

    @Test
    fun thisFieldAccessTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("MemberAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val fieldX = foo.fields["x"]
        assertNotNull(fieldX)

        val method = foo.methods["thisFieldAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // this.x = 42;
        val assign = body.statements.firstOrNull()
        assertIs<Assign>(assign)

        val lhs = assign.lhs.singleOrNull()
        assertIs<MemberAccess>(lhs)
        assertEquals("x", lhs.name.localName)
        assertEquals(fieldX, lhs.refersTo)

        val base = lhs.base
        assertIs<Reference>(base)
        assertEquals("this", base.name.localName)
        assertEquals(method.receiver, base.refersTo)
    }
}
