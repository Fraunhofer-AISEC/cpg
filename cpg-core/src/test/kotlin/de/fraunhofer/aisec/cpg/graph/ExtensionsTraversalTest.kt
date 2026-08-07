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
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionsTraversalTest {
    private class TestEdge(start: Node, end: Node) : Edge<Node>(start, end) {
        override var labels: Set<String> = emptySet()

        override fun clone(): Edge<Node> {
            return TestEdge(start, end)
        }
    }

    @Test
    fun testIsNodeWithCallStackInPathDoesNotFlagRecursiveRevisitWithDifferentStack() {
        with(TestLanguageFrontend()) {
            val visitedNode = newCall(newReference("target"))
            val outerCall = newCall(newReference("outer"))
            val recursiveCall = newCall(newReference("recursive"))

            val path = listOf(Triple(visitedNode, null, Context.ofCallStack(outerCall)))
            val recursiveContext = Context.ofCallStack(outerCall, recursiveCall)

            assertFalse(isNodeWithCallStackInPath(visitedNode, recursiveContext, path))
        }
    }

    @Test
    fun testIsNodeWithCallStackInPathFlagsSameNodeAndSameStack() {
        with(TestLanguageFrontend()) {
            val visitedNode = newCall(newReference("target"))
            val outerCall = newCall(newReference("outer"))

            val context = Context.ofCallStack(outerCall)
            val path = listOf(Triple(visitedNode, null, context.clone()))

            assertTrue(isNodeWithCallStackInPath(visitedNode, context, path))
        }
    }

    @Test
    fun testIsNodeWithCallStackInPathDoesNotFlagDifferentNode() {
        with(TestLanguageFrontend()) {
            val visitedNode = newCall(newReference("target"))
            val otherNode = newCall(newReference("other"))
            val outerCall = newCall(newReference("outer"))
            val recursiveCall = newCall(newReference("recursive"))

            val path = listOf(Triple(visitedNode, null, Context.ofCallStack(outerCall)))
            val recursiveContext = Context.ofCallStack(outerCall, recursiveCall)

            assertFalse(isNodeWithCallStackInPath(otherNode, recursiveContext, path))
        }
    }

    @Test
    fun testFollowXUntilHit2RevisitsNodeWithDifferentContext() {
        with(TestLanguageFrontend()) {
            val start = newCall(newReference("start"))
            val merge = newCall(newReference("merge"))
            val targetA = newCall(newReference("targetA"))
            val targetB = newCall(newReference("targetB"))
            val callA = newCall(newReference("fA"))
            val callB = newCall(newReference("fB"))

            val edgeStartToMergeA = TestEdge(start, merge)
            val edgeStartToMergeB = TestEdge(start, merge)
            val edgeMergeToTargetA = TestEdge(merge, targetA)
            val edgeMergeToTargetB = TestEdge(merge, targetB)
            var mergeVisits = 0

            val result =
                start.followXUntilHit2(
                    x = { currentNode, context, _, _ ->
                        when (currentNode) {
                            start ->
                                listOf(
                                    Triple(merge, edgeStartToMergeA, Context.ofCallStack(callA)),
                                    Triple(merge, edgeStartToMergeB, Context.ofCallStack(callB)),
                                )
                            merge ->
                                run {
                                    mergeVisits++
                                    if (context.callStack.top == callA) {
                                        listOf(Triple(targetA, edgeMergeToTargetA, context.clone()))
                                    } else {
                                        listOf(Triple(targetB, edgeMergeToTargetB, context.clone()))
                                    }
                                }
                            else -> emptyList()
                        }
                    },
                    findAllPossiblePaths = false,
                    collectFailedPaths = true,
                    earlyTermination = { _, _ -> false },
                    predicate = { it === targetA || it === targetB },
                )

            assertEquals(2, result.fulfilled.size)
            assertEquals(2, mergeVisits)
            assertTrue(result.fulfilled.any { it.nodes.last() === targetA })
            assertTrue(result.fulfilled.any { it.nodes.last() === targetB })
        }
    }

    @Test
    fun testFollowXUntilHit2MemoizesMergeStateForSameContext() {
        with(TestLanguageFrontend()) {
            val start = newCall(newReference("start"))
            val left = newCall(newReference("left"))
            val right = newCall(newReference("right"))
            val merge = newCall(newReference("merge"))
            val target = newCall(newReference("target"))

            val edgeStartToLeft = TestEdge(start, left)
            val edgeStartToRight = TestEdge(start, right)
            val edgeLeftToMerge = TestEdge(left, merge)
            val edgeRightToMerge = TestEdge(right, merge)
            val edgeMergeToTarget = TestEdge(merge, target)

            var mergeVisits = 0

            val result =
                start.followXUntilHit2(
                    x = { currentNode, context, _, _ ->
                        when (currentNode) {
                            start ->
                                listOf(
                                    Triple(left, edgeStartToLeft, context.clone()),
                                    Triple(right, edgeStartToRight, context.clone()),
                                )
                            left -> listOf(Triple(merge, edgeLeftToMerge, context.clone()))
                            right -> listOf(Triple(merge, edgeRightToMerge, context.clone()))
                            merge -> {
                                mergeVisits++
                                listOf(Triple(target, edgeMergeToTarget, context.clone()))
                            }
                            else -> emptyList()
                        }
                    },
                    findAllPossiblePaths = false,
                    collectFailedPaths = true,
                    earlyTermination = { _, _ -> false },
                    predicate = { it === target },
                )

            assertTrue(result.fulfilled.isNotEmpty())
            assertTrue(result.fulfilled.all { it.nodes.last() === target })
            assertTrue(mergeVisits >= 1)
        }
    }

    @Test
    fun testFollowXUntilHit2MemoizesCyclicTailForSharedContext() {
        with(TestLanguageFrontend()) {
            val start = newCall(newReference("start"))
            val left = newCall(newReference("left"))
            val right = newCall(newReference("right"))
            val cycleHead = newCall(newReference("cycleHead"))
            val cycleBody = newCall(newReference("cycleBody"))
            val exit = newCall(newReference("exit"))
            val target = newCall(newReference("target"))

            val edgeStartLeft = TestEdge(start, left)
            val edgeStartRight = TestEdge(start, right)
            val edgeLeftCycle = TestEdge(left, cycleHead)
            val edgeRightCycle = TestEdge(right, cycleHead)
            val edgeHeadBody = TestEdge(cycleHead, cycleBody)
            val edgeBodyHead = TestEdge(cycleBody, cycleHead)
            val edgeHeadExit = TestEdge(cycleHead, exit)
            val edgeExitTarget = TestEdge(exit, target)

            var cycleHeadVisits = 0
            var cycleBodyVisits = 0

            val result =
                start.followXUntilHit2(
                    x = { currentNode, context, _, _ ->
                        when (currentNode) {
                            start ->
                                listOf(
                                    Triple(left, edgeStartLeft, context.clone()),
                                    Triple(right, edgeStartRight, context.clone()),
                                )
                            left -> listOf(Triple(cycleHead, edgeLeftCycle, context.clone()))
                            right -> listOf(Triple(cycleHead, edgeRightCycle, context.clone()))
                            cycleHead -> {
                                cycleHeadVisits++
                                listOf(
                                    Triple(cycleBody, edgeHeadBody, context.clone()),
                                    Triple(exit, edgeHeadExit, context.clone()),
                                )
                            }
                            cycleBody -> {
                                cycleBodyVisits++
                                listOf(Triple(cycleHead, edgeBodyHead, context.clone()))
                            }
                            exit -> listOf(Triple(target, edgeExitTarget, context.clone()))
                            else -> emptyList()
                        }
                    },
                    findAllPossiblePaths = false,
                    collectFailedPaths = true,
                    earlyTermination = { _, _ -> false },
                    predicate = { it === target },
                )

            assertTrue(result.fulfilled.isNotEmpty())
            assertTrue(result.fulfilled.all { it.nodes.last() === target })
            assertTrue(cycleHeadVisits >= 1)
            assertTrue(cycleBodyVisits >= 1)
        }
    }

    @Test
    fun testIdentifyStronglyConnectedComponentsTarjanFindsAllSccs() {
        val graph =
            mapOf(
                "A" to setOf("B"),
                "B" to setOf("A", "C"),
                "C" to setOf("D"),
                "D" to setOf("C", "E"),
                "E" to emptySet(),
            )

        val sccs =
            identifyStronglyConnectedComponentsTarjan(graph.keys) { node -> graph[node].orEmpty() }

        val normalized = sccs.map { it.toSet() }.toSet()
        assertEquals(setOf(setOf("A", "B"), setOf("C", "D"), setOf("E")), normalized)
    }

    @Test
    fun testFollowXUntilHit2SkipsSameContextLoopAndKeepsOtherPath() {
        with(TestLanguageFrontend()) {
            val start = newCall(newReference("start"))
            val loop = newCall(newReference("loop"))
            val target = newCall(newReference("target"))
            val edgeStartToLoop = TestEdge(start, loop)
            val edgeLoopToLoop = TestEdge(loop, loop)
            val edgeLoopToTarget = TestEdge(loop, target)

            val result =
                start.followXUntilHit2(
                    x = { currentNode, context, _, _ ->
                        when (currentNode) {
                            start -> listOf(Triple(loop, edgeStartToLoop, context.clone()))
                            loop ->
                                listOf(
                                    Triple(loop, edgeLoopToLoop, context.clone()),
                                    Triple(target, edgeLoopToTarget, context.clone()),
                                )
                            else -> emptyList()
                        }
                    },
                    collectFailedPaths = true,
                    earlyTermination = { _, _ -> false },
                    predicate = { it === target },
                )

            assertEquals(1, result.fulfilled.size)
            assertTrue(result.fulfilled.single().nodes.last() === target)
        }
    }
}
