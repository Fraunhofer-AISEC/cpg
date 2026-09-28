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
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class PredefinedTypeAccessTest : BaseTest() {

    @Test
    fun testStaticCallOnPredefinedType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("PredefinedTypeAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["PredefinedTypeAccess"]
        assertNotNull(record)

        val method = record.methods["Parse"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return int.Parse(s);
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val call = ret.returnValue
        assertIs<MemberCall>(call)
        assertLocalName("Parse", call)

        val callee = call.callee
        assertIs<MemberAccess>(callee)
        val base = callee.base
        assertIs<Reference>(base)
        assertLocalName("int", base)
    }

    @Test
    fun testStaticFieldOnPredefinedType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("PredefinedTypeAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["PredefinedTypeAccess"]
        assertNotNull(record)

        val method = record.methods["Empty"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return string.Empty;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val memberAccess = ret.returnValue
        assertIs<MemberAccess>(memberAccess)
        assertLocalName("Empty", memberAccess)

        val base = memberAccess.base
        assertIs<Reference>(base)
        assertLocalName("string", base)
    }
}
