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

import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Ad-hoc benchmark, not part of CI: reports how many characters of `code` payload are retained with
 * [de.fraunhofer.aisec.cpg.TranslationConfiguration.codeInterning] on vs. off.
 *
 * Java's `codeOf` (`JavaLanguageFrontend.codeOf`) returns `astNode.tokenRange.toString()` (falling
 * back to the pretty-printer), i.e. a reconstruction from JavaParser's token stream, not a plain
 * substring of the file. FileContentCache.rangeOf only interns when its own line/column-derived
 * substring matches that reconstruction character for character, so this measures how often that
 * happens to be the case in practice.
 */
@Ignore
class CodeInterningBenchmarkTest {
    @Test
    fun testMemoryFootprint() {
        val file = File("src/test/resources/bouncycastle/AES_CBC.java")
        println("File: ${file.absolutePath} (${file.readText().length} chars)")

        val withInterning =
            analyze(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<JavaLanguage>()
                it.codeInterning(true)
            }

        val totalNodesWithCode = withInterning.nodes.count { it.code != null }
        val internedNodes = withInterning.nodes.filter { it.isCodeInterned }
        val literalNodesAfter = withInterning.nodes.filter { it.code != null && !it.isCodeInterned }
        val internedChars = internedNodes.sumOf { it.code!!.length.toLong() }
        val literalCharsAfter = literalNodesAfter.sumOf { it.code!!.length.toLong() }
        val baselineChars = internedChars + literalCharsAfter
        val sharedFileChars = file.readText().length.toLong()
        val afterChars =
            literalCharsAfter + (if (internedNodes.isNotEmpty()) sharedFileChars else 0L)

        println("Nodes with code: $totalNodesWithCode")
        println(
            "Interned nodes: ${internedNodes.size} (${literalNodesAfter.size} fell back to literal)"
        )
        println("Retained code chars before: $baselineChars")
        println(
            "Retained code chars after:  $afterChars  (literal=$literalCharsAfter, shared file=$sharedFileChars, would-have-been-duplicated=$internedChars)"
        )
        if (baselineChars > 0) {
            println(
                "Reduction: ${baselineChars - afterChars} chars (${"%.1f".format((baselineChars - afterChars) * 100.0 / baselineChars)}%)"
            )
        }
    }
}
