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

import de.fraunhofer.aisec.cpg.frontends.TranslationException
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
 * Notes on writing the [message]: The message should specify what we assume, the condition, and
 * ideally the reason why this assumption was necessary and how it can be verified. The text should
 * start with a pattern such as "We assume that ... .", where "..." contains a description of the
 * assumption with the reference to concrete nodes or edges affected by it. Afterward, it is
 * beneficial to continue with a paragraph "To verify this assumption, we need to check ...".
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
        if (listOf(node != null, edge != null).filter { it }.size > 1) {
            throw TranslationException(
                "An assumption must be created with only one of the following arguments/properties: node or edge. But multiple of those are provided"
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
        // If the assumption is created from an edge, the edge is not null and therefore influences
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

/**
 * The purpose of the assumption types is to group assumptions by their semantics. These groupings
 * can then be used to evaluate better what causes or impact an assumption can have.
 */
@Suppress("unused")
enum class AssumptionType {
    /**
     * Used when an assumption is made regarding inferring the existence of code represented as
     * nodes or edges.
     */
    InferenceAssumption,
    /**
     * Assuming that the found solutions for a problem contains all possible solutions in the given
     * system, e.g., the found nodes are a complete set of nodes that we had to find in our search.
     */
    CompletenessAssumption,
    /**
     * A subtype of [CompletenessAssumption]. Describes the assumption that we considered all
     * possible cases when handling a certain subject. e.g., assuming that an operation can be done
     * by one of exactly four function calls specified in a list.
     */
    ExhaustiveEnumerationAssumption,
    /**
     * Assuming that all solutions for a problem are correct, and no over-approximation happened,
     * e.g., all nodes listed can be the target of a call during runtime.
     */
    SoundnessAssumption,
    /**
     * Used when making assumptions on preprocessing instructions that were not processed before CPG
     * translation.
     */
    PreprocessingAssumption,
    /**
     * A subtype of [PreprocessingAssumption]. Assuming that a macro that was not resolved before
     * CPG translation adheres to some constraint, e.g., assuming a macro expands into complete
     * syntactic unit.
     */
    MacroAssumption,
    /**
     * Used when marking a subtree in the CPG that we could not translate due to the language not
     * being supported, e.g. we assume that the contained code has no relevant influence on the
     * execution of the surrounding translated and analyzed CPG.
     */
    UnsupportedLanguageAssumption,
    /**
     * Used to declare an assumption on missing code that could not be analyzed and its impact on
     * CPG translation or the resulting analysis.
     */
    MissingCodeAssumption,
    /** Used when having to decide between several potential program executions. */
    AmbiguityAssumption,
    /**
     * A subtype of [AmbiguityAssumption]. Used when assuming that one of several interpretations of
     * a syntactic ambiguity is correct.
     */
    SyntaxAmbiguityAssumption,
    /**
     * Used to declare assumptions related to concept placement, or concept behavior, e.g., assuming
     * the heuristic that a function name contains "open" and "file" results in the existence of a
     * file concept.
     */
    ConceptAssumption,
    /** A general type to declare an assumption on the control flow of a program. */
    ControlFlowAssumption,
    /**
     * A subtype of [ControlFlowAssumption] when assuming that the entire program, or parts of the
     * program, execute linearly without any unexpected or malicious changes in control flow.
     */
    CFIntegrityAssumption,
    /**
     * A subtype of [ControlFlowAssumption]. Used to make assumptions about the uninterrupted
     * execution of a code block that is either executed entirely or not at all.
     */
    AtomicExecutionAssumption,
    /**
     * A subtype of [ControlFlowAssumption]. Used to make assumptions about the exception-free
     * execution, or on constraints what exceptions can be thrown.
     */
    ExceptionsAssumption,
    /** Assumptions on data flows that occur during program execution. */
    DataFlowAssumption,
    /** Used to declare more general assumptions on input data. */
    InputAssumptions,
    /**
     * An assumption on input data, that assumes an additional constraint on the data, which is not
     * enforced through the type or implementation of the data type.
     */
    DataRangeAssumption,
    /**
     * A Subtype of [InputAssumptions]. Used to declare assumptions on the trustworthiness of input
     * data, e.g., can be used to see if the result of an analysis requires data to be from a
     * trustworthy source.
     */
    TrustedInputAssumption,
    /**
     * Used for assumptions on the trustworthiness of configurations that are used during runtime.
     */
    ConfigTrustAssumption,
    /**
     * Used for assumptions on external data, that may not be from the user or trusted
     * configurations, e.g., the format of this external data adheres to some constraint.
     */
    ExternalDataAssumption,
    /**
     * Used to declare assumptions on trust boundaries, e.g., that an endpoint establishes a trust
     * boundary or that specific constraints have to hold for users or data within the trust
     * boundary.
     */
    TrustBoundaryAssumption,
    /**
     * Assumptions on resources and their availability, e.g. , the file that is opened here exists
     * or the database is available.
     */
    ResourceAvailableAssumption,
    /**
     * Used to declare assumptions on the network connection, e.g., availability, latency, or
     * stability assumptions.
     */
    NetworkAssumption,
    /**
     * A subtype of [ResourceAvailableAssumption]. Used to make assumptions on specific services
     * being available, e.g., over the network.
     */
    ServiceReachableAssumption,
}

/**
 * This interface declares that the implementing class can hold assumptions necessary for static
 * code analysis. The assumptions are stored in a field [assumptions] and can be newly created with
 * [HasAssumptions.assume]. If an object that implements this interface is created conditionally
 * from other objects that implement this interface, the assumptions can be carried over by calling
 * [HasAssumptions.addAssumptionDependence] or [HasAssumptions.addAssumptionDependence].
 */
interface HasAssumptions {

    /**
     * This set only contains the assumptions that were added by invoking [assume] or [addAssumptionDependence]
     * to this object. To gather all assumptions that are relevant for this object, call [collectAssumptions].
     * This is necessary as different parts of cpg construction and augmentation can add assumptions to a
     * dependent object.
     */
    val assumptions: MutableSet<Assumption>

    /**
     * This is function is used to collect all assumptions stored in the [HasAssumptions] object and
     * its contained objects that can have assumptions. While this default implementation is simple,
     * composite [HasAssumptions] objects should return the assumptions of the contained objects.
     */
    fun collectAssumptions(): Set<Assumption> {
        return assumptions.toSet()
    }
}

/**
 * This function adds a new assumption to the object it is called on. If the object is a node or
 * edge. The Assumption is added as an overlaying node for presentation in the graph. The assumption
 * is also added to the [assumptions] list. In the future the [Node.id] will be deterministic across
 * functions.
 *
 * Notes on writing the [message]: The message should specify what we assume, the condition, and
 * ideally the reason why this assumption was necessary and how it can be verified. The text should
 * start with a pattern such as "We assume that ... .", where "..." contains a description of the
 * assumption with the reference to concrete nodes or edges affected by it. Afterward, it is
 * beneficial to continue with a paragraph "To verify this assumption, we need to check ...".
 *
 * @param assumptionType The type of assumption used to differentiate between assumptions and group
 *   similar assumptions.
 * @param scope The scope that the assumption has validity for, here the scope is a node, because
 *   the assumption is valid for every node in its ast subtree.
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
 * graph that can depend on assumptions made on other objects. One example is when creating concepts
 * after following a DFG path. Concepts are added to the graph and then serve as nodes for further
 * queries, and therefore indirect assumptions would be lost if not copied over with this function.
 *
 * This is the current implementation for assumption propagation and is not needed when the object
 * already contains the assumption dependent node.
 *
 * @param haveAssumptions nodes that hold assumptions this object dependent on.
 */
fun <T : HasAssumptions> T.addAssumptionDependence(vararg haveAssumptions: HasAssumptions): T {
    this.assumptions.addAll(haveAssumptions.flatMap { it.collectAssumptions() })
    return this
}

/**
 * This function is supposed to be used when a new object is created after searching through the
 * graph that can depend on assumptions made on other objects. One example is when creating concepts
 * after following a DFG path. Concepts are added to the graph and then serve as nodes for further
 * queries, and therefore indirect assumptions would be lost if not copied over with this function.
 *
 * This is the current implementation for assumption propagation and is not needed when the object
 * already contains the assumption dependent node.
 *
 * @param haveAssumptions nodes that hold assumptions this object dependent on.
 */
fun <T : HasAssumptions> T.addAssumptionDependence(haveAssumptions: Collection<HasAssumptions>): T {
    return this.addAssumptionDependence(*haveAssumptions.toTypedArray())
}

/**
 * This function returns a SARIF formatted location of the caller that creates an assumption. The
 * function is intentionally made private to avoid outside use and functions if it is called inside
 * of [HasAssumptions].
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
