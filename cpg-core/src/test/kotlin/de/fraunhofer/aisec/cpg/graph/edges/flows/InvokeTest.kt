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
package de.fraunhofer.aisec.cpg.graph.edges.flows

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.newCallExpression
import de.fraunhofer.aisec.cpg.graph.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.newReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InvokeTest {
    @Test
    fun testMirror() {
        with(TestLanguageFrontend()) {
            val func = newFunctionDeclaration("myFunc")
            val call = newCallExpression(newReference("myFunc"))
            call.invokes += func

            assertEquals(1, func.calledByEdges.size)

            val edge = func.calledByEdges.firstOrNull()
            assertNotNull(edge)
            assertEquals(call, edge.start)
            assertEquals(func, edge.end)

            val test = func.calledBy
            assertNotNull(test)
            val test1 = test.firstOrNull()
            assertNotNull(test1)
        }
    }
}
