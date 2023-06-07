/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.io.File
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CXXLiteralTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testZeroIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val zero = tu.getDeclarationsByName("zero", FunctionDeclaration::class.java)
        assertFalse(zero.isEmpty())

        val funcDecl = zero.iterator().next()
        assertLocalName("zero", funcDecl)
        assertLiteral(0, tu.parseType("int"), funcDecl, "i")
        assertLiteral(0L, tu.parseType("long"), funcDecl, "l_with_suffix")
        assertLiteral(0L, tu.parseType("long long"), funcDecl, "l_long_long_with_suffix")
        assertLiteral(
            BigInteger.valueOf(0),
            tu.parseType("unsigned long long"),
            funcDecl,
            "l_unsigned_long_long_with_suffix"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testDecimalIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val decimal = tu.getDeclarationsByName("decimal", FunctionDeclaration::class.java)
        assertFalse(decimal.isEmpty())
        val funcDecl = decimal.iterator().next()
        assertLocalName("decimal", funcDecl)
        assertLiteral(42, tu.parseType("int"), funcDecl, "i")
        assertLiteral(1000, tu.parseType("int"), funcDecl, "i_with_literal")
        assertLiteral(9223372036854775807L, tu.parseType("long"), funcDecl, "l")
        assertLiteral(9223372036854775807L, tu.parseType("long"), funcDecl, "l_with_suffix")
        assertLiteral(
            9223372036854775807L,
            tu.parseType("long long"),
            funcDecl,
            "l_long_long_with_suffix"
        )
        assertLiteral(
            BigInteger("9223372036854775809"),
            tu.parseType("unsigned long"),
            funcDecl,
            "l_unsigned_long_with_suffix"
        )
        assertLiteral(
            BigInteger("9223372036854775808"),
            tu.parseType("unsigned long long"),
            funcDecl,
            "l_long_long_implicit"
        )
        assertLiteral(
            BigInteger("9223372036854775809"),
            tu.parseType("unsigned long long"),
            funcDecl,
            "l_unsigned_long_long_with_suffix"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOctalIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val octal = tu.getDeclarationsByName("octal", FunctionDeclaration::class.java)
        assertFalse(octal.isEmpty())
        val funcDecl = octal.iterator().next()
        assertLocalName("octal", funcDecl)
        assertLiteral(42, tu.parseType("int"), funcDecl, "i")
        assertLiteral(42L, tu.parseType("long"), funcDecl, "l_with_suffix")
        assertLiteral(
            BigInteger.valueOf(42),
            tu.parseType("unsigned long long"),
            funcDecl,
            "l_unsigned_long_long_with_suffix"
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["octal", "hex", "binary"])
    @Throws(Exception::class)
    fun testNonDecimalIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val hex = tu.getDeclarationsByName("hex", FunctionDeclaration::class.java)
        assertFalse(hex.isEmpty())
        val funcDecl = hex.iterator().next()
        assertLocalName("hex", funcDecl)
        assertLiteral(42, tu.parseType("int"), funcDecl, "i")
        assertLiteral(42L, tu.parseType("long"), funcDecl, "l_with_suffix")
        assertLiteral(
            BigInteger.valueOf(42),
            tu.parseType("unsigned long long"),
            funcDecl,
            "l_unsigned_long_long_with_suffix"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLargeNegativeNumber() {
        val file = File("src/test/resources/largenegativenumber.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val funcDecl = main.iterator().next()
        val a = funcDecl.variables["a"]
        assertNotNull(a)
        assertEquals(1, (a.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value)

        // there are no negative literals, so the construct "-2147483648" is
        // a unary expression and the literal "2147483648". Since "2147483648" is too large to fit
        // in an integer, it should be automatically converted to a long. The resulting value
        // -2147483648 however is small enough to fit into an int, so it is ok for the variable a to
        // have an int type
        val b = funcDecl.variables["b"]
        assertNotNull(b)
        assertEquals(
            2147483648L,
            (b.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value
        )

        val c = funcDecl.variables["c"]
        assertNotNull(c)
        assertEquals(
            2147483649L,
            (c.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value
        )

        val d = funcDecl.variables["d"]
        assertNotNull(d)
        assertEquals(
            BigInteger("9223372036854775808"),
            (d.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value
        )
    }

    private fun assertLiteral(
        expectedValue: Number,
        expectedType: Type,
        functionDeclaration: FunctionDeclaration,
        name: String
    ) {
        val variableDeclaration = functionDeclaration.variables[name]
        assertNotNull(variableDeclaration)

        val literal = variableDeclaration.getInitializerAs(Literal::class.java)!!
        assertNotNull(literal)
        assertEquals(expectedType, literal.type)
        assertEquals(expectedValue, literal.value)
    }
}
