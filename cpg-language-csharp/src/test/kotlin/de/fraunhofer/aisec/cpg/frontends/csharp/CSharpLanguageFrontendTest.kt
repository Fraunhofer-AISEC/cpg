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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Constructor
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CSharpLanguageFrontendTest : BaseTest() {

    @Test
    fun testNamespaces() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Namespaces.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.namespaces["Foo"]
        assertNotNull(foo)

        val bar = foo.namespaces["Bar"]
        assertNotNull(bar)

        val baz = bar.records["Baz"]
        assertNotNull(baz)

        val dottedNameSpace = tu.namespaces["Dotted.NameSpace"]
        assertNotNull(dottedNameSpace)
    }

    @Test
    fun testFileScopedNamespace() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("FileScopedNamespace.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val ns = tu.namespaces["HelloWorld"]
        assertNotNull(ns)

        val foo = ns.records["Foo"]
        assertNotNull(foo)

        assertEquals("bar", foo.fields["bar"]?.name?.localName)
    }

    @Test
    fun testFieldDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Fields.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
    }

    @Test
    fun testMethodDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Methods.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.namespaces["HelloWorld"]?.records["Foo"]
        assertNotNull(foo)

        val bar = foo.methods["Bar"]
        assertNotNull(bar)
        assertEquals(0, bar.parameters.size)

        val baz = foo.methods["Baz"]
        assertNotNull(baz)
        assertEquals(2, baz.parameters.size)
        assertEquals("a", baz.parameters[0].name.localName)
        assertEquals("b", baz.parameters[1].name.localName)
    }

    @Test
    fun testConstructorDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Constructor.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.namespaces["HelloWorld"]?.records["Foo"]
        assertNotNull(foo)

        val constructors = foo.constructors
        assertEquals(2, constructors.size)

        val noParameter = constructors.single { it.parameters.isEmpty() }
        assertNotNull(noParameter)
        assertIs<Constructor>(noParameter)

        val twoParameters = constructors.single { it.parameters.size == 2 }
        assertNotNull(twoParameters)
        assertIs<Constructor>(twoParameters)
        assertEquals("x", twoParameters.parameters[0].name.localName)
        assertEquals("y", twoParameters.parameters[1].name.localName)
    }

    @Test
    fun testReturnStatement() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Methods.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.namespaces["HelloWorld"]?.records["Foo"]
        assertNotNull(foo)

        // return 1;
        val returnSomething = foo.methods["returnSomething"]
        assertNotNull(returnSomething)
        val body = returnSomething.body
        assertIs<Block>(body)
        assertEquals(1, body.statements.size)

        val returnStmt = body.statements.first()
        assertIs<Return>(returnStmt)

        val literal = returnStmt.returnValue
        assertIs<Literal<*>>(literal)
        assertEquals(1, literal.value)

        // return;
        val returnWithout = foo.methods["returnWithoutExpression"]
        assertNotNull(returnWithout)
        val body2 = returnWithout.body
        assertIs<Block>(body2)

        val returnStmt2 = body2.statements.first()
        assertIs<Return>(returnStmt2)
        assertNull(returnStmt2.returnValue)
    }

    @Test
    fun testLiteralExpressionsTypes() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Literals.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        // int
        val returnInt = foo.methods["returnInt"]
        assertNotNull(returnInt)
        val intReturn = (returnInt.body as Block).statements.first()
        assertIs<Return>(intReturn)
        val intLiteral = intReturn.returnValue
        assertIs<Literal<*>>(intLiteral)
        assertEquals(42, intLiteral.value)
        assertEquals("int", intLiteral.type.name.localName)

        // string
        val returnString = foo.methods["returnString"]
        assertNotNull(returnString)
        val stringReturn = (returnString.body as Block).statements.first()
        assertIs<Return>(stringReturn)
        val stringLiteral = stringReturn.returnValue
        assertIs<Literal<*>>(stringLiteral)
        assertEquals("hello", stringLiteral.value)
        assertEquals("string", stringLiteral.type.name.localName)

        // bool true
        val returnTrue = foo.methods["returnTrue"]
        assertNotNull(returnTrue)
        val trueReturn = (returnTrue.body as Block).statements.first()
        assertIs<Return>(trueReturn)
        val trueLiteral = trueReturn.returnValue
        assertIs<Literal<*>>(trueLiteral)
        assertEquals(true, trueLiteral.value)
        assertEquals("bool", trueLiteral.type.name.localName)

        // bool false
        val returnFalse = foo.methods["returnFalse"]
        assertNotNull(returnFalse)
        val falseReturn = (returnFalse.body as Block).statements.first()
        assertIs<Return>(falseReturn)
        val falseLiteral = falseReturn.returnValue
        assertIs<Literal<*>>(falseLiteral)
        assertEquals(false, falseLiteral.value)
        assertEquals("bool", falseLiteral.type.name.localName)

        // char
        val returnChar = foo.methods["returnChar"]
        assertNotNull(returnChar)
        val charReturn = (returnChar.body as Block).statements.first()
        assertIs<Return>(charReturn)
        val charLiteral = charReturn.returnValue
        assertIs<Literal<*>>(charLiteral)
        assertEquals('a', charLiteral.value)
        assertEquals("char", charLiteral.type.name.localName)
    }

    @Test
    fun testIfStatement() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IfStatements.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val bar = tu.records["Bar"]
        assertNotNull(bar)

        // if without else
        val doIf = bar.methods["doIf"]
        assertNotNull(doIf)
        val doIfBody = doIf.body
        assertIs<Block>(doIfBody)

        val ifStmt = doIfBody.statements[0]
        assertIs<IfElse>(ifStmt)

        val condition = ifStmt.condition
        assertIs<BinaryOperator>(condition)
        assertEquals("<", condition.operatorCode)
        assertIs<Reference>(condition.lhs)
        assertEquals("a", condition.lhs.name.localName)
        assertIs<Literal<*>>(condition.rhs)
        assertEquals(10, (condition.rhs as Literal<*>).value)

        val thenBlock = ifStmt.thenStatement
        assertIs<Block>(thenBlock)
        assertIs<Return>(thenBlock.statements.firstOrNull())

        assertNull(ifStmt.elseStatement)

        // if-else
        val doIfElse = bar.methods["doIfElse"]
        assertNotNull(doIfElse)
        val doIfElseBody = doIfElse.body
        assertIs<Block>(doIfElseBody)

        val ifElseStmt = doIfElseBody.statements.firstOrNull()
        assertIs<IfElse>(ifElseStmt)
        assertNotNull(ifElseStmt.thenStatement)
        assertNotNull(ifElseStmt.elseStatement)
        assertIs<Block>(ifElseStmt.elseStatement)

        // if-else if-else
        val doIfElseIf = bar.methods["doIfElseIf"]
        assertNotNull(doIfElseIf)
        val doIfElseIfBody = doIfElseIf.body
        assertIs<Block>(doIfElseIfBody)

        val outerIf = doIfElseIfBody.statements.firstOrNull()
        assertIs<IfElse>(outerIf)
        assertNotNull(outerIf.thenStatement)

        // else if is modeled as a nested IfElse in the elseStatement
        val innerIf = outerIf.elseStatement
        assertIs<IfElse>(innerIf)
        assertNotNull(innerIf.thenStatement)
        assertNotNull(innerIf.elseStatement)
    }
}
