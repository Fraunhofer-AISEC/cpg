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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

internal class CXXSymbolConfigurationTest : BaseTest() {
    @Test
    @Throws(TranslationException::class)
    fun testWithoutSymbols() {
        // parse without symbols
        val tu =
            CXXLanguageFrontend(
                    CPPLanguage(),
                    TranslationConfiguration.builder().defaultPasses().build(),
                    ScopeManager()
                )
                .parse(File("src/test/resources/symbols.cpp"))
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val funcDecl = main.iterator().next()
        var binaryOperator = funcDecl.getBodyStatementAs(0, BinaryOperator::class.java)
        assertNotNull(binaryOperator)

        // without additional symbols, the first line will look like a reference (to something we do
        // not know)
        val dre = binaryOperator.getRhsAs(DeclaredReferenceExpression::class.java)
        assertNotNull(dre)
        assertEquals("HELLO_WORLD", dre.name)

        binaryOperator = funcDecl.getBodyStatementAs(1, BinaryOperator::class.java)
        assertNotNull(binaryOperator)

        // without additional symbols, the second line will look like a function call (to something
        // we do not know)
        val call = binaryOperator.getRhsAs(CallExpression::class.java)
        assertNotNull(call)
        assertEquals("INCREASE", call.name)
    }

    @Test
    @Throws(TranslationException::class)
    fun testWithSymbols() {
        // let's try with symbol definitions
        val tu =
            CXXLanguageFrontend(
                    CPPLanguage(),
                    TranslationConfiguration.builder()
                        .symbols(
                            mapOf(
                                Pair("HELLO_WORLD", "\"Hello World\""),
                                Pair("INCREASE(X)", "X+1")
                            )
                        )
                        .defaultPasses()
                        .build(),
                    ScopeManager()
                )
                .parse(File("src/test/resources/symbols.cpp"))
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val funcDecl = main.iterator().next()
        var binaryOperator = funcDecl.getBodyStatementAs(0, BinaryOperator::class.java)
        assertNotNull(binaryOperator)

        // should be a literal now
        val literal = binaryOperator.getRhsAs(Literal::class.java)
        assertEquals("Hello World", literal.value)

        binaryOperator = funcDecl.getBodyStatementAs(1, BinaryOperator::class.java)
        assertNotNull(binaryOperator)

        // should be expanded to another binary operation 1+1
        val add = binaryOperator.getRhsAs(BinaryOperator::class.java)
        assertNotNull(add)
        assertEquals("+", add.operatorCode)

        val literal2 = add.getLhsAs(Literal::class.java)
        assertEquals(2, literal2.value)

        val literal1 = add.getRhsAs(Literal::class.java)
        assertEquals(1, literal1.value)
    }
}
