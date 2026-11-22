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
 * An interface is routinely implemented from a different package than the one declaring it. The
 * satisfaction relation may well be established -- the implementer appears among the interface's
 * sub types -- while the override relation that actually drives dispatch is not, in which case the
 * call still resolves only to the body-less interface method.
 *
 * go.uber.org/zap implements go.uber.org/zap/zapcore's `ArrayMarshaler` with two dozen named slice
 * types, and that is the bulk of what certmagic still misses.
 */
class CrossPackageInterfaceTest {

    private fun analyzeFixture() =
        analyze(
            listOf("core/core.go", "zap/zap.go").map {
                Path.of("src", "test", "resources", "golang", "cross_pkg_iface")
                    .resolve(it)
                    .toFile()
            },
            Path.of("src", "test", "resources", "golang", "cross_pkg_iface"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    /** Shows satisfaction and override separately, so it is clear which of the two fails. */
    @Test
    fun testDiagnoseCrossPackageChain() {
        val result = analyzeFixture()
        assertNotNull(result)

        println("=== records ===")
        for (r in result.records) println("  ${r.name} kind=${r.kind}")

        val iface = result.records.firstOrNull { it.name.localName == "ArrayMarshaler" }
        val bools = result.records.firstOrNull { it.name.localName == "bools" }
        println("=== satisfaction ===")
        println("  bools.superTypes = ${bools?.superTypes?.map { it.name }}")
        println("  bools.implementedInterfaces = ${bools?.implementedInterfaces?.map { it.name }}")
        println("  bools.superTypeDeclarations = ${bools?.superTypeDeclarations?.map { it.name }}")
        for (r in
            listOf(bools, result.records.firstOrNull { it.name.localName == "samePkgImpl" })) {
            println(
                "  ${r?.name}: methods=${r?.methods?.map { it.name.localName }} " +
                    "declaringScope=${r?.declaringScope?.javaClass?.simpleName} " +
                    "scope=${r?.scope?.javaClass?.simpleName} " +
                    "recordScopeSymbols=${(r?.declaringScope as? de.fraunhofer.aisec.cpg.graph.scopes.RecordScope)?.symbols?.keys}"
            )
        }
        for (m in result.methods.filter { it.name.localName == "MarshalLogArray" }) {
            println(
                "  method ${m.name} recordDecl=${m.recordDeclaration?.name}\n" +
                    "      type      = ${m.type}\n" +
                    "      signature = ${m.signatureTypes}\n" +
                    "      paramFQNs = ${m.parameters.map { p -> p.type.name.toString() + " [" + p.type.javaClass.simpleName + "]" }}"
            )
        }
        println("=== override ===")
        val im = iface?.methods?.get("MarshalLogArray")
        println("  iface method = ${im?.name}")
        println("  overriddenBy = ${im?.overriddenBy?.map { it.name }}")
        println("=== dispatch ===")
        val call =
            result.functions
                .firstOrNull { it.name.localName == "Encode" }
                ?.calls
                ?.firstOrNull { it.name.localName == "MarshalLogArray" }
        println("  invokes = ${call?.invokes?.map { "${it.name} body=${it.body != null}" }}")
    }

    /** The implementer from the other package must be related to the interface. */
    @Test
    fun testCrossPackageImplementerIsASubType() {
        val result = analyzeFixture()
        assertNotNull(result)

        val bools = result.records.firstOrNull { it.name.localName == "bools" }
        assertNotNull(bools, "the implementing type should be parsed")
        val related =
            (bools.superTypes.map { it.name.localName } +
                bools.implementedInterfaces.map { it.name.localName })
        assertContains(related, "ArrayMarshaler", "got $related")
    }

    /** And it must become a dispatch target, which is what the call graph depends on. */
    @Test
    fun testCrossPackageImplementerIsADispatchTarget() {
        val result = analyzeFixture()
        assertNotNull(result)

        val iface = result.records.firstOrNull { it.name.localName == "ArrayMarshaler" }
        assertNotNull(iface)
        val im = iface.methods["MarshalLogArray"]
        assertNotNull(im)

        val owners =
            im.overriddenBy.filterIsInstance<Method>().mapNotNull {
                it.recordDeclaration?.name?.localName ?: it.name.parent?.localName
            }
        println("  overriddenBy owners = $owners")
        assertContains(owners, "samePkgImpl", "the same-package implementer must be found")
        assertContains(owners, "bools", "the cross-package implementer must be found too")
    }
}
