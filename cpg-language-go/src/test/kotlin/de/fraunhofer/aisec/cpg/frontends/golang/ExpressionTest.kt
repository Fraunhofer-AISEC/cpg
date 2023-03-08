/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLiteralValue
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import java.nio.file.Path
import kotlin.test.*

class ExpressionTest {
    @Test
    fun testCastExpression() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("type_assert.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.namespaces["main"]
        assertNotNull(main)

        val mainFunc = main.functions["main"]
        assertNotNull(mainFunc)

        val f =
            (mainFunc.bodyOrNull<DeclarationStatement>(0))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(f)

        val s =
            (mainFunc.bodyOrNull<DeclarationStatement>(1))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(s)

        val cast = s.initializer as? CastExpression
        assertNotNull(cast)
        assertFullName("main.MyStruct", cast.castType)
        assertSame(f, (cast.expression as? DeclaredReferenceExpression)?.refersTo)

        val ignored = main.variables("_")
        ignored.forEach { assertIs<CastExpression>(it.initializer) }
    }

    @Test
    fun testSliceExpression() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("slices.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val a = tu.variables["a"]
        assertNotNull(a)
        assertLocalName("int[]", a.type)

        val b = tu.variables["b"]
        assertNotNull(b)
        assertLocalName("int[]", b.type)

        // [:1]
        var slice =
            assertIs<SliceExpression>(
                assertIs<ArraySubscriptionExpression>(b.initializer).subscriptExpression
            )
        assertNull(slice.lowerBound)
        assertLiteralValue(1, slice.upperBound)
        assertNull(slice.third)

        val c = tu.variables["c"]
        assertNotNull(c)
        assertLocalName("int[]", c.type)

        // [1:]
        slice = assertIs(assertIs<ArraySubscriptionExpression>(c.initializer).subscriptExpression)
        assertLiteralValue(1, slice.lowerBound)
        assertNull(slice.upperBound)
        assertNull(slice.third)

        val d = tu.variables["d"]
        assertNotNull(d)
        assertLocalName("int[]", d.type)

        // [0:1]
        slice = assertIs(assertIs<ArraySubscriptionExpression>(d.initializer).subscriptExpression)
        assertLiteralValue(0, slice.lowerBound)
        assertLiteralValue(1, slice.upperBound)
        assertNull(slice.third)

        val e = tu.variables["e"]
        assertNotNull(e)
        assertLocalName("int[]", e.type)

        // [0:1:1]
        slice = assertIs(assertIs<ArraySubscriptionExpression>(e.initializer).subscriptExpression)
        assertLiteralValue(0, slice.lowerBound)
        assertLiteralValue(1, slice.upperBound)
        assertLiteralValue(1, slice.third)
    }
}
