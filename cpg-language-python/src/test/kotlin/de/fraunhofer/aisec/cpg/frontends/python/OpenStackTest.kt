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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edge.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edge.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edge.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.Stack
import kotlin.test.Test
import kotlin.test.assertNotNull

class CallStack(val stack: Stack<CallExpression> = Stack<CallExpression>()) {

    fun push(nextContext: CallExpression) {
        if (
            nextContext !in stack
        ) // Only push if it's not in the chain already. Otherwise, it looks like a circle.
         stack.push(nextContext)
    }

    /**
     * Checks if the [edge] specifies the Calling Context and, if so, checks if it matches the last
     * item on the stack. If it matches, pops the item, otherwise does nothing. Returns an
     * indication of whether to continue with this path (either because the edge doesn't specify the
     * calling context or because it's the right call expression for the last "return").
     */
    private fun pop(callExpr: CallExpression?): Boolean {
        if (callExpr == null) return true
        return if (stack.isNotEmpty() && stack.peek() == callExpr) {
            // If the stack is not empty, the top element has to match to follow the correct path
            stack.pop()
            true
        } else
            stack
                .isEmpty() // If the stack is empty, we still follow this path because we don't have
        // any information.
    }

    /**
     * Changes the CallStack according to [edge] and determines if this path should be followed
     * further. This is the case as long as the edge does not try to follow a call-chain which
     * doesn't make sense w.r.t. the call stack.
     */
    fun handlePropertyEdge(
        edge: PropertyEdge<Node>,
        forwardAnalysis: Boolean
    ): Pair<CallStack, Boolean> {
        val continuePath =
            if (edge !is ContextSensitiveDataflow) true
            else if (forwardAnalysis) {
                if (edge.callingContext is CallingContextIn) {
                    push((edge.callingContext as CallingContextIn).call)
                    true
                } else {
                    pop((edge.callingContext as CallingContextOut).call)
                }
            } else {
                if (edge.callingContext is CallingContextOut) {
                    push((edge.callingContext as CallingContextOut).call)
                    true
                } else {
                    pop((edge.callingContext as CallingContextIn).call)
                }
            }
        return Pair(this, continuePath)
    }

    fun clone(): CallStack {
        return CallStack(this.stack.clone() as Stack<CallExpression>)
    }
}

/**
 * Checks if there's an DFG path starting at node [from] amd reaching node fulfilling [predicate].
 * [forwardAnalysis] determines if we follow the [Node.nextDFGEdges] (if set to `true`) or the
 * [Node.prevDFGEdges] (if set to `false`). Compared to other dfg-checking methods, this one
 * considers inter-procedural paths in a context-sensitive way, i.e., it can distinguish between
 * different function calls and follows only the correct path.
 */
fun hasContextSensitiveDFG(
    from: Node,
    forwardAnalysis: Boolean = false,
    predicate: (Node) -> Boolean,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    continueAfterHit: Boolean = false
): FulfilledAndFailedPaths {
    val worklist = mutableListOf(Pair(listOf(from), CallStack()))
    val fulfilledPaths = mutableListOf<List<Node>>()
    val failedPaths = mutableListOf<List<Node>>()
    val alreadySeen = mutableSetOf<Int>()
    while (worklist.isNotEmpty()) {
        val (currentPath, callStack) = worklist.removeFirst()
        val currentNode = currentPath.last()
        if (!alreadySeen.add(Pair(currentPath, callStack).hashCode())) {
            /*            println(
                "In hasContextSensitiveDFG, Pair ${Pair(currentPath, callStack)} already in alreadySeennodes, skipping"
            )*/
            continue
        }
        if (predicate(currentNode)) {
            fulfilledPaths.add(currentPath)
            if (!findAllPossiblePaths) {
                return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
            }
            if (!continueAfterHit) {
                break
            }
        }
        val edges =
            if (forwardAnalysis) {
                currentNode.nextDFGEdges
            } else {
                currentNode.prevDFGEdges
            }
        if (edges.isEmpty() && collectFailedPaths) {
            failedPaths.add(currentPath)
        } else {
            edges.forEach { edge ->
                val (nextStack, continueIndicator) =
                    if (edges.size == 1) {
                            callStack
                        } else {
                            callStack.clone()
                        }
                        .handlePropertyEdge(edge, forwardAnalysis)
                val nextPair =
                    if (forwardAnalysis) Pair(currentPath + edge.end, nextStack)
                    else Pair(currentPath + edge.start, nextStack)
                if (
                    continueIndicator &&
                        nextPair.hashCode() !in alreadySeen &&
                        // It's ok to go into a function and back out, seeing the node twice, but no
                        // more
                        currentPath.filter { it == nextPair.first.last() }.size <= 2
                )
                    worklist.add(nextPair)
            }
        }
    }
    return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
}

class OpenStackTest : BaseTest() {
    @Test
    fun testOpenStack() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("cert-manager.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        check(result)
    }

    @Test
    fun testOpenStackMagnum() {
        val topLevel =
            Path.of("/Users/chr55316/Repositories/magnum/magnum/tests/unit/api/controllers/v1/")
        val result =
            analyze(listOf(topLevel.toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        check(result)
    }

    @Test
    fun testOpenStackFixed() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("cert-manager-fixed.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        check(result)
    }

    fun check(result: TranslationResult) {
        /*var openWrites =
            result.calls {
                it.name.localName == "open" &&
                    it.arguments[1].evaluate() in listOf("w+", "w", "x", "a")
            }
        assertNotNull(openWrites)

        openWrites.forEach {
            if (checkSensitiveWrite(it)) {
                errorWithFileLocation(it, log, "Write from potential sensitive source")
            }
        }*/
        result
            .calls {
                it.name.localName == "open" &&
                    it.arguments[1].evaluate() in listOf("w+", "w", "x", "a")
            }
            .forEach {
                val (isProblem, where) = checkSensitiveWrite(it)
                if (isProblem) {
                    println(
                        "${PhysicalLocation.locationLink(it.location)}: Write from potential sensitive source to unprotected file"
                    )
                    //where?.printCode(showNumbers = true, linesAhead = 2)
                }
            }
    }

    fun checkSensitiveWrite(call: CallExpression): Pair<Boolean, Node?> {
        // the first param is our file, we want to get the last write
        var file = call.arguments[0].prevFullDFG.singleOrNull()

        // the return value is our io object
        val io = call.nextDFG.singleOrNull()
        if (io == null) {
            return Pair(false, null)
        }

        // let's follow the DFG from this point to a "write" to our IO
        var paths =
            io.followNextEOGEdgesUntilHit {
                it is MemberCallExpression &&
                    it.name.localName == "write" &&
                    it.base?.prevFullDFG?.contains(io) == true &&
                    // we are only interested in writes from sensitive functions
                    hasContextSensitiveDFG(
                            it,
                            predicate = { it ->
                                it is CallExpression && it.name.contains("key") ||
                                    it.name.contains("certificate")
                            }
                        )
                        .fulfilled
                        .isNotEmpty()
            }

        val problemPaths =
            paths.fulfilled.filter {
                !it.any { it ->
                    it is CallExpression &&
                        it.name.localName == "chmod" &&
                        it.arguments[0].prevFullDFG.contains(file) &&
                        it.arguments[1].evaluate() == 384L
                }
            }

        return Pair(problemPaths.isNotEmpty(), problemPaths.firstOrNull()?.lastOrNull())
    }
}
