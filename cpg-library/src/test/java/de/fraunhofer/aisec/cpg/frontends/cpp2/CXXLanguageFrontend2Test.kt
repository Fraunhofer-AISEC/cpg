/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp2

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.io.File
import java.util.function.Consumer
import kotlin.test.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CXXLanguageFrontend2Test {
    @Test
    fun testBinaryOperator() {
        val file = File("src/test/resources/binaryoperator.cpp")

        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as CompoundStatement)
                .statements

        // first two statements are just declarations
        // a = b * 2
        var operator = statements[2] as BinaryOperator

        assertEquals("a", operator.lhs.name)
        assertTrue(operator.rhs is BinaryOperator)

        var rhs = operator.rhs as BinaryOperator

        assertTrue(rhs.lhs is DeclaredReferenceExpression)
        assertEquals("b", rhs.lhs.name)
        assertTrue(rhs.rhs is Literal<*>)
        assertEquals(2, (rhs.rhs as Literal<*>).value)

        // a = 1 * 1
        operator = statements[3] as BinaryOperator

        assertEquals("a", operator.lhs.name)
        assertTrue(operator.rhs is BinaryOperator)

        rhs = operator.rhs as BinaryOperator

        assertTrue(rhs.lhs is Literal<*>)
        assertEquals(1, (rhs.lhs as Literal<*>).value)
        assertTrue(rhs.rhs is Literal<*>)
        assertEquals(1, (rhs.rhs as Literal<*>).value)

        // std::string* notMultiplication
        // this is not a multiplication, but a variable declaration with a pointer type, but
        // syntactically no different than the previous ones
        val stmt = statements[4] as DeclarationStatement
        val decl = stmt.singleDeclaration as VariableDeclaration

        assertEquals(TypeParser.createFrom("std.string*", true), decl.type)
        assertEquals("notMultiplication", decl.name)
        assertTrue(decl.initializer is BinaryOperator)

        operator = decl.initializer as BinaryOperator

        assertTrue(operator.lhs is Literal<*>)
        assertEquals(0, (operator.lhs as Literal<*>).value)

        assertTrue(operator.rhs is Literal<*>)
        assertEquals(0, (operator.rhs as Literal<*>).value)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testUnaryOperator() {
        val file = File("src/test/resources/unaryoperator.cpp")

        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as CompoundStatement)
                .statements

        var line = -1

        // int a
        var statement = statements[++line] as DeclarationStatement
        Assertions.assertNotNull(statement)

        // a++
        val postfix = statements[++line] as UnaryOperator
        var input = postfix.input
        Assertions.assertEquals("a", input.name)
        Assertions.assertEquals("++", postfix.operatorCode)
        Assertions.assertTrue(postfix.isPostfix)

        // --a
        val prefix = statements[++line] as UnaryOperator
        input = prefix.input
        Assertions.assertEquals("a", input.name)
        Assertions.assertEquals("--", prefix.operatorCode)
        Assertions.assertTrue(prefix.isPrefix)

        // int len = sizeof(a);
        statement = statements[++line] as DeclarationStatement
        var declaration = statement.singleDeclaration as VariableDeclaration
        val sizeof = declaration.initializer as UnaryOperator
        input = sizeof.input
        Assertions.assertEquals("a", input.name)
        Assertions.assertEquals("sizeof", sizeof.operatorCode)
        Assertions.assertTrue(sizeof.isPrefix)

        // bool b = !false;
        statement = statements[++line] as DeclarationStatement
        declaration = statement.singleDeclaration as VariableDeclaration
        val negation = declaration.initializer as UnaryOperator
        input = negation.input
        Assertions.assertTrue(input is Literal<*>)
        Assertions.assertEquals(false, (input as Literal<*>).value)
        Assertions.assertEquals("!", negation.operatorCode)
        Assertions.assertTrue(negation.isPrefix)

        // int* ptr = 0;
        statement = statements[++line] as DeclarationStatement
        Assertions.assertNotNull(statement)

        // b = *ptr;
        val assign = statements[++line] as BinaryOperator
        val dereference = assign.rhs as UnaryOperator
        input = dereference.input
        Assertions.assertEquals("ptr", input.name)
        Assertions.assertEquals("*", dereference.operatorCode)
        Assertions.assertTrue(dereference.isPrefix)
    }

    @Test
    @Throws(Exception::class)
    fun testDeclarationStatement() {
        val file = File("src/test/resources/declstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as? CompoundStatement)
                ?.statements
                ?: listOf()

        statements.forEach(
            Consumer { node: Statement ->
                assertTrue(
                    node is DeclarationStatement ||
                        statements.indexOf(node) == statements.size - 1 && node is ReturnStatement
                )
            }
        )
        val declfromMultiplyExpression =
            (statements[0] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("SSL_CTX*", true), declfromMultiplyExpression.type)
        assertEquals("ptr", declfromMultiplyExpression.name)

        val withInitializer =
            (statements[1] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        var initializer = withInitializer.initializer
        assertNotNull(initializer)
        assertTrue(initializer is Literal<*>)
        assertEquals(1, initializer.value)

        val twoDeclarations = statements[2].declarations
        assertEquals(2, twoDeclarations.size)

        val b = twoDeclarations[0] as VariableDeclaration
        assertNotNull(b)
        assertEquals("b", b.name)
        assertEquals(TypeParser.createFrom("int*", false), b.type)

        val c = twoDeclarations[1] as VariableDeclaration
        assertNotNull(c)
        assertEquals("c", c.name)
        assertEquals(TypeParser.createFrom("int", false), c.type)

        val withoutInitializer =
            (statements[3] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        initializer = withoutInitializer.initializer
        assertEquals(TypeParser.createFrom("int*", true), withoutInitializer.type)
        assertEquals("d", withoutInitializer.name)
        assertNull(initializer)

        val qualifiedType =
            (statements[4] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("std.string", true), qualifiedType.type)
        assertEquals("text", qualifiedType.name)
        assertTrue(qualifiedType.initializer is Literal<*>)
        assertEquals("some text", (qualifiedType.initializer as Literal<*>).value)

        val pointerWithAssign =
            (statements[5] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("void*", true), pointerWithAssign.type)
        assertEquals("ptr2", pointerWithAssign.name)
        assertTrue((pointerWithAssign.initializer as? Literal<*>)?.value == null)

        val classWithVariable = statements[6].declarations
        assertEquals(2, classWithVariable.size)

        val classA = classWithVariable[0] as RecordDeclaration
        assertNotNull(classA)
        assertEquals("A", classA.name)

        val field = classA.getField("myField")
        assertNotNull(field)
        assertEquals("myField", field.name)
        assertEquals("int", field.type.typeName)

        val myA = classWithVariable[1] as VariableDeclaration
        assertNotNull(myA)
        assertEquals("myA", myA.name)
        assertEquals(classA, (myA.type as ObjectType).recordDeclaration)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testRecordDeclaration() {
        val file = File("src/test/resources/recordstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val recordDeclaration = tu.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)
        assertEquals("SomeClass", recordDeclaration.name)
        assertEquals("class", recordDeclaration.kind)
        assertEquals(3, recordDeclaration.fields.size)

        val field = recordDeclaration.getField("field")
        assertNotNull(field)

        val constant = recordDeclaration.getField("CONSTANT")
        assertNotNull(constant)
        assertEquals(TypeParser.createFrom("void*", true), field.type)
        assertEquals(3, recordDeclaration.methods.size)

        val method = recordDeclaration.methods[0]
        assertEquals("method", method.name)
        assertEquals(0, method.parameters.size)
        assertEquals(TypeParser.createFrom("void*", true), method.type)
        assertFalse(method.hasBody())

        var definition = method.definition as MethodDeclaration
        assertNotNull(definition)
        assertEquals("method", definition.name)
        assertEquals(0, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val methodWithParam = recordDeclaration.methods[1]
        assertEquals("method", methodWithParam.name)
        assertEquals(1, methodWithParam.parameters.size)
        assertEquals(TypeParser.createFrom("int", true), methodWithParam.parameters[0].type)
        assertEquals(TypeParser.createFrom("void*", true), methodWithParam.type)
        assertFalse(methodWithParam.hasBody())
        definition = methodWithParam.definition as MethodDeclaration
        assertNotNull(definition)
        assertEquals("method", definition.name)
        assertEquals(1, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val inlineMethod = recordDeclaration.methods[2]
        assertEquals("inlineMethod", inlineMethod.name)
        assertEquals(TypeParser.createFrom("void*", true), inlineMethod.type)
        assertTrue(inlineMethod.hasBody())

        val inlineConstructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration.name, inlineConstructor.name)
        assertEquals(TypeParser.createFrom("SomeClass", true), inlineConstructor.type)
        assertTrue(inlineConstructor.hasBody())

        val constructorDefinition = tu.getDeclarationAs(3, ConstructorDeclaration::class.java)
        assertNotNull(constructorDefinition)
        assertEquals(1, constructorDefinition.parameters.size)
        assertEquals(TypeParser.createFrom("int", true), constructorDefinition.parameters[0].type)
        assertEquals(TypeParser.createFrom("SomeClass", true), constructorDefinition.type)
        assertTrue(constructorDefinition.hasBody())

        val constructorDeclaration = recordDeclaration.constructors[1]
        assertNotNull(constructorDeclaration)
        assertFalse(constructorDeclaration.isDefinition)
        assertEquals(constructorDefinition, constructorDeclaration.definition)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        val methodCallWithConstant = main.getBodyStatementAs(2, CallExpression::class.java)
        assertNotNull(methodCallWithConstant)

        val arg = methodCallWithConstant.arguments[0]
        assertSame(constant, (arg as DeclaredReferenceExpression).refersTo)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testAssignmentExpression() {
        val file = File("src/test/resources/assignmentexpression.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements =
            (tu.byNameOrNull<FunctionDeclaration>("main")?.body as? CompoundStatement)?.statements
                ?: listOf()

        val declareA = statements[0]
        val a = (declareA as DeclarationStatement).singleDeclaration
        val assignA = statements[1]
        assertTrue(assignA is BinaryOperator)

        var lhs = assignA.lhs
        var rhs = assignA.rhs
        assertEquals("a", lhs.name)
        assertEquals(2, (rhs as Literal<*>).value)
        assertRefersTo(lhs, a)

        val declareB = statements[2]
        assertTrue(declareB is DeclarationStatement)
        val b = declareB.singleDeclaration

        // a = b
        val assignB = statements[3]
        assertTrue(assignB is BinaryOperator)
        lhs = assignB.lhs
        rhs = assignB.rhs
        assertEquals("a", lhs.name)
        assertTrue(rhs is DeclaredReferenceExpression)
        assertEquals("b", rhs.name)
        assertRefersTo(rhs, b)

        val assignBWithFunction = statements[4]
        assertTrue(assignBWithFunction is BinaryOperator)
        assertEquals("a", assignBWithFunction.lhs.name)
        assertTrue(assignBWithFunction.rhs is CallExpression)

        val call = assignBWithFunction.rhs as CallExpression
        assertEquals("someFunction", call.name)
        assertRefersTo(call.arguments[0], b)
    }
}
