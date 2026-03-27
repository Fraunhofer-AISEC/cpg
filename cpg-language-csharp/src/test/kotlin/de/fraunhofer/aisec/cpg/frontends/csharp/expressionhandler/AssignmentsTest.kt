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
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AssignmentsTest : BaseTest() {

    @Test
    fun testSimpleAssignment() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Assignments.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val method = foo.methods["simpleAssignment"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // a = 5;
        val assign = body.statements.getOrNull(1)
        assertIs<Assign>(assign)
        assertEquals("=", assign.operatorCode)

        val lhs = assign.lhs.singleOrNull()
        assertIs<Reference>(lhs)
        assertEquals("a", lhs.name.localName)

        val rhs = assign.rhs.singleOrNull()
        assertIs<Literal<*>>(rhs)
        assertEquals(5, rhs.value)
    }

    @Test
    fun testCompoundAssignment() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Assignments.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)

        val method = foo.methods["compoundAssignment"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // a += 5;
        val assign = body.statements.getOrNull(1)
        assertIs<Assign>(assign)
        assertEquals("+=", assign.operatorCode)
        assertEquals(true, assign.isCompoundAssignment)

        val lhs = assign.lhs.singleOrNull()
        assertIs<Reference>(lhs)
        assertEquals("a", lhs.name.localName)

        val rhs = assign.rhs.singleOrNull()
        assertIs<Literal<*>>(rhs)
        assertEquals(5, rhs.value)
    }
}
