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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.query.AcceptedResult
import de.fraunhofer.aisec.cpg.query.NotYetEvaluated
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.SinglePathResult
import de.fraunhofer.aisec.cpg.query.UndecidedResult
import io.github.detekt.sarif4k.*
import java.io.File
import kotlin.io.path.relativeToOrSelf
import kotlin.io.path.toPath

/** Builds the SARIF report for the given [AnalysisResult.requirementsResults]. */
fun AnalysisProject.buildSarif(
    result: AnalysisResult
): Pair<List<ReportingDescriptor>, List<Result>> {
    val sarifRules = mutableListOf<ReportingDescriptor>()
    val sarifResults = mutableListOf<Result>()

    for ((requirementID, passFail) in result.requirementsResults) {
        val sarifResult = passFail.toSarif(requirementID)
        val req = builder?.allRequirements[requirementID]

        val sarifRule =
            ReportingDescriptor(
                id = requirementID,
                name = req?.name,
                shortDescription = req?.description?.let { MultiformatMessageString(text = it) },
            )

        sarifResults += sarifResult
        sarifRules += sarifRule
    }

    return Pair(sarifRules, sarifResults)
}

/**
 * Converts a [QueryTree] to a list of [Result]s. This expects that the query tree is of type
 * [Boolean] and that the [QueryTree.children] represent the individual findings.
 */
fun QueryTree<Boolean>.toSarif(ruleID: String): List<Result> {
    return this.children.map { child ->
        val result = (child.value as? Boolean) ?: this.value

        Result(
            ruleID = ruleID,
            message =
                if (result && child.confidence == AcceptedResult) {
                    Message(text = "The query was successful")
                } else if (child is NotYetEvaluated) {
                    Message(text = "The query has not been evaluated yet")
                } else {
                    Message(text = "The query failed")
                },
            level =
                if (result) {
                    Level.None
                } else {
                    Level.Error
                },
            kind =
                if (result && child.confidence == AcceptedResult) {
                    ResultKind.Pass
                } else if (child is NotYetEvaluated) {
                    ResultKind.Open
                } else if (child.confidence == UndecidedResult) {
                    ResultKind.Review
                } else {
                    ResultKind.Fail
                },
            locations = listOfNotNull(child.node?.toSarifLocation()),
            stacks = child.node?.toSarifCallStack(),
            properties =
                if (child is SinglePathResult) {
                    PropertyBag(mapOf("Termination reason" to child.terminationReason.toString()))
                } else {
                    null
                },
            codeFlows =
                child.children
                    .flatMap { it.getCodeflow() }
                    .map {
                        CodeFlow(
                            threadFlows =
                                listOf(
                                    ThreadFlow(
                                        message = Message(text = "Thread flow"),
                                        locations = it.toSarifThreadFlowLocation(),
                                    )
                                )
                        )
                    },
        )
    }
}

/** Tries to extract the nodes which were visited to retrieve this result. */
fun QueryTree<*>.getCodeflow(): List<List<Node>> {
    return if (this is SinglePathResult) {
        this.children.flatMap { it.getCodeflow() }
    } else if (this.value is List<*>) {
        listOf((this.value as Iterable<*>).filterIsInstance<Node>())
    } else if (this.value is Boolean) {
        this.children.flatMap { it.getCodeflow() }
    } else {
        listOf<List<Node>>()
    }
}

/**
 * Converts a [Node] into a [Message] for SARIF output. Currently, this is a short representation of
 * the node type and node. In the future, we want to include a brief description of any eventual
 * overlay nodes that better describe the node semantically.
 */
private fun Node?.toSarifMessage(): Message? {
    return this?.let { Message(text = "${it.javaClass.simpleName}[name=${it.name}]") }
}

/**
 * Converts a [Node] into a [Stack] for SARIF output. Currently, this is a single stack frame with
 * the location of the node. In the future, we want to include a call stack that leads to the node's
 * current function.
 */
private fun Node.toSarifCallStack(): List<Stack> {
    val currentFunc = this.firstParentOrNull<FunctionDeclaration>()
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
            location =
                node.toSarifLocation(message = node.toSarifMessage(), onlyFunctionHeader = true),
            executionOrder = idx.toLong(),
        )
    } ?: listOf()
}

/** Converts a [Node.location] to a [Location]. */
fun Node?.toSarifLocation(
    message: Message? = this.toSarifMessage(),
    /**
     * If this option is set to true, we only emit the location of the function header, not the
     * entire function body.
     *
     * This is helpful for cases where we want to highlight the function declaration in the code
     * editor, but not the entire function body.
     */
    onlyFunctionHeader: Boolean = false,
): Location? {
    val location = this?.location ?: return null

    return if (this is FunctionDeclaration && this.body != null && onlyFunctionHeader) {
            // Try to calculate the end of the header by using the beginning of the body. This is
            // not entirely correct since in some programming languages we need to start the body
            // location at the first statement, since we are missing location information for the
            // body "block", but it's the best we can do
            de.fraunhofer.aisec.cpg.sarif.PhysicalLocation(
                uri = location.artifactLocation.uri,
                region =
                    de.fraunhofer.aisec.cpg.sarif.Region(
                        startLine = location.region.startLine,
                        startColumn = location.region.startColumn,
                        endLine = this.body?.location?.region?.startLine ?: location.region.endLine,
                        endColumn =
                            this.body?.location?.region?.startColumn ?: location.region.endColumn,
                    ),
            )
        } else {
            this.location
        }
        ?.let {
            Location(
                physicalLocation = it.toSarif(component),
                logicalLocations =
                    listOf(
                        LogicalLocation(
                            fullyQualifiedName =
                                if (this is Declaration) this.name.toString() else null,
                            name = this.name.localName,
                            kind = this.toSarifKind(),
                        )
                    ),
                message = message,
            )
        }
}

/**
 * Converts a [File] to a [Location]. This is used for top-level locations in the SARIF output.
 *
 * We aim to provide all remaining locations in [PhysicalLocation] as relative URLs using
 * [ArtifactLocation.uriBaseID].
 */
fun Map.Entry<String, File>.toSarifLocation(
    message: Message = Message(text = "Top level location for component $key")
): ArtifactLocation {
    return ArtifactLocation(uri = value.absolutePath, description = message)
}

/** Converts a [de.fraunhofer.aisec.cpg.sarif.PhysicalLocation] to a [PhysicalLocation]. */
fun de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.toSarif(
    component: Component? = null
): PhysicalLocation {
    var uri = this.artifactLocation.uri.toPath()
    val uriBase = component?.location?.artifactLocation?.uri?.toPath()
    if (uriBase != null) {
        uri = uri.relativeToOrSelf(uriBase)
    }

    return PhysicalLocation(
        artifactLocation =
            ArtifactLocation(uri = uri.toString(), uriBaseID = component?.name?.localName),
        region =
            Region(
                startLine = this.region.startLine.toLong(),
                startColumn = this.region.startColumn.toLong(),
                endLine = this.region.endLine.toLong(),
                endColumn = this.region.endColumn.toLong(),
            ),
    )
}

/**
 * Converts a [Node] to a well-known SARIF kind. This is used to categorize the node in the SARIF
 * output.
 */
private fun Node.toSarifKind(): String? {
    return when (this) {
        is FunctionDeclaration -> "function"
        is FieldDeclaration -> "member"
        is TranslationUnitDeclaration -> "module"
        is NamespaceDeclaration -> "namespace"
        is ParameterDeclaration -> "parameter"
        is VariableDeclaration -> "variable"
        is Type -> "type"
        else -> null
    }
}
