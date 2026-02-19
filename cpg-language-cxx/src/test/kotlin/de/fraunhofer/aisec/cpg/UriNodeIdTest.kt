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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertNotNull

class UriNodeIdTest : BaseTest() {

    val astIds = mutableListOf<String>()

    @Test
    fun testUriNodeId() {
        val file = File("src/test/resources/c/hello-world.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        for (c in result.components) {
            printAst(c)
        }

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

    fun printAst(n: AstNode, depth: Int = 0) {
        println("${"\t".repeat(depth)}${n.idAst}")
        // println("${"\t".repeat(depth)}${n.idAst} $n")
        astIds.add(n.idAst)

        for (child in n.astChildren) {
            printAst(child, depth)
        }
    }
}
