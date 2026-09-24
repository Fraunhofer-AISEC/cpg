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

import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.walk
import kotlin.streams.asStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AstSanityTest {

    /** Tests whether a node's AST parent is the same as the parent from AST traversal. */
    @ParameterizedTest
    @MethodSource("getTestFilePaths")
    fun testAstParentSanity(path: Path) {
        val file = path.toFile()
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val walker = SubgraphWalker.IterativeGraphWalker(Strategy::AST_FORWARD)
        walker.registerOnNodeVisit { node, parent ->
            if (parent != null) {
                assertTrue("$parent <> ${node.astParent}") { parent === node.astParent }
            }
        }
        for (c in result.components) {
            walker.iterate(c)
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
