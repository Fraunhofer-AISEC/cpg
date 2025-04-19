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
import de.fraunhofer.aisec.cpg.helpers.neo4j.LocationConverter
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert

/**
 * The minimal properties to identify an assumption, is the assumption type and either node, edge,
 * or assumption ID [assumptionID]. Only one of the three should be provided.
 *
 * @param assumptionType One of many finite assumption types used to differentiate between
 *   assumptions and group similar assumptions.
 * @param message The message describing the assumption that was taken.
 * @param assumptionLocation The location where an assumption was made. The location can be reported
 *   to the developers as the location in the cpg code base that MADE the assumption.
 * @param node The node that cause the assumption to be necessary, even if the assumption has
 *   validity for the entire program, the node location can be reported to the user as the code
 *   piece that CAUSED the assumption.
 * @param edge The edge that cause the assumption to be necessary, even if the assumption has
 *   validity for the entire program, the location of the start node can be reported to the user as
 *   the code piece that CAUSED the assumption.
 * @param assumptionID an ID chosen by the caller to identify the assumption across translation,
 *   e.g. filename or classname, with a function name and a potential counter. The assumptionID
 *   should be chosen in a way, such that the id stays the same across CPG runs with addition of
 *   other assumptions.
 * @param assumptionScope The scope that the assumption has validity for, the scope is a node and
 *   the assumption is valid for every node in its ast subtree. It can be equal to [underlyingNode]
 *   or can be connected to its ancestor nodes in the AST if the scope is wider.
 * @param status the [AssumptionStatus] that an assumption can have, default being set to
 *   [AssumptionStatus.Undecided]. When an assumption is [AssumptionStatus.Accepted] or
 *   [AssumptionStatus.Ignored], the object it is attached to is considered correct, e.g. nodes,
 *   edges, paths are considered to exist and results are true or false, if based on a contradiction
 *   result. If it is [AssumptionStatus.Rejected], nodes and edges containing them are supposed to
 *   be considered nonexistent, or not existing with the same properties. Results with assumptions
 *   and status [AssumptionStatus.Rejected] should be considered false positives.
 */
class Assumption(
    val assumptionType: AssumptionType,
    var message: String,
    @Convert(LocationConverter::class) var assumptionLocation: PhysicalLocation,
    node: Node? = null,
    @DoNotPersist var edge: Edge<*>? = null,
    var assumptionID: String? = null,
    @Relationship(value = "ASSUMPTION_SCOPE", direction = Relationship.Direction.OUTGOING)
    var assumptionScope: Node? = node,
    var status: AssumptionStatus = AssumptionStatus.Ignored,
) : OverlayNode() {

    init {
        super.underlyingNode = node ?: edge?.start
        if (super.underlyingNode == null && assumptionID == null) {
            log.warn(
                "Creating an assumption with no associated node or edge requires setting and assumptionID but was null"
            )
        }

        if (assumptionScope == null) {
            this@Assumption.assumptionScope = super.underlyingNode
        }
        name = Name(assumptionType.name)
        location = node?.location
    }
}

/**
 * the [AssumptionStatus] that an assumption can have, default being set to
 * [AssumptionStatus.Undecided]. When an assumption is [AssumptionStatus.Accepted] or
 * [AssumptionStatus.Ignored], the object it is attached to is considered correct, e.g. nodes,
 * edges, paths are considered to exist and results are true or false, if based on a contradiction
 * result. If it is [AssumptionStatus.Rejected], nodes and edges containing them are supposed to be
 * considered nonexistent, or not existing with the same properties. Results with assumptions and
 * status [AssumptionStatus.Rejected] should be considered false positives.
 */
enum class AssumptionStatus {
    /**
     * Default state: Patterns(Paths) depending on the assumption are returned, result has an
     * undecided state, assumptions are shown.
     */
    Undecided,
    /**
     * User or Algorithm: Patterns(Paths) depending on the assumption are not returned, results have
     * a decided state, assumptions are (not?) shown.
     */
    Rejected,
    /**
     * User or Algorithm: Patterns(Paths) depending on the assumption are returned, results have a
     * decided state, assumptions are shown.
     */
    Accepted,
    /**
     * User or Algorithm: Patterns(Paths) depending on the assumption are returned, results have a
     * decided state, assumptions are not shown.
     */
    Ignored,
}

