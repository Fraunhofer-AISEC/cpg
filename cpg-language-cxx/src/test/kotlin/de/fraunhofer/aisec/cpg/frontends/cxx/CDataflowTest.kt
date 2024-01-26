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
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
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
                call.arguments.firstOrNull() { it.unwrapReference()?.refersTo == startVariable }
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

        println(main.variables["ctx"]?.printDFG())
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
}

private fun Node.printDFG(): String {
    val builder = StringBuilder()

    builder.append("```mermaid\n")
    builder.append("%%{init: {\"flowchart\": {\"htmlLabels\": false}} }%%\n")
    builder.append("flowchart TD\n")

    val worklist = mutableListOf<PropertyEdge<Node>>()
    val alreadySeen = mutableSetOf<PropertyEdge<Node>>()
    val maxConnections = 25
    var conns = 0

    worklist.addAll(this.nextDFGEdges)

    while (worklist.isNotEmpty() && conns < maxConnections) {
        // Take one edge out of the work-list
        val edge = worklist.removeFirst()
        // Add it to the seen-list
        alreadySeen += edge

        val start = edge.start
        val end = edge.end
        builder.append(
            "${start.hashCode()}[\"${start.nodeLabel}\"]-->|DFG|${end.hashCode()}[\"${end.nodeLabel}\"]\n"
        )
        conns++

        // Add next and prev edges to the work-list (if not already seen)
        var next = end.nextDFGEdges.filter { it !in alreadySeen }
        worklist += next

        var prev = end.prevDFGEdges.filter { it !in alreadySeen }
        worklist += prev

        next = start.nextDFGEdges.filter { it !in alreadySeen }
        worklist += next

        prev = start.prevDFGEdges.filter { it !in alreadySeen }
        worklist += prev
    }

    builder.append("```")

    return builder.toString()
}

private val Node.nodeLabel: String
    get() {
        return "`${this.name}\n(${this::class.simpleName})\n${this.location}`"
    }
