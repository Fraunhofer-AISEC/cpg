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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Ad-hoc benchmark, not part of CI: reports how many characters of `code` payload are retained with
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.codeInterning] on vs. off, for a `.jar`.
 *
 * This frontend has no original source text at all -- `code` is SootUp's re-serialization of the
 * decompiled bytecode (`JVMLanguageFrontend.codeOf`: `SootMethod.body.toString()` /
 * `astNode.toString()`), never a substring of any file on disk. FileContentCache.rangeOf can't
 * verify a match against nothing, so interning is expected to never engage.
 */
@Ignore
class CodeInterningBenchmarkTest {
    @Test
    fun testMemoryFootprint() {
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val jar = topLevel.resolve("literals.jar").toFile()
        println("File: ${jar.absolutePath}")

        val withInterning =
            analyze(listOf(jar), topLevel, false) {
                it.registerLanguage<JVMLanguage>()
                it.codeInterning(true)
            }

        val totalNodesWithCode = withInterning.nodes.count { it.code != null }
        val internedNodes = withInterning.nodes.filter { it.isCodeInterned }
        val literalNodesAfter = withInterning.nodes.filter { it.code != null && !it.isCodeInterned }
        val internedChars = internedNodes.sumOf { it.code!!.length.toLong() }
        val literalCharsAfter = literalNodesAfter.sumOf { it.code!!.length.toLong() }
        val baselineChars = internedChars + literalCharsAfter

        println("Nodes with code: $totalNodesWithCode")
        println(
            "Interned nodes: ${internedNodes.size} (${literalNodesAfter.size} fell back to literal)"
        )
        println("Retained code chars before: $baselineChars")
        println("Retained code chars would-have-been-duplicated if interned: $internedChars")
    }
}
