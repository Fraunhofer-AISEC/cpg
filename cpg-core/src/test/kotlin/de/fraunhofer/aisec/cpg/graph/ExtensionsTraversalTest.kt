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
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionsTraversalTest {
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
}
