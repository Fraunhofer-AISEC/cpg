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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.testcases.FlowQueriesTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExecutionOrderQueriesTest {

    @Test
    fun testLoopDetection() {
        val result = FlowQueriesTest.loopDetection()
        val start = result.literals.singleOrNull { it.value == "start" }
        assertNotNull(start)
        val executionPath =
            executionPath(
                startNode = start,
                direction = Forward(GraphToFollow.EOG),
                type = Must,
                scope = Interprocedural(),
                predicate = { (it as? Literal<*>)?.value == "Then branch" },
            )
        assertFalse(executionPath.value)
        val paths = executionPath.children
        // There are six paths which succeed and four which fail (two for each branch of the
        // function "a")
        assertEquals(6, paths.filter { it.value == true }.size)
        assertEquals(6, paths.filter { it.value == false }.size)
    }

    @Test
    fun testIntraproceduralExecutionOrderForward() {
        val result = FlowQueriesTest.verySimpleDataflow()

        val literal5 = result.literals.singleOrNull { it.value == 5 }
        assertNotNull(literal5)

        val literal10 = result.literals.singleOrNull { it.value == 10 }
        assertNotNull(literal10)

        val callBaz = result.calls["baz"]
        assertNotNull(callBaz)

        val value5MayReach10 =
            executionPath(
                startNode = literal5,
                direction = Forward(GraphToFollow.EOG),
                type = May,
                scope = Intraprocedural(),
                predicate = { node -> node == literal10 },
            )
        assertTrue(
            value5MayReach10.value,
            "Theres an EOG path between the 5 and the 10. For the may-analysis, we can ignore the failing path.",
        )

        val value5MustReach10 =
            executionPath(
                startNode = literal5,
                direction = Forward(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == literal10 },
            )
        assertFalse(
            value5MustReach10.value,
            "Theres an EOG path between the 5 and the 10. For the must-analysis, we cannot ignore the failing path.",
        )

        val value5MustReachBaz =
            executionPath(
                startNode = literal5,
                direction = Forward(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == callBaz },
            )
        assertTrue(value5MustReachBaz.value, "All EOG paths starting at 5 reach the call to baz.")
    }

    @Test
    fun testIntraproceduralExecutionOrderBackward() {
        val result = FlowQueriesTest.verySimpleDataflow()

        val literal5 = result.literals.singleOrNull { it.value == 5 }
        assertNotNull(literal5)

        val literal10 = result.literals.singleOrNull { it.value == 10 }
        assertNotNull(literal10)

        val callBaz = result.calls["baz"]
        assertNotNull(callBaz)
        val callBazMayReach10 =
            executionPath(
                startNode = callBaz,
                direction = Backward(GraphToFollow.EOG),
                type = May,
                scope = Intraprocedural(),
                predicate = { node -> node == literal10 },
            )
        assertTrue(
            callBazMayReach10.value,
            "Theres a backward EOG path between the call to baz and the 10. For the may-analysis, we can ignore the failing path.",
        )

        val callBazMustReach10 =
            executionPath(
                startNode = callBaz,
                direction = Backward(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == literal10 },
            )
        assertFalse(
            callBazMustReach10.value,
            "Theres a backward EOG path between the call to baz and the 10. For the must-analysis, we cannot ignore the failing path.",
        )

        val callBazMustReach5 =
            executionPath(
                startNode = callBaz,
                direction = Backward(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == literal5 },
            )
        assertTrue(
            callBazMustReach5.value,
            "All backward EOG paths starting the call to baz reach the 5.",
        )
    }

    @Test
    fun testIntraproceduralExecutionOrderBidirectional() {
        val result = FlowQueriesTest.verySimpleDataflow()

        val literal5 = result.literals.singleOrNull { it.value == 5 }
        assertNotNull(literal5)

        val literal10 = result.literals.singleOrNull { it.value == 10 }
        assertNotNull(literal10)

        val callBaz = result.calls["baz"]
        assertNotNull(callBaz)

        val literal10MayReachBaz =
            executionPath(
                startNode = literal10,
                direction = Bidirectional(GraphToFollow.EOG),
                type = May,
                scope = Intraprocedural(),
                predicate = { node -> node == callBaz },
            )
        assertTrue(
            literal10MayReachBaz.value,
            "Theres an EOG path between the 10 and the call to baz. For the may-analysis, we can ignore the failing path.",
        )

        val literal10MayReach5 =
            executionPath(
                startNode = literal10,
                direction = Bidirectional(GraphToFollow.EOG),
                type = May,
                scope = Intraprocedural(),
                predicate = { node -> node == literal5 },
            )
        assertTrue(
            literal10MayReach5.value,
            "Theres an EOG path between the 10 and the call to baz. For the may-analysis, we can ignore the failing path.",
        )

        // TODO: I'm not sure if this actually makes sense (i.e., if we need such a thing). It is
        // probably more interesting to figure out whether there's always a path either before or
        // after the node. Then, the naming could be confusing though
        val literal10MustReachBaz =
            executionPath(
                startNode = literal10,
                direction = Bidirectional(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == callBaz },
            )
        assertFalse(
            literal10MustReachBaz.value,
            "Theres an EOG path between the 10 and the call to baz. For the must-analysis, we cannot ignore the failing backward path.",
        )

        val literal10MustReach5 =
            executionPath(
                startNode = literal10,
                direction = Bidirectional(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == literal5 },
            )
        assertFalse(
            literal10MustReach5.value,
            "Theres an EOG path between the 10 and the 5. For the must-analysis, we cannot ignore the failing forward path.",
        )

        val literal10MustReachBazOr5 =
            executionPath(
                startNode = literal10,
                direction = Bidirectional(GraphToFollow.EOG),
                type = Must,
                scope = Intraprocedural(),
                predicate = { node -> node == callBaz || node == literal5 },
            )
        assertTrue(
            literal10MustReachBazOr5.value,
            "Theres always an EOG path between the 10 and either the 5 or the call to baz.",
        )
    }
}
