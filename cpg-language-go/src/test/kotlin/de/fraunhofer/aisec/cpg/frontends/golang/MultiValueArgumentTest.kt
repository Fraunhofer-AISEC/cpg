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
 * `f(g())` passes all results of `g` as the complete argument list of `f`, provided there are as
 * many of them as `f` has parameters (https://go.dev/ref/spec#Calls). The single argument carries
 * them as a tuple, and comparing that tuple against the first parameter makes the arity look wrong,
 * so the call does not match its declaration and a body-less stub is inferred.
 *
 * text/template relies on this throughout, e.g. `t.newIf(t.parseControl(true, "if"))`.
 */
class MultiValueArgumentTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("multi_value_argument.go")
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
    fun testMultiValueCallAsArgumentList() {
        val result = analyzeFixture()
        assertNotNull(result)
        val failures = mutableListOf<String>()
        for ((caller, callee) in listOf("ifControl" to "newIf", "useTwo" to "takesTwo")) {
            val call = result.functions[caller]?.calls?.firstOrNull { it.name.localName == callee }
            val targets = call?.invokes?.map { "${it.name} inferred=${it.isInferred}" } ?: listOf()
            println("  $caller -> $callee : args=${call?.arguments?.map { it.type.name }} $targets")
            if (call == null || call.invokes.none { !it.isInferred })
                failures += "$caller -> $callee $targets"
        }
        assertTrue(failures.isEmpty(), "unresolved:\n" + failures.joinToString("\n"))
    }
}
