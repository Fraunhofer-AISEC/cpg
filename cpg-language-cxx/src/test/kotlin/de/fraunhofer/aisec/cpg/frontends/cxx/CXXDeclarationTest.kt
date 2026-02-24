/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*

class CXXDeclarationTest {
    @Test
    fun testDefinitionDeclaration() {
        val file = File("src/test/resources/cxx/definition.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val declaration = result.functions[{ it.name.localName == "function" && !it.isDefinition }]
        assertNotNull(declaration)

        val definition = result.functions[{ it.name.localName == "function" && it.isDefinition }]
        assertNotNull(definition)

        assertEquals(definition, declaration.definition)
    }

    @Test
    fun testDefinitionDeclarationWithMockStd() {
        val topLevel = File("src/test/resources/c/foobar")
        val result =
            analyze(
                listOf(topLevel.resolve("foo.c"), topLevel.resolve("bar.c")),
                topLevel.toPath(),
                true,
            ) {
                it.registerLanguage<CLanguage>()
                it.includePath("src/test/resources/c/foobar/std")
            }
        assertNotNull(result)

        val declarations = result.functions { it.name.localName == "foo" && !it.isDefinition }
        assertTrue(declarations.isNotEmpty())

        val definition = result.functions[{ it.name.localName == "foo" && it.isDefinition }]
        assertNotNull(definition)

        declarations.forEach { assertEquals(definition, it.definition) }

        // With the "std" lib, we know that size_t is a typedef for an int-type and therefore we can
        // resolve all the calls
        val calls = result.calls("foo")
        calls.forEach { assertInvokes(it, definition) }
    }

    @Test
    fun testDefinitionDeclarationWithoutMockStd() {
        val topLevel = File("src/test/resources/c/foobar")
        val result =
            analyze(
                listOf(topLevel.resolve("foo.c"), topLevel.resolve("bar.c")),
                topLevel.toPath(),
                true,
            ) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        val declarations =
            result.functions { it.name.localName == "foo" && !it.isDefinition && !it.isInferred }
        assertTrue(declarations.isNotEmpty())

        val definition = result.functions[{ it.name.localName == "foo" && it.isDefinition }]
        assertNotNull(definition)

        declarations.forEach { assertEquals(definition, it.definition) }

        // without the "std" lib, int will not match with size_t and we will infer a new function;
        // and this will actually result in a problematic resolution, since C does not allow
        // function overloading.
        val inferredDefinition =
            result.functions[{ it.name.localName == "foo" && !it.isDefinition && it.isInferred }]
        assertNotNull(inferredDefinition)
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionDeclaration() {
        val file = File("src/test/resources/cxx/functiondecl.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        // should be eight function nodes
        assertEquals(8, tu.functions.size)

        var method = tu.declarations<Function>(0)
        assertEquals("function0(int)void", method!!.signature)

        method = tu.declarations<Function>(1)
        assertEquals("function1(int, std::string, SomeType*, AnotherType&)int", method!!.signature)

        val args = method.parameters.map { it.name.localName }
        assertEquals(listOf("arg0", "arg1", "arg2", "arg3"), args)

        val function0 = tu.functions[{ it.name.localName == "function0" && it.isDefinition }]
        assertNotNull(function0)

        val function0DeclOnly =
            tu.functions[{ it.name.localName == "function0" && !it.isDefinition }]
        assertNotNull(function0DeclOnly)

        // the declaration should be connected to the definition
        assertEquals(function0, function0DeclOnly.definition)

        method = tu.declarations<Function>(2)
        assertEquals("function0(int)void", method!!.signature)

        var statements = (method.body as Block).statements
        assertFalse(statements.isEmpty())
        assertEquals(2, statements.size)

        // last statement should be an implicit return
        var statement = method.bodyOrNull<ReturnStatement>(-1)
        assertNotNull(statement)
        assertTrue(statement.isImplicit)

        method = tu.declarations<Function>(3)
        assertEquals("function2()void*", method!!.signature)

        statements = (method.body as Block).statements
        assertFalse(statements.isEmpty())
        assertEquals(1, statements.size)

        // should only contain 1 explicit return statement
        statement = method.returns.singleOrNull()
        assertNotNull(statement)
        assertFalse(statement.isImplicit)

        method = tu.declarations<Function>(4)
        assertNotNull(method)
        assertEquals("function3()UnknownType*", method.signature)

        method = tu.declarations<Function>(5)
        assertNotNull(method)
        assertEquals("function4(int)void", method.signature)

        method = tu.declarations<Function>(6)
        assertNotNull(method)
        assertEquals(0, method.parameters.size)
        assertEquals("function5()void", method.signature)

        method = tu.declarations<Function>(7)
        assertNotNull(method)
        assertEquals(1, method.parameters.size)

        val param = method.parameters.firstOrNull()
        assertNotNull(param)

        val fpType = param.type as? FunctionPointerType
        assertNotNull(fpType)
        assertEquals(1, fpType.parameters.size)
        assertLocalName("void", fpType.returnType)
    }

    @Test
    fun testAlias() {
        val file = File("src/test/resources/cxx/alias.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val manipulateString = result.functions["manipulateString"]
        assertNotNull(manipulateString)
        assertFalse(manipulateString.isInferred)

        val size = result.functions["size"]
        assertNotNull(size)
        assertFalse(size.isInferred)

        // We should be able to resolve all calls to manipulateString
        var calls = result.calls("manipulateString")
        calls.forEach { assertContains(it.invokes, manipulateString) }

        // We should be able to resolve all calls to size
        calls = result.calls("size")
        calls.forEach { assertContains(it.invokes, size) }
    }

    @Test
    fun testAliasLoop() {
        val file = File("src/test/resources/cxx/alias_loop.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)
        with(result) {
            val a = result.variables["a"]
            assertNotNull(a)
            assertEquals(assertResolvedType("ABC::A"), a.type)
        }
    }

    @Test
    fun testArithmeticOperator() {
        val file = File("src/test/resources/cxx/operators/arithmetic.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val integer = result.records["Integer"]
        assertNotNull(integer)

        val plusplus = integer.operators["operator++"]
        assertNotNull(plusplus)
        assertEquals("++", plusplus.operatorCode)

        val plus = integer.operators("operator+")
        assertEquals(2, plus.size)
        assertEquals("+", plus.map { it.operatorCode }.distinct().singleOrNull())

        val main = result.functions["main"]
        assertNotNull(main)

        val unaryOp = main.operatorCalls["++"]
        assertNotNull(unaryOp)
        assertInvokes(unaryOp, plusplus)

        val binaryOp0 = main.operatorCalls("+").getOrNull(0)
        assertNotNull(binaryOp0)
        assertInvokes(binaryOp0, plus.getOrNull(0))

        val binaryOp1 = main.operatorCalls("+").getOrNull(1)
        assertNotNull(binaryOp1)
        assertInvokes(binaryOp1, plus.getOrNull(1))
    }

    @Test
    fun testMemberAccessOperator() {
        val file = File("src/test/resources/cxx/operators/member_access.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val proxy = result.records["Proxy"]
        assertNotNull(proxy)

        val op = proxy.operators["operator->"]
        assertNotNull(op)

        val data = result.records["Data"]
        assertNotNull(data)

        val size = data.fields["size"]
        assertNotNull(size)

        val p = result.refs["p"]
        assertNotNull(p)
        assertEquals(proxy.toType(), p.type)

        val sizeRef = result.memberExpressions["size"]
        assertNotNull(sizeRef)
        assertRefersTo(sizeRef, size)

        // we should now have an implicit call to our operator in-between "p" and "size"
        val opCall = sizeRef.base
        assertNotNull(opCall)
        assertIs<OperatorCall>(opCall)
        assertEquals(p, opCall.base)
        assertInvokes(opCall, op)
    }

    @Test
    fun testCallOperator() {
        val file = File("src/test/resources/cxx/operators/call_expression.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val proxy = result.records["Proxy"]
        assertNotNull(proxy)

        val funcBar = proxy.functions["bar"]
        assertNotNull(funcBar)

        val op = proxy.operators["operator->"]
        assertNotNull(op)

        val data = result.records["Data"]
        assertNotNull(data)

        val funcFoo = data.functions["foo"]
        assertNotNull(funcFoo)

        val p = result.refs["p"]
        assertNotNull(p)
        assertEquals(proxy.toType(), p.type)

        val funcFooRef = result.memberExpressions["foo"]
        assertNotNull(funcFooRef)
        assertRefersTo(funcFooRef, funcFoo)

        val funcBarRef = result.memberExpressions["bar"]
        assertNotNull(funcBarRef)
        assertRefersTo(funcBarRef, funcBar)

        // we should now have an implicit call to our operator in-between "p" and "foo"
        val opCall = funcFooRef.base
        assertNotNull(opCall)
        assertIs<OperatorCall>(opCall)
        assertEquals(p, opCall.base)
        assertInvokes(opCall, op)
    }
}
