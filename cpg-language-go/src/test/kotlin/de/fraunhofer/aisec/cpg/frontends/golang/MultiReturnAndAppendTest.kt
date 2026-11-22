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
 * `x, err := f()` and the `append` builtin are everywhere in Go. If the bound variable or the
 * result of `append` has no type, the value flows on untyped and any call it is passed to stops
 * matching its declaration, so a body-less stub is inferred and the call graph stops.
 */
class MultiReturnAndAppendTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("multi_return_and_append.go")
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

    @Test
    fun testMultiReturnAndAppendKeepTheirTypes() {
        val result = analyzeFixture()
        assertNotNull(result)
        val failures = mutableListOf<String>()
        for ((caller, callee) in
            listOf(
                "multiReturn" to "parseCheckpoint",
                "multiReturnInIf" to "parseCheckpoint",
                "appendResult" to "keyHash",
            )) {
            val call = result.functions[caller]?.calls?.firstOrNull { it.name.localName == callee }
            val args = call?.arguments?.map { it.type.name.toString() }
            val targets = call?.invokes?.map { "${it.name} inferred=${it.isInferred}" } ?: listOf()
            println("  $caller -> $callee args=$args targets=$targets")
            if (call == null || call.invokes.none { !it.isInferred }) {
                failures += "$caller -> $callee args=$args targets=$targets"
            }
        }
        assertTrue(failures.isEmpty(), "unresolved:\n" + failures.joinToString("\n"))
    }
}
