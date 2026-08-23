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
package de.fraunhofer.aisec.cpg.persistence

import kotlin.test.Test
import kotlin.test.assertEquals

class CypherLiteralTest {

    @Test
    fun testRenderScalars() {
        assertEquals("null", CypherLiteral.render(null))
        assertEquals("true", CypherLiteral.render(true))
        assertEquals("false", CypherLiteral.render(false))
        assertEquals("42", CypherLiteral.render(42))
        assertEquals("42", CypherLiteral.render(42L))
        assertEquals("\"foo\"", CypherLiteral.render("foo"))
        assertEquals("\"a\"", CypherLiteral.render('a'))
    }

    @Test
    fun testRenderNonFiniteNumbers() {
        // NaN and Infinity have no Cypher literal, so they must not end up in the query
        assertEquals("null", CypherLiteral.render(Double.NaN))
        assertEquals("null", CypherLiteral.render(Double.POSITIVE_INFINITY))
        assertEquals("null", CypherLiteral.render(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun testRenderEnum() {
        assertEquals("\"SECOND\"", CypherLiteral.render(TestEnum.SECOND))
    }

    @Test
    fun testEscapeStrings() {
        assertEquals("\"say \\\"hi\\\"\"", CypherLiteral.render("say \"hi\""))
        assertEquals("\"back\\\\slash\"", CypherLiteral.render("back\\slash"))
        assertEquals("\"line\\nbreak\"", CypherLiteral.render("line\nbreak"))
        assertEquals("\"carriage\\rreturn\"", CypherLiteral.render("carriage\rreturn"))
        assertEquals("\"tab\\tstop\"", CypherLiteral.render("tab\tstop"))
        // Single quotes need no escaping inside a double-quoted literal
        assertEquals("\"it's\"", CypherLiteral.render("it's"))
        // Non-ASCII characters are passed through verbatim, FalkorDB has no \\uXXXX escape
        assertEquals("\"mäh €\"", CypherLiteral.render("mäh €"))
    }

    @Test
    fun testEscapingPreventsBreakingOutOfTheLiteral() {
        // A value that tries to close the string and inject a clause must stay a single literal
        val malicious = "\" RETURN 1 //"
        assertEquals("\"\\\" RETURN 1 //\"", CypherLiteral.render(malicious))

        // A trailing backslash must not escape the closing quote
        assertEquals("\"c:\\\\\"", CypherLiteral.render("c:\\"))
    }

    @Test
    fun testRenderCollections() {
        assertEquals("[1, 2, 3]", CypherLiteral.render(listOf(1, 2, 3)))
        assertEquals("[\"a\", null]", CypherLiteral.render(listOf("a", null)))
        assertEquals("[]", CypherLiteral.render(emptyList<String>()))
        assertEquals("[1, 2]", CypherLiteral.render(arrayOf(1, 2)))
        assertEquals("[1, 2]", CypherLiteral.render(intArrayOf(1, 2)))
    }

    @Test
    fun testRenderMap() {
        assertEquals(
            "{`name`: \"foo\", `line`: 1}",
            CypherLiteral.render(linkedMapOf<String, Any?>("name" to "foo", "line" to 1)),
        )
        assertEquals("{}", CypherLiteral.render(emptyMap<String, Any?>()))
    }

    @Test
    fun testRenderMapSkipsNullValues() {
        // FalkorDB does not store null properties, so they are left out entirely
        assertEquals(
            "{`name`: \"foo\"}",
            CypherLiteral.render(linkedMapOf<String, Any?>("name" to "foo", "code" to null)),
        )
    }

    @Test
    fun testRenderNestedStructures() {
        val value =
            listOf(
                linkedMapOf<String, Any?>(
                    "startId" to "a",
                    "properties" to linkedMapOf<String, Any?>("index" to 0),
                )
            )
        assertEquals(
            "[{`startId`: \"a\", `properties`: {`index`: 0}}]",
            CypherLiteral.render(value),
        )
    }

    @Test
    fun testRenderIdentifier() {
        assertEquals("`CallExpression`", CypherLiteral.renderIdentifier("CallExpression"))
        // Backticks are escaped by doubling them
        assertEquals("`we``ird`", CypherLiteral.renderIdentifier("we`ird"))
    }

    @Test
    fun testWithParameters() {
        assertEquals(
            "CYPHER props=[1, 2] UNWIND \$props AS map RETURN map",
            CypherLiteral.withParameters(
                mapOf("props" to listOf(1, 2)),
                "UNWIND \$props AS map RETURN map",
            ),
        )
    }

    @Test
    fun testWithoutParameters() {
        assertEquals("RETURN 1", CypherLiteral.withParameters(emptyMap(), "RETURN 1"))
    }

    enum class TestEnum {
        FIRST,
        SECOND,
    }
}
