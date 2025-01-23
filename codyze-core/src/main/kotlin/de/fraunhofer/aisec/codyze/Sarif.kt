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
package de.fraunhofer.aisec.codyze

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.query.QueryTree
import io.github.detekt.sarif4k.*

/**
 * Converts a [QueryTree] to a list of [Result]s. This expects that the query tree is of type
 * [Boolean] and that the [QueryTree.children] represent the individual findings.
 */
fun QueryTree<Boolean>.toSarif(ruleID: String): List<Result> {
    return this.children.map {
        Result(
            ruleID = ruleID,
            message =
                if (this.value) {
                    Message(text = "Query was successful")
                } else {
                    Message(text = "Query failed")
                },
            level =
                if (this.value) {
                    Level.None
                } else {
                    Level.Error
                },
            kind =
                if (this.value) {
                    ResultKind.Pass
                } else {
                    ResultKind.Fail
                },
            locations = listOfNotNull(it.node?.toSarifLocation()),
            stacks = it.node?.toSarifCallStack(),
            codeFlows =
                it.children.map { child ->
                    @Suppress("UNCHECKED_CAST") val list = (child.value as? List<Node>)
                    CodeFlow(
                        threadFlows =
                            listOf(
                                ThreadFlow(
                                    message = Message(text = "Thread flow"),
                                    locations = list.toSarifThreadFlowLocation(),
                                )
                            )
                    )
                },
        )
    }
}

/**
 * Converts a [Node] into a [Message] for SARIF output. Currently, this is a short representation of
 * the node type and node. In the future, we want to include a brief description of any eventual
 * overlay nodes that better describe the node semantically.
 */
private fun Node?.toSarifMessage(): Message? {
    return Message(text = "${this?.javaClass?.simpleName}[name=${this?.name}]")
}

/**
 * Converts a [Node] into a [Stack] for SARIF output. Currently, this is a single stack frame with
 * the location of the node. In the future, we want to include a call stack that leads to the node's
 * current function.
 */
private fun Node.toSarifCallStack(): List<Stack> {
    val currentFunc = this.firstParentOrNull { it is FunctionDeclaration }
    return listOf(
        Stack(
            message = Message(text = "Stack"),
            frames =
                listOf(
                    StackFrame(
                        location = this.toSarifLocation(message = currentFunc.toSarifMessage())
                    )
                ),
        )
    )
}

/** Converts a list of [Node]s into a list of [ThreadFlowLocation]s for SARIF output. */
private fun List<Node>?.toSarifThreadFlowLocation(): List<ThreadFlowLocation> {
    return this?.mapIndexed { idx, node ->
        ThreadFlowLocation(
            location = node.toSarifLocation(message = node.toSarifMessage()),
            executionOrder = idx.toLong(),
        )
    } ?: listOf()
}

/** Converts a [Node.location] to a [Location]. */
fun Node?.toSarifLocation(message: Message? = this.toSarifMessage()): Location? {
    return this?.location?.let { Location(physicalLocation = it.toSarif(), message = message) }
}

/** Converts a [de.fraunhofer.aisec.cpg.sarif.PhysicalLocation] to a [PhysicalLocation]. */
fun de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.toSarif(): PhysicalLocation {
    return PhysicalLocation(
        artifactLocation = ArtifactLocation(uri = this.artifactLocation.uri.toString()),
        region =
            Region(
                startLine = this.region.startLine.toLong(),
                startColumn = this.region.startColumn.toLong(),
                endLine = this.region.endLine.toLong(),
                endColumn = this.region.endColumn.toLong(),
            ),
    )
}
