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
 * In Go a method may be declared on *any* named type, not only on a struct: `type bools []bool`
 * with a `MarshalLogArray` method is an ordinary implementation of an interface. Structural
 * satisfaction is computed over records, so an implementer that is a named slice, map or function
 * type has no record and is never considered, and every call through the interface loses it.
 *
 * zap declares its whole array-encoding API this way (`bools`, `byteStringsArray`, `complex128s`,
 * ...), which is the bulk of what certmagic still misses.
 */
class NamedNonStructMethodsTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("named_non_struct_methods.go")
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

    /** Shows whether a record exists for a named non-struct type at all. */
    @Test
    fun testDiagnoseNamedNonStructTypes() {
        val result = analyzeFixture()
        assertNotNull(result)

        println("=== records ===")
        for (r in result.records) {
            println("  ${r.name} kind=${r.kind}")
        }
        println("=== methods named MarshalLogArray ===")
        for (m in result.methods.filter { it.name.localName == "MarshalLogArray" }) {
            println(
                "  ${m.name} record=${m.recordDeclaration?.name} " +
                    "receiverType=${m.receiver?.type} body=${m.body != null}"
            )
        }
        val iface = result.records["main.ArrayMarshaler"]
        println(
            "  ArrayMarshaler.MarshalLogArray.overriddenBy = " +
                "${iface?.methods?.get("MarshalLogArray")?.overriddenBy?.map { it.name }}"
        )
    }

    /** The interface method must know the named-slice and named-func implementations. */
    @Test
    fun testNamedNonStructTypesImplementTheInterface() {
        val result = analyzeFixture()
        assertNotNull(result)

        val iface = result.records["main.ArrayMarshaler"]
        assertNotNull(iface, "the interface should be parsed")
        val ifaceMethod = iface.methods["MarshalLogArray"]
        assertNotNull(ifaceMethod)

        val owners =
            ifaceMethod.overriddenBy.filterIsInstance<Method>().mapNotNull {
                it.recordDeclaration?.name?.localName ?: it.name.parent?.localName
            }
        println("  overriddenBy owners = $owners")

        assertContains(owners, "structImpl", "the struct implementer must be found")
        assertContains(owners, "bools", "a named slice type with the method implements it too")
        assertContains(owners, "optionFunc", "a named function type with the method does as well")
    }

    /** The consequence: the call through the interface reaches the implementations. */
    @Test
    fun testCallThroughInterfaceReachesNamedNonStructImpl() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call =
            result.functions["encode"]?.calls?.singleOrNull {
                it.name.localName == "MarshalLogArray"
            }
        assertNotNull(call, "encode should contain the m.MarshalLogArray(enc) call")

        val targets =
            call.invokes.map { "${(it as? Method)?.recordDeclaration?.name?.localName}.${it.name}" }
        assertTrue(
            call.invokes.any {
                (it.name.parent?.localName
                    ?: (it as? Method)?.recordDeclaration?.name?.localName) == "bools"
            },
            "the call should reach bools.MarshalLogArray, resolved to: $targets",
        )
    }
}
