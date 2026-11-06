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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.ParameterMemoryValue
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointsToInferredRecordTest {
    @Test
    fun testSummaryAppliesThroughParametersOfAnInferredRecordType() {
        val topLevel = Path.of("src", "test", "resources", "golang", "summaries")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("copy_reader.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
                // Explicit, so the test does not depend on PointsToPass being a default pass
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File(topLevel.toFile(), "io_copy.yml"))
            }
        assertNotNull(tu)

        // io.Copy(&body, r) copies what r refers to into body (see io_copy.yml). The record of
        // io.Reader is only inferred, so r is treated like a parameter of an unknown type and
        // reading body afterward reads the memory behind the caller's argument.
        val copyFunction = tu.functions["copyFromReader"]
        assertNotNull(copyFunction)
        val reached = copyFunction.readOfBodyAfterCopy().prevDFGClosure()
        assertTrue(
            reached.any { it is ParameterMemoryValue && it.name.parent?.localName == "r" },
            "the memory behind r does not reach body, only $reached",
        )
    }

    /** The reference to `body` in `afterCopy := body`. */
    private fun Function.readOfBodyAfterCopy(): Reference {
        val copy = calls["Copy"]
        assertNotNull(copy)
        val read =
            refs.singleOrNull {
                it.name.localName == "body" &&
                    (it.location?.region?.startLine ?: 0) > (copy.location?.region?.startLine ?: 0)
            }
        assertNotNull(read)
        return read
    }

    private fun Node.prevDFGClosure(): Set<Node> {
        val reached = mutableSetOf<Node>()
        val worklist = ArrayDeque(listOf(this))
        while (worklist.isNotEmpty()) {
            val node = worklist.removeFirst()
            if (reached.add(node)) worklist.addAll(node.prevDFG)
        }
        return reached
    }
}
