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
 * The builtin `error` interface is the single most frequently dispatched-through interface in real
 * Go code, and it is a special case for structural satisfaction: unlike a user-declared interface,
 * `error` is not parsed from source but synthesized by [GoExtraPass], so its record and its `Error`
 * method come into existence through a different route than every other interface.
 *
 * If that synthesized record is not treated like any other interface, only the types that *embed*
 * `error` explicitly end up related to it, while the far more common structural implementers -- a
 * plain struct with an `Error() string` method -- do not. Every `err.Error()` call then resolves to
 * the body-less `error.Error` and the call graph stops there.
 */
class BuiltinErrorSatisfactionTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("builtin_error_satisfaction.go")
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

    /**
     * Prints the state of each link in the chain, so that the first empty one identifies the pass
     * that fails. Kept as a diagnostic rather than an assertion.
     */
    @Test
    fun testDiagnoseBuiltinErrorChain() {
        val result = analyzeFixture()
        assertNotNull(result)

        val errorRecords = result.records.filter { it.name.localName == "error" }
        println("=== builtin error record ===")
        println("  records named 'error'  = ${errorRecords.map { "${it.name} (kind=${it.kind})" }}")
        for (e in errorRecords) {
            println("  ${e.name}.methods      = ${e.methods.map { it.name.localName }}")
            println("  ${e.name}.kind         = ${e.kind}")
        }

        val apiError = result.records["main.APIError"]
        println("=== concrete implementer ===")
        println("  APIError                = ${apiError?.name}")
        println("  APIError.methods        = ${apiError?.methods?.map { it.name.localName }}")
        println(
            "  APIError.implementedInterfaces = ${apiError?.implementedInterfaces?.map { it.name }}"
        )
        println("  APIError.superTypes     = ${apiError?.superTypes?.map { it.name }}")

        val errorMethods = result.methods.filter { it.name.localName == "Error" }
        println("=== Error methods in the graph ===")
        for (m in errorMethods) {
            println(
                "  ${m.name} record=${m.recordDeclaration?.name} params=${m.parameters.size} body=${m.body != null} overriddenBy=${m.overriddenBy.map { it.name }}"
            )
        }

        println("=== dispatch ===")
        val call = result.functions["report"]?.calls?.firstOrNull { it.name.localName == "Error" }
        println("  call.invokes            = ${call?.invokes?.map { it.name }}")
        println("  invokes have bodies     = ${call?.invokes?.map { it.body != null }}")
    }

    /**
     * A struct that merely provides `Error() string` must be registered as an implementation of the
     * builtin interface, exactly like a structural implementer of a user-declared interface.
     */
    @Test
    fun testStructuralImplementerIsRelatedToBuiltinError() {
        val result = analyzeFixture()
        assertNotNull(result)

        val apiError = result.records["main.APIError"]
        assertNotNull(apiError, "APIError should be parsed")

        val related =
            (apiError.implementedInterfaces.map { it.name.localName } +
                apiError.superTypes.map { it.name.localName })
        assertContains(
            related,
            "error",
            "APIError provides Error() string and must therefore implement the builtin error",
        )
    }

    /**
     * The assertion that actually governs the call graph: the builtin `error.Error` has to know its
     * implementations, because that is what `SymbolResolver.getOverridingCandidates` consumes.
     */
    @Test
    fun testBuiltinErrorMethodKnowsItsImplementations() {
        val result = analyzeFixture()
        assertNotNull(result)

        val builtinError =
            result.methods.firstOrNull {
                it.name.localName == "Error" &&
                    it.recordDeclaration?.name?.localName == "error" &&
                    it.body == null
            }
        assertNotNull(builtinError, "the synthesized error.Error method should exist")

        val overridingRecords =
            builtinError.overriddenBy.filterIsInstance<Method>().mapNotNull {
                it.recordDeclaration?.name?.localName
            }

        assertContains(overridingRecords, "APIError")
        assertTrue(
            "NotAnError" !in overridingRecords,
            "Error with a different arity must not count as an implementation, got: $overridingRecords",
        )
    }

    /** A call through `error` must reach the implementation rather than the body-less method. */
    @Test
    fun testCallThroughBuiltinErrorIsDevirtualized() {
        val result = analyzeFixture()
        assertNotNull(result)

        val report = result.functions["report"]
        assertNotNull(report, "report should be parsed")

        val call = report.calls.singleOrNull { it.name.localName == "Error" }
        assertNotNull(call, "report should contain the err.Error() call")

        val targets =
            call.invokes.map { "${(it as? Method)?.recordDeclaration?.name?.localName}.${it.name}" }
        assertTrue(
            call.invokes.any { (it as? Method)?.recordDeclaration?.name?.localName == "APIError" },
            "the call should reach APIError.Error, but resolved to: $targets",
        )
    }
}
