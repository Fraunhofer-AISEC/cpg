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

class CastTest : BaseTest() {

    @Test
    fun primitiveCastTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["PrimitiveCast"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return (int)d;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val cast = ret.returnValue
        assertIs<Cast>(cast)
        assertLocalName("int", cast.castType)

        val input = cast.expression
        assertIs<Reference>(input)
        assertUsageOf(input, method.parameters["d"])
    }

    @Test
    fun referenceCastTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["ReferenceCast"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return (Base)o;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val cast = ret.returnValue
        assertIs<Cast>(cast)
        assertLocalName("Base", cast.castType)
        assertIs<Reference>(cast.expression)
    }

    @Test
    fun nestedCastTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["NestedCast"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return (int)(long)o;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<Cast>(outer)
        assertLocalName("int", outer.castType)

        val inner = outer.expression
        assertIs<Cast>(inner)
        assertLocalName("long", inner.castType)
        assertLocalName("o", inner.expression)
    }

    @Test
    fun castOfCallTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["CastOfCall"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return (int)Get(o);
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val cast = ret.returnValue
        assertIs<Cast>(cast)
        assertLocalName("int", cast.castType)

        val call = cast.expression
        assertIs<Call>(call)
        assertLocalName("Get", call)
        assertInvokes(call, tu.methods["Get"])
    }

    @Test
    fun safeCastTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["SafeCast"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o as Base;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val cast = ret.returnValue
        assertIs<Cast>(cast)
        assertLocalName("Base", cast.castType)

        val input = cast.expression
        assertIs<Reference>(input)
        assertUsageOf(input, method.parameters["o"])
    }

    @Test
    fun safeCastToPredefinedTypeTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["SafeCastToPredefinedType"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o as string;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val cast = ret.returnValue
        assertIs<Cast>(cast)
        assertLocalName("string", cast.castType)
        assertLocalName("o", cast.expression)
    }

    @Test
    fun safeCastInConditionTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["SafeCastInCondition"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o as Base != null;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val binOp = ret.returnValue
        assertIs<BinaryOperator>(binOp)
        assertEquals("!=", binOp.operatorCode)

        val cast = binOp.lhs
        assertIs<Cast>(cast)
        assertLocalName("Base", cast.castType)
        assertLocalName("o", cast.expression)
    }

    @Test
    fun castInBinaryOperatorTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Casts.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["CastInBinaryOperator"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return (double)i + d;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val binOp = ret.returnValue
        assertIs<BinaryOperator>(binOp)

        val cast = binOp.lhs
        assertIs<Cast>(cast)
        assertLocalName("double", cast.castType)
        assertLocalName("i", cast.expression)
    }
}
