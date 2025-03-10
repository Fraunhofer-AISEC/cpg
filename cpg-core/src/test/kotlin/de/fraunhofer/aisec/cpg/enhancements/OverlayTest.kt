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
package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OverlayTest {

    @Test
    fun testCheckOverlayingDFGAndEOG() {
        with(TestLanguageFrontend()) {
            var overlay1: OverlayNode = object : OverlayNode() {}
            var overlay2: OverlayNode = object : OverlayNode() {}

            overlay1.nextDFG += overlay2

            var codeNode1 = newLiteral(1)
            var codeNode2 = newLiteral(2)

            overlay2.nextDFG += codeNode1
            codeNode1.nextDFG += codeNode2

            codeNode2.nextDFG += overlay1

            assertTrue(overlay1.nextDFGEdges.first().overlaying)
            assertTrue(overlay2.nextDFGEdges.first().overlaying)
            assertFalse(codeNode1.nextDFGEdges.first().overlaying)
            assertTrue(codeNode2.nextDFGEdges.first().overlaying)
        }
    }
}
