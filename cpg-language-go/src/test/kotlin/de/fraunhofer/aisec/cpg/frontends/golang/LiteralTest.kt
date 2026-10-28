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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
import java.math.BigInteger
import java.nio.file.Path
import kotlin.test.*

class LiteralTest {

    @Test
    fun testParseIntLiteral() {
        assertEquals(BigInteger.valueOf(42), parseGoIntLiteral("42"))
        assertEquals(BigInteger.valueOf(1_000_000), parseGoIntLiteral("1_000_000"))
        assertEquals(BigInteger.valueOf(255), parseGoIntLiteral("0x_Ff"))
        assertEquals(BigInteger.valueOf(31), parseGoIntLiteral("0X1F"))
        assertEquals(BigInteger.valueOf(15), parseGoIntLiteral("0o17"))
        assertEquals(BigInteger.valueOf(15), parseGoIntLiteral("0O17"))
        assertEquals(BigInteger.valueOf(15), parseGoIntLiteral("017"))
        assertEquals(BigInteger.valueOf(5), parseGoIntLiteral("0b101"))
        assertEquals(BigInteger.ZERO, parseGoIntLiteral("0"))
        assertNull(parseGoIntLiteral("08"))
    }

    @Test
    fun testParseRuneLiteral() {
        assertEquals('a'.code, parseGoRuneLiteral("'a'"))
        assertEquals('\n'.code, parseGoRuneLiteral("'\\n'"))
        assertEquals('\''.code, parseGoRuneLiteral("'\\''"))
        assertEquals(0xe9, parseGoRuneLiteral("'\\u00e9'"))
        assertEquals(0x1F600, parseGoRuneLiteral("'\\U0001F600'"))
        assertEquals(0x1F600, parseGoRuneLiteral("'\uD83D\uDE00'"))
        assertEquals(0x41, parseGoRuneLiteral("'\\x41'"))
        assertEquals(0x41, parseGoRuneLiteral("'\\101'"))
        assertNull(parseGoRuneLiteral("'ab'"))
        assertNull(parseGoRuneLiteral("'\\q'"))
    }

    @Test
    fun testParseStringLiteral() {
        assertEquals("hi", parseGoStringLiteral("\"hi\""))
        assertEquals("a\tb\"", parseGoStringLiteral("\"a\\tb\\\"\""))
        assertEquals("€", parseGoStringLiteral("\"\\xe2\\x82\\xac\""))
        assertEquals("é", parseGoStringLiteral("\"\\u00e9\""))
        assertEquals("a\\tb", parseGoStringLiteral("`a\\tb`"))
        assertEquals("ab", parseGoStringLiteral("`a\rb`"))
        assertEquals("", parseGoStringLiteral("\"\""))
        assertNull(parseGoStringLiteral("\"\\x4\""))
    }

    @Test
    fun testLiteralValues() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal_values.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        fun valueOf(name: String): Any? {
            val variable = tu.variables[name]
            assertNotNull(variable, "variable $name not found")
            return assertIs<Literal<*>>(variable.initializer).value
        }

        assertEquals(15, valueOf("octal"))
        assertEquals(15, valueOf("legacy"))
        assertEquals(31, valueOf("hexUpper"))
        assertEquals(5, valueOf("binUpper"))
        assertEquals(BigInteger("FFFFFFFFFFFFFFFF", 16), valueOf("big"))

        assertEquals('a'.code, valueOf("r1"))
        assertEquals('\n'.code, valueOf("r2"))
        assertEquals(0xe9, valueOf("r3"))
        assertEquals(0x41, valueOf("r4"))

        assertEquals("a\tb", valueOf("s1"))
        assertEquals("€", valueOf("s2"))
        assertEquals("a\\tb", valueOf("s3"))

        // iota is only defined in constant declarations
        val notIota = tu.variables["notIota"]
        assertNotNull(notIota)
        assertIs<ProblemExpression>(notIota.initializer)
    }

    @Test
    fun testBuiltins() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal_values.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        // make passes all arguments after the type to the construction
        val ch = main.variables["ch"]
        assertNotNull(ch)
        val construction = assertIs<Construction>(ch.initializer)
        assertEquals(listOf<Any?>(10), construction.arguments.map { (it as? Literal<*>)?.value })

        // a method called "make" is not the built-in make
        val m = main.variables["m"]
        assertNotNull(m)
        val call = assertIs<MemberCall>(m.initializer)
        assertLocalName("make", call)

        // a local variable called "true" shadows the predeclared constant
        val trueVar = main.variables["true"]
        assertNotNull(trueVar)
        val x = main.variables["x"]
        assertNotNull(x)
        val ref = assertIs<Reference>(x.initializer)
        assertSame(trueVar, ref.refersTo)
    }
}
