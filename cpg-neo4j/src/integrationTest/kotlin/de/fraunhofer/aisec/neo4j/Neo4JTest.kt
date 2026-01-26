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
package de.fraunhofer.aisec.neo4j

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.persistence.pushToNeo4j
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class Neo4JTest {
    @Test
    fun testPush() {
        val (_, result) = createTranslationResult()

        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, result.functions.size)

        result.pushToNeo4j()
    }

    @Test
    fun testPushVeryLong() {
        val (_, result) = createTranslationResult("very_long.cpp")

        assertEquals(1, result.variables.size)

        val lit = result.variables["l"]?.initializer
        assertIs<Literal<BigInteger>>(lit)
        assertEquals(BigInteger("10958011617037158669"), lit.value)

        result.pushToNeo4j()
    }

    @Test
    fun testPushConcepts() {
        val (_, result) = createTranslationResult()

        val tu = result.components.firstOrNull()?.translationUnits?.firstOrNull()
        assertNotNull(tu)

        val connectCall = result.calls["connect"]
        assertNotNull(connectCall)

        abstract class NetworkingOperation(underlyingNode: Node, concept: Concept) :
            Operation(underlyingNode = underlyingNode, concept = concept)
        class Connect(underlyingNode: Node, concept: Concept) :
            NetworkingOperation(underlyingNode = underlyingNode, concept = concept)
        class Networking(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

        abstract class FileOperation(underlyingNode: Node, concept: Concept) :
            Operation(underlyingNode = underlyingNode, concept = concept)
        class FileHandling(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

        val nw = Networking(underlyingNode = tu)
        nw.name = Name("Networking")

        val connect = Connect(underlyingNode = connectCall, concept = nw)
        connect.name = Name("connect")
        nw.ops += connect

        val f = FileHandling(underlyingNode = tu)
        f.name = Name("FileHandling")

        assertEquals(setOf<Node>(connect), connectCall.overlays)
        assertEquals(setOf<Node>(nw, f), tu.overlays)

        assertEquals(
            2,
            tu.conceptNodes.size,
            "Expected to find the `Networking` and `FileHandling` concept.",
        )
        assertEquals(1, tu.operationNodes.size, "Expected to find the `Connect` operation.")

        result.pushToNeo4j()
    }
}
