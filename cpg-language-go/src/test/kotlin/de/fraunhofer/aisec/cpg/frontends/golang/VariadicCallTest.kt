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
 * A call to a function with a variadic parameter has to resolve to that function, whether the
 * variadic arguments are passed individually, omitted entirely, or spread from a slice with `...`.
 *
 * text/template's `parse.Tree.Parse` is declared variadic and called with `funcs...`; failing to
 * match it costs every function reachable through the template parser.
 */
class VariadicCallTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang").resolve("variadic_calls.go").toFile()
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
    fun testVariadicCallsResolve() {
        val result = analyzeFixture()
        assertNotNull(result)

        val cases =
            listOf(
                Triple("Parse", "Parse", "method call where a package function shares the name"),
                Triple("Tree.Parse", "reachedOnlyViaMethod", "body of the method is reachable"),
            )
        val failures = mutableListOf<String>()
        for ((caller, callee, what) in cases) {
            val fn =
                if (caller.contains('.'))
                    result.methods.firstOrNull {
                        it.name.localName == caller.substringAfter('.') &&
                            it.recordDeclaration?.name?.localName == caller.substringBefore('.')
                    }
                else result.functions[caller]
            val call = fn?.calls?.firstOrNull { it.name.localName == callee }
            val targets = call?.invokes?.map { "${it.name} inferred=${it.isInferred}" } ?: listOf()
            println("  $caller -> $callee ($what): $targets")
            if (call == null || call.invokes.none { !it.isInferred }) {
                failures += "$caller -> $callee [$what] got $targets"
            }
        }
        assertTrue(
            failures.isEmpty(),
            "variadic calls not resolved:\n" + failures.joinToString("\n"),
        )
    }
}
