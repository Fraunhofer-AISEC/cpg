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
package de.fraunhofer.aisec.cpg.frontends.csharp.statementhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VariableDeclarationsTest : BaseTest() {

    @Test
    fun testExplicitlyAndImplicitlyTypedVariables() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Variables.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val bar = foo.methods["bar"]
        assertNotNull(bar)
        val barBody = bar.body
        assertIs<Block>(barBody)

        // int a = 1;
        val intDecl = barBody.statements[0]
        assertIs<DeclarationStatement>(intDecl)
        val a = intDecl.singleDeclaration
        assertIs<Variable>(a)
        assertEquals("a", a.name.localName)
        assertEquals("int", a.type.name.localName)
        val aInit = a.initializer
        assertIs<Literal<*>>(aInit)
        assertEquals(1, aInit.value)

        // string b = "2";
        val stringDecl = barBody.statements[1]
        assertIs<DeclarationStatement>(stringDecl)
        val b = stringDecl.singleDeclaration
        assertIs<Variable>(b)
        assertEquals("b", b.name.localName)
        assertEquals("string", b.type.name.localName)
        val bInit = b.initializer
        assertIs<Literal<*>>(bInit)
        assertEquals("2", bInit.value)

        // var c = 5;
        val varIntDecl = barBody.statements[2]
        assertIs<DeclarationStatement>(varIntDecl)
        val c = varIntDecl.singleDeclaration
        assertIs<Variable>(c)
        assertEquals("c", c.name.localName)
        assertEquals("var", c.type.name.localName)
        assertIs<IntegerType>(c.assignedTypes.elementAt(1))

        // var d = "Hello";
        val varStringDecl = barBody.statements[3]
        assertIs<DeclarationStatement>(varStringDecl)
        val d = varStringDecl.singleDeclaration
        assertIs<Variable>(d)
        assertEquals("d", d.name.localName)
        assertEquals("var", d.type.name.localName)
        assertIs<StringType>(d.assignedTypes.elementAt(1))
    }

    @Test
    fun testMultipleDeclarators() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Variables.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        // int a = 1, b = 2, c = 3;
        val multipleDeclarations = foo.methods["multipleDeclarations"]
        assertNotNull(multipleDeclarations)
        val multiBody = multipleDeclarations.body
        assertIs<Block>(multiBody)
        assertEquals(1, multiBody.statements.size)

        val multiStmt = multiBody.statements[0]
        assertIs<DeclarationStatement>(multiStmt)
        assertEquals(3, multiStmt.declarations.size)

        val a = multiStmt.declarations[0]
        assertIs<Variable>(a)
        assertEquals("a", a.name.localName)
        assertIs<IntegerType>(a.type)

        val b = multiStmt.declarations[1]
        assertIs<Variable>(b)
        assertEquals("b", b.name.localName)
        assertIs<IntegerType>(a.type)

        val c = multiStmt.declarations[2]
        assertIs<Variable>(c)
        assertEquals("c", c.name.localName)
        assertIs<IntegerType>(a.type)
    }

    @Test
    fun testWithoutInitializer() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Variables.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val withoutInit = foo.methods["withoutInitializer"]
        assertNotNull(withoutInit)
        val body = withoutInit.body
        assertIs<Block>(body)
        assertEquals(2, body.statements.size)

        // int a;
        val intDecl = body.statements[0]
        assertIs<DeclarationStatement>(intDecl)
        val a = intDecl.singleDeclaration
        assertIs<Variable>(a)
        assertEquals("a", a.name.localName)
        assertIs<IntegerType>(a.type)
        assertNull(a.initializer)

        // string b;
        val stringDecl = body.statements[1]
        assertIs<DeclarationStatement>(stringDecl)
        val b = stringDecl.singleDeclaration
        assertIs<Variable>(b)
        assertEquals("b", b.name.localName)
        assertIs<StringType>(b.type)
        assertNull(b.initializer)
    }

    @Test
    fun testExpressionInitializer() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Variables.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val withInit = foo.methods["withInitializer"]
        assertNotNull(withInit)
        val body = withInit.body
        assertIs<Block>(body)
        assertEquals(2, body.statements.size)

        // int b = a + 2;
        val exprStmt = body.statements[1]
        assertIs<DeclarationStatement>(exprStmt)
        val b = exprStmt.singleDeclaration
        assertIs<Variable>(b)
        assertEquals("b", b.name.localName)
        val bInit = b.initializer
        assertIs<BinaryOperator>(bInit)
        assertEquals("+", bInit.operatorCode)
        assertIs<IntegerType>(b.type)
    }
}
