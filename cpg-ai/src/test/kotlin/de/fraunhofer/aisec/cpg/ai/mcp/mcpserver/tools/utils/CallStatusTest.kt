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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import kotlin.test.Test
import kotlin.test.assertEquals

class CallStatusTest {
    private val defined = Function().apply { body = Block() }
    // E.g. declared in a header, as with loadIncludes
    private val declared = Function()
    private val inferred = Function().apply { isInferred = true }

    private fun callTo(vararg targets: Function) = Call().apply { invokes += targets }

    @Test
    fun testResolved() {
        assertEquals(CallStatus.RESOLVED, callTo(defined).status)
        // A virtual call can reach a declaration without a body next to an override
        assertEquals(CallStatus.RESOLVED, callTo(declared, defined).status)
    }

    @Test
    fun testExternal() {
        assertEquals(CallStatus.EXTERNAL, callTo(declared).status)
        assertEquals(CallStatus.EXTERNAL, callTo(inferred).status)
    }

    @Test
    fun testUnresolved() {
        assertEquals(CallStatus.UNRESOLVED, callTo().status)
    }
}
