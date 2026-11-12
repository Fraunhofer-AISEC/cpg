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
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * A package-level variable whose initializer contains function literals must not cost the file the
 * declarations that follow it.
 *
 * This is the shape of miekg/dns' generated `ztypes.go`, which opens with a `TypeToRR` map of ~100
 * `func() RR { ... }` literals and then declares the `Header` and `copy` methods for essentially
 * every RR type. Those methods are the only place most RR types obtain their full method set, so
 * dropping them removes those types from the `RR` interface, and every call dispatched through `RR`
 * loses its targets.
 */
class DeclsAfterFuncLiteralMapTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("decls_after_func_literal_map.go")
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

    /** Every method declared after the map has to be present, and bound to its receiver type. */
    @Test
    fun testMethodsAfterFuncLiteralMapArePresent() {
        val result = analyzeFixture()
        assertNotNull(result)

        val byOwner =
            result.methods.groupBy {
                it.recordDeclaration?.name?.localName
                    ?: it.receiver?.type?.root?.name?.localName
                    ?: it.name.parent?.localName
            }
        println("methods by owner: ${byOwner.mapValues { e -> e.value.map { it.name.localName } }}")

        for (owner in listOf("A", "AAAA", "OPT")) {
            val names = byOwner[owner]?.map { it.name.localName } ?: emptyList()
            assertContains(names, "Header", "$owner.Header should be parsed, got $names")
            assertContains(names, "copy", "$owner.copy should be parsed, got $names")
        }
    }

    /**
     * The consequence that actually matters: with the full method set present, the concrete types
     * satisfy the interface and a call through it is devirtualized.
     */
    @Test
    fun testCallThroughInterfaceReachesImplementations() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call = result.functions["useRR"]?.calls?.singleOrNull { it.name.localName == "Header" }
        assertNotNull(call, "useRR should contain the r.Header() call")

        assertTrue(
            call.invokes.any { it.body != null },
            "the call must reach an implementation with a body, otherwise traversal stops; " +
                "resolved to: ${call.invokes.map { it.name }}",
        )
    }
}
