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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * Go auto-dereferences a pointer to an array when indexing it: for `s *[16]uint32`, `s[0]` has type
 * `uint32`. Taking only a single dereference step yields the array type instead, and the resulting
 * argument types then no longer match the parameters of the called function, so the call resolves
 * to an inferred stub rather than the real declaration and the call graph stops there.
 *
 * This shape is pervasive in cryptographic code, which is exactly the code this analysis targets:
 * blake3's `compress_pure` passes `s[0]`, `m[0]` and friends to `g(a, b, c, d, mx, my uint32)`
 * throughout.
 */
class PointerToArrayIndexTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("pointer_to_array_index.go")
                    .toFile()
            ),
            Path.of("src", "test", "resources", "golang"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    /** Indexing a pointer to an array must yield the element type. */
    @Test
    fun testIndexingPointerToArrayYieldsElementType() {
        val result = analyzeFixture()
        assertNotNull(result)

        val subscriptions = result.allChildren<Subscription>()
        assertTrue(subscriptions.isNotEmpty(), "the fixture should contain subscriptions")
        for (s in subscriptions) {
            println("  ${s.arrayExpression.name}[...] base=${s.arrayExpression.type} -> ${s.type}")
        }

        for (s in subscriptions) {
            assertEquals(
                "uint32",
                s.type.name.toString(),
                "indexing ${s.arrayExpression.type} must yield the element type",
            )
        }
    }

    /** The consequence: the call reaches the real function rather than an inferred stub. */
    @Test
    fun testCallResolvesToRealFunction() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call = result.functions["rcompress"]?.calls?.singleOrNull { it.name.localName == "g" }
        assertNotNull(call, "rcompress should contain the call to g")

        println("  g invokes = ${call.invokes.map { "${it.name} inferred=${it.isInferred}" }}")
        assertTrue(
            call.invokes.any { !it.isInferred && it.body != null },
            "the call must reach the real g, resolved to: " +
                "${call.invokes.map { "${it.name} inferred=${it.isInferred}" }}",
        )
    }
}
