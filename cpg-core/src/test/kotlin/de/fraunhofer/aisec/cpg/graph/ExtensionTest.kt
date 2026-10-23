/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.functional.MIN_CHUNK_SIZE
import de.fraunhofer.aisec.cpg.test.assertLocalName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking

class ExtensionTest {
    @Test
    fun testBodyOrNull() {
        var func = Function()
        var body = Block()

        for (i in 0 until 5) {
            var ref = Reference()
            ref.name = Name("$i")
            body.statements += ref
        }

        func.body = body

        var last = func.bodyOrNull<Reference>(-1)
        assertNotNull(last)
        assertLocalName("4", last)

        var ref = Reference()
        ref.name = Name("single")
        func.body = ref

        var single = func.bodyOrNull<Reference>(0)
        assertEquals(ref, single)
    }

    @Test
    fun testAllUniqueEOGStartersOrSingles() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnit("file")
            val record = newRecord("MyClass", "class")
            tu.declarations += record

            val func = newFunction("myFunc")
            val block = newBlock()
            func.body = block
            func.nextEOG += block
            tu.declarations += func

            assertEquals(listOf(record, func, tu), tu.allUniqueEOGStartersOrSingles)
        }
    }

    /**
     * [collectMaybeParallel] has to return every element its action collected, no matter whether
     * the collection was small enough to be processed sequentially or was split into chunks - the
     * batched writes in [de.fraunhofer.aisec.cpg.helpers.functional.ConcurrentMapLattice.lub]
     * silently lose updates otherwise.
     */
    @Test
    fun testCollectMaybeParallel() = runBlocking {
        for (size in listOf(0, 1, MIN_CHUNK_SIZE - 1, MIN_CHUNK_SIZE, MIN_CHUNK_SIZE * 7 + 3)) {
            val input = (0 until size).toList()
            for (parallelism in listOf(1, 4, 8)) {
                val collected =
                    input.collectMaybeParallel<Int, Int>(parallelism = parallelism) { item, into ->
                        // Collect a varying number of results per item, including none at all, so
                        // that a chunk's output size is not simply its input size.
                        repeat(item % 3) { into += item }
                    }

                assertEquals(
                    input.flatMap { item -> List(item % 3) { item } },
                    collected,
                    "size=$size, parallelism=$parallelism",
                )
            }
        }
    }
}
