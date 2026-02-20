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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Problem
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import java.math.BigInteger
import kotlin.test.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class CXXLiteralTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testZeroIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val zero = tu.functions["zero"]
        assertNotNull(zero)

        val funcDecl = zero
        assertLocalName("zero", funcDecl)
        assertLiteral(0, tu.primitiveType("int"), funcDecl, "i")
        assertLiteral(0L, tu.primitiveType("long int"), funcDecl, "l_with_suffix")
        assertLiteral(0L, tu.primitiveType("long long int"), funcDecl, "l_long_long_with_suffix")
        assertLiteral(
            BigInteger.valueOf(0),
            tu.primitiveType("unsigned long long int"),
            funcDecl,
            "l_unsigned_long_long_with_suffix",
        )
    }

    @Test
    @Throws(Exception::class)
    fun testDecimalIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val decimal = tu.functions["decimal"]
        assertNotNull(decimal)

        val funcDecl = decimal
        assertLocalName("decimal", funcDecl)
        assertLiteral(42, tu.primitiveType("int"), funcDecl, "i")
        assertLiteral(1000, tu.primitiveType("int"), funcDecl, "i_with_literal")
        assertLiteral(9223372036854775807L, tu.primitiveType("long int"), funcDecl, "l")
        assertLiteral(9223372036854775807L, tu.primitiveType("long int"), funcDecl, "l_with_suffix")
        assertLiteral(
            9223372036854775807L,
            tu.primitiveType("long long int"),
            funcDecl,
            "l_long_long_with_suffix",
        )
        assertLiteral(
            BigInteger("9223372036854775809"),
            tu.primitiveType("unsigned long int"),
            funcDecl,
            "l_unsigned_long_with_suffix",
        )
        assertLiteral(
            BigInteger("9223372036854775808"),
            tu.primitiveType("unsigned long long int"),
            funcDecl,
            "l_long_long_implicit",
        )
        assertLiteral(
            BigInteger("9223372036854775809"),
            tu.primitiveType("unsigned long long int"),
            funcDecl,
            "l_unsigned_long_long_with_suffix",
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOctalIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val octal = tu.functions["octal"]
        assertNotNull(octal)

        val funcDecl = octal
        assertLocalName("octal", funcDecl)
        assertLiteral(42, tu.primitiveType("int"), funcDecl, "i")
        assertLiteral(42L, tu.primitiveType("long int"), funcDecl, "l_with_suffix")
        assertLiteral(
            BigInteger.valueOf(42),
            tu.primitiveType("unsigned long long int"),
            funcDecl,
            "l_unsigned_long_long_with_suffix",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["octal", "hex", "binary"])
    @Throws(Exception::class)
    fun testNonDecimalIntegerLiterals() {
        val file = File("src/test/resources/integer_literals.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val hex = tu.functions["hex"]
        assertNotNull(hex)

        val funcDecl = hex
        assertLocalName("hex", funcDecl)
        assertLiteral(42, tu.primitiveType("int"), funcDecl, "i")
        assertLiteral(42L, tu.primitiveType("long int"), funcDecl, "l_with_suffix")
        assertLiteral(
            BigInteger.valueOf(42),
            tu.primitiveType("unsigned long long int"),
            funcDecl,
            "l_unsigned_long_long_with_suffix",
        )
    }

    @Test
    @Throws(Exception::class)
    fun testLargeNegativeNumber() {
        val file = File("src/test/resources/largenegativenumber.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val funcDecl = main
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
            (b.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value,
        )

        val c = funcDecl.variables["c"]
        assertNotNull(c)
        assertEquals(
            2147483649L,
            (c.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value,
        )

        val d = funcDecl.variables["d"]
        assertNotNull(d)
        assertEquals(
            BigInteger("9223372036854775808"),
            (d.getInitializerAs(UnaryOperator::class.java)?.input as Literal<*>).value,
        )
    }

    @Test
    fun testCharLiteral() {
        val file = File("src/test/resources/c/char_literal.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        with(tu) {
            val main = tu.functions["main"]
            assertNotNull(main)

            assertLiteral('a', primitiveType("char"), main, "a")
            assertLiteral('\u0000', primitiveType("char"), main, "zero")
            assertLiteral(Char(8), primitiveType("char"), main, "eight")
            assertLiteral(Char(255), primitiveType("char"), main, "hex")
            assertLiteral(Char(255), primitiveType("char"), main, "max_digits")
            assertLiteral('\n', primitiveType("char"), main, "newline")
            assertLiteral(258, primitiveType("int"), main, "multi")
            assertLiteral(21300, primitiveType("int"), main, "multi2")

            val invalid = tu.variables["invalid"]?.initializer
            assertIs<Problem>(invalid)

            val invalid2 = tu.variables["invalid2"]?.initializer
            assertIs<Problem>(invalid2)
        }
    }

    private fun assertLiteral(
        expectedValue: Any,
        expectedType: Type,
        functionDeclaration: Function,
        name: String,
    ) {
        val variableDeclaration = functionDeclaration.variables[name]
        assertNotNull(variableDeclaration)

        val literal = variableDeclaration.initializer<Literal<*>>()
        assertNotNull(literal)
        assertEquals(expectedType, literal.type)
        assertEquals(expectedValue, literal.value)
    }
}
