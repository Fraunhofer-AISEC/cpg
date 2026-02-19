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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.assertLocalName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExtensionTest {
    @Test
    fun testBodyOrNull() {
        var func = FunctionDeclaration()
        var body = Block()

        for (i in 0 until 5) {
            var ref = Reference()
            ref.name = Name("$i")
            body += ref
        }

        func.body = body

        var last = func.bodyOrNull<Reference>(-1)
        assertNotNull(last)
        assertLocalName("4", last)

        var ref = Reference()
        ref.name = Name("single")
        func.body = ref

        var single = func.bodyOrNull<Reference>(0)
        assertEquals(ref, single)
    }

    @Test
    fun testAllUniqueEOGStartersOrSingles() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnit("file")
            val record = newRecord("MyClass", "class")
            tu.declarations += record

            val func = newFunctionDeclaration("myFunc")
            val block = newBlock()
            func.body = block
            func.nextEOG += block
            tu.declarations += func

            assertEquals(listOf(record, func, tu), tu.allUniqueEOGStartersOrSingles)
        }
    }
}
