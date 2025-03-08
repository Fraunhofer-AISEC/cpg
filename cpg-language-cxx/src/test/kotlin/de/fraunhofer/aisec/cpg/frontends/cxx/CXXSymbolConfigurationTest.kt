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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class CXXSymbolConfigurationTest : BaseTest() {
    @Test
    @Throws(TranslationException::class)
    fun testWithoutSymbols() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())

        // parse without symbols
        val tu =
            CXXLanguageFrontend(ctx, CPPLanguage(ctx)).parse(File("src/test/resources/symbols.cpp"))
        val main = tu.functions["main"]
        assertNotNull(main)

        val funcDecl = main
        var binaryOperator = funcDecl.bodyOrNull<BinaryOperator>(0)
        assertNotNull(binaryOperator)

        // without additional symbols, the first line will look like a reference (to something we do
        // not know)
        val dre = binaryOperator.rhs<Reference>()
        assertNotNull(dre)
        assertLocalName("HELLO_WORLD", dre)

        binaryOperator = funcDecl.bodyOrNull<BinaryOperator>(1)
        assertNotNull(binaryOperator)

        // without additional symbols, the second line will look like a function call (to something
        // we do not know)
        val call = binaryOperator.rhs<CallExpression>()
        assertNotNull(call)
        assertLocalName("INCREASE", call)
    }

    @Test
    @Throws(TranslationException::class)
    fun testWithSymbols() {
        val config =
            TranslationConfiguration.builder()
                .symbols(mapOf(Pair("HELLO_WORLD", "\"Hello World\""), Pair("INCREASE(X)", "X+1")))
                .defaultPasses()
                .build()

        // let's try with symbol definitions
        val ctx = TranslationContext(config)
        val tu =
            CXXLanguageFrontend(ctx, CPPLanguage(ctx)).parse(File("src/test/resources/symbols.cpp"))
        val main = tu.functions["main"]
        assertNotNull(main)

        val funcDecl = main
        var binaryOperator = funcDecl.bodyOrNull<BinaryOperator>(0)
        assertNotNull(binaryOperator)

        // should be a literal now
        val literal = binaryOperator.rhs<Literal<String>>()
        assertNotNull(literal)
        assertEquals("Hello World", literal.value)

        binaryOperator = funcDecl.bodyOrNull<BinaryOperator>(1)
        assertNotNull(binaryOperator)

        // should be expanded to another binary operation 1+1
        val add = binaryOperator.rhs<BinaryOperator>()
        assertNotNull(add)
        assertEquals("+", add.operatorCode)

        val literal2 = add.lhs<Literal<Int>>()
        assertNotNull(literal2)
        assertEquals(2, literal2.value)

        val literal1 = add.rhs<Literal<Int>>()
        assertNotNull(literal1)
        assertEquals(1, literal1.value)
    }
}
