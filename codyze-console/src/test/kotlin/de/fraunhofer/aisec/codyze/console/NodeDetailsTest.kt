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
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.partial
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.*

class NodeDetailsTest {
    private val uri = URI("file:/details.c")

    private fun at(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) =
        PhysicalLocation(uri, Region(startLine, startColumn, endLine, endColumn))

    // void main() {        line 1
    //   puts(x);           line 2, the call spans the columns 3 to 11, its callee 3 to 7
    // }                    line 3
    private val callee =
        Reference().apply {
            name = Name("puts")
            location = at(2, 3, 2, 7)
        }
    private val argument =
        Reference().apply {
            name = Name("x")
            location = at(2, 8, 2, 9)
        }
    private val call =
        Call().apply {
            name = Name("puts")
            location = at(2, 3, 2, 11)
            this.callee = this@NodeDetailsTest.callee
            arguments += argument
        }
    private val body =
        Block().apply {
            location = at(1, 13, 3, 2)
            statements += call
        }
    private val tu =
        TranslationUnit().apply {
            declarations +=
                Function().apply {
                    name = Name("main")
                    location = at(1, 1, 3, 2)
                    body = this@NodeDetailsTest.body
                }
        }

    @Test
    fun testInnermostNode() {
        assertSame(callee, tu.nodeAt(2, 4))
        assertSame(argument, tu.nodeAt(2, 8))
        // Between the argument and the closing parenthesis, only the call contains the position
        assertSame(call, tu.nodeAt(2, 10))
        assertSame(body, tu.nodeAt(3, 1))
    }

    @Test
    fun testEndColumnIsExclusive() {
        assertSame(body, tu.nodeAt(2, 11))
    }

    @Test
    fun testNoNodeOutsideOfTheCode() {
        assertNull(tu.nodeAt(10, 1))
    }

    @Test
    fun testExplicitNodeIsPreferredOverImplicitOne() {
        // An implicit node with the same region as the call, e.g. an inserted conversion
        call.arguments +=
            Literal<Int>().apply {
                isImplicit = true
                location = at(2, 3, 2, 11)
            }
        assertSame(call, tu.nodeAt(2, 10))
    }

    @Test
    fun testDeeperNodeIsPreferredWithTheSameRegion() {
        // A callee that spans the whole call, e.g. a call without parentheses
        callee.location = at(2, 3, 2, 11)
        assertSame(callee, tu.nodeAt(2, 10))
    }

    @Test
    fun testUnresolvedCall() {
        val details = call.toDetailsJSON()
        assertTrue(details.callTargets.isEmpty())
        assertTrue(details.warnings.any { "could not be resolved" in it }, "${details.warnings}")
    }

    @Test
    fun testCallOfInferredFunction() {
        val puts =
            Function().apply {
                name = Name("puts")
                isInferred = true
            }
        call.invokes += puts

        val details = call.toDetailsJSON()
        assertEquals(listOf(puts.id), details.callTargets.map { it.id })
        assertEquals(1, details.warnings.size, "${details.warnings}")
        assertContains(details.warnings.single(), "'puts' is not part of the analysed code")

        // The inferred function itself has neither code nor a body
        val warnings = puts.toDetailsJSON().warnings
        assertTrue(warnings.any { "inferred by the analysis" in it }, "$warnings")
        assertTrue(warnings.any { "only declared" in it }, "$warnings")
    }

    @Test
    fun testCallersCalleesAndEnclosingFunction() {
        val main = tu.declarations.single() as Function
        val puts = Function().apply { name = Name("puts") }
        call.invokes += puts

        assertEquals(listOf(call.id), puts.toDetailsJSON().callers.map { it.id })
        assertEquals(listOf(call.id), main.toDetailsJSON().callees.map { it.id })
        assertEquals(main.id, call.toDetailsJSON().enclosingFunction?.id)
        assertEquals(main.id, main.toDetailsJSON().enclosingFunction?.id)
    }

    @Test
    fun testProblemNode() {
        val problem = ProblemExpression("cannot parse this")
        val warnings = problem.toDetailsJSON().warnings
        assertTrue(
            warnings.any { "could not handle" in it && "cannot parse this" in it },
            "$warnings",
        )
    }

    @Test
    fun testDataflowLabels() {
        val field = Reference().apply { name = Name("field") }
        val fromCall = Reference().apply { name = Name("result") }
        val plain = Reference().apply { name = Name("plain") }
        val intoCall = Reference().apply { name = Name("parameter") }
        argument.prevDFGEdges.add(field) { granularity = partial("field") }
        argument.prevDFGEdges.addContextSensitive(
            fromCall,
            callingContexts = listOf(CallingContextOut(mutableListOf(call))),
        )
        argument.prevDFGEdges += plain
        argument.nextDFGEdges.addContextSensitive(
            intoCall,
            callingContexts = listOf(CallingContextIn(mutableListOf(call))),
        )

        val details = argument.toDetailsJSON()
        assertEquals(
            mapOf("field" to "partial: field", "result" to "out of call", "plain" to null),
            details.dataflowFrom.associate { it.node.name to it.label },
        )
        assertEquals(listOf("into call"), details.dataflowTo.map { it.label })
    }

    @Test
    fun testRelatedNodesAreLimited() {
        repeat(250) { argument.nextDFGEdges += Reference().apply { name = Name("use$it") } }
        assertEquals(200, argument.toDetailsJSON().dataflowTo.size)
    }

    @Test
    fun testReference() {
        val block = Block().apply { code = "if (x) {\n  y();\n}" }
        assertEquals("if (x) {", block.toRefJSON().code)

        val long = Literal<String>().apply { code = "\"" + "a".repeat(200) + "\"" }
        val code = long.toRefJSON().code
        assertEquals(121, code.length)
        assertTrue(code.endsWith("…"))

        val ref = call.toRefJSON()
        assertEquals("details.c", ref.fileName)
        assertEquals(2, ref.startLine)
        assertTrue(ref.prevDFG.isEmpty() && ref.nextDFG.isEmpty())
        assertNull(Reference().toRefJSON().fileName)
    }
}
