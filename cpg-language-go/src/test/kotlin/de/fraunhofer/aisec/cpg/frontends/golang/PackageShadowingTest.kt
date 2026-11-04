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
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * A declaration shadows a package of the same name
 * (https://go.dev/ref/spec#Declarations_and_scope), and naming a variable after the package it
 * comes from is ordinary Go: IBM-Cloud/redli writes `liner := liner.NewLiner()` and then calls
 * `liner.AppendHistory(...)` on the *variable*.
 *
 * Reading such a selector as a package-qualified name instead produces a reference to a function
 * that does not exist, so the call resolves to an inferred stub and everything reached through it
 * is lost with it.
 */
class PackageShadowingTest {

    private fun analyzeFixture() =
        analyze(
            listOf("main.go", "liner/liner.go").map {
                Path.of("src", "test", "resources", "golang", "package_shadowing")
                    .resolve(it)
                    .toFile()
            },
            Path.of("src", "test", "resources", "golang", "package_shadowing"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    @Test
    fun testVariableShadowingItsPackage() {
        val result = analyzeFixture()
        assertNotNull(result)

        val main = result.functions["main"]
        assertNotNull(main)

        // The right-hand side of the declaration still refers to the package.
        val ctor = main.calls.firstOrNull { it.name.localName == "NewLiner" }
        assertNotNull(ctor, "the liner.NewLiner() call should be parsed")
        assertTrue(
            ctor.invokes.any { !it.isInferred },
            "liner.NewLiner must still resolve to the package function, got " +
                "${ctor.invokes.map { "${it.name} inferred=${it.isInferred}" }}",
        )

        // Everything after it is a method call on the variable.
        for (name in listOf("AppendHistory", "Close")) {
            val call = main.calls.firstOrNull { it.name.localName == name }
            assertNotNull(call, "the $name call should be parsed")
            val targets = call.invokes.map { "${it.name} inferred=${it.isInferred}" }
            println("  $name -> $targets")
            assertTrue(
                call.invokes.any {
                    !it.isInferred && (it as? Method)?.recordDeclaration?.name?.localName == "State"
                },
                "$name must resolve to the method on State, got $targets",
            )
        }
    }
}
