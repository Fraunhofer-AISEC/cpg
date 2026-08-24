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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
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
        val file = File("../test-files/double_scalarmult_vartime.cpp")
        println("File: ${file.absolutePath} (${file.readText().length} chars)")

        lateinit var withoutInterning: de.fraunhofer.aisec.cpg.TranslationResult
        lateinit var withInterning: de.fraunhofer.aisec.cpg.TranslationResult
        // JIT warmup dominates a single-shot measurement in one JVM; alternate the two configs
        // over several rounds and report the last two of each, discarding the first (warmup)
        // round, to get a fairer read on the verification step's actual overhead.
        val timesWithout = mutableListOf<Long>()
        val timesWith = mutableListOf<Long>()
        repeat(4) { round ->
            timesWithout += measureTimeMillis {
                withoutInterning =
                    analyze(listOf(file), file.parentFile.toPath(), false) {
                        it.registerLanguage<CPPLanguage>()
                        it.codeInterning(false)
                    }
            }
            timesWith += measureTimeMillis {
                withInterning =
                    analyze(listOf(file), file.parentFile.toPath(), false) {
                        it.registerLanguage<CPPLanguage>()
                        it.codeInterning(true)
                    }
            }
            println("Round $round: without=${timesWithout.last()} ms, with=${timesWith.last()} ms")
        }
        println(
            "Median (last 2 rounds) without interning: ${timesWithout.takeLast(2).average()} ms"
        )
        println("Median (last 2 rounds) with interning:    ${timesWith.takeLast(2).average()} ms")

        val baselineChars = withoutInterning.nodes.sumOf { (it.code?.length ?: 0).toLong() }
        val totalNodesWithCode = withInterning.nodes.count { it.code != null }
        val internedNodes = withInterning.nodes.filter { it.isCodeInterned }
        val literalNodesAfter = withInterning.nodes.filter { it.code != null && !it.isCodeInterned }
        val internedChars = internedNodes.sumOf { it.code!!.length.toLong() }
        val literalCharsAfter = literalNodesAfter.sumOf { it.code!!.length.toLong() }
        // The shared file text is retained exactly once, regardless of how many nodes intern
        // a range of it.
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

        val file = File("../test-files/double_scalarmult_vartime.cpp")

        val withoutInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<CPPLanguage>()
                it.codeInterning(false)
            }
        val withInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<CPPLanguage>()
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

    /**
     * Compares the cost of materializing `code` two ways: (a) what we actually do -- a
     * `String.substring()` slice out of one in-memory copy of the file's text, shared by all its
     * nodes (no per-node caching of the *materialized* substring -- only the offsets are stored);
     * vs. (b) a more radical alternative that keeps *no* decoded text in memory at all and instead
     * seeks/reads the needed byte range from disk on every access. (b) would shave the ~0.4% of
     * retained heap that the shared file text itself accounts for (see testRetainedHeapSize), at
     * the cost of a syscall pair per access instead of a memcpy.
     */
    @Test
    fun testSliceCost() {
        val file = File("../test-files/double_scalarmult_vartime.cpp")
        val content = file.readText()

        // Build the same line-start index FileContentCache uses internally, and derive a
        // representative sample of (start, end) spans from every node's region -- this file is
        // ASCII, so char offsets and byte offsets coincide, which is what lets approach (b) below
        // seek directly by char offset.
        val lineStarts = mutableListOf(0)
        content.forEachIndexed { i, c -> if (c == '\n') lineStarts += i + 1 }
        fun offsetOf(line: Int, column: Int) = lineStarts.getOrNull(line - 1)?.plus(column - 1)

        val result =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<CPPLanguage>()
                it.codeInterning(false)
            }
        val spans =
            result.nodes.mapNotNull { node ->
                val region = node.location?.region ?: return@mapNotNull null
                val start = offsetOf(region.startLine, region.startColumn) ?: return@mapNotNull null
                val end = offsetOf(region.endLine, region.endColumn) ?: return@mapNotNull null
                if (end in start..content.length) start to end else null
            }
        println("Sample size: ${spans.size} spans")

        val rounds = 5

        // (a) current approach: slice out of the one shared, already-decoded String.
        val inMemoryNanos = measureNanoTime {
            repeat(rounds) { for ((start, end) in spans) content.substring(start, end) }
        }

        // (b) alternative: no decoded text kept around; seek + read the byte range from disk and
        // decode it, every single time `code` is read. A single open file handle is reused (the
        // best case for this approach -- opening a fresh handle per access would be much worse).
        val raf = RandomAccessFile(file, "r")
        val diskNanos = measureNanoTime {
            repeat(rounds) {
                for ((start, end) in spans) {
                    raf.seek(start.toLong())
                    val bytes = ByteArray(end - start)
                    raf.readFully(bytes)
                    String(bytes, StandardCharsets.UTF_8)
                }
            }
        }
        raf.close()

        val totalOps = spans.size.toLong() * rounds
        println(
            "In-memory substring: ${inMemoryNanos / 1_000_000} ms total, ${inMemoryNanos / totalOps} ns/op"
        )
        println(
            "Disk seek+read:      ${diskNanos / 1_000_000} ms total, ${diskNanos / totalOps} ns/op"
        )
        println("Disk approach is ${"%.1f".format(diskNanos.toDouble() / inMemoryNanos)}x slower")
    }
}
