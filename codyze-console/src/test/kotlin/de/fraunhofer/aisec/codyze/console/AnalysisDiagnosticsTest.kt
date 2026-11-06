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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.*

class AnalysisDiagnosticsTest {
    private val uri = URI("file:/diagnostics.c")

    private fun at(line: Int) = PhysicalLocation(uri, Region(line, 1, line, 10))

    // void main() {
    //   f(x);          line 2, calls the function f
    //   g();           line 3, calls an unknown function
    // }
    private val x =
        Reference().apply {
            name = Name("x")
            location = at(2)
        }
    private val callF =
        Call().apply {
            callee = Reference().apply { name = Name("f") }
            location = at(2)
            arguments += x
        }
    private val callG =
        Call().apply {
            callee = Reference().apply { name = Name("g") }
            location = at(3)
        }
    private val main =
        Function().apply {
            name = Name("main")
            location = at(1)
            body = Block().apply { statements += listOf(callF, callG) }
        }

    init {
        TranslationUnit().apply { declarations += main }
    }

    private val f = Function().apply { name = Name("f") }

    @Test
    fun testGapKinds() {
        assertEquals(listOf(DiagnosticKind.UNRESOLVED_CALL), callG.diagnostics().map { it.kind })
        assertEquals(
            listOf(DiagnosticKind.ANALYSIS_PROBLEM),
            ProblemExpression("cannot parse this").diagnostics().map { it.kind },
        )

        // A call whose target is declared, but has no body, and the target itself
        callF.invokes += f
        assertEquals(listOf(DiagnosticKind.EXTERNAL_CODE), callF.diagnostics().map { it.kind })
        assertEquals(listOf(DiagnosticKind.EXTERNAL_CODE), f.diagnostics().map { it.kind })

        // A call of a function with a body, and a node without any diagnostic
        f.body = Block()
        assertEquals(emptyList(), callF.diagnostics())
        assertEquals(emptyList(), x.diagnostics())
    }

    @Test
    fun testReliesOnCallsAndFunction() {
        callF.invokes += f
        // The argument relies on the call it is passed to, and on the unresolved call in main, but
        // not on the resolved or external calls elsewhere in the function
        assertEquals(setOf<Node>(x, callF, callG), x.reliesOn())
        assertEquals(setOf<Node>(callG), callG.reliesOn())
    }

    @Test
    fun testEvidenceGaps() {
        callF.invokes += f
        val diagnostics = evidenceDiagnostics(mapOf("x" to x))

        // The most severe first
        assertEquals(
            listOf(DiagnosticKind.UNRESOLVED_CALL, DiagnosticKind.EXTERNAL_CODE),
            diagnostics.map { it.kind },
        )
        assertEquals(
            listOf(DiagnosticSeverity.WARNING, DiagnosticSeverity.INFO),
            diagnostics.map { it.severity },
        )
        assertEquals(callG.id, diagnostics[0].location.id)
        assertEquals("an unresolved call to g", diagnostics[0].reason)
        assertEquals(callF.id, diagnostics[1].location.id)
        assertEquals("an external call to f", diagnostics[1].reason)
        assertEquals(listOf("x"), diagnostics[1].evidence)
    }

    @Test
    fun testGapsOfSeveralEvidenceAreMerged() {
        f.body = Block()
        callF.invokes += f
        val diagnostics = evidenceDiagnostics(mapOf("x" to x, "f" to callF))

        // Both rely on the unresolved call in main, which is listed once
        val diagnostic = diagnostics.single()
        assertEquals(callG.id, diagnostic.location.id)
        assertEquals(listOf("x", "f"), diagnostic.evidence)
    }

    @Test
    fun testNoGaps() {
        f.body = Block()
        callF.invokes += f
        main.body = Block().apply { statements += callF }
        assertEquals(emptyList(), evidenceDiagnostics(mapOf("x" to x)))
    }
}
