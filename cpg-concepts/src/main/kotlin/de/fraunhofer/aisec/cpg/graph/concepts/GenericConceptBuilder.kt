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
        // Note: Update the conceptBuildHelper if you change this.
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
fun MetadataProvider.conceptBuildHelper(
    name: String,
    underlyingNode: Node,
    constructorArguments: Map<String, Any?> = emptyMap(),
    connectDFGUnderlyingNodeToConcept: Boolean = false,
    connectDFGConceptToUnderlyingNode: Boolean = false,
) {
    val conceptClass = Class.forName(name).kotlin
    val constructor = conceptClass.constructors.singleOrNull()
    (constructor?.callBy(
            mapOf(
                (constructor.parameters.singleOrNull { it.name == "underlyingNode" }
                    ?: throw IllegalArgumentException(
                        "There is no argument with name \"underlyingNode\" which is required for the constructor of concept ${conceptClass.simpleName}"
                    )) to underlyingNode,
                *constructorArguments
                    .map { (key, value) ->
                        (constructor.parameters.singleOrNull { it.name == key }
                            ?: throw IllegalArgumentException(
                                "There is no argument with name \"key\" which is specified to generate the concept ${conceptClass.simpleName}"
                            )) to value
                    }
                    .toTypedArray(),
            )
        ) as? Concept)
        ?.also { concept ->
            // Note: Update "newConcept" if you change this.
            concept.codeAndLocationFrom(underlyingNode)
            concept.name = Name("${conceptClass.simpleName}", underlyingNode.name)
            NodeBuilder.log(concept)

            if (connectDFGUnderlyingNodeToConcept) {
                underlyingNode.nextDFG += concept
            }
            if (connectDFGConceptToUnderlyingNode) {
                concept.nextDFG += underlyingNode
            }
        }
}
