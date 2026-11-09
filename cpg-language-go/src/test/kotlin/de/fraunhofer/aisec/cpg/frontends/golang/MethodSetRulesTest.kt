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
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * Go's method set rules (https://go.dev/ref/spec#Method_sets) decide which types implement an
 * interface, and therefore which targets a call through that interface has. A type whose method set
 * is computed too narrowly drops out of the relation entirely and every call dispatched through the
 * interface loses it.
 */
class MethodSetRulesTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("method_set_rules.go")
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
    fun testImplementersOfReader() {
        val result = analyzeFixture()
        assertNotNull(result)

        val reader = result.records["main.Reader"]
        assertNotNull(reader)
        val read = reader.methods["Read"]
        assertNotNull(read)

        val owners =
            read.overriddenBy
                .filterIsInstance<Method>()
                .mapNotNull { it.recordDeclaration?.name?.localName ?: it.name.parent?.localName }
                .toSet()
        println("  Reader.Read.overriddenBy owners = $owners")
        println(
            "  call targets = " +
                result.functions["consume"]
                    ?.calls
                    ?.firstOrNull { it.name.localName == "Read" }
                    ?.invokes
                    ?.map { "${it.name} inferred=${it.isInferred}" }
        )

        for (n in listOf("middle", "outer", "wrapper", "pointerEmbed", "base")) {
            val r = result.records["main.$n"]
            println(
                "  $n: kind=${r?.kind} fields=" +
                    r?.fields?.map {
                        it.name.localName +
                            ":" +
                            it.type +
                            " mods=" +
                            it.modifiers +
                            " rec=" +
                            ((it.type.root as? ObjectType)?.recordDeclaration?.name)
                    }
            )
        }
        // Promotion means the promoted type has no Read of its own -- the override candidate
        // stays base.Read -- so the relation to check is the satisfaction itself.
        val notRelated =
            listOf("base", "middle", "outer", "wrapper", "pointerEmbed").filter { n ->
                val r = result.records["main.$n"]
                val names =
                    (r?.implementedInterfaces?.map { it.name.localName } ?: listOf()) +
                        (r?.superTypes?.map { it.name.localName } ?: listOf())
                "Reader" !in names
            }
        assertTrue(
            notRelated.isEmpty(),
            "these types implement Reader under Go's method set rules but were not related: " +
                "$notRelated",
        )
    }
}
