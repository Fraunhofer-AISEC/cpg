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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Lambda
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.methods
import de.fraunhofer.aisec.cpg.graph.problems
import de.fraunhofer.aisec.cpg.graph.variables
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MethodReferenceTest {
    @Test
    fun testMethodReference() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/MethodReference.java"))
                .defaultPasses()
                .registerLanguage<JavaLanguage>()
                .build()
        val result = TranslationManager.builder().config(config).build().analyze().get()
        assertTrue(result.problems.none { "not supported" in it.problem })

        val mapCalls = result.calls.filter { it.name.localName == "map" }
        assertEquals(2, mapCalls.size)

        // MethodReference::staticMethod -> (arg0) -> MethodReference.staticMethod(arg0)
        val staticRef = mapCalls[0].arguments.firstOrNull()
        assertIs<Lambda>(staticRef)
        assertEquals("MethodReference::staticMethod", staticRef.code)
        val staticFunction = staticRef.function
        assertNotNull(staticFunction)
        assertEquals(1, staticFunction.parameters.size)
        val staticCall = staticFunction.calls["staticMethod"]
        assertNotNull(staticCall)
        assertEquals(result.methods["staticMethod"], staticCall.invokes.singleOrNull())
        assertEquals(
            staticFunction.parameters.first(),
            (staticCall.arguments.singleOrNull() as? Reference)?.refersTo,
        )

        // String::length -> (arg0) -> arg0.length()
        val unboundRef = mapCalls[1].arguments.firstOrNull()
        assertIs<Lambda>(unboundRef)
        val unboundFunction = unboundRef.function
        assertNotNull(unboundFunction)
        assertEquals(1, unboundFunction.parameters.size)
        val unboundCall = unboundFunction.calls["length"]
        assertIs<MemberCall>(unboundCall)
        assertTrue(unboundCall.arguments.isEmpty())
        assertEquals(unboundFunction.parameters.first(), (unboundCall.base as? Reference)?.refersTo)

        // this::instanceMethod -> (arg0) -> this.instanceMethod(arg0)
        val boundThisRef = result.calls["forEach"]?.arguments?.firstOrNull()
        assertIs<Lambda>(boundThisRef)
        val boundThisCall = boundThisRef.function?.calls["instanceMethod"]
        assertNotNull(boundThisCall)
        assertEquals(1, boundThisCall.arguments.size)
        assertEquals(result.methods["instanceMethod"], boundThisCall.invokes.singleOrNull())

        // s::length -> () -> s.length()
        val boundRef = result.variables["bound"]?.initializer
        assertIs<Lambda>(boundRef)
        assertEquals(0, boundRef.function?.parameters?.size)
        val boundCall = boundRef.function?.calls["length"]
        assertIs<MemberCall>(boundCall)
        assertEquals(result.variables["s"], (boundCall.base as? Reference)?.refersTo)

        // ArrayList::new -> () -> new ArrayList()
        val constructorRef = result.variables["constructor"]?.initializer
        assertIs<Lambda>(constructorRef)
        assertIs<New>(constructorRef.function?.body)

        // int[]::new -> (arg0) -> new int[arg0]
        val arrayRef = result.variables["array"]?.initializer
        assertIs<Lambda>(arrayRef)
        assertEquals(1, arrayRef.function?.parameters?.size)
        assertIs<ArrayConstruction>(arrayRef.function?.body)
    }
}
