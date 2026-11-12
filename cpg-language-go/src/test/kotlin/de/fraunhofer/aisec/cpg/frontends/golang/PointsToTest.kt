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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointsToTest {
    @Test
    fun testSummaryAppliesThroughInterfaceArguments() {
        val topLevel = Path.of("src", "test", "resources", "golang", "summaries")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("copy.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
                // Explicit, so the test does not depend on PointsToPass being a default pass
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File(topLevel.toFile(), "io_copy.yml"))
            }
        assertNotNull(tu)

        // io.Copy(&body, src) copies what src refers to into body (see io_copy.yml), so reading
        // body afterward reads the memory behind the caller's argument
        for ((function, source) in
            listOf(
                // An interface refers to other memory, see GoLanguage.hasReferenceSemantics
                "copyFromSource" to "s",
                // A pointer, which already worked before
                "copyFromPointer" to "src",
            )) {
            val copyFunction = tu.functions[function]
            assertNotNull(copyFunction)
            val afterCopy = copyFunction.readOfBodyAfterCopy()
            val reached = afterCopy.prevDFGClosure()
            assertTrue(
                reached.any { it is ParameterMemoryValue && it.name.parent?.localName == source },
                "$function: the memory behind $source does not reach body, only $reached",
            )
        }
    }

    @Test
    fun testReferenceSemantics() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("reference_semantics.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        // Maps, channels, functions and interfaces (declared, anonymous and built-in), also behind
        // a named type, refer to other memory; the others are values
        for ((function, expected) in listOf("references" to true, "values" to false)) {
            val parameters = tu.functions[function]?.parameters
            assertNotNull(parameters, function)
            for (parameter in parameters) {
                assertEquals(
                    expected,
                    parameter.language.hasReferenceSemantics(parameter.type),
                    "${parameter.name.localName}: ${parameter.type.name}",
                )
            }
        }
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
