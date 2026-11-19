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
 * A Go method may be declared in any file of its package, including one that is parsed before the
 * file declaring the receiver's type. Whether a method survives must not depend on that order.
 *
 * The frontend can only bind a method to its record while parsing if the record already exists, and
 * a method that could not be bound used to be added neither to a scope nor to the AST -- it was
 * dropped from the graph entirely. In miekg/dns that silently removed 227 of the 232 methods
 * declared in the generated ztypes.go, which is where nearly every resource record type obtains
 * `Header`, `copy` and `len`. Those types then no longer satisfied the `RR` interface and every
 * call dispatched through `RR` lost its targets.
 *
 * The fixture names its files so that the methods are parsed first.
 */
class MethodBeforeTypeTest {

    private fun analyzeFixture() =
        analyze(
            listOf("a_methods.go", "z_types.go").map {
                Path.of("src", "test", "resources", "golang", "method_before_type")
                    .resolve(it)
                    .toFile()
            },
            Path.of("src", "test", "resources", "golang", "method_before_type"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    /** The methods must be in the graph at all, and bound to the record of their receiver. */
    @Test
    fun testMethodsDeclaredBeforeTheirTypeSurvive() {
        val result = analyzeFixture()
        assertNotNull(result)

        val byOwner =
            result.methods.groupBy {
                it.recordDeclaration?.name?.localName
                    ?: it.receiver?.type?.root?.name?.localName
                    ?: it.name.parent?.localName
            }
        println("methods by owner: ${byOwner.mapValues { e -> e.value.map { it.name.localName } }}")

        for (owner in listOf("A", "OPT")) {
            val methods = byOwner[owner] ?: emptyList()
            val names = methods.map { it.name.localName }
            assertContains(names, "Header", "$owner.Header must not be dropped, got $names")
            assertContains(names, "copy", "$owner.copy must not be dropped, got $names")

            // Being present is not enough: anything reasoning over a type's method set reaches it
            // through the record.
            for (m in methods.filter { it.name.localName in setOf("Header", "copy") }) {
                assertEquals(
                    owner,
                    m.recordDeclaration?.name?.localName,
                    "${m.name} must be bound to the record of its receiver",
                )
            }
        }
    }

    /**
     * The consequence that governs the call graph: with complete method sets the concrete types
     * satisfy the interface, so a call through it is devirtualized to an implementation.
     */
    @Test
    fun testCallThroughInterfaceReachesImplementations() {
        val result = analyzeFixture()
        assertNotNull(result)

        for (n in listOf("main.A", "main.OPT", "main.RR")) {
            val r = result.records[n]
            println(
                "  $n implementedInterfaces=${r?.implementedInterfaces?.map { it.name }} " +
                    "superTypes=${r?.superTypes?.map { it.name }} " +
                    "methods=${r?.methods?.map { it.name.localName }}"
            )
        }
        val rrHeader = result.records["main.RR"]?.methods?.get("Header")
        println("  RR.Header.overriddenBy=${rrHeader?.overriddenBy?.map { it.name }}")

        val call = result.functions["useRR"]?.calls?.singleOrNull { it.name.localName == "Header" }
        assertNotNull(call, "useRR should contain the r.Header() call")

        assertTrue(
            call.invokes.any { it.body != null },
            "the call must reach an implementation with a body, otherwise traversal stops; " +
                "resolved to: ${call.invokes.map { it.name }}",
        )
    }
}
