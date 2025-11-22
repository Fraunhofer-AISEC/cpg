/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertNotNull

internal class StronglyConnectedComponentTest : BaseTest() {
    @Test
    fun testNestedLoop() {
        val file = File("src/test/resources/nestedLoop.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val mainFD = result.functions.singleOrNull { it.name.localName == "main" }
        assertNotNull(mainFD)

        val printfLine9Ref = mainFD.refs[1]
        assertNotNull(printfLine9Ref)

        val forLoopNode = mainFD.allChildren<ForStatement>().first()
        assertNotNull(forLoopNode)
        // The for loop node should have one nextEOG edge with an SCC-property, and one without
        // (exiting the loop)
        assertEquals(
            1,
            forLoopNode.nextEOGEdges.filter { it.scc != null && it.end == printfLine9Ref }.size,
        )
        assertEquals(1, forLoopNode.nextEOGEdges.filter { it.scc == null }.size)

        val unaryOP = mainFD.allChildren<UnaryOperator>().singleOrNull()
        assertNotNull(unaryOP)
        // on the end of the loop, the unaryOP also has an SCC-labeled edge (not sure if this is
        // really necessary)
        assertNotNull(unaryOP.nextEOGEdges.singleOrNull()?.scc)

        val forLoopBlock = forLoopNode.basicBlock.singleOrNull()
        assertNotNull(forLoopBlock)
        // The forLoop BB has 2 next Edges. On into the loop (with SCC), and one to the outside
        assertEquals(1, forLoopBlock.nextEOGEdges.filter { it.scc != null }.size)
        assertEquals(1, forLoopBlock.nextEOGEdges.filter { it.scc == null }.size)

        // The other BB in the loop has only one edge, also with an SCC
        assertEquals(
            1,
            forLoopBlock.nextEOGEdges
                .singleOrNull { it.scc != null }
                ?.end
                ?.nextEOGEdges
                ?.filter { it.scc != null }
                ?.size,
        )
    }
}
