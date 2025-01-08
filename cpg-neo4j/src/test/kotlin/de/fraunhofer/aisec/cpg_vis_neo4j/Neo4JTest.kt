/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag

@Tag("integration")
class Neo4JTest {
    @Test
    @SkipIfLanguageIsNotAvailable("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
    fun testPush() {
        val (application, result) = createTranslationResult()

        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, result.functions.size)

        application.pushToNeo4j(result)
    }

    @Test
    @SkipIfLanguageIsNotAvailable("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
    fun testPushVeryLong() {
        val (application, result) = createTranslationResult("very_long.cpp")

        assertEquals(1, result.variables.size)

        val lit = result.variables["l"]?.initializer
        assertIs<Literal<BigInteger>>(lit)
        assertEquals(BigInteger("10958011617037158669"), lit.value)

        application.pushToNeo4j(result)
    }

    @Test
    @SkipIfLanguageIsNotAvailable("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
    fun testPushConcepts() {
        val (application, result) = createTranslationResult()

        val tu = result.translationUnits.firstOrNull()
        assertNotNull(tu)

        val connectCall = result.calls["connect"]
        assertNotNull(connectCall)

        abstract class NetworkingOperation(concept: Concept<out Operation>) : Operation(concept)
        class Connect(concept: Concept<out Operation>) : NetworkingOperation(concept)
        class Networking() : Concept<NetworkingOperation>()

        abstract class FileOperation(concept: Concept<out Operation>) : Operation(concept)
        class FileHandling() : Concept<FileOperation>()

        val nw = Networking()
        nw.name = Name("Networking")
        nw.underlyingNode = tu

        val connect = Connect(concept = nw)
        connect.underlyingNode = connectCall
        connect.name = Name("connect")
        nw.ops += connect

        val f = FileHandling()
        f.name = Name("FileHandling")
        f.underlyingNode = tu

        assertEquals(setOf<Node>(connect), connectCall.overlays)
        assertEquals(setOf<Node>(nw, f), tu.overlays)

        application.pushToNeo4j(result)
    }
}
