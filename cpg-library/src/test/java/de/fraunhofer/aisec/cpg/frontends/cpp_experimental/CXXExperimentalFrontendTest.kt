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
package de.fraunhofer.aisec.cpg.frontends.cpp_experimental

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend.log
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
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
import org.junit.jupiter.api.Test

class CXXExperimentalFrontendTest {
    @Test
    fun testSimple() {
        val file = File("src/test/resources/simple.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXExperimentalFrontend::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        assertNotNull(tu)
        assertEquals(2, tu.declarations.size)

        val someFunc =
            tu.getDeclarationsByName("someFunc", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(someFunc)
        assertEquals("someFunc", someFunc.name)
        assertEquals("int", someFunc.type.typeName)
        assertEquals(1, someFunc.parameters.size)

        val a = someFunc.parameters.firstOrNull { it.name == "a" }
        assertNotNull(a)
        assertEquals("int", a.type.typeName)

        val r = someFunc.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(r)

        val binOp = r.returnValue as? BinaryOperator
        assertNotNull(binOp)

        val ref = binOp.lhs as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals("a", ref.name)
        assertSame(a, ref.refersTo)

        val literal = binOp.rhs as? Literal<*>
        assertNotNull(literal)
        assertEquals(1, literal.value)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)
        assertEquals("main", main.name)
        assertEquals("int", main.type.typeName)

        val call = main.getBodyStatementAs(0, CallExpression::class.java)
        assertNotNull(call)
        assertEquals("someFunc", call.name)
        assertTrue(call.invokes.contains(someFunc))
    }

    @Test
    fun testDeclarations2() {
        val file = File("src/test/resources/declstmt2.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXExperimentalFrontend::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testDeclarations() {
        val file = File("src/test/resources/declstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXExperimentalFrontend::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val function = tu.getDeclarationAs(0, FunctionDeclaration::class.java)
        assertNotNull(function)

        val statements: List<Statement> = getStatementsOfFunction(function)

        statements.forEach(
            Consumer { node: Statement ->
                log.debug("{}", node)
                assertTrue(
                    node is DeclarationStatement ||
                        statements.indexOf(node) == statements.size - 1 && node is ReturnStatement
                )
            }
        )

        val declFromMultiplicateExpression =
            (statements[0] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )

        assertEquals(TypeParser.createFrom("SSL_CTX*", true), declFromMultiplicateExpression.type)
        assertEquals("ptr", declFromMultiplicateExpression.name)

        val withInitializer =
            (statements[1] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        var initializer = withInitializer.initializer

        assertNotNull(initializer)
        assertTrue(initializer is Literal<*>)
        assertEquals(1, (initializer as Literal<*>).value)

        val twoDeclarations = (statements[2] as DeclarationStatement).declarations

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
        // since this type is not known, clang marks this declaration as invalid and does not parse
        // the initializer :(
        // assertTrue(qualifiedType.initializer is Literal<*>)
        // assertEquals("some text", (qualifiedType.initializer as Literal<*>).value)

        val pointerWithAssign =
            (statements[5] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )

        assertEquals(TypeParser.createFrom("void*", true), pointerWithAssign.type)
        assertEquals("ptr2", pointerWithAssign.name)
        assertEquals(null, (pointerWithAssign.initializer as? Literal<*>)?.value)

        val classWithVariable = (statements[6] as DeclarationStatement).declarations
        assertEquals(2, classWithVariable.size)

        val classA = classWithVariable[0] as RecordDeclaration
        assertNotNull(classA)
        assertEquals("A", classA.name)

        val myA = classWithVariable[1] as VariableDeclaration
        assertNotNull(myA)
        assertEquals("myA", myA.name)
        assertEquals(classA, (myA.type as ObjectType).recordDeclaration)
    }

    private fun getStatementsOfFunction(declaration: FunctionDeclaration): List<Statement> {
        return (declaration.body as CompoundStatement).statements
    }
}
