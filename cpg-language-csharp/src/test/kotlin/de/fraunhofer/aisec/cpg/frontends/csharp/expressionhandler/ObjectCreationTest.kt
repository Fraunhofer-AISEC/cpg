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

        val exprList = fVariable.initializer
        assertNotNull(exprList)
        assertIs<ExpressionList>(exprList)
        assertEquals(3, exprList.expressions.size)

        // DeclarationStatement with implicit tmp variable: __tmp = new Foo(1)
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

        // Implicit assign: __tmp.x = 2
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

        // Reference to tmp variable
        val ref = exprList.expressions[2]
        assertIs<Reference>(ref)
        assertTrue(ref.isImplicit)
        assertRefersTo(ref, tmpVar)
    }

    @Test
    fun nestedObjectCreationWithInitializerTest() {
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

        /**
         * Rectangle r = new Rectangle { P1 = new Point { X = 0, Y = 1 }, P2 = new Point { X = 2, Y
         * = 3 } } };
         */
        val rectangleClass = tu.records["Rectangle"]
        assertNotNull(rectangleClass)

        val rVariable = tu.variables["r"]
        assertNotNull(rVariable)

        val exprList = rVariable.initializer
        assertNotNull(exprList)
        assertIs<ExpressionList>(exprList)
        assertEquals(4, exprList.expressions.size)

        // DeclarationStatement: tmpRectangle = new Rectangle()
        val outerDeclStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(outerDeclStmt)
        val outerTmpVar = outerDeclStmt.declarations.firstOrNull()
        assertNotNull(outerTmpVar)
        assertIs<Variable>(outerTmpVar)
        val outerNew = outerTmpVar.initializer
        assertNotNull(outerNew)
        assertIs<New>(outerNew)

        // Assign: tmpRectangle.P1 = ExpressionList(tmpPoint = new Point(), tmpPoint.X = 0,
        // tmpPoint.Y = 1, tmpPoint)
        val p1Assign = exprList.expressions[1]
        assertIs<Assign>(p1Assign)
        val p1MemberAccess = p1Assign.lhs.firstOrNull()
        assertIs<MemberAccess>(p1MemberAccess)
        assertRefersTo(p1MemberAccess.base, outerTmpVar)

        val p1Rhs = p1Assign.rhs.firstOrNull()
        assertNotNull(p1Rhs)
        assertIs<ExpressionList>(p1Rhs)
        assertEquals(4, p1Rhs.expressions.size)

        // Nested: tmpPoint = new Point()
        val p1DeclStmt = p1Rhs.expressions[0]
        assertIs<DeclarationStatement>(p1DeclStmt)
        val p1TmpVar = p1DeclStmt.declarations.firstOrNull()
        assertNotNull(p1TmpVar)
        assertIs<Variable>(p1TmpVar)
        val p1New = p1TmpVar.initializer
        assertNotNull(p1New)
        assertIs<New>(p1New)

        // Nested: tmpPoint.X = 0
        val xAssign = p1Rhs.expressions[1]
        assertIs<Assign>(xAssign)
        val xMemberAccess = xAssign.lhs.firstOrNull()
        assertIs<MemberAccess>(xMemberAccess)
        assertEquals("X", xMemberAccess.name.localName)
        val xRhs = xAssign.rhs.firstOrNull()
        assertIs<Literal<*>>(xRhs)
        assertEquals(0, xRhs.value)

        // Nested: tmpPoint.Y = 1
        val yAssign = p1Rhs.expressions[2]
        assertIs<Assign>(yAssign)
        val yMemberAccess = yAssign.lhs.firstOrNull()
        assertIs<MemberAccess>(yMemberAccess)
        assertEquals("Y", yMemberAccess.name.localName)
        val yRhs = yAssign.rhs.firstOrNull()
        assertIs<Literal<*>>(yRhs)
        assertEquals(1, yRhs.value)

        val p1Ref = p1Rhs.expressions[3]
        assertIs<Reference>(p1Ref)
        assertRefersTo(p1Ref, p1TmpVar)

        // Assign: tmpRectangle.P2 = ExpressionList()
        val p2Assign = exprList.expressions[2]
        assertIs<Assign>(p2Assign)
        val p2MemberAccess = p2Assign.lhs.firstOrNull()
        assertIs<MemberAccess>(p2MemberAccess)
        assertEquals("P2", p2MemberAccess.name.localName)
        assertRefersTo(p2MemberAccess.base, outerTmpVar)

        val p2Rhs = p2Assign.rhs.firstOrNull()
        assertNotNull(p2Rhs)
        assertIs<ExpressionList>(p2Rhs)
        assertEquals(4, p2Rhs.expressions.size)

        // Reference to tmpRectangle
        val outerRef = exprList.expressions[3]
        assertIs<Reference>(outerRef)
        assertRefersTo(outerRef, outerTmpVar)
    }

    @Test
    fun nestedObjectInitializerWithoutNewTest() {
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

        /**
         * Rectangle2 r = new Rectangle2 { P1 = { X = 0, Y = 1 }, P2 = { X = 2, Y = 3 } };
         *
         * translated into: Rectangle2 __tmp = new Rectangle2(); __tmp.P1.X = 0; __tmp.P1.Y = 1;
         * __tmp.P2.X = 2; __tmp.P2.Y = 3; Rectangle2 r = __tmp;
         */
        val rectangle2Class = tu.records["Rectangle2"]
        assertNotNull(rectangle2Class)

        val r2Variable = tu.variables["r2"]
        assertNotNull(r2Variable)

        val exprList = r2Variable.initializer
        assertNotNull(exprList)
        assertIs<ExpressionList>(exprList)
        assertEquals(6, exprList.expressions.size)

        // DeclarationStatement: __tmp = new Rectangle2()
        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        assertTrue(declStmt.isImplicit)
        val tmpVar = declStmt.declarations.firstOrNull()
        assertNotNull(tmpVar)
        assertIs<Variable>(tmpVar)
        val newNode = tmpVar.initializer
        assertNotNull(newNode)
        assertIs<New>(newNode)

        // __tmp.P1.X = 0
        val p1xAssign = exprList.expressions[1]
        assertIs<Assign>(p1xAssign)
        assertTrue(p1xAssign.isImplicit)
        val p1xAccess = p1xAssign.lhs.firstOrNull()
        assertIs<MemberAccess>(p1xAccess)
        assertEquals("X", p1xAccess.name.localName)
        val p1Access = p1xAccess.base
        assertIs<MemberAccess>(p1Access)
        assertEquals("P1", p1Access.name.localName)
        assertRefersTo(p1Access.base, tmpVar)
        val p1xRhs = p1xAssign.rhs.firstOrNull()
        assertIs<Literal<*>>(p1xRhs)
        assertEquals(0, p1xRhs.value)

        // __tmp.P1.Y = 1
        val p1yAssign = exprList.expressions[2]
        assertIs<Assign>(p1yAssign)
        val p1yAccess = p1yAssign.lhs.firstOrNull()
        assertIs<MemberAccess>(p1yAccess)
        assertEquals("Y", p1yAccess.name.localName)
        val p1yRhs = p1yAssign.rhs.firstOrNull()
        assertIs<Literal<*>>(p1yRhs)
        assertEquals(1, p1yRhs.value)

        // __tmp.P2.X = 2
        val p2xAssign = exprList.expressions[3]
        assertIs<Assign>(p2xAssign)
        val p2xAccess = p2xAssign.lhs.firstOrNull()
        assertIs<MemberAccess>(p2xAccess)
        assertEquals("X", p2xAccess.name.localName)
        val p2Access = p2xAccess.base
        assertIs<MemberAccess>(p2Access)
        assertEquals("P2", p2Access.name.localName)
        assertRefersTo(p2Access.base, tmpVar)
        val p2xRhs = p2xAssign.rhs.firstOrNull()
        assertIs<Literal<*>>(p2xRhs)
        assertEquals(2, p2xRhs.value)

        // __tmp.P2.Y = 3
        val p2yAssign = exprList.expressions[4]
        assertIs<Assign>(p2yAssign)
        val p2yAccess = p2yAssign.lhs.firstOrNull()
        assertIs<MemberAccess>(p2yAccess)
        assertEquals("Y", p2yAccess.name.localName)
        val p2yRhs = p2yAssign.rhs.firstOrNull()
        assertIs<Literal<*>>(p2yRhs)
        assertEquals(3, p2yRhs.value)

        // Reference to __tmp
        val ref = exprList.expressions[5]
        assertIs<Reference>(ref)
        assertRefersTo(ref, tmpVar)
    }
}
