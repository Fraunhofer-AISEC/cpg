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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import org.openjdk.jol.info.GraphLayout

/**
 * Ad-hoc benchmark, not part of CI: reports how many characters of `code` payload are retained with
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.codeInterning] on vs. off, on a real
 * multi-thousand-line file.
 */
@Ignore
class CodeInterningBenchmarkTest {
    @Test
    fun testMemoryFootprint() {
        val file = File("../test-files/nova-master/nova/compute/api.py")
        println("File: ${file.absolutePath} (${file.readText().length} chars)")

        val withoutInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<PythonLanguage>()
                it.codeInterning(false)
            }
        val withInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<PythonLanguage>()
                it.codeInterning(true)
            }

        val baselineChars = withoutInterning.nodes.sumOf { (it.code?.length ?: 0).toLong() }
        val totalNodesWithCode = withInterning.nodes.count { it.code != null }
        val internedNodes = withInterning.nodes.filter { it.isCodeInterned }
        val literalNodesAfter = withInterning.nodes.filter { it.code != null && !it.isCodeInterned }
        val internedChars = internedNodes.sumOf { it.code!!.length.toLong() }
        val literalCharsAfter = literalNodesAfter.sumOf { it.code!!.length.toLong() }
        val sharedFileChars = file.readText().length.toLong()
        val afterChars = literalCharsAfter + sharedFileChars

        println("Nodes with code: $totalNodesWithCode")
        println(
            "Interned nodes: ${internedNodes.size} (${literalNodesAfter.size} fell back to literal)"
        )
        println("Retained code chars before: $baselineChars")
        println(
            "Retained code chars after:  $afterChars  (literal=$literalCharsAfter, shared file=$sharedFileChars, would-have-been-duplicated=$internedChars)"
        )
        println(
            "Reduction: ${baselineChars - afterChars} chars (${"%.1f".format((baselineChars - afterChars) * 100.0 / baselineChars)}%)"
        )
    }

    /**
     * Measures the *actual* retained heap size of the parsed graph via JOL's [GraphLayout], which
     * walks the real object graph (including private fields, via reflection) and de-duplicates
     * shared objects by identity -- so a `CodeSpan`'s shared `content` String is counted once no
     * matter how many nodes reference it. This is a ground-truth cross-check of the analytical
     * char-count metric in [testMemoryFootprint].
     */
    @Test
    fun testRetainedHeapSize() {
        // Newer JDKs use record classes (e.g. in ClassLoader internals) that JOL's default
        // Unsafe-based field-offset lookup can't introspect.
        System.setProperty("jol.magicFieldOffset", "true")

        val file = File("../test-files/nova-master/nova/compute/api.py")

        val withoutInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<PythonLanguage>()
                it.codeInterning(false)
            }
        val withInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<PythonLanguage>()
                it.codeInterning(true)
            }

        val layoutWithout = GraphLayout.parseInstance(*withoutInterning.nodes.toTypedArray())
        val layoutWith = GraphLayout.parseInstance(*withInterning.nodes.toTypedArray())

        val sizeWithout = layoutWithout.totalSize()
        val sizeWith = layoutWith.totalSize()

        println("File: ${file.absolutePath}")
        println(
            "Retained heap without interning: $sizeWithout bytes across ${layoutWithout.totalCount()} objects"
        )
        println(
            "Retained heap with interning:    $sizeWith bytes across ${layoutWith.totalCount()} objects"
        )
        println(
            "Reduction: ${sizeWithout - sizeWith} bytes (${"%.1f".format((sizeWithout - sizeWith) * 100.0 / sizeWithout)}%)"
        )
    }
}
