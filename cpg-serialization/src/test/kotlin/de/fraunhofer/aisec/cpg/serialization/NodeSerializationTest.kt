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
package de.fraunhofer.aisec.cpg.serialization

import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeSerializationTest {
    @Test
    fun testFullCodeByDefault() {
        val node = Reference().apply { code = "a +\n  b" }

        assertEquals("a +\n  b", node.toJSON().code)
    }

    @Test
    fun testShortenedCode() {
        val node = Reference().apply { code = "  first line  \nsecond line" }

        assertEquals("first line", node.toJSON(maxCodeLength = 20).code)
        assertEquals("first…", node.toJSON(maxCodeLength = 5).code)
    }

    @Test
    fun testIsInferred() {
        assertFalse(Reference().toJSON().isInferred)
        assertTrue(Reference().apply { isInferred = true }.toJSON().isInferred)
    }

    @Test
    fun testIsImplicit() {
        assertFalse(Reference().toJSON().isImplicit)
        assertTrue(Reference().apply { isImplicit = true }.toJSON().isImplicit)
    }

    @Test
    fun testTypeName() {
        val int = ObjectType("int", listOf(), true, UnknownLanguage)
        assertEquals("int", Reference().apply { type = int }.toJSON().typeName)
        // A node without a type
        assertNull(TranslationUnit().toJSON().typeName)
    }

    // The status of calls, see CallStatus
    private val defined = Function().apply { body = Block() }
    // E.g. declared in a header, as with loadIncludes
    private val declared = Function()
    private val inferred = Function().apply { isInferred = true }

    private fun callTo(vararg targets: Function) = Call().apply { invokes += targets }

    @Test
    fun testCallStatusResolved() {
        assertEquals(CallStatus.RESOLVED, callTo(defined).status)
        // A virtual call can reach a declaration without a body next to an override
        assertEquals(CallStatus.RESOLVED, callTo(declared, defined).status)
        // A declaration, e.g. in a header, whose definition in another file is analysed
        assertEquals(CallStatus.RESOLVED, callTo(Function().apply { definition = defined }).status)
    }

    @Test
    fun testCallStatusExternal() {
        assertEquals(CallStatus.EXTERNAL, callTo(declared).status)
        assertEquals(CallStatus.EXTERNAL, callTo(inferred).status)
    }

    @Test
    fun testCallStatusUnresolved() {
        assertEquals(CallStatus.UNRESOLVED, callTo().status)
    }
}
