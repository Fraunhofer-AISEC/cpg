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
import java.util.*
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert

/**
 * The minimal properties to identify an assumption, is the assumption type and either node, edge,
 * or node [id] if deterministic. Only one of the three should be provided.
 *
 * @param assumptionType One of many finite assumption types used to differentiate between
 *   assumptions and group similar assumptions.
 * @param message The message describing the assumption that was taken.
 * @param assumptionLocation The location where an assumption was made. The location can be reported
 *   to the developers as the location in the CPG's codebase that created the [Assumption] object.
 * @param node The node that causes the assumption to be necessary. Even if the assumption has
 *   validity for the entire program, the node location can be reported to the user as the piece of
 *   code under analysis that CAUSED the assumption.
 * @param edge The edge that cause the assumption to be necessary, even if the assumption has
 *   validity for the entire program, the location of the start node can be reported to the user as
 *   the code piece that CAUSED the assumption.
 * @param assumptionScope The scope that the assumption has validity for, the scope is a node and
 *   the assumption is valid for every node in its ast subtree. It can be equal to [underlyingNode]
 *   or can be connected to its ancestor nodes in the AST if the scope is wider.
 * @param status the [AssumptionStatus] that an assumption can have, default being set to
 *   [AssumptionStatus.Undecided]. When an assumption is [AssumptionStatus.Accepted] or
 *   [AssumptionStatus.Ignored], the object it is attached to is considered correct, e.g. nodes,
 *   edges, paths are considered to exist and results are true or false, if based on a contradiction
 *   result. If it is [AssumptionStatus.Rejected], nodes and edges containing them are supposed to
 *   be considered nonexistent, or not existing with the same properties. Results with assumptions
 *   and status [AssumptionStatus.Rejected] should be considered false positives or false negatives.
 */
class Assumption(
    val assumptionType: AssumptionType,
    var message: String,
    @Convert(LocationConverter::class) var assumptionLocation: PhysicalLocation,
    node: Node? = null,
    @DoNotPersist var edge: Edge<*>? = null,
    @Relationship(value = "ASSUMPTION_SCOPE", direction = Relationship.Direction.OUTGOING)
    var assumptionScope: Node? = node,
    var status: AssumptionStatus = AssumptionStatus.Ignored,
) : OverlayNode() {

    init {
        super.underlyingNode = node ?: edge?.start
        // Currently, this condition is always false due to id being initialized, however, this is
        // may change in the future
        @Suppress("SENSELESS_COMPARISON")
        if (super.underlyingNode == null && id == null) {
            log.warn(
                "Creating an assumption with no associated node or edge requires having a deterministic ID for identification."
            )
        }

        @Suppress("SENSELESS_COMPARISON")
        if (listOf(node != null, edge != null, id != null).filter { it }.size > 1) {
            log.warn(
                "An assumption should be created with only one of the following arguments/properties: id, node or edge. But multiple of those are provided"
            )
        }

        if (assumptionScope == null) {
            this@Assumption.assumptionScope = super.underlyingNode
        }
        name = Name(assumptionType.name)
        location = node?.location
    }

    /**
     * The hash code of this [Assumption]. It is based on the hash code of the [underlyingNode] or
     * the [edge] that caused the assumption to be necessary, the [assumptionType], the [message],
     * and the [assumptionLocation]. This makes the assumption node identifiable across cpg
     * translations.
     */
    override fun hashCode(): Int {
        // The underlying node is already in the hashCode of the super class implementation.
        // If the assumption is created from an edge, the edge is != null and therefore influences
        // the hashCode.
        return Objects.hash(super.hashCode(), edge, assumptionType, message, assumptionLocation)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Assumption) return false
        return super.equals(other) &&
            edge == other.edge &&
            assumptionType == other.assumptionType &&
            message == other.message &&
            assumptionLocation == other.assumptionLocation
    }
}

/**
 * The [AssumptionStatus] that an assumption can have, default being set to
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

@Suppress("unused")
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

/**
 * This interface declares that the implementing class can hold assumptions necessary for static
 * code analysis. The assumptions are stored in a field [assumptions] and can be newly created with
 * [HasAssumptions.assume]. If an object that implements this interface is created conditionally
 * from other objects that implement this interface, the assumptions can be carried over by calling
 * [HasAssumptions.addAssumptionDependences] or [HasAssumptions.addAssumptionDependence].
 */
interface HasAssumptions {
    val assumptions: MutableList<Assumption>

    /**
     * This function adds a new assumption to the object it is called on. If the object is a node or
     * edge. The Assumption is added as an overlaying node for presentation in the graph. The
     * assumption is also added to the [assumptions] list. In the future the [Node.id] will be
     * deterministic across functions.
     *
     * @param assumptionType The type of assumption used to differentiate between assumptions and
     *   group similar assumptions.
     * @param scope The scope that the assumption has validity for, here the scope is a node,
     *   because the assumption is valid for every node in its ast subtree.
     * @param message The message describing the assumption that was taken.
     */
    fun <T : HasAssumptions> T.assume(
        assumptionType: AssumptionType,
        message: String,
        scope: Node? = null,
    ): T {
        this.assumptions.add(
            Assumption(
                assumptionType,
                message,
                getCallerFileAndLine(),
                node = this as? Node,
                edge = this as? Edge<*>,
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
    fun <T : HasAssumptions> T.addAssumptionDependences(
        haveAssumptions: Collection<HasAssumptions>
    ): T {
        this.assumptions.addAll(haveAssumptions.flatMap { it.assumptions })
        return this
    }

    /**
     * A convenience function to add a dependency to a single object holding assumptions. For more
     * documentation see [HasAssumptions.addAssumptionDependences].
     *
     * @param hasAssumptions add dependence to assumptions of a single other node.
     */
    fun <T : HasAssumptions> T.addAssumptionDependence(hasAssumptions: HasAssumptions): T {
        this.addAssumptionDependences(listOf(hasAssumptions))
        return this
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
        // implementing class is the caller of assumption creation
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
