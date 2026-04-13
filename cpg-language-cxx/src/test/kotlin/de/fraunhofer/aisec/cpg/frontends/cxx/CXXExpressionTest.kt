/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CXXExpressionTest {
    @Test
    fun testExplicitTypeConversion() {
        val file = File("src/test/resources/cxx/explicit_type_conversion.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // We should have two calls (int and myint64)
        val casts = tu.casts
        assertEquals(2, casts.size)
        assertEquals(listOf("int", "long long int"), casts.map { it.name.localName })

        val cast = tu.casts.firstOrNull()
        assertNotNull(cast)
        assertEquals(cast, cast.expression.astParent)
    }

    @Test
    fun testNewWithTemplateAndInitializerList() {
        val file = File("src/test/resources/cxx/new_initializer_list.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // Template brace-init: `new Foo<int>{42}` – initializer must be a Construction
        val f = tu.variables["f"]
        assertNotNull(f)

        val fNew = f.initializer
        assertIs<New>(fNew)

        val fConstruct = fNew.initializer
        assertIs<Construction>(fConstruct)
        assertEquals(1, fConstruct.arguments.size)
        val fArg = fConstruct.arguments[0]
        assertIs<Literal<*>>(fArg)
        assertEquals(42, (fArg.value as Number).toInt())

        // Non-template brace-init: `new Bar{7}` – initializer must also be a Construction
        val g = tu.variables["g"]
        assertNotNull(g)

        val gNew = g.initializer
        assertIs<New>(gNew)

        val gConstruct = gNew.initializer
        assertIs<Construction>(gConstruct)
        assertEquals(1, gConstruct.arguments.size)
        val gArg = gConstruct.arguments[0]
        assertIs<Literal<*>>(gArg)
        assertEquals(7, (gArg.value as Number).toInt())
    }
}
