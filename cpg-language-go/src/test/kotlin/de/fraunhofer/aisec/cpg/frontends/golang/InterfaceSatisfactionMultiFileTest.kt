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
 * Same scenario as [InterfaceSatisfactionTest], but with the receiver type declared in a different
 * translation unit than its methods -- the common Go layout, and the one real projects use.
 *
 * [DeclarationHandler.handleFuncDecl] resolves the receiver via
 * `frontend.scopeManager.lookupScope(fqnRecord)`, which carries the note that it "will only find
 * methods within the current translation unit". If that lookup fails, the method is never bound to
 * its record at all and later resolution has nothing to bind to, which produces inferred, body-less
 * stubs instead of the real declaration.
 *
 * Note that `Record.methods` staying empty is intentional: in Go a method is declared outside the
 * record, so it is not an AST child of it, and the frontend deliberately does not add it there.
 * These tests therefore do not require `Record.methods` to be populated. What they do require is
 * that the override relation is established anyway, from the type hierarchy, because that is what
 * `SymbolResolver.getOverridingCandidates` consumes.
 */
class InterfaceSatisfactionMultiFileTest {

    private val topLevel: Path =
        Path.of("src", "test", "resources", "golang", "interface_satisfaction_multifile")

    private fun analyzeFixture() =
        analyze(
            listOf(topLevel.resolve("types.go").toFile(), topLevel.resolve("methods.go").toFile()),
            topLevel,
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            // Devirtualizing a call whose receiver is statically typed as the
            // interface requires considering every override, since the concrete
            // type is a subtype of the base type rather than a member of its
            // possible-type set.
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    /** Both files must parse and contribute their declarations. */
    @Test
    fun testFixtureStructure() {
        val result = analyzeFixture()
        assertNotNull(result)

        val etype = result.records["main.EType"]
        assertNotNull(etype, "the EType interface should be parsed")
        assertEquals("interface", etype.kind)
        assertNotNull(result.records["main.Aes256"], "Aes256 should be parsed")
        assertNotNull(result.functions["getEncryptedData"], "getEncryptedData should be parsed")

        val impl =
            result.methods.firstOrNull {
                it.name.localName == "EncryptMessage" && it.receiver != null
            }
        assertNotNull(
            impl,
            "the EncryptMessage implementation should exist as a method with a receiver, " +
                "even though it is declared in a different file than its receiver type",
        )
    }

    /**
     * The method is declared in another translation unit than its receiver type, so this pins down
     * whether the cross-translation-unit receiver lookup succeeds. If it does not, the method is
     * never associated with its record and every later step fails for that reason alone.
     */
    @Test
    fun testMethodIsBoundToItsRecordAcrossTranslationUnits() {
        val result = analyzeFixture()
        assertNotNull(result)

        val impl =
            result.methods.firstOrNull {
                it.name.localName == "EncryptMessage" && it.receiver != null
            }
        assertNotNull(impl)
        assertEquals(
            "Aes256",
            impl.recordDeclaration?.name?.localName,
            "the method must be bound to the record declared in the other translation unit",
        )
        assertFalse(impl.isInferred, "the method must be a real declaration, not an inferred stub")
        assertNotNull(impl.body, "the method must have a body, otherwise traversal stops here")
    }

    /**
     * The actual requirement. `Record.methods` is expected to stay empty -- the method is not an
     * AST child of the record in Go -- but the override relation must still be established via the
     * type hierarchy.
     */
    @Test
    fun testOverriddenByIsPopulatedDespiteEmptyRecordMethods() {
        val result = analyzeFixture()
        assertNotNull(result)

        val etype = result.records["main.EType"]
        val aes256 = result.records["main.Aes256"]
        assertNotNull(etype)
        assertNotNull(aes256)

        // Documented, not asserted: Record.methods is empty by design here, and a
        // future change that populates it should not fail this test.
        println("  Aes256.methods               = ${aes256.methods.map { it.name.localName }}")
        println("  Aes256.superTypes            = ${aes256.superTypes.map { it.name }}")
        println("  Aes256.superTypeDeclarations = ${aes256.superTypeDeclarations.map { it.name }}")

        val ifaceMethod = etype.methods["EncryptMessage"]
        assertNotNull(ifaceMethod, "EType should declare EncryptMessage")

        val overridingRecords =
            ifaceMethod.overriddenBy.filterIsInstance<Method>().mapNotNull {
                it.recordDeclaration?.name?.localName
            }
        println("  EType.EncryptMessage.overriddenBy = $overridingRecords")

        assertContains(
            overridingRecords,
            "Aes256",
            "the override relation must be derived from the type hierarchy, " +
                "independently of whether Record.methods is populated",
        )
        assertFalse(
            "NotAnEType" in overridingRecords,
            "a type providing only part of the method set must not be an implementation",
        )
    }

    /** With the override relation in place, the dispatch must reach the implementation. */
    @Test
    fun testCallThroughInterfaceIsDevirtualized() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call =
            result.functions["getEncryptedData"]?.calls?.firstOrNull {
                it.name.localName == "EncryptMessage"
            }
        assertNotNull(call, "getEncryptedData should contain the EncryptMessage call")

        val targets = call.invokes.map { it.name.toString() }
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
