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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInEOGPath

/**
 * This function creates a new [Concept] node based on [ConceptClass]. It is neither connected by
 * the EOG nor the DFG.
 */
inline fun <reified ConceptClass : Concept> MetadataProvider.newConcept(
    constructor: (underlyingNode: Node) -> (ConceptClass),
    underlyingNode: Node,
): ConceptClass =
    constructor(underlyingNode).apply {
        this.codeAndLocationFrom(underlyingNode)
        this.name = Name("${ConceptClass::class.simpleName}", underlyingNode.name)
        NodeBuilder.log(this)
    }

/**
 * This function creates a new [Operation] node based on [OperationClass]. It is inserted in the EOG
 * after the [underlyingNode] but it is not connected by the DFG.
 */
inline fun <reified OperationClass : Operation, ConceptClass : Concept> MetadataProvider
    .newOperation(
    constructor: (underlyingNode: Node, concept: ConceptClass) -> (OperationClass),
    underlyingNode: Node,
    concept: ConceptClass,
): OperationClass =
    constructor(underlyingNode, concept).apply {
        this.codeAndLocationFrom(underlyingNode)
        this.name =
            Name(
                "${OperationClass::class.simpleName}".replaceFirstChar { it.lowercaseChar() },
                concept.name,
            )
        concept.ops += this
        underlyingNode.insertNodeAfterwardInEOGPath(this)
        NodeBuilder.log(this)
    }

/** TODO */
fun MetadataProvider.conceptBuildHelper(name: String, underlyingNode: Node): Concept {
    val constructor: (Node) -> Concept =
        when (name) {
            "de.fraunhofer.aisec.cpg.graph.concepts.logging.Log" -> { node ->
                    de.fraunhofer.aisec.cpg.graph.concepts.logging.Log(node)
                }
            "de.fraunhofer.aisec.cpg.graph.concepts.file.File" -> { node ->
                    de.fraunhofer.aisec.cpg.graph.concepts.file.File(node, "filename" /* TODO */)
                }
            "de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret" -> { node ->
                    de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret(node)
                }
            else -> {
                throw IllegalArgumentException("Unknown concept: \"${name}\".")
            }
        }
    return newConcept(constructor, underlyingNode)
}
