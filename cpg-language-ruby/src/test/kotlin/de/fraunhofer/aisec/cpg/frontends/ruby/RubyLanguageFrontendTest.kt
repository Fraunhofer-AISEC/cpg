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
import de.fraunhofer.aisec.cpg.graph.expressions.Lambda
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RubyLanguageFrontendTest {
    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "ruby")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("function.rb").toFile()), topLevel, true) {
                it.registerLanguage<RubyLanguage>()
            }
        assertNotNull(tu)

        val myFunction = tu.functions["my_function"]
        assertNotNull(myFunction)

        val anotherFunction = tu.functions["another_function"]
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

        val each = tu.calls["each"]
        assertNotNull(each)

        val arg0 = each.arguments[0]
        assertIs<Lambda>(arg0)

        val i = arg0.function.parameters[0]
        assertLocalName("i", i)
    }

    @Test
    fun testMethodVisibility() {
        val topLevel = Path.of("src", "test", "resources", "ruby")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("visibility.rb").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RubyLanguage>()
            }
        assertNotNull(tu)

        val account = tu.records["Account"]
        assertNotNull(account)

        // Ruby forces `initialize` to be private regardless of the ambient default (public here).
        val initialize = account.methods["initialize"]
        assertNotNull(initialize)
        assertEquals(Visibility.PRIVATE, initialize.visibility)
        assertTrue("private" in initialize.modifiers)

        // The default (no modifier) visibility for a Ruby method is public.
        val deposit = account.methods["deposit"]
        assertNotNull(deposit)
        assertEquals(Visibility.PUBLIC, deposit.visibility)
        assertTrue("public" in deposit.modifiers)

        // A bare `protected` statement flips the default for subsequently-defined methods.
        val compare = account.methods["compare"]
        assertNotNull(compare)
        assertEquals(Visibility.PROTECTED, compare.visibility)
        assertTrue("protected" in compare.modifiers)

        // A bare `private` statement flips the default for subsequently-defined methods.
        val recompute = account.methods["recompute"]
        assertNotNull(recompute)
        assertEquals(Visibility.PRIVATE, recompute.visibility)
        assertTrue("private" in recompute.modifiers)

        // A bare `public` statement flips the default back to public.
        val balance = account.methods["balance"]
        assertNotNull(balance)
        assertEquals(Visibility.PUBLIC, balance.visibility)
        assertTrue("public" in balance.modifiers)

        // `private def audit` applies to this single method only.
        val audit = account.methods["audit"]
        assertNotNull(audit)
        assertEquals(Visibility.PRIVATE, audit.visibility)
        assertTrue("private" in audit.modifiers)

        // The `private def` above must not have flipped the default: `close` is still public.
        val close = account.methods["close"]
        assertNotNull(close)
        assertEquals(Visibility.PUBLIC, close.visibility)
        assertTrue("public" in close.modifiers)

        // `private :withdraw, :transfer` retroactively re-tags two already-public methods. The
        // multi-symbol form must re-tag both, and the retag must replace the previous `public`
        // modifier rather than accumulate it.
        for (name in listOf("withdraw", "transfer")) {
            val method = account.methods[name]
            assertNotNull(method)
            assertEquals(Visibility.PRIVATE, method.visibility)
            assertTrue("private" in method.modifiers)
            assertFalse("public" in method.modifiers)
        }
    }
}
