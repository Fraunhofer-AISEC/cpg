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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.graph.concepts.file.File
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OverlayNodeEquality {
    @Test
    fun testOverlayNodeEquality() {
        val file1 = File(fileName = "node1")
        val file2 = File(fileName = "node1")
        val file3 = File(fileName = "node2")
        assertEquals(file1, file2)
        assertNotEquals(file1, file3)
        assertNotEquals(file2, file3)

        val call1 = Call()
        call1.location = PhysicalLocation(URI("./some/path"), Region(1, 1, 1, 1))
        call1.code = "open()"
        val call2 = Call()
        call2.location = PhysicalLocation(URI("./some/path"), Region(1, 2, 1, 3))

        // Attaching file1 copies the code and location of call1 into it, which does not make it
        // different from the unattached file2.
        file1.underlyingNode = call1
        assertEquals(file1, file2)
        file2.underlyingNode = call2
        assertNotEquals(file1, file2)
    }
}
