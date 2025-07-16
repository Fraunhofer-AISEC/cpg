/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.ruby

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.LambdaExpression
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class RubyLanguageFrontendTest {
    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "ruby")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("function.rb").toFile()), topLevel, true) {
                it.registerLanguage<RubyLanguage>()
            }
        assertNotNull(tu)

        val myFunction = tu.dFunctions["my_function"]
        assertNotNull(myFunction)

        val anotherFunction = tu.dFunctions["another_function"]
        assertNotNull(anotherFunction)
    }

    @Test
    fun testVariables() {
        val topLevel = Path.of("src", "test", "resources", "ruby")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("variables.rb").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RubyLanguage>()
            }
        assertNotNull(tu)
    }

    @Test
    fun testIter() {
        val topLevel = Path.of("src", "test", "resources", "ruby")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("iter.rb").toFile()), topLevel, true) {
                it.registerLanguage<RubyLanguage>()
            }
        assertNotNull(tu)

        val each = tu.dCalls["each"]
        assertNotNull(each)

        val arg0 = each.arguments[0]
        assertIs<LambdaExpression>(arg0)

        val i = arg0.function.dParameters[0]
        assertLocalName("i", i)
    }
}
