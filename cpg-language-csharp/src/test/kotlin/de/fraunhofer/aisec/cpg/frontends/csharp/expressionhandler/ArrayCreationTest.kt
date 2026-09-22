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
import de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ArrayCreationTest : BaseTest() {

    @Test
    fun testSizedArray() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ArrayCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["sized"]
        assertNotNull(method)
        val param = method.parameters["n"]
        assertNotNull(param)
        val body = method.body
        assertIs<Block>(body)

        // return new int[n];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val array = ret.returnValue
        assertIs<ArrayConstruction>(array)

        val type = array.type
        assertIs<PointerType>(type)
        assertIs<IntegerType>(type.elementType)

        val dimension = array.dimensions.single()
        assertIs<Reference>(dimension)
        assertRefersTo(dimension, param)
        assertNull(array.initializer)
    }

    @Test
    fun testInitializedArray() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ArrayCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["initialized"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return new int[] { 1, 2, 3 };
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val array = ret.returnValue
        assertIs<ArrayConstruction>(array)
        assertEquals(0, array.dimensions.size)

        val type = array.type
        assertIs<PointerType>(type)
        assertIs<IntegerType>(type.elementType)

        val initializer = array.initializer
        assertIs<InitializerList>(initializer)
        assertEquals(type, initializer.type)
        val values = initializer.initializers.map { (it as Literal<*>).value }
        assertEquals(listOf(1, 2, 3), values)
    }

    @Test
    fun testImplicitlyTypedArray() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ArrayCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["implicitlyTyped"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return new[] { 1, 2, 3 };
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val array = ret.returnValue
        assertIs<ArrayConstruction>(array)

        val initializer = array.initializer
        assertIs<InitializerList>(initializer)
        val values = initializer.initializers.map { (it as Literal<*>).value }
        assertEquals(listOf(1, 2, 3), values)
    }
}
