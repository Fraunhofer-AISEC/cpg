/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.GranularityType
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class CDataflowTest {
    @Test
    fun testTLSContext() {
        val file = File("src/test/resources/c/dataflow/tls.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val renegotiate = tu.functions["renegotiate"]
        assertNotNull(renegotiate)

        // Our start function and variable/parameter
        var startFunction = renegotiate
        var startVariable = renegotiate.parameters["ctx"]!!

        // In this first, very basic example we want to have the list of all fields of "ctx" that
        // are written to in the renegotiate function itself (independent of the order). This
        // excludes writes that happen in functions that are called by renegotiate.
        var writtenFields =
            renegotiate
                .memberExpressions {
                    it.access == AccessValues.WRITE &&
                        it.base.unwrapReference()?.refersTo == startVariable
                }
                .toMutableList()
        println(
            "renegotiate itself writes to the following fields: " + writtenFields.map { it.name }
        )

        // In the second example, we want to extend this
        writtenFields =
            startFunction
                .memberExpressions {
                    it.access == AccessValues.WRITE &&
                        it.base.unwrapReference()?.refersTo == startVariable
                }
                .toMutableList()
        // Loop through functions within renegotiate. only one level for now. need to make that
        // recursive
        for (call in renegotiate.calls) {
            // We need to see, if the call connects our ctx with a ctx of the function
            val ctxArg =
                call.arguments.firstOrNull { it.unwrapReference()?.refersTo == startVariable }
                    ?: // function call does not forward our context
                continue

            // update our start function and variable
            startFunction = call.invokes.first()
            startVariable = ctxArg.nextDFG.filterIsInstance<ParameterDeclaration>().first()

            // Add the appropriate fields
            writtenFields +=
                startFunction.memberExpressions {
                    it.access == AccessValues.WRITE &&
                        it.base.unwrapReference()?.refersTo == startVariable
                }
        }

        println(
            "renegotiate and its direct callees writes to the following fields: " +
                writtenFields.map { it.name }
        )

        println(main.variables["ctx"]?.printDFG(100))
    }

    @Test
    fun testRef() {
        val file = File("src/test/resources/c/dataflow/ref.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        println(tu.variables["var"]!!.printDFG())
    }

    @Test
    fun testField() {
        val file = File("src/test/resources/c/dataflow/field.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        println(tu.variables["s1"]!!.printDFG())
    }

    @Test
    fun testNestedField() {
        val file = File("src/test/resources/c/dataflow/nested.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        println(tu.variables["o"]!!.printDFG())
    }
}

private fun Node.printDFG(maxConnections: Int = 25): String {
    val builder = StringBuilder()

    builder.append("```mermaid\n")
    builder.append("flowchart TD\n")

    // We use a set with a defined ordering to hold our worklist to have a somewhat consistent
    // ordering of statements in the mermaid file.
    val worklist = LinkedHashSet<PropertyEdge<Node>>()
    val alreadySeen = identitySetOf<PropertyEdge<Node>>()
    var conns = 0

    worklist.addAll(this.nextDFGEdges)

    while (worklist.isNotEmpty() && conns < maxConnections) {
        // Take one edge out of the work-list
        val edge = worklist.first()
        worklist.remove(edge)

        // Add it to the seen-list
        alreadySeen += edge

        val start = edge.start
        val end = edge.end
        builder.append(
            "${start.hashCode()}[\"${start.nodeLabel}\"]-->|${edge.dfgLabel}|${end.hashCode()}[\"${end.nodeLabel}\"]\n"
        )
        conns++

        // Add next and prev edges to the work-list (if not already seen). We sort the entries by
        // name to have this somewhat consistent across multiple invocations of this function
        var next = end.nextDFGEdges.filter { it !in alreadySeen }.sortedBy { it.end.name }
        worklist += next

        var prev = end.prevDFGEdges.filter { it !in alreadySeen }.sortedBy { it.start.name }
        worklist += prev

        next = start.nextDFGEdges.filter { it !in alreadySeen }.sortedBy { it.end.name }
        worklist += next

        prev = start.prevDFGEdges.filter { it !in alreadySeen }.sortedBy { it.start.name }
        worklist += prev
    }

    builder.append("```")

    return builder.toString()
}

private val PropertyEdge<Node>.dfgLabel: String
    get() {
        val builder = StringBuilder()
        builder.append("\"")
        builder.append("DFG")

        if (this.getProperty(Properties.DFG_GRANULARITY) == GranularityType.PARTIAL) {
            builder.append(
                " (partial, ${(this.getProperty(Properties.DFG_RECORD_MEMBER_FIELD) as? FieldDeclaration)?.name})"
            )
        } else {
            builder.append(" (full)")
        }

        builder.append("\"")
        return builder.toString()
    }
private val Node.nodeLabel: String
    get() {
        return "${this.name}\n(${this::class.simpleName})\n${this.location}"
    }
