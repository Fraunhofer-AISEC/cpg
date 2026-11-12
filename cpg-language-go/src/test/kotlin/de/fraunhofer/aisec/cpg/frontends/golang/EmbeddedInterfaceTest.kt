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
 * Go composes interfaces by embedding: `type ArrayEncoder interface { PrimitiveArrayEncoder; ... }`
 * makes every method of the embedded interface part of ArrayEncoder's method set. A call through
 * the embedding interface to one of those methods therefore has to resolve to the embedded
 * declaration, and from there to the implementations.
 *
 * If it does not, the resolver invents an inferred method on the embedding interface instead. That
 * stub has no body and no implementations, so the call graph stops. zapcore composes its encoders
 * exactly like this, and that is what certmagic still loses.
 */
class EmbeddedInterfaceTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("embedded_interface.go")
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
    fun testDiagnoseEmbeddedInterface() {
        val result = analyzeFixture()
        assertNotNull(result)

        val arr = result.records["main.ArrayEncoder"]
        val prim = result.records["main.PrimitiveArrayEncoder"]
        val json = result.records["main.jsonEncoder"]
        println("  ArrayEncoder.methods      = ${arr?.methods?.map { it.name.localName }}")
        println("  ArrayEncoder.superTypes   = ${arr?.superTypes?.map { it.name }}")
        println("  Primitive.methods         = ${prim?.methods?.map { it.name.localName }}")
        println("  jsonEncoder.implements    = ${json?.implementedInterfaces?.map { it.name }}")
        println(
            "  ArrayEncoder scope symbols = " +
                "${(arr?.declaringScope as? de.fraunhofer.aisec.cpg.graph.scopes.RecordScope)?.symbols?.keys}"
        )
        println(
            "  ArrayEncoder.methods detail = ${arr?.methods?.map { it.name.toString() + " inferred=" + it.isInferred }}"
        )
        val pm = prim?.methods?.get("AppendInt")
        println("  Primitive.AppendInt.overriddenBy = ${pm?.overriddenBy?.map { it.name }}")

        val call =
            result.methods
                .firstOrNull { it.name.localName == "MarshalLogArray" && it.body != null }
                ?.calls
                ?.firstOrNull { it.name.localName == "AppendInt" }
        println(
            "  call args = ${call?.arguments?.map { it.type.toString() + " <- " + it.javaClass.simpleName }}"
        )
        println("  Primitive.AppendInt params = ${pm?.parameters?.map { it.type }}")
        println(
            "  call AppendInt invokes = " +
                "${call?.invokes?.map { "${it.name} inferred=${it.isInferred} body=${it.body != null}" }}"
        )
    }

    /** A call through the embedding interface must reach a real implementation. */
    @Test
    fun testCallThroughEmbeddingInterfaceResolves() {
        val result = analyzeFixture()
        assertNotNull(result)

        val marshal =
            result.methods.firstOrNull {
                it.name.localName == "MarshalLogArray" &&
                    (it.recordDeclaration?.name?.localName ?: it.name.parent?.localName) == "ints"
            }
        assertNotNull(marshal, "ints.MarshalLogArray should be parsed")

        val call = marshal.calls.firstOrNull { it.name.localName == "AppendInt" }
        assertNotNull(call, "it should contain the arr.AppendInt(...) call")

        val targets =
            call.invokes.map { "${it.name} inferred=${it.isInferred} body=${it.body != null}" }
        assertTrue(
            call.invokes.any { !it.isInferred },
            "the call must not resolve only to an inferred stub, got: $targets",
        )
        assertTrue(
            call.invokes.any {
                (it as? Method)?.recordDeclaration?.name?.localName == "jsonEncoder"
            },
            "the call should reach jsonEncoder.AppendInt, got: $targets",
        )
    }
}
