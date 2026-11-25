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
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * Subscripting a map has to yield the map's value type.
 *
 * Go models its built-in map as an object type with the key and the value as type arguments, and a
 * plain object type dereferences to an unknown type. Every `m[k]` was therefore untyped, and an
 * untyped value flows on: a call on it, or with it as an argument, can no longer be matched against
 * a declaration and resolves to an inferred stub instead.
 *
 * This is the single largest source of unresolved dispatch in kerbrute, through spf13/pflag's
 * `flag, ok := f.formal[name]` followed by `flag.Value.Set(value)`.
 */
class MapSubscriptTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang").resolve("map_subscript.go").toFile()
            ),
            Path.of("src", "test", "resources", "golang"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    /** The subscript itself must carry the value type, both plain and in the comma-ok form. */
    @Test
    fun testMapSubscriptHasValueType() {
        val result = analyzeFixture()
        assertNotNull(result)

        val subscriptions = result.allChildren<Subscription>()
        assertTrue(subscriptions.isNotEmpty(), "the fixture should contain subscriptions")
        for (s in subscriptions) {
            println("  ${s.arrayExpression.name}[...] base=${s.arrayExpression.type} -> ${s.type}")
        }

        val onMap = subscriptions.filter { it.arrayExpression.type is GoMapType }
        assertTrue(onMap.isNotEmpty(), "at least one subscription should be on a map")
        for (s in onMap) {
            assertFalse(
                s.type is UnknownType,
                "subscripting ${s.arrayExpression.type} must not yield an unknown type",
            )
            assertEquals("Flag", s.type.root.name.localName, "must yield the map's value type")
        }
    }

    /** The consequence: dispatch through the value obtained from the map works. */
    @Test
    fun testCallOnValueFromMapIsResolved() {
        val result = analyzeFixture()
        assertNotNull(result)

        // Two methods are named `Set`; we want the one on FlagSet, which dispatches through the
        // value obtained from the map.
        val flagSetSet =
            result.methods.singleOrNull {
                it.name.localName == "Set" && it.recordDeclaration?.name?.localName == "FlagSet"
            }
        assertNotNull(flagSetSet, "FlagSet.Set should be parsed")

        val call = flagSetSet.calls.singleOrNull { it.name.localName == "Set" }
        assertNotNull(call, "FlagSet.Set should contain the flag.Value.Set(value) call")

        println("  invokes = ${call.invokes.map { "${it.name} inferred=${it.isInferred}" }}")
        assertTrue(
            call.invokes.any { !it.isInferred },
            "the call must reach a real declaration, resolved to: " +
                "${call.invokes.map { "${it.name} inferred=${it.isInferred}" }}",
        )
    }

    /** Ranging over a map must still bind both the key and the value. */
    @Test
    fun testRangeOverMapStillYieldsKeyAndValue() {
        val result = analyzeFixture()
        assertNotNull(result)

        val names = result.methods["names"]
        assertNotNull(names)

        val k = names.variables["k"]
        val v = names.variables["v"]
        assertNotNull(k, "the key variable should be declared")
        assertNotNull(v, "the value variable should be declared")
        println("  k=${k.type}  v=${v.type}")
        assertEquals("string", k.type.name.localName, "the key keeps the map's key type")
        assertEquals("Flag", v.type.root.name.localName, "the value keeps the map's value type")
    }
}
