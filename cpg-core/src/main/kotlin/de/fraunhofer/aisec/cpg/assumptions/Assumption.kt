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
package de.fraunhofer.aisec.cpg.assumptions

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import org.neo4j.ogm.annotation.Relationship

/**
 * The minimal properties to identify an assumption, is the assumption type and either node, edge,
 * or reference Class. Only one of the three should be provided.
 */
class Assumption(
    val assumptionType: AssumptionType,
    var message: String,
    // If the assumption was made the CPG sourcecode, the cpg sourcecode locations is used
    var assumptionLocation: String,
    node: Node? = null,
    @DoNotPersist var edge: Edge<*>? = null,
    var aID: String? = null,
    assumptionScope: Node? = node,
    var status: AssumptionStatus = AssumptionStatus.Ignored,
) : OverlayNode() {

    @Relationship(value = "ASSUMPTION_SCOPE", direction = Relationship.Direction.OUTGOING)
    /**
     * The Scope in which this assumption has validity. It is equal to [underlyingNode] or can be
     * connected to its ancestor nodes in the AST if the scope is wider.
     */
    var assumptionScope = assumptionScope

    init {
        super.underlyingNode = node ?: edge?.start
        if (assumptionScope == null) {
            this@Assumption.assumptionScope = super.underlyingNode
        }
        name = Name(assumptionType.name)
        location = node?.location
    }
}

enum class AssumptionStatus {
    Undecided, // Default state: Patterns(Paths) depending on the assumption are returned, result
    // has an undecided state, assumptions are shown.
    Rejected, // User or Algorithm: Patterns(Paths) depending on the assumption are not returned,
    // results have a decided state, assumptions are (not?) shown.
    Confirmed, // User or Algorithm: Patterns(Paths) depending on the assumption are returned,
    // results have a decided state, assumptions are shown.
    Ignored, // User or Algorithm: Patterns(Paths) depending on the assumption are returned, results
    // have a decided state, assumptions are not shown.
}

enum class AssumptionType {
    InferenceAssumption,
    ClosedMacroAssumption,
    UnsupportedLanguageProblem,
    MissingCodeProblem,
    AmbiguityAssumption,
    ConceptPlacementAssumption,
    ExhaustiveEnumerationAssumption,
    CompletenessAssumption,
    SoundnessAssumption,
    CFIntegrityAssumption,
    NoExceptionsAssumption,
    CFAllOrNothingExecutesAssumption,
    TrustedConfigAssumption,
    ExternalDataAssumption,
    NetworkAvailableAssumption,
    ResourceExistsAssumption,
    ServiceReachableAssumption,
    AtomicExecutionAssumption,
    TrustBoundaryAssumption,
    DataRangeAssumption,
    TrustedInputAssumption,
}

/**
 * @param assumptionType The type of assumption used to differentiate between assumptions and group
 *   similar assumptions.
 * @param node The node that cause the assumption to be necessary, even if the assumption has
 *   validity for the entire program, the node serves as location to be reported to the user.
 * @param scope The scope that the assumption has validity for, here the scope is a node, because
 *   the assumption is valid for every node in its ast subtree.
 * @param message The message describing the assumption that was taken.
 */
fun assume(assumptionType: AssumptionType, message: String, node: Node, scope: Node? = null) {
    // This connects the assumption as an overlay node to the code graph
    Assumption(assumptionType, message, getCurrentFileAndLine(), node, assumptionScope = scope)
}

/**
 * @param assumptionType The type of assumption used to differentiate between assumptions and group
 *   similar assumptions.
 * @param edge The edge that cause the assumption to be necessary, even if the assumption has
 *   validity for the entire program, the node that the edge starts at serves as location to be
 *   reported to the user.
 * @param scope The scope that the assumption has validity for, here the scope is a node, because
 *   the assumption is valid for every node in its ast subtree.
 * @param message The message describing the assumption that was taken.
 */
fun assume(assumptionType: AssumptionType, message: String, edge: Edge<*>?, scope: Node? = null) {
    // This connects the assumption as an overlay node to the code graph
    Assumption(
        assumptionType,
        message,
        getCurrentFileAndLine(),
        edge = edge,
        assumptionScope = scope,
    )
}

fun getCurrentFileAndLine(): String {
    val stackTrace = Thread.currentThread().stackTrace
    return "File: ${stackTrace[4].fileName}, Line: ${stackTrace[4].lineNumber}"
}
