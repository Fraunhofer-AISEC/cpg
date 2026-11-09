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
 * A map type is structural, so passing a map to a parameter of the same map type has to resolve the
 * call. The two type objects are created independently -- a type carrying generics is not interned
 * -- and must still compare equal even though they were written in different scopes.
 */
class MapParamIdentityTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("map_param_identity.go")
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
    fun testMapArgumentMatchesMapParameter() {
        val result = analyzeFixture()
        assertNotNull(result)
        val failures = mutableListOf<String>()
        for ((caller, callee) in listOf("Parse" to "Parse", "useCountKeys" to "countKeys")) {
            // `Parse` exists both as a package-level function and as a method; we want the
            // package-level one, i.e. the declaration without a receiver.
            val fn =
                result.functions.firstOrNull {
                    it.name.localName == caller &&
                        it !is de.fraunhofer.aisec.cpg.graph.declarations.Method
                }
            val call = fn?.calls?.firstOrNull { it.name.localName == callee }
            val targets = call?.invokes?.map { "${it.name} inferred=${it.isInferred}" } ?: listOf()
            println("  $caller -> $callee : $targets")
            if (call == null || call.invokes.none { !it.isInferred })
                failures += "$caller -> $callee $targets"
        }
        assertTrue(
            failures.isEmpty(),
            "map arguments did not match their parameters:\n" + failures.joinToString("\n"),
        )
    }
}
