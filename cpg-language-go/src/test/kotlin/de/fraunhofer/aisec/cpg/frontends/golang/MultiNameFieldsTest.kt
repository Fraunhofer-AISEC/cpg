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
 * A single struct field entry may declare several fields sharing one type: in `class, tag, length
 * int` all three are int fields. Keeping only the first name does not merely leave the others
 * untyped -- they are not in the graph at all, so a reference to one has no type, and a call it is
 * passed to can no longer be matched against a declaration and resolves to an inferred stub.
 *
 * encoding/asn1 declares `tagAndLength` exactly like this and passes `t.length` to `invalidLength`,
 * which is where kerbrute loses the remainder of its unresolved plain calls.
 */
class MultiNameFieldsTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("multi_name_fields.go")
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

    /** Every name of a grouped field declaration must become a field of the right type. */
    @Test
    fun testAllNamesOfAFieldGroupBecomeFields() {
        val result = analyzeFixture()
        assertNotNull(result)

        val record = result.records["main.tagAndLength"]
        assertNotNull(record, "tagAndLength should be parsed")
        val byName = record.fields.associateBy { it.name.localName }
        println("  tagAndLength fields = ${byName.mapValues { it.value.type.name }}")

        for (n in listOf("class", "tag", "length")) {
            val f = byName[n]
            assertNotNull(f, "field $n should exist, got ${byName.keys}")
            assertEquals("int", f.type.name.localName, "field $n should be an int")
        }
        assertEquals("bool", byName["isCompound"]?.type?.name?.localName)
    }

    /** Embedded fields must keep working next to grouped ones. */
    @Test
    fun testEmbeddedAndGroupedFieldsCoexist() {
        val result = analyzeFixture()
        assertNotNull(result)

        val outer = result.records["main.Outer"]
        assertNotNull(outer)
        val byName = outer.fields.associateBy { it.name.localName }
        println("  Outer fields = ${byName.keys}")

        val embedded = byName["Inner"]
        assertNotNull(embedded, "the embedded Inner field should exist, got ${byName.keys}")
        assertContains(embedded.modifiers, "embedded", "Inner should still be marked embedded")
        for (n in listOf("x", "y")) {
            assertNotNull(byName[n], "field $n should exist, got ${byName.keys}")
        }
    }

    /** The consequence: the call carrying such a field resolves to the real function. */
    @Test
    fun testCallWithGroupedFieldArgumentResolves() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call =
            result.functions["use"]?.calls?.singleOrNull { it.name.localName == "invalidLength" }
        assertNotNull(call, "use should contain the invalidLength call")

        println(
            "  args = ${call.arguments.map { it.type.name }} invokes = " +
                "${call.invokes.map { "${it.name} inferred=${it.isInferred}" }}"
        )
        assertTrue(
            call.invokes.any { !it.isInferred && it.body != null },
            "the call must reach the real invalidLength, resolved to: " +
                "${call.invokes.map { "${it.name} inferred=${it.isInferred}" }}",
        )
    }
}
