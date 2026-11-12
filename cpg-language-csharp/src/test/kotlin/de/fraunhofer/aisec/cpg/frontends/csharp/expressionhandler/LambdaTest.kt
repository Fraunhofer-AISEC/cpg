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
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Lambda
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LambdaTest : BaseTest() {

    @Test
    fun testLambdas() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Lambdas.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
        assertTrue(tu.allChildren<ProblemExpression>().isEmpty())

        val method = tu.methods["lambdas"]
        assertNotNull(method)

        // `x => x + 1`: the parameter has no type, the expression body is returned
        val inc = method.variables["inc"]?.initializer
        assertIs<Lambda>(inc)
        val incFunction = inc.function
        assertNotNull(incFunction)
        val x = incFunction.parameters.single()
        assertEquals("x", x.name.localName)
        assertIs<UnknownType>(x.type)
        val incBody = incFunction.body
        assertIs<Block>(incBody)
        val incReturn = incBody.statements.single()
        assertIs<Return>(incReturn)
        val sum = incReturn.returnValue
        assertIs<BinaryOperator>(sum)
        val xRef = sum.lhs
        assertIs<Reference>(xRef)
        assertEquals(x, xRef.refersTo)

        // `(int a, int b) => { return a + b + offset; }`
        val add = method.variables["add"]?.initializer
        assertIs<Lambda>(add)
        val addFunction = add.function
        assertNotNull(addFunction)
        assertEquals(listOf("a", "b"), addFunction.parameters.map { it.name.localName })
        assertTrue(addFunction.parameters.all { it.type is IntegerType })
        val offset = addFunction.refs["offset"]
        assertNotNull(offset)
        assertIs<Field>(offset.refersTo)

        // `delegate (int v) { return v * 2; }`
        val twice = method.variables["twice"]?.initializer
        assertIs<Lambda>(twice)
        val v = twice.function?.parameters?.singleOrNull()
        assertNotNull(v)
        assertIs<IntegerType>(v.type)
        assertEquals(v, twice.function?.refs["v"]?.refersTo)

        // `delegate { }` without parameters
        val noParameters = method.variables["noParameters"]?.initializer
        assertIs<Lambda>(noParameters)
        assertEquals(0, noParameters.function?.parameters?.size)
        assertIs<Block>(noParameters.function?.body)
    }
}
