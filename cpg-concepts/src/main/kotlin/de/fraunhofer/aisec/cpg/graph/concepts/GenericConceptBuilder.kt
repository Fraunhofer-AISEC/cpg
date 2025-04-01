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

/**
 * Generates an object of the [Concept] with the FQN [name] and the [Concept.underlyingNode]
 * [underlyingNode]. The constructor arguments of the respective class have to be provided by the
 * map [constructorArguments] where the key is the name of the argument and the value is the
 * argument to pass. [connectDFGUnderlyingNodeToConcept] specifies if the created [Concept] should
 * be in the [Node.nextDFGEdges] edges of the [underlyingNode]. [connectDFGConceptToUnderlyingNode]
 * specifies if the created [Concept] should be in the [Node.prevDFGEdges] of the [underlyingNode].
 *
 * @param name The fully qualified name of the concept class to be created.
 * @param underlyingNode The underlying node for which the concept is created.
 * @param constructorArguments A map of constructor arguments to be passed to the concept class
 *   (excluding the `underlyingNode`).
 * @param connectDFGUnderlyingNodeToConcept If true, the created concept will be added to the next
 *   DFG edges of the underlying node.
 * @param connectDFGConceptToUnderlyingNode If true, the created concept will be added to the prev
 *   DFG edges of the underlying node.
 * @return The created concept with the specified DFG edges.
 * @throws ClassNotFoundException If the class with the specified [name] does not exist
 * @throws IllegalArgumentException If the class with the given [name] does not have exactly one
 *   constructor or if one of the arguments in [constructorArguments] is not a valid argument name
 *   for this constructor.
 */
fun MetadataProvider.conceptBuildHelper(
    name: String,
    underlyingNode: Node,
    constructorArguments: Map<String, Any?> = emptyMap(),
    connectDFGUnderlyingNodeToConcept: Boolean = false,
    connectDFGConceptToUnderlyingNode: Boolean = false,
): Concept {
    val conceptClass = Class.forName(name).kotlin
    val constructor =
        conceptClass.constructors.singleOrNull()
            ?: throw IllegalArgumentException(
                "The class with $name does not have exactly one constructor."
            )
    return (constructor.callBy(
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
        } ?: throw IllegalArgumentException("The class $name does not create a Concept.")
}

/**
 * Generates an object of the [Operation] with the FQN [name], the [Operation.underlyingNode]
 * [underlyingNode], and the [Operation.concept] [concept]. The constructor arguments of the
 * respective class have to be provided by the map [constructorArguments] where the key is the name
 * of the argument and the value is the argument to pass. [connectDFGUnderlyingNodeToConcept]
 * specifies if the created [Operation] should be in the [Node.nextDFGEdges] edges of the
 * [underlyingNode]. [connectDFGConceptToUnderlyingNode] specifies if the created [Operation] should
 * be in the [Node.prevDFGEdges] of the [underlyingNode].
 *
 * @param name The fully qualified name of the operation class to be created.
 * @param underlyingNode The underlying node for which the operation is created.
 * @param concept The [Concept] the [Operation] belongs to. It also has to be explicitly stated in
 *   the [constructorArguments] as the name of the argument often differ.
 * @param constructorArguments A map of constructor arguments to be passed to the operation class
 *   (excluding the `underlyingNode`).
 * @param connectDFGUnderlyingNodeToConcept If true, the created operation will be added to the next
 *   DFG edges of the underlying node.
 * @param connectDFGConceptToUnderlyingNode If true, the created operation will be added to the prev
 *   DFG edges of the underlying node.
 * @return The created operation with the specified DFG edges.
 * @throws ClassNotFoundException If the class with the specified [name] does not exist.
 * @throws IllegalArgumentException If the class with the given [name] does not have exactly one
 *   constructor or if one of the arguments in [constructorArguments] is not a valid argument name
 *   for this constructor.
 */
fun MetadataProvider.operationBuildHelper(
    name: String,
    underlyingNode: Node,
    concept: Concept,
    constructorArguments: Map<String, Any?> = emptyMap(),
    connectDFGUnderlyingNodeToConcept: Boolean = false,
    connectDFGConceptToUnderlyingNode: Boolean = false,
): Operation {
    val operationClass = Class.forName(name).kotlin
    val constructor =
        operationClass.constructors.singleOrNull()
            ?: throw IllegalArgumentException(
                "The class with $name does not have exactly one constructor."
            )
    return (constructor.callBy(
            mapOf(
                (constructor.parameters.singleOrNull { it.name == "underlyingNode" }
                    ?: throw IllegalArgumentException(
                        "There is no argument with name \"underlyingNode\" which is required for the constructor of operation ${operationClass.simpleName}"
                    )) to underlyingNode,
                *constructorArguments
                    .map { (key, value) ->
                        (constructor.parameters.singleOrNull { it.name == key }
                            ?: throw IllegalArgumentException(
                                "There is no argument with name \"key\" which is specified to generate the operation ${operationClass.simpleName}"
                            )) to value
                    }
                    .toTypedArray(),
            )
        ) as? Operation)
        ?.also { operation ->
            // Note: Update "newOperation" if you change this.
            operation.codeAndLocationFrom(underlyingNode)
            operation.name =
                Name(
                    "${operationClass.simpleName}".replaceFirstChar { it.lowercaseChar() },
                    concept.name,
                )
            concept.ops += operation
            NodeBuilder.log(operation)

            underlyingNode.insertNodeAfterwardInEOGPath(operation)

            if (connectDFGUnderlyingNodeToConcept) {
                underlyingNode.nextDFG += operation
            }
            if (connectDFGConceptToUnderlyingNode) {
                operation.nextDFG += underlyingNode
            }
        } ?: throw IllegalArgumentException("The class $name does not create an Operation.")
