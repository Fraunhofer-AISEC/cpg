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
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.expressions.TypeReference
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TypeOfExpressionTest : BaseTest() {

    @Test
    fun testTypeOfPredefinedType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("TypeOfExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["ofInt"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return typeof(int);
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val typeRef = ret.returnValue
        assertIs<TypeReference>(typeRef)
        assertLocalName("typeof", typeRef)
        assertIs<IntegerType>(typeRef.referencedType)
    }

    @Test
    fun testTypeOfRecord() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("TypeOfExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val bar = tu.records["Bar"]
        assertNotNull(bar)
        val method = foo.methods["ofBar"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return typeof(Bar);
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val typeRef = ret.returnValue
        assertIs<TypeReference>(typeRef)
        assertLocalName("typeof", typeRef)
        val referencedType = typeRef.referencedType
        assertNotNull(referencedType)
        assertIs<ObjectType>(referencedType)
        assertLocalName(bar.name.localName, referencedType)
    }
}
