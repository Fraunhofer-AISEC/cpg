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
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class CXXTreeSitterLanguageFrontendTest {
    @Test
    fun testParse() {
        val file = File("src/test/resources/binaryoperator.cpp")

        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXTreeSitterLanguageFrontend::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as CompoundStatement)
                .statements

        // first two statements are just declarations

        // a = b * 2

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
}
