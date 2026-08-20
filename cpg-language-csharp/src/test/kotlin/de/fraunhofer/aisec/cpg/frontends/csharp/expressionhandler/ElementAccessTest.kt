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
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElementAccessTest : BaseTest() {

    @Test
    fun arrayAccessTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["ArrayAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return values[i];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val subscription = ret.returnValue
        assertIs<Subscription>(subscription)

        val array = subscription.arrayExpression
        assertIs<Reference>(array)
        assertUsageOf(array, method.parameters["values"])

        val index = subscription.subscriptExpression
        assertIs<Reference>(index)
        assertUsageOf(index, method.parameters["i"])
    }

    @Test
    fun arrayWriteTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["ArrayWrite"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // values[0] = 42;
        val assign = body.statements.single()
        assertIs<Assign>(assign)
        val subscription = assign.lhs.single()
        assertIs<Subscription>(subscription)
        assertEquals(AccessValues.WRITE, subscription.access)

        val index = subscription.subscriptExpression
        assertIs<Literal<*>>(index)
        assertEquals(0, index.value)
    }

    @Test
    fun indexerAccessTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["IndexerAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return map["key"];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val subscription = ret.returnValue
        assertIs<Subscription>(subscription)

        val key = subscription.subscriptExpression
        assertIs<Literal<*>>(key)
        assertEquals("key", key.value)
    }

    @Test
    fun multiDimensionalTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["MultiDimensional"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return matrix[i, j];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val subscription = ret.returnValue
        assertIs<Subscription>(subscription)
        assertLocalName("matrix", subscription.arrayExpression)

        val indices = subscription.subscriptExpression
        assertIs<InitializerList>(indices)
        assertTrue(indices.isImplicit)
        assertEquals(2, indices.initializers.size)
        assertUsageOf(indices.initializers[0], method.parameters["i"])
        assertUsageOf(indices.initializers[1], method.parameters["j"])
    }

    @Test
    fun jaggedTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["Multiple"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return values[i][j];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<Subscription>(outer)
        assertLocalName("j", outer.subscriptExpression)

        val inner = outer.arrayExpression
        assertIs<Subscription>(inner)
        assertLocalName("values", inner.arrayExpression)
        assertLocalName("i", inner.subscriptExpression)
    }

    @Test
    fun nestedIndexTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["NestedIndex"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return values[indices[i]];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<Subscription>(outer)
        assertLocalName("values", outer.arrayExpression)

        val inner = outer.subscriptExpression
        assertIs<Subscription>(inner)
        assertLocalName("indices", inner.arrayExpression)
        assertLocalName("i", inner.subscriptExpression)
    }

    @Test
    fun memberAccessBaseTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ElementAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["MemberAccessBase"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return container.Values[i];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val subscription = ret.returnValue
        assertIs<Subscription>(subscription)

        val member = subscription.arrayExpression
        assertIs<MemberAccess>(member)
        assertLocalName("Values", member)
    }
}
