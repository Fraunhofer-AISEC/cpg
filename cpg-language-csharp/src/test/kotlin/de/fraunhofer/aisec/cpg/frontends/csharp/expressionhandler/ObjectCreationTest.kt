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
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ObjectCreationTest : BaseTest() {

    @Test
    fun objectCreationTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ObjectCreation.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val bar = tu.records["Bar"]
        assertNotNull(bar)

        val createFooMethod = bar.methods["createFoo"]
        assertNotNull(createFooMethod)
        val body = createFooMethod.body
        assertIs<Block>(body)

        // Foo f = new Foo(42);
        val fVariable = body.variables["f"]
        assertNotNull(fVariable)
        assertEquals("Foo", fVariable.type.name.localName)

        val initializer = fVariable.initializer
        assertNotNull(initializer)
        assertIs<New>(initializer)
        assertEquals("Foo", initializer.type.name.localName)

        val constructCall = initializer.initializer
        assertNotNull(constructCall)
        assertIs<Construction>(constructCall)
        val constructor = constructCall.constructor
        assertNotNull(constructor)
        assertEquals("Foo", constructor.name.localName)

        val args = constructCall.arguments
        assertEquals(1, args.size)
        val firstArg = args.firstOrNull()
        assertNotNull(firstArg)
        assertIs<IntegerType>(firstArg.type)
        assertIs<Literal<*>>(firstArg)
        assertEquals(42, firstArg.value)
    }
}
