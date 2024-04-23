/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.console.fancyCode
import de.fraunhofer.aisec.cpg.graph.body
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.testcases.GraphExamples
import kotlin.test.Test
import kotlin.test.assertNotNull

class AnalysisTest {
    @Test
    fun testOutOfBounds() {
        val result = GraphExamples.ArrayCpp()

        OutOfBoundsCheck().run(result)
    }

    @Test
    fun testNullPointer() {
        val result = GraphExamples.ArrayJava()

        NullPointerCheck().run(result)
    }

    @Test
    fun testAttribute() {
        val result = GraphExamples.ArrayJava()
        val tu = result.components.flatMap { it.translationUnits }.first()

        val main = tu.byNameOrNull<FunctionDeclaration>("Array.main", true)
        assertNotNull(main)
        val call = main.body<CallExpression>(0)

        var code = call.fancyCode(showNumbers = false)

        // assertEquals("obj.\u001B[36mdoSomething\u001B[0m();", code)
        println(code)

        var decl = main.body<DeclarationStatement>(0)
        code = decl.fancyCode(showNumbers = false)
        println(code)

        decl = main.body(1)
        code = decl.fancyCode(showNumbers = false)
        println(code)

        code = main.fancyCode(showNumbers = false)
        println(code)

        code = call.fancyCode(3, true)
        println(code)
    }
}
