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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.assertInvokes
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
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
                true
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
                true
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
            TestUtils.analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        // should be eight function nodes
        assertEquals(8, tu.functions.size)

        var method = tu.getDeclarationAs(0, FunctionDeclaration::class.java)
        assertEquals("function0(int)void", method!!.signature)

        method = tu.getDeclarationAs(1, FunctionDeclaration::class.java)
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

        method = tu.getDeclarationAs(2, FunctionDeclaration::class.java)
        assertEquals("function0(int)void", method!!.signature)

        var statements = (method.body as Block).statements
        assertFalse(statements.isEmpty())
        assertEquals(2, statements.size)

        // last statement should be an implicit return
        var statement = method.getBodyStatementAs(statements.size - 1, ReturnStatement::class.java)
        assertNotNull(statement)
        assertTrue(statement.isImplicit)

        method = tu.getDeclarationAs(3, FunctionDeclaration::class.java)
        assertEquals("function2()void*", method!!.signature)

        statements = (method.body as Block).statements
        assertFalse(statements.isEmpty())
        assertEquals(1, statements.size)

        // should only contain 1 explicit return statement
        statement = method.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(statement)
        assertFalse(statement.isImplicit)

        method = tu.getDeclarationAs(4, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals("function3()UnknownType*", method.signature)

        method = tu.getDeclarationAs(5, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals("function4(int)void", method.signature)

        method = tu.getDeclarationAs(6, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals(0, method.parameters.size)
        assertEquals("function5()void", method.signature)

        method = tu.getDeclarationAs(7, FunctionDeclaration::class.java)
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

        // We should be able to resolve all calls to manipulateString
        val calls = result.calls("manipulateString")
        calls.forEach { it.invokes.isNotEmpty() && it.invokes.all { decl -> !decl.isInferred } }
    }
}
