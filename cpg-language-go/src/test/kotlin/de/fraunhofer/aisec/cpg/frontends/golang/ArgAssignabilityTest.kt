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
 * A call has to resolve to its declaration whenever Go considers the arguments assignable to the
 * parameters, not only when the types are written identically. If it does not, the resolver invents
 * an inferred function without a body and the call graph stops there -- while the real declaration
 * sits unused next to it.
 */
class ArgAssignabilityTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("arg_assignability.go")
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

    private fun check(
        result: de.fraunhofer.aisec.cpg.TranslationResult,
        caller: String,
        callee: String,
    ): String {
        val call = result.functions[caller]?.calls?.firstOrNull { it.name.localName == callee }
        val targets = call?.invokes?.map { "${it.name} inferred=${it.isInferred}" } ?: listOf()
        return "$caller -> $callee : $targets"
    }

    @Test
    fun testDiagnoseAssignability() {
        val result = analyzeFixture()
        assertNotNull(result)
        for ((caller, callee) in
            listOf(
                "callNamedToUnnamed" to "marshalOID",
                "callUnnamedToNamed" to "forEachSAN",
                "callThroughAlias" to "hashF",
                "callImplementerToInterface" to "terminalHookOverride",
            )) {
            println("  " + check(result, caller, callee))
        }
    }

    @Test
    fun testAllCallsResolveToRealDeclarations() {
        val result = analyzeFixture()
        assertNotNull(result)
        val failures = mutableListOf<String>()
        for ((caller, callee) in
            listOf(
                "callNamedToUnnamed" to "marshalOID",
                "callUnnamedToNamed" to "forEachSAN",
                "callThroughAlias" to "hashF",
                "callImplementerToInterface" to "terminalHookOverride",
            )) {
            val call = result.functions[caller]?.calls?.firstOrNull { it.name.localName == callee }
            if (call == null || call.invokes.none { !it.isInferred }) {
                failures += check(result, caller, callee)
            }
        }
        assertTrue(
            failures.isEmpty(),
            "these calls did not reach a real declaration:\n" + failures.joinToString("\n"),
        )
    }
}
