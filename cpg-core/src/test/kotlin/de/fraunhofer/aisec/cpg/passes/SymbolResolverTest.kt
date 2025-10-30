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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.ReferenceTag
import de.fraunhofer.aisec.cpg.test.assertInvokes
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SymbolResolverTest {
    @Test
    fun testCombinedVariableAndCallResolution() {
        val result = GraphExamples.getCombinedVariableAndCallTest()

        with(result) {
            val type = result.records["TestClass"]?.toType()
            assertNotNull(type)

            val method1 = result.methods["method1"]
            assertNotNull(method1)

            val method2 = result.methods["method2"]
            assertNotNull(method2)

            val constructor = result.methods["TestClass"]
            assertNotNull(constructor)

            val variable = method2.variables["variable"]
            assertEquals(type, variable?.type)

            val ref = method2.refs["variable"]
            assertEquals(type, ref?.type)

            val callmethod1 = method2.calls["method1"]
            assertIs<MemberCallExpression>(callmethod1)
            assertRefersTo(callmethod1.base, method2.receiver)
            assertInvokes(callmethod1, method1)

            val callmethod2 = method2.calls["method2"]
            assertInvokes(callmethod2, method2)

            val construct = method1.calls { it is ConstructExpression }.firstOrNull()
            assertNotNull(construct)
            assertInvokes(construct, constructor)
        }
    }

    @Test
    fun testUniqueTags() {
        val result = GraphExamples.getConditionalExpression()

        val map = mutableMapOf<ReferenceTag, MutableList<Reference>>()

        val refs = result.refs
        refs.forEach {
            // Build a unique tag based on the scope of the reference is in (since this is usually
            // the start scope)
            val list = map.computeIfAbsent(it.referenceTag) { mutableListOf() }
            list += it

            // All elements in the list must have the same scope and name
            assertEquals(1, list.map { ref -> ref.scope }.toSet().size)
            assertEquals(1, list.map { ref -> ref.name }.toSet().size)
        }
    }
}
