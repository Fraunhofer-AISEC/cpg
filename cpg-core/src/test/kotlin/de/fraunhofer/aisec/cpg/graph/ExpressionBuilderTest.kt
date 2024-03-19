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

import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edge.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edge.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpressionBuilderTest {
    @Test
    fun testDuplicateWithDFGProperties() {
        val node1 = Literal<Int>()
        val node2 = Reference()
        val granularity = PartialDataflowGranularity(FieldDeclaration())
        val callingContextIn = CallingContextIn(CallExpression())
        node1.addPrevDFG(node2, granularity, callingContextIn)

        val clone = node1.duplicate(false)
        val clonedPrevDFG = clone.prevDFGEdges.single()
        assertTrue(clonedPrevDFG is ContextSensitiveDataflow)
        assertEquals(callingContextIn, clonedPrevDFG.callingContext)
        assertEquals(granularity, clonedPrevDFG.granularity)

        assertEquals(setOf<Node>(node1, clone), node2.nextDFG)
    }

    @Test
    fun testDuplicateWithDFGProperties2() {
        val node1 = Literal<Int>()
        val node2 = Reference()
        val granularity = PartialDataflowGranularity(FieldDeclaration())
        val callingContextIn = CallingContextIn(CallExpression())
        node1.addNextDFG(node2, granularity, callingContextIn)

        val clone = node1.duplicate(false)
        val clonedPrevDFG = clone.nextDFGEdges.single()
        assertTrue(clonedPrevDFG is ContextSensitiveDataflow)
        assertEquals(callingContextIn, clonedPrevDFG.callingContext)
        assertEquals(granularity, clonedPrevDFG.granularity)

        assertEquals(setOf<Node>(node1, clone), node2.prevDFG)
    }
}
