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
import de.fraunhofer.aisec.cpg.graph.declarations.Enumeration
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.test.*
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun usingDirectivesTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Usings.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        // `using System;` (using namespace directive)
        val system = tu.imports["System"]
        assertNotNull(system)
        assertEquals(ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE, system.style)

        // `using static System.Math;` (using static directive)
        val math = tu.imports["System.Math"]
        assertNotNull(math)
        assertEquals(ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE, math.style)

        // `using Col = System.Collections.Generic;` (alias for a namespace)
        val alias = tu.imports.singleOrNull { it.alias?.localName == "Col" }
        assertNotNull(alias)
        assertEquals(ImportStyle.IMPORT_NAMESPACE, alias.style)
        assertEquals("System.Collections.Generic", alias.import.toString())

        // TODO Create further tests
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
    fun expressionBodiedMethodTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Methods.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.namespaces["HelloWorld"]?.records["Foo"]
        assertNotNull(foo)

        // An expression-bodied method (`int expressionBodied() => 1;`) has no block body, so
        // Roslyn's Body is null and the expression becomes an implicit block with a return.
        val expressionBodied = foo.methods["expressionBodied"]
        assertNotNull(expressionBodied)
        assertEquals(0, expressionBodied.parameters.size)
        val body = expressionBodied.body
        assertIs<Block>(body)
        val returnStmt = body.statements.single()
        assertIs<Return>(returnStmt)
        val literal = returnStmt.returnValue
        assertIs<Literal<*>>(literal)
        assertEquals(1, literal.value)

        val voidExpressionBodied = foo.methods["voidExpressionBodied"]
        assertNotNull(voidExpressionBodied)
        val voidBody = voidExpressionBodied.body
        assertIs<Block>(voidBody)
        val call = voidBody.statements.single()
        assertIs<Call>(call)
        assertEquals("Bar", call.name.localName)
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
        assertEquals(3, constructors.size)

        val defaultConstructor = constructors.single { it.parameters.isEmpty() }
        assertNotNull(defaultConstructor)
        assertIs<Constructor>(defaultConstructor)

        val emptyBody = defaultConstructor.body
        assertIs<Block>(emptyBody)
        assertEquals(0, emptyBody.statements.size)

        val constructorWithParams = constructors.single { it.parameters.size == 2 }
        assertNotNull(constructorWithParams)
        assertIs<Constructor>(constructorWithParams)

        val body = constructorWithParams.body
        assertIs<Block>(body)
        assertEquals(1, body.statements.size)

        val assignment = body.statements[0]
        assertIs<Assign>(assignment)
        assertEquals("=", assignment.operatorCode)

        // lhs: this.x
        val lhs = assignment.lhs.firstOrNull()
        assertIs<MemberAccess>(lhs)
        val fieldX = foo.fields["x"]
        assertNotNull(fieldX)
        assertIs<Field>(lhs.refersTo)
        assertEquals(fieldX, lhs.refersTo)

        // rhs: x
        val rhs = assignment.rhs.firstOrNull()
        assertIs<Reference>(rhs)
        assertIs<Parameter>(rhs.refersTo)
        val refersTo = rhs.refersTo
        assertNotNull(refersTo)
        assertEquals(constructorWithParams.parameters.first(), refersTo)

        // `Foo(int x) => this.x = x;`
        val expressionBodied = constructors.single { it.parameters.size == 1 }
        assertIs<Constructor>(expressionBodied)

        val expressionBody = expressionBodied.body
        assertIs<Block>(expressionBody)
        assertTrue(expressionBody.isImplicit)

        val expressionAssignment = expressionBody.statements.single()
        assertIs<Assign>(expressionAssignment)
        assertEquals("=", expressionAssignment.operatorCode)

        // lhs: this.x, resolved to the same field as in the block-bodied constructor
        val expressionLhs = expressionAssignment.lhs.firstOrNull()
        assertIs<MemberAccess>(expressionLhs)
        assertEquals("x", expressionLhs.name.localName)
        assertIs<Field>(expressionLhs.refersTo)
        assertEquals(fieldX, expressionLhs.refersTo)
        val expressionBase = expressionLhs.base
        assertIs<Reference>(expressionBase)
        assertEquals("this", expressionBase.name.localName)

        // rhs: x, resolved to this constructor's own parameter and not the other one's
        val expressionRhs = expressionAssignment.rhs.firstOrNull()
        assertIs<Reference>(expressionRhs)
        assertEquals("x", expressionRhs.name.localName)
        val expressionRefersTo = expressionRhs.refersTo
        assertIs<Parameter>(expressionRefersTo)
        assertEquals(expressionBodied.parameters.single(), expressionRefersTo)

        assertContains(expressionRhs.prevDFG, expressionRefersTo)
        assertContains(expressionLhs.prevDFG, expressionRhs)
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

        // uint, a literal that no longer fits into an int
        val returnUInt = foo.methods["returnUInt"]
        assertNotNull(returnUInt)
        val uintReturn = (returnUInt.body as Block).statements.first()
        assertIs<Return>(uintReturn)
        val uintLiteral = uintReturn.returnValue
        assertIs<Literal<*>>(uintLiteral)
        assertEquals(3000000000L, uintLiteral.value)
        assertEquals("uint", uintLiteral.type.name.localName)

        // long
        val returnLong = foo.methods["returnLong"]
        assertNotNull(returnLong)
        val longReturn = (returnLong.body as Block).statements.first()
        assertIs<Return>(longReturn)
        val longLiteral = longReturn.returnValue
        assertIs<Literal<*>>(longLiteral)
        assertEquals(9999999999L, longLiteral.value)
        assertEquals("long", longLiteral.type.name.localName)

        // ulong, kept as a BigInteger since it exceeds Long.MAX_VALUE
        val returnULong = foo.methods["returnULong"]
        assertNotNull(returnULong)
        val ulongReturn = (returnULong.body as Block).statements.first()
        assertIs<Return>(ulongReturn)
        val ulongLiteral = ulongReturn.returnValue
        assertIs<Literal<*>>(ulongLiteral)
        assertEquals(BigInteger("18446744073709551615"), ulongLiteral.value)
        assertEquals("ulong", ulongLiteral.type.name.localName)

        // float
        val returnFloat = foo.methods["returnFloat"]
        assertNotNull(returnFloat)
        val floatReturn = (returnFloat.body as Block).statements.first()
        assertIs<Return>(floatReturn)
        val floatLiteral = floatReturn.returnValue
        assertIs<Literal<*>>(floatLiteral)
        assertEquals(1.5f, floatLiteral.value)
        assertEquals("float", floatLiteral.type.name.localName)

        // double
        val returnDouble = foo.methods["returnDouble"]
        assertNotNull(returnDouble)
        val doubleReturn = (returnDouble.body as Block).statements.first()
        assertIs<Return>(doubleReturn)
        val doubleLiteral = doubleReturn.returnValue
        assertIs<Literal<*>>(doubleLiteral)
        assertEquals(1.5, doubleLiteral.value)
        assertEquals("double", doubleLiteral.type.name.localName)

        // decimal
        val returnDecimal = foo.methods["returnDecimal"]
        assertNotNull(returnDecimal)
        val decimalReturn = (returnDecimal.body as Block).statements.first()
        assertIs<Return>(decimalReturn)
        val decimalLiteral = decimalReturn.returnValue
        assertIs<Literal<*>>(decimalLiteral)
        assertEquals(BigDecimal("1.5"), decimalLiteral.value)
        assertEquals("decimal", decimalLiteral.type.name.localName)

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

        // '\0' used to arrive as an empty string, because it terminated the marshalled C string
        val returnNulChar = foo.methods["returnNulChar"]
        assertNotNull(returnNulChar)
        val nulCharReturn = (returnNulChar.body as Block).statements.first()
        assertIs<Return>(nulCharReturn)
        val nulCharLiteral = nulCharReturn.returnValue
        assertIs<Literal<*>>(nulCharLiteral)
        assertEquals(Char(0), nulCharLiteral.value)
        assertEquals("char", nulCharLiteral.type.name.localName)
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

    @Test
    fun testInheritance() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Inheritance.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val iBase = tu.records["IBase"]
        assertNotNull(iBase)
        assertEquals("interface", iBase.kind)
        assertEquals(0, iBase.superClasses.size)
        assertContains(iBase.modifiers, "public")

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        assertEquals("class", foo.kind)
        assertEquals(1, foo.superClasses.size)
        assertEquals(iBase, foo.superClasses.first().recordDeclaration)
        assertEquals(foo.modifiers.size, 2)
        assertContains(foo.modifiers, "public")
        assertContains(foo.modifiers, "abstract")

        val bar = tu.records["Bar"]
        assertNotNull(bar)
        assertEquals(1, bar.superClasses.size)
        assertEquals(foo, bar.superClasses.first().recordDeclaration)

        val accessMethod = bar.methods["AccessField"]
        assertNotNull(accessMethod)
        val accessBody = accessMethod.body
        assertIs<Block>(accessBody)
        val returnStmt = accessBody.statements.firstOrNull()
        assertNotNull(returnStmt)
        assertIs<Return>(returnStmt)
        val memberAccess = returnStmt.returnValue
        assertIs<MemberAccess>(memberAccess)
        assertEquals("x", memberAccess.name.localName)
        val fieldX = foo.fields["x"]
        assertNotNull(fieldX)
        assertEquals(fieldX, memberAccess.refersTo)

        val callMethod = bar.methods["CallInheritedMethod"]
        assertNotNull(callMethod)
        val callBody = callMethod.body
        assertIs<Block>(callBody)
        val callReturn = callBody.statements.firstOrNull()
        assertNotNull(callReturn)
        assertIs<Return>(callReturn)
        val memberCall = callReturn.returnValue
        assertIs<MemberCall>(memberCall)
        val doSomething = foo.methods["DoSomething"]
        assertNotNull(doSomething)
        assertEquals(doSomething, memberCall.invokes.firstOrNull())
        assertContains(doSomething.modifiers, "public")
    }

    @Test
    fun testGenericType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val result =
            analyze(listOf(topLevel.resolve("Inheritance.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        val tu = result.components.firstOrNull()?.translationUnits?.firstOrNull()
        assertNotNull(tu)

        val container = tu.records["GenericClass"]
        assertNotNull(container)
        assertContains(container.modifiers, "public")

        val typeT = result.finalCtx.typeManager.getTypeParameter(container, "T")
        assertNotNull(typeT)
        assertIs<ParameterizedType>(typeT)
    }

    @Test
    fun testEnums() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Enums.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val enumsEnum = tu.records["Enums"]
        assertIs<Enumeration>(enumsEnum)
        assertEquals("enum", enumsEnum.kind)
        assertEquals(listOf("A", "B", "C"), enumsEnum.entries.map { it.name.localName })

        val b = enumsEnum.entries.single { it.name.localName == "B" }
        assertEquals(enumsEnum, b.type.recordDeclaration)
        val equalsValueExpression = b.initializer
        assertIs<Literal<*>>(equalsValueExpression)
        assertEquals(5, equalsValueExpression.value)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val bar = foo.methods["Bar"]
        assertNotNull(bar)
        assertEquals(enumsEnum, bar.returnTypes.singleOrNull()?.recordDeclaration)

        val body = bar.body
        assertIs<Block>(body)
        val returnStmt = body.statements.singleOrNull()
        assertIs<Return>(returnStmt)
        val returnValue = returnStmt.returnValue
        assertIs<MemberAccess>(returnValue)
        val base = returnValue.base
        assertIs<Reference>(base)
        assertEquals(enumsEnum, base.refersTo)
    }

    @Test
    fun testPropertyDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Properties.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val person = tu.records["Person"]
        assertNotNull(person)

        // public string Name { get; set; }
        val nameField = person.fields["Name"]
        assertNotNull(nameField)
        assertEquals(true, nameField.isImplicit)
        assertLocalName("string", nameField.type)

        val getName = person.methods.singleOrNull { it.name.localName.startsWith("get_Name") }
        assertNotNull(getName)
        assertLocalName("string", getName.returnTypes.singleOrNull())
        val getNameBody = getName.body
        assertIs<Block>(getNameBody)
        val getNameReturn = getNameBody.statements.singleOrNull()
        assertIs<Return>(getNameReturn)
        val getNameValue = getNameReturn.returnValue
        assertIs<MemberAccess>(getNameValue)
        assertLocalName("Name", getNameValue)

        val setName = person.methods.singleOrNull { it.name.localName.startsWith("set_Name") }
        assertNotNull(setName)
        val valueParam = setName.parameters.singleOrNull()
        assertNotNull(valueParam)
        assertLocalName("value", valueParam)
        assertLocalName("string", valueParam.type)
        val setNameBody = setName.body
        assertIs<Block>(setNameBody)
        val setNameAssign = setNameBody.statements.singleOrNull()
        assertIs<Assign>(setNameAssign)
        val setNameLhs = setNameAssign.lhs.singleOrNull()
        assertIs<MemberAccess>(setNameLhs)
        assertLocalName("Name", setNameLhs)
        val setNameRhs = setNameAssign.rhs.singleOrNull()
        assertIs<Reference>(setNameRhs)
        assertLocalName("value", setNameRhs)

        // public int BirthYear { get; } = 1990;
        val birthYearField = person.fields["BirthYear"]
        assertNotNull(birthYearField)
        val birthYearInitializer = birthYearField.initializer
        assertIs<Literal<*>>(birthYearInitializer)
        assertEquals(1990, birthYearInitializer.value)
        assertNotNull(person.methods.singleOrNull { it.name.localName.startsWith("get_BirthYear") })
        assertNull(person.methods.firstOrNull { it.name.localName.startsWith("set_BirthYear") })

        // public int Age { get { return age; } }
        val getAge = person.methods.singleOrNull { it.name.localName.startsWith("get_Age_") }
        assertNotNull(getAge)
        val getAgeBody = getAge.body
        assertIs<Block>(getAgeBody)
        val getAgeReturn = getAgeBody.statements.singleOrNull()
        assertIs<Return>(getAgeReturn)
        val getAgeValue = getAgeReturn.returnValue
        assertIs<Reference>(getAgeValue)
        assertLocalName("age", getAgeValue)

        // public string FullName => Name;
        val getFullName =
            person.methods.singleOrNull { it.name.localName.startsWith("get_FullName") }
        assertNotNull(getFullName)
        assertLocalName("string", getFullName.returnTypes.singleOrNull())
        val getFullNameBody = getFullName.body
        assertIs<Block>(getFullNameBody)
        val getFullNameReturn = getFullNameBody.statements.singleOrNull()
        assertIs<Return>(getFullNameReturn)
        val getFullNameValue = getFullNameReturn.returnValue
        assertIs<Reference>(getFullNameValue)
        assertLocalName("Name", getFullNameValue)

        // public int AgeInMonths { get => age * 12; }
        val getAgeInMonths =
            person.methods.singleOrNull { it.name.localName.startsWith("get_AgeInMonths") }
        assertNotNull(getAgeInMonths)
        val getAgeInMonthsBody = getAgeInMonths.body
        assertIs<Block>(getAgeInMonthsBody)
        val getAgeInMonthsReturn = getAgeInMonthsBody.statements.singleOrNull()
        assertIs<Return>(getAgeInMonthsReturn)
        val multiplication = getAgeInMonthsReturn.returnValue
        assertIs<BinaryOperator>(multiplication)
        assertEquals("*", multiplication.operatorCode)
        val multiplicationLhs = multiplication.lhs
        assertIs<Reference>(multiplicationLhs)
        assertLocalName("age", multiplicationLhs)
        val multiplicationRhs = multiplication.rhs
        assertIs<Literal<*>>(multiplicationRhs)
        assertEquals(12, multiplicationRhs.value)

        // public string Email { get; private set; }
        val getEmail = person.methods.singleOrNull { it.name.localName.startsWith("get_Email") }
        assertNotNull(getEmail)
        assertContains(getEmail.modifiers, "public")
        val setEmail = person.methods.singleOrNull { it.name.localName.startsWith("set_Email") }
        assertNotNull(setEmail)
        assertContains(setEmail.modifiers, "private")
    }
}
