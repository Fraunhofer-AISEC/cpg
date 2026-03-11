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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.walk
import kotlin.streams.asStream
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UriNodeIdTest : BaseTest() {

    @ParameterizedTest
    @MethodSource("getTestFilePaths")
    fun testAstIdUniqueness(path: Path) {
        val file = path.toFile()
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        val astIds = mutableListOf<String>()
        for (c in result.components) {
            collectAstIds(c, astIds)
        }

        // for manual checks
        astIds.forEach { println(it) }

        assertEquals(
            astIds.size,
            astIds.toSet().size,
            astIds
                .toSet()
                .filter { x -> astIds.count { it == x } > 1 }
                .toSet()
                .joinToString("\n", prefix = "Duplicate values:\n"),
        )
    }

    fun collectAstIds(n: AstNode, ids: MutableList<String>) {
        ids.add(n.idAst)

        for (child in n.astChildren) {
            collectAstIds(child, ids)
        }
    }

    companion object {
        @JvmStatic
        fun getTestFilePaths(): Stream<Path> {
            return Path("src/test/resources/")
                .walk()
                .filter { it.toString().endsWith("c") || it.toString().endsWith(".cpp") }
                .asStream()
        }
    }
}
