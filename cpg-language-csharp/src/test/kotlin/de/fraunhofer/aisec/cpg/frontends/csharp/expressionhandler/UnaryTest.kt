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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class UnaryTest : BaseTest() {

    @Test
    fun testPrefixOperators() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Unary.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val method = foo.methods["prefixOperators"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // int b = -a;
        val minusDecl = body.statements.getOrNull(1)
        assertIs<DeclarationStatement>(minusDecl)
        val b = minusDecl.singleDeclaration
        assertIs<Variable>(b)
        val minus = b.initializer
        assertIs<UnaryOperator>(minus)
        assertEquals("-", minus.operatorCode)
        assertEquals(true, minus.isPrefix)
        assertEquals(false, minus.isPostfix)
        assertIs<Reference>(minus.input)

        // bool d = !c;
        val notDecl = body.statements.getOrNull(3)
        assertIs<DeclarationStatement>(notDecl)
        val d = notDecl.singleDeclaration
        assertIs<Variable>(d)
        val not = d.initializer
        assertIs<UnaryOperator>(not)
        assertEquals("!", not.operatorCode)
        assertEquals(true, not.isPrefix)
        assertEquals(false, not.isPostfix)
        assertIs<Reference>(not.input)

        // int e = ~a;
        val complementDecl = body.statements.getOrNull(4)
        assertIs<DeclarationStatement>(complementDecl)
        val e = complementDecl.singleDeclaration
        assertIs<Variable>(e)
        val complement = e.initializer
        assertIs<UnaryOperator>(complement)
        assertEquals("~", complement.operatorCode)
        assertEquals(true, complement.isPrefix)
        assertEquals(false, complement.isPostfix)
        assertIs<Reference>(complement.input)

        // ++a;
        val preIncrement = body.statements.getOrNull(5)
        assertIs<UnaryOperator>(preIncrement)
        assertEquals("++", preIncrement.operatorCode)
        assertEquals(true, preIncrement.isPrefix)
        assertEquals(false, preIncrement.isPostfix)
        assertIs<Reference>(preIncrement.input)

        // --a;
        val preDecrement = body.statements.getOrNull(6)
        assertIs<UnaryOperator>(preDecrement)
        assertEquals("--", preDecrement.operatorCode)
        assertEquals(true, preDecrement.isPrefix)
        assertEquals(false, preDecrement.isPostfix)
        assertIs<Reference>(preDecrement.input)
    }

    @Test
    fun testPostfixOperators() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Unary.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val method = foo.methods["postfixOperators"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // a++;
        val postIncrement = body.statements.getOrNull(1)
        assertIs<UnaryOperator>(postIncrement)
        assertEquals("++", postIncrement.operatorCode)
        assertEquals(false, postIncrement.isPrefix)
        assertEquals(true, postIncrement.isPostfix)
        val incrementInput = postIncrement.input
        assertIs<Reference>(incrementInput)
        assertEquals("a", incrementInput.name.localName)

        // a--;
        val postDecrement = body.statements.getOrNull(2)
        assertIs<UnaryOperator>(postDecrement)
        assertEquals("--", postDecrement.operatorCode)
        assertEquals(false, postDecrement.isPrefix)
        assertEquals(true, postDecrement.isPostfix)
        val decrementInput = postDecrement.input
        assertIs<Reference>(decrementInput)
        assertEquals("a", decrementInput.name.localName)
    }
}
