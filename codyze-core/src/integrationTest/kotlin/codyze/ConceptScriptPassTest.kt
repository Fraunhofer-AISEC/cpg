/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package codyze

import de.fraunhofer.aisec.codyze.ConceptScriptPass
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertNotNull

class ConceptScriptPassTest {

    @Test
    fun testConceptScriptPass() {
        class MySpecialSecret() : Concept(null)

        class SpecialOperation(underlyingNode: Node?, concept: MySpecialSecret) :
            Operation(underlyingNode, concept)

        class Encrypt(concept: MySpecialSecret) : Concept(concept)

        val topLevel = Path("src/integrationTest/resources")
        val result =
            analyze(listOf(topLevel.resolve("encrypt.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<ConceptScriptPass>()
                it.configurePass<ConceptScriptPass>(
                    ConceptScriptPass.Configuration(
                        scripts = listOf(topLevel.resolve("encryption.concept.kts").toFile())
                    )
                )
            }
        assertNotNull(result)

        with(result) {
            assign {
                val secrets = { MySpecialSecret() } to calls("get_secret_from_server")

                ops(secrets) { secret ->
                    Encrypt(concept = secret) to calls("encrypt").reachableFrom(secret)
                }
            }
        }
    }

    class ConceptAssignmentContext {

        fun <T : Concept> ops(
            assign: ConceptAssignment<T>,
            blocks: OperationAssignmentContext.(T) -> Unit,
        ) {}
    }

    class OperationAssignmentContext {}

    class ConceptAssignment<T : Concept>(var concepts: List<T>) {}

    class OperationAssignment(var op: Operation? = null) {}

    private fun assign(blocks: ConceptAssignmentContext.() -> Unit) {
        TODO("Not yet implemented")
    }

    infix fun <T : Concept> (() -> T).to(nodes: List<Node>): ConceptAssignment<T> {
        return ConceptAssignment(nodes.map { this() })
    }

    infix fun <T : Concept> T.to(node: Node): ConceptAssignment<T> {
        return ConceptAssignment(listOf(this))
    }

    infix fun Operation.to(nodes: List<Node>): OperationAssignment {
        return OperationAssignment()
    }

    infix fun Operation.to(node: Node): OperationAssignment {
        return OperationAssignment()
    }
}

private fun List<Node>.reachableFrom(secret: Node): List<Node> {
    TODO("Not yet implemented")
}
