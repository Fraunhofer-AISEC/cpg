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
 * Go satisfies interfaces structurally: a type implements an interface as soon as its method set
 * contains every method of that interface, and it never names the interface. The type hierarchy
 * therefore has to be derived from the method sets rather than from declared super types.
 *
 * If that does not happen, [de.fraunhofer.aisec.cpg.graph.declarations.Function.overriddenBy] stays
 * empty for interface methods and `SymbolResolver.getOverridingCandidates` cannot devirtualize a
 * call through an interface. The call is then resolved to the interface method, which has no body,
 * and any call graph traversal stops there -- which in turn makes everything that is only reachable
 * through the implementation look unreachable.
 */
class InterfaceSatisfactionTest {

    /**
     * Walks the chain that has to hold for a call through an interface to be devirtualized, and
     * prints the state of every link. Each link is produced by a different pass, so the first one
     * that is empty identifies the pass that fails:
     * 1. `implementedInterfaces` -- GoInterfaceSatisfactionPass
     * 2. `superTypeDeclarations` -- TypeHierarchyResolver.findSupertypeRecords
     * 3. `overriddenBy` -- TypeHierarchyResolver.analyzeOverridingMethods
     * 4. `invokes` -- SymbolResolver.getOverridingCandidates
     */
    @Test
    fun testDiagnoseHierarchyChain() {
        val result = analyzeFixture()
        assertNotNull(result)

        val etype = result.records["main.EType"]
        val aes256 = result.records["main.Aes256"]
        assertNotNull(etype)
        assertNotNull(aes256)

        println("=== 1. GoInterfaceSatisfactionPass ===")
        println("  Aes256.implementedInterfaces = ${aes256.implementedInterfaces.map { it.name }}")
        println("  Aes256.superClasses          = ${aes256.superClasses.map { it.name }}")
        println("  Aes256.superTypes            = ${aes256.superTypes.map { it.name }}")
        println("  Aes256.toType().superTypes   = ${aes256.toType().superTypes.map { it.name }}")

        println("=== 2. TypeHierarchyResolver.findSupertypeRecords ===")
        println("  Aes256.superTypeDeclarations = ${aes256.superTypeDeclarations.map { it.name }}")
        println("  EType.toType().recordDeclaration = ${etype.toType().recordDeclaration?.name}")

        println("=== 3. TypeHierarchyResolver.analyzeOverridingMethods ===")
        val ifaceMethod = etype.methods["EncryptMessage"]
        println("  EType.methods                = ${etype.methods.map { it.name.localName }}")
        println("  Aes256.methods               = ${aes256.methods.map { it.name.localName }}")
        println("  EType.EncryptMessage         = ${ifaceMethod?.name}")
        println("  .overriddenBy                = ${ifaceMethod?.overriddenBy?.map { it.name }}")
        val impl =
            result.methods.firstOrNull {
                it.name.localName == "EncryptMessage" &&
                    it.recordDeclaration?.name?.localName == "Aes256"
            }
        println(
            "  Aes256.EncryptMessage        = ${impl?.name} (record=${impl?.recordDeclaration?.name})"
        )
        println("  .overrides                   = ${impl?.overrides?.map { it.name }}")

        println("=== 4. SymbolResolver ===")
        val call =
            result.functions["getEncryptedData"]?.calls?.firstOrNull {
                it.name.localName == "EncryptMessage"
            }
        println("  call.invokes                 = ${call?.invokes?.map { it.name }}")
        println("  invokes have bodies          = ${call?.invokes?.map { it.body != null }}")
    }

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("interface_satisfaction.go")
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

    /** The fixture itself must parse into the declarations the remaining tests rely on. */
    @Test
    fun testFixtureStructure() {
        val result = analyzeFixture()
        assertNotNull(result)

        val etype = result.records["main.EType"]
        assertNotNull(etype, "the EType interface should be parsed")
        assertEquals("interface", etype.kind)

        for (name in listOf("main.Aes256", "main.Aes128", "main.NotAnEType")) {
            assertNotNull(result.records[name], "$name should be parsed")
        }

        // Go declares methods as top-level functions carrying a receiver, so a
        // concrete type's methods are not part of its record declaration in the
        // source. They must still end up somewhere in the graph.
        val encryptImpls =
            result.methods.filter { it.name.localName == "EncryptMessage" && it.receiver != null }
        assertEquals(
            2,
            encryptImpls.size,
            "both EncryptMessage implementations should be present as methods with a receiver",
        )
    }

    /**
     * The core assertion: the interface method has to know its implementations, because that is
     * what `SymbolResolver.getOverridingCandidates` consumes.
     */
    @Test
    fun testInterfaceMethodKnowsItsImplementations() {
        val result = analyzeFixture()
        assertNotNull(result)

        val etype = result.records["main.EType"]
        assertNotNull(etype)

        val ifaceMethod = etype.methods["EncryptMessage"]
        assertNotNull(ifaceMethod, "EType should declare EncryptMessage")

        val overridingRecords =
            ifaceMethod.overriddenBy.filterIsInstance<Method>().mapNotNull {
                it.recordDeclaration?.name?.localName
            }

        assertContains(overridingRecords, "Aes256")
        assertContains(overridingRecords, "Aes128")
        assertTrue(
            "NotAnEType" !in overridingRecords,
            "a type providing only part of the method set must not be an implementation",
        )
    }

    /**
     * With the hierarchy in place, a call through the interface must reach the implementations
     * rather than stopping at the body-less interface method.
     */
    @Test
    fun testCallThroughInterfaceIsDevirtualized() {
        val result = analyzeFixture()
        assertNotNull(result)

        val dispatcher = result.functions["getEncryptedData"]
        assertNotNull(dispatcher, "getEncryptedData should be parsed")

        val call = dispatcher.calls.singleOrNull { it.name.localName == "EncryptMessage" }
        assertNotNull(call, "getEncryptedData should contain the EncryptMessage call")

        val targets = call.invokes.map { it.name.localName }
        assertTrue(
            call.invokes.any { (it as? Method)?.recordDeclaration?.name?.localName == "Aes256" },
            "the call should reach the Aes256 implementation, but resolved to: $targets",
        )
        assertTrue(
            call.invokes.any { it.body != null },
            "at least one target must have a body, otherwise the call graph stops here",
        )
    }
}
