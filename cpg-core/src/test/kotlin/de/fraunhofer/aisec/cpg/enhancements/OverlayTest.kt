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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.newLiteral
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OverlayTest {

    @Test
    fun testDisconnect() {
        with(TestLanguageFrontend()) {
            val underlay = newLiteral(1)
            val overlay = object : OverlayNode() {}
            overlay.underlyingNode = underlay
            assertTrue(
                underlay.overlayEdges.isNotEmpty(),
                "Overlay edges of underlay should not be empty after connecting overlay to underlay",
            )

            underlay.disconnectFromGraph()
            assertTrue(
                underlay.overlayEdges.isEmpty(),
                "Overlay edges of underlay should be empty after disconnecting underlay from graph",
            )

            // Let's reconnect and try from the other side
            underlay.overlayEdges += overlay
            overlay.disconnectFromGraph()
            assertTrue(
                underlay.overlayEdges.isEmpty(),
                "Overlay edges of underlay should be empty after disconnecting overlay from graph",
            )
        }
    }

    @Test
    fun testCheckOverlayingDFGAndEOG() {
        with(TestLanguageFrontend()) {
            val overlay1: OverlayNode = object : OverlayNode() {}
            val overlay2: OverlayNode = object : OverlayNode() {}

            overlay1.nextDFG += overlay2

            val codeNode1: Node = newLiteral(1)
            val codeNode2: Node = newLiteral(2)

            overlay2.nextDFG += codeNode1
            codeNode1.nextDFG += codeNode2

            codeNode2.nextDFG += overlay1

            assertTrue(overlay1.nextDFGEdges.first().overlaying)
            assertTrue(overlay2.nextDFGEdges.first().overlaying)
            assertFalse(codeNode1.nextDFGEdges.first().overlaying)
            assertTrue(codeNode2.nextDFGEdges.first().overlaying)
        }
    }

    @Test
    fun testUnderlyingNodePropagation() {
        with(TestLanguageFrontend()) {
            val overlay: OverlayNode = object : OverlayNode() {}
            assertTrue { overlay.underlyingNode == null }

            val node = object : Node() {}
            node.location = PhysicalLocation(URI.create(""), Region(10, 10, 10, 10))
            node.code = "Test"

            overlay.underlyingNode = node
            assertTrue { overlay.underlyingNode != null }
            assertTrue { overlay.location == node.location }
            assertTrue { overlay.code == node.code }
        }
    }
}
