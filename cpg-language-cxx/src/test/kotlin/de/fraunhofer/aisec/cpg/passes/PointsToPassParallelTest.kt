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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.helpers.orderEOGStartersInWaves
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointsToPassParallelTest {
    private fun analyze(file: File, parallelism: Int) =
        analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
            it.registerLanguage<CLanguage>()
            it.registerPass<PointsToPass>()
            it.configurePass<PointsToPass>(PointsToPass.Configuration(parallelism = parallelism))
        }

    /** Every data flow edge of the result, described by the classes and locations of its nodes. */
    private fun edges(
        tu: de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
    ): List<String> {
        fun describe(n: Node) =
            "${n::class.simpleName}:${n.name}:${n.location?.region?.startLine}:${n.location?.region?.startColumn}"
        return tu.allChildren<Node>()
            .flatMap { node ->
                node.prevDFGEdges.map { e ->
                    "${describe(e.start)} -> ${describe(node)} [${e.granularity}]"
                }
            }
            .sorted()
    }

    @Test
    fun testParallelAnalysisGivesTheSameEdges() {
        // The C files of the points-to tests that call functions of their own
        for (name in
            listOf(
                "returnfree.c",
                "global_to_arg.c",
                "pmv_deref.c",
                "member_write_in_function.c",
            )) {
            val file = File("src/test/resources/pointsToPass/$name")
            val sequential = analyze(file, 1)
            val parallel = analyze(file, 4)
            assertNotNull(sequential)
            assertNotNull(parallel)
            val expected = edges(sequential)
            assertTrue(expected.isNotEmpty(), "no edges for $name")
            assertContentEquals(expected, edges(parallel), "the edges of $name differ")
        }
    }

    @Test
    fun testWavesAreOrderedCalleesFirst() {
        val file = File("src/test/resources/pointsToPass/returnfree.c")
        val tu = analyze(file, 1)
        assertNotNull(tu)
        val waves = orderEOGStartersInWaves(tu.eogStarters)
        val wave = HashMap<Node, Int>()
        waves.forEachIndexed { i, tasks -> tasks.forEach { task -> task.forEach { wave[it] = i } } }
        assertEquals(tu.eogStarters.size, wave.size, "every starter is in exactly one wave")
        for (f in tu.functions) {
            for (callee in f.callees) {
                val w = wave[f]
                val cw = wave[callee]
                if (w != null && cw != null && callee !== f) {
                    assertTrue(cw <= w, "${callee.name} has to be analyzed before ${f.name}")
                }
            }
        }
    }
}
