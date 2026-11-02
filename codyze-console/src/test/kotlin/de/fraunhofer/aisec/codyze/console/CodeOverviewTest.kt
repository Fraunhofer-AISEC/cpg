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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.file.File
import de.fraunhofer.aisec.cpg.graph.concepts.file.OpenFile
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.*

class CodeOverviewTest {
    private val uri = URI("file:/overview.c")

    private fun at(line: Int, startColumn: Int = 1, endColumn: Int = 10) =
        PhysicalLocation(uri, Region(line, startColumn, line, endColumn))

    private fun call(name: String, line: Int, vararg targets: Function) =
        Call().apply {
            this.name = Name(name)
            location = at(line)
            callee =
                Reference().apply {
                    this.name = Name(name)
                    location = at(line, 1, name.length + 1)
                }
            invokes += targets
        }

    private val helper = Function().apply { name = Name("helper") }
    private val puts =
        Function().apply {
            name = Name("puts")
            isInferred = true
        }
    private val resolvedCall = call("helper", 2, helper)
    private val externalCall = call("puts", 3, puts)
    private val unresolvedCall = call("unknown", 4)
    private val file = File(underlyingNode = resolvedCall, fileName = "data.txt")
    private val open = OpenFile(underlyingNode = externalCall, concept = file)

    private val tu =
        TranslationUnit().apply {
            declarations +=
                Function().apply {
                    name = Name("main")
                    location = PhysicalLocation(uri, Region(1, 1, 5, 2))
                    body =
                        Block().apply {
                            statements += listOf(resolvedCall, externalCall, unresolvedCall)
                        }
                }
        }

    @Test
    fun testCallStatus() {
        assertEquals(CallStatus.RESOLVED, resolvedCall.status)
        assertEquals(CallStatus.EXTERNAL, externalCall.status)
        assertEquals(CallStatus.UNRESOLVED, unresolvedCall.status)
    }

    @Test
    fun testFunctionCallStats() {
        val main = assertNotNull(tu.toFunctionCallStatsJSON().singleOrNull())
        assertEquals("main", main.function.name)
        assertEquals(3, main.callees)
        assertEquals(1, main.externalCalls)
        assertEquals(1, main.unresolvedCalls)
    }

    @Test
    fun testCallSites() {
        val calls = tu.toCallSitesJSON().associateBy { it.call.name }
        assertEquals(setOf("helper", "puts", "unknown"), calls.keys)

        val puts = assertNotNull(calls["puts"])
        assertEquals(CallStatus.EXTERNAL, puts.status)
        // The callee is located at the name of the target only
        assertEquals(5, puts.callee?.endColumn)
    }

    @Test
    fun testOverlays() {
        val overlays = tu.toOverlaysJSON().associateBy { it.overlay.id }

        val concept = assertNotNull(overlays[file.id])
        assertEquals("file", concept.category)
        assertEquals(resolvedCall.id, concept.underlyingNode?.id)
        assertNull(concept.concept)

        val operation = assertNotNull(overlays[open.id])
        assertEquals(externalCall.id, operation.underlyingNode?.id)
        assertEquals(file.id, operation.concept?.id)
        assertEquals(3, operation.overlay.startLine)
    }

    @Test
    fun testNodeDetailsUseOverlays() {
        val overlay = assertNotNull(externalCall.toDetailsJSON().overlays.singleOrNull())
        assertEquals(open.id, overlay.overlay.id)
        assertEquals(file.id, overlay.concept?.id)
    }
}
