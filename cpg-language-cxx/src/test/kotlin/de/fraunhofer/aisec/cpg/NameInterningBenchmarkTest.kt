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

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import java.util.IdentityHashMap
import kotlin.test.Ignore
import kotlin.test.Test
import org.openjdk.jol.info.GraphLayout

/**
 * Ad-hoc benchmark, not part of CI: unlike `code`, `Name` interning has no on/off config flag (it
 * is purely additive and behaviorally invisible, like `Node`'s cached hashCode), so there's no
 * "before" state to toggle back to in the same run. Instead, this measures sharing directly: for
 * every node's name chain (the name plus every ancestor via [Name.parent]), it counts how many
 * *chain positions* exist in total (what would have been allocated with zero sharing, since every
 * node used to build its own fresh chain) vs. how many *distinct* [Name] instances those positions
 * actually resolve to (by identity) now that they're interned.
 */
@Ignore
class NameInterningBenchmarkTest {
    @Test
    fun testSmallFile() {
        val file = File("../test-files/double_scalarmult_vartime.cpp")
        reportNameSharing(file, file.parentFile.toPath()) { it.registerLanguage<CPPLanguage>() }
    }

    @Test
    fun testLargeFile() {
        val file = File("../test-files/morbi.c")
        reportNameSharing(file, file.parentFile.toPath()) { it.registerLanguage<CLanguage>() }
    }

    private fun reportNameSharing(
        file: File,
        topLevel: java.nio.file.Path,
        configure: (de.fraunhofer.aisec.cpg.TranslationConfiguration.Builder) -> Unit,
    ) {
        System.setProperty("jol.magicFieldOffset", "true")

        val result = analyze(listOf(file), topLevel, false) { configure(it) }

        var totalChainPositions = 0L
        val distinct = IdentityHashMap<Name, Unit>()
        for (node in result.nodes) {
            var name: Name? = node.name
            while (name != null) {
                totalChainPositions++
                distinct.putIfAbsent(name, Unit)
                name = name.parent
            }
        }

        val distinctNames = distinct.keys.toTypedArray()
        val layout = GraphLayout.parseInstance(*distinctNames)
        val distinctBytes = layout.totalSize()
        val avgBytesPerName =
            if (distinctNames.isNotEmpty()) distinctBytes / distinctNames.size else 0
        val estimatedWithoutInterningBytes = avgBytesPerName * totalChainPositions

        println("File: ${file.absolutePath}")
        println("Total name-chain positions (nodes' names + ancestors): $totalChainPositions")
        println("Distinct Name instances actually allocated:            ${distinctNames.size}")
        println(
            "Sharing ratio: ${"%.1f".format(totalChainPositions.toDouble() / distinctNames.size)}x"
        )
        println("Retained heap of the distinct Name instances: $distinctBytes bytes")
        println(
            "Estimated heap without interning (avg size x total positions): $estimatedWithoutInterningBytes bytes"
        )
        println(
            "Estimated reduction: ${estimatedWithoutInterningBytes - distinctBytes} bytes (${"%.1f".format((estimatedWithoutInterningBytes - distinctBytes) * 100.0 / estimatedWithoutInterningBytes)}%)"
        )
    }
}
