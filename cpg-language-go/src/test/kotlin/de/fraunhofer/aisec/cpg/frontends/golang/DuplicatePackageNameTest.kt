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
 * A Go package is identified by its import path, not by the last element of that path. Two packages
 * may therefore share a name: a vendored fork such as
 * `github.com/google/certificate-transparency-go/asn1` sits next to the standard `encoding/asn1`,
 * and both declare `ObjectIdentifier` with a `String` method.
 *
 * If both collapse into one name, a call through an interface reaches only one of them and every
 * method of the other becomes unreachable. crtsh/ct_monitor has 263 such colliding names over 816
 * records, and its `fmt.Stringer.String` dispatch resolves exclusively to the standard library
 * copies, never to the forks that actually carry the bodies.
 */
class DuplicatePackageNameTest {

    private fun analyzeFixture() =
        analyze(
            listOf("main.go", "encoding/asn1/asn1.go", "fork/asn1/asn1.go").map {
                Path.of("src", "test", "resources", "golang", "duplicate_package_names")
                    .resolve(it)
                    .toFile()
            },
            Path.of("src", "test", "resources", "golang", "duplicate_package_names"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    @Test
    fun testBothPackagesAreDistinguished() {
        val result = analyzeFixture()
        assertNotNull(result)

        val strings =
            result.methods.filter {
                it.name.localName == "String" && it.name.parent?.localName == "ObjectIdentifier"
            }
        println(
            "  String methods: " +
                strings.map {
                    it.name.toString() +
                        "@" +
                        (it.location
                            ?.artifactLocation
                            ?.uri
                            ?.path
                            ?.substringAfterLast("duplicate_package_names/") ?: "?")
                }
        )
        assertEquals(2, strings.size, "both packages declare a String method")

        val call = result.functions["use"]?.calls?.firstOrNull { it.name.localName == "String" }
        assertNotNull(call, "use should contain the s.String() call")
        val targets =
            call.invokes.map {
                (it.location?.artifactLocation?.uri?.path ?: "?").substringAfterLast(
                    "duplicate_package_names/"
                )
            }
        println("  dispatch targets: $targets")

        // Both implementations must be reachable through the interface.
        assertTrue(
            targets.any { "encoding/asn1" in it },
            "the standard copy must be a target, got $targets",
        )
        assertTrue(
            targets.any { "fork/asn1" in it },
            "the forked copy must be a target, got $targets",
        )
    }
}
