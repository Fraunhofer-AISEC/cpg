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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.walk
import kotlin.streams.asStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class UriNodeIdTest : BaseTest() {

    @ParameterizedTest
    @MethodSource("getTestFilePaths")
    fun testAstNodeUniqueness(path: Path) {
        val file = path.toFile()
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val astNodes = mutableListOf<AstNode>()

        val workQueue = ArrayDeque<AstNode>()
        workQueue.add(result)

        while (workQueue.isNotEmpty()) {
            val node = workQueue.removeFirst()
            astNodes.add(node)
            workQueue.addAll(node.astChildren)
        }
        kotlin.test.assertNotNull(astNodes)
        assertTrue(astNodes.isNotEmpty())

        astNodes
            .associateWith { node -> astNodes.count { it === node } }
            .forEach { (node, i) -> assertEquals(i, 1, "$node -> $i") }
    }

    @ParameterizedTest
    @MethodSource("getTestFilePaths")
    fun testAstIdUniqueness(path: Path) {
        val file = path.toFile()
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<CPPLanguage>()
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

    @Test
    fun testSpecificAstIds() {
        val file = File("src/test/resources/c/examples/git/odb.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val astIds = mutableListOf<String>()
        for (c in result.components) {
            collectAstIds(c, astIds)
        }

        // for manual checks
        astIds.forEach { println(it) }
        assertContains(
            astIds,
            "trs/components/application/tus/odb.c/functions/odb_is_source_usable%28object_database*%2C+char*%29bool/blocks/0/ifelses/1/blocks/0/assigns/%3D_2/memberaccesses/object_database%3A%3Asources/references/o",
        )
        assertContains(
            astIds,
            "trs/components/application/tus/odb.c/functions/odb_free_sources%28object_database*%29void/blocks/0/whiles/_0/blocks/0/calls/odb_source_free/memberaccesses/object_database%3A%3Asources",
        )
        assertContains(
            astIds,
            "trs/components/application/tus/odb.c/functions/convert_object_file%28repository*%2C+strbuf*%2C+UNKNOWN%2C+git_hash_algo*%2C+void*%2C+unsigned+long+int%2C+object_type%2C+int%29UNKNOWN/parameters/unsigned+long+int5",
        )
        assertContains(
            astIds,
            "trs/components/application/tus/odb.c/functions/pthread_mutex_init%28UNKNOWN%2C+UNKNOWN%29UNKNOWN",
        )
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