enum class AssumptionType {
    SyntaxAmbiguityAssumption,
    InferenceAssumption,
    ClosedMacroAssumption,
    UnsupportedLanguageProblem,
    MissingCodeProblem,
    AmbiguityAssumption,
    ConceptAssumption,
    ExhaustiveEnumerationAssumption,
    CompletenessAssumption,
    SoundnessAssumption,
    ControlFlowAssumption,
    CFIntegrityAssumption,
    NoExceptionsAssumption,
    CFAllOrNothingExecutesAssumption,
    TrustedConfigAssumption,
    DataFlowAssumption,
    ExternalDataAssumption,
    NetworkAvailableAssumption,
    ResourceExistsAssumption,
    ServiceReachableAssumption,
    AtomicExecutionAssumption,
    TrustBoundaryAssumption,
    DataRangeAssumption,
    TrustedInputAssumption,
}

interface HasAssumptions {
    val assumptionNodes: MutableList<Assumption>

    /**
     * This function adds a new assumption to the object it is called on. If the object is a node or
     * edge. The Assumption is added as an overlaying node for presentation in the graph. If the
     * base object it is neither node nor edge, an assumptionID has to be provided to make
     * assumptions identifiable across CPG runs.
     *
     * @param assumptionType The type of assumption used to differentiate between assumptions and
     *   group similar assumptions.
     * @param assumptionID an ID chosen by the caller, e.g. filename or classname, with a function
     *   name and a potential counter. The assumptionID is needed if the assumption is neither on a
     *   node, nor on an edge.
     * @param scope The scope that the assumption has validity for, here the scope is a node,
     *   because the assumption is valid for every node in its ast subtree.
     * @param message The message describing the assumption that was taken.
     */
    fun HasAssumptions.assume(
        assumptionType: AssumptionType,
        message: String,
        assumptionID: String? = null,
        scope: Node? = null,
    ): HasAssumptions {
        // This connects the assumption as an overlay node to the code graph
        Assumption(
            assumptionType,
            message,
            getCallerFileAndLine(),
            node = this as? Node,
            edge = this as? Edge<*>,
            assumptionID = assumptionID,
            assumptionScope = scope,
        )

        this.assumptionNodes.add(
            Assumption(
                assumptionType,
                message,
                getCallerFileAndLine(),
                node = this as? Node,
                edge = this as? Edge<*>,
                assumptionID = assumptionID,
                assumptionScope = scope,
            )
        )
        return this
    }

    /**
     * This function is supposed to be used when a new object is created after searching through the
     * graph that can depend on assumptions made on other objects. On example is when creating
     * concepts after following a DFG path. Concepts are added to the graph and then serve as nodes
     * for further queries, and therefore indirect assumptions would be lost if not copied over with
     * this function.
     *
     * This is the current implementation for assumption propagation and is not needed when the
     * object already contains the assumption dependent node.
     *
     * @param haveAssumptions nodes that hold assumptions this object dependent on.
     */
    fun HasAssumptions.addAssumptionDependences(haveAssumptions: Collection<HasAssumptions>) {
        this.assumptionNodes.addAll(haveAssumptions.flatMap { it.assumptionNodes })
    }

    /**
     * A convenience function to add a dependency to a single object holding assumptions. For more
     * documentation see [HasAssumptions.addAssumptionDependences].
     *
     * @param hasAssumptions add dependence to assumptions of a single other node.
     */
    fun HasAssumptions.addAssumptionDependence(hasAssumptions: HasAssumptions) {
        this.addAssumptionDependences(listOf(hasAssumptions))
    }

    /**
     * This function returns a SARIF formatted location of the caller that creates an assumption.
     * The function is intentionally made private to avoid outside use and functions if it is called
     * inside of [HasAssumptions].
     */
    private fun getCallerFileAndLine(): PhysicalLocation {
        // The first stack trace element is the call to Thread.getStackTrace. which we do not need
        val stackTrace = Thread.currentThread().stackTrace.toList().drop(1)

        val thisFileName = stackTrace[0].fileName
        val interfaceImplementingFileName =
            stackTrace.firstOrNull { it.fileName != thisFileName }?.fileName
        // The first stack trace with the filename that is neither this, nor the interface
        // implementing
        // class
        // is the caller of assumption creation
        val stackTraceElement =
            stackTrace.firstOrNull {
                it.fileName !in listOf(thisFileName, interfaceImplementingFileName)
            }
        stackTraceElement?.let {
            return PhysicalLocation(
                URI(stackTraceElement.fileName ?: ""),
                Region(stackTraceElement.lineNumber, 0, stackTraceElement.lineNumber, 0),
            )
        }

        return PhysicalLocation(URI(""), Region(-1, -1, -1, -1))
    }
}
