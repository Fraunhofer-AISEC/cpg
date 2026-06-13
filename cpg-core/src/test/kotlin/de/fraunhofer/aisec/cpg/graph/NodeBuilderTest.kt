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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NodeBuilderTest {
    @Test
    fun testReferenceCodeReusesNameStringWhenEqual() {
        with(TestLanguageFrontend()) {
            val ref = Reference()
            ref.code = buildString { append("foo") }

            // applyMetadata sets a Name and should canonicalize matching Reference.code strings
            ref.applyMetadata(this, name = "foo", rawNode = null, doNotPrependNamespace = true)

            assertEquals("foo", ref.code)
            assertSame(ref.name.toString(), ref.code)
        }
    }
}
