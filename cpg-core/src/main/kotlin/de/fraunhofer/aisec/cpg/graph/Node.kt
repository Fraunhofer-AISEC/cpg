/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.graph

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.flows.*
import de.fraunhofer.aisec.cpg.graph.edges.overlay.Overlays
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.neo4j.LocationConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.processing.IVisitable
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.util.*
import kotlin.uuid.Uuid
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient
import org.neo4j.ogm.annotation.typeconversion.Convert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** The base class for all graph objects that are going to be persisted in the database. */
abstract class Node() :
    IVisitable<Node>,
    Persistable,
    LanguageProvider,
    ScopeProvider,
    HasNameAndLocation,
    HasScope,
    HasAssumptions {

    /** This property holds the full name using our new [Name] class. */
    @Convert(NameConverter::class) override var name: Name = Name(EMPTY_NAME)

    /**
     * Original code snippet of this node. Most nodes will have a corresponding "code", but in cases
     * where nodes are created artificially, it may be null.
     */
    var code: String? = null

    /**
     * The language of this node. This property is set in [Node.applyMetadata] by a
     * [LanguageProvider] at the time when the node is created.
     */
    @Relationship(value = "LANGUAGE", direction = Relationship.Direction.OUTGOING)
    @JsonBackReference
    override var language: Language<*> = UnknownLanguage

    /**
     * The scope this node "lives" in / in which it is defined. This property is set in
     * [Node.applyMetadata] by a [ScopeProvider] at the time when the node is created.
     *
     * For example, if a [RecordDeclaration] is defined in a [TranslationUnitDeclaration] (without
     * any namespaces), the scope of the [RecordDeclaration] is most likely a [GlobalScope]. Since
     * the declaration itself creates a [RecordScope], the scope of a [MethodDeclaration] within the
     * class would be a [RecordScope] pointing to the [RecordDeclaration].
     */
    @Relationship(value = "SCOPE", direction = Relationship.Direction.OUTGOING)
    @JsonBackReference
    override var scope: Scope? = null

    /** Optional comment of this node. */
    var comment: String? = null

    @Convert(LocationConverter::class) override var location: PhysicalLocation? = null

    /** Incoming control flow edges. */
    @Relationship(value = "EOG", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(EvaluationOrderGraphPass::class)
    var prevEOGEdges: EvaluationOrders<Node> =
        EvaluationOrders<Node>(this, mirrorProperty = Node::nextEOGEdges, outgoing = false)
        protected set

    /** Outgoing control flow edges. */
    @Relationship(value = "EOG", direction = Relationship.Direction.OUTGOING)
    @PopulatedByPass(EvaluationOrderGraphPass::class)
    var nextEOGEdges: EvaluationOrders<Node> =
        EvaluationOrders<Node>(this, mirrorProperty = Node::prevEOGEdges, outgoing = true)
        protected set

    /**
     * The nodes which are control-flow dominated, i.e., the children of the Control Dependence
     * Graph (CDG).
     */
    @PopulatedByPass(ControlDependenceGraphPass::class)
    @Relationship(value = "CDG", direction = Relationship.Direction.OUTGOING)
    var nextCDGEdges: ControlDependences<Node> =
        ControlDependences(this, mirrorProperty = Node::prevCDGEdges, outgoing = true)
        protected set

    var nextCDG by unwrapping(Node::nextCDGEdges)

    /**
     * The nodes which dominate this node via the control-flow, i.e., the parents of the Control
     * Dependence Graph (CDG).
     */
    @PopulatedByPass(ControlDependenceGraphPass::class)
    @Relationship(value = "CDG", direction = Relationship.Direction.INCOMING)
    var prevCDGEdges: ControlDependences<Node> =
        ControlDependences<Node>(this, mirrorProperty = Node::nextCDGEdges, outgoing = false)
        protected set

    var prevCDG by unwrapping(Node::prevCDGEdges)

    /**
     * Virtual property to return a list of the node's children. Uses the [SubgraphWalker] to
     * retrieve the appropriate nodes.
     *
     * Note: This only returns the *direct* children of this node. If you want to have *all*
     * children, e.g., a flattened AST, you need to call [Node.allChildren].
     *
     * For Neo4J OGM, this relationship will be automatically filled by a pre-save event before OGM
     * persistence. Therefore, this property is a `var` and not a `val`.
     */
    @Relationship("AST")
    @JsonIgnore
    var astChildren: List<Node> = listOf()
        get() = SubgraphWalker.getAstChildren(this)

    @DoNotPersist @Transient var astParent: Node? = null

    /** Virtual property for accessing [prevEOGEdges] without property edges. */
    @PopulatedByPass(EvaluationOrderGraphPass::class) var prevEOG by unwrapping(Node::prevEOGEdges)

    /** Virtual property for accessing [nextEOGEdges] without property edges. */
    @PopulatedByPass(EvaluationOrderGraphPass::class) var nextEOG by unwrapping(Node::nextEOGEdges)

    /** Incoming data flow edges */
    @Relationship(value = "DFG", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    var prevDFGEdges: Dataflows<Node> =
        Dataflows<Node>(this, mirrorProperty = Node::nextDFGEdges, outgoing = false)
        protected set

    /** Virtual property for accessing [prevDFGEdges] without property edges. */
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    var prevDFG by unwrapping(Node::prevDFGEdges)

    /**
     * Virtual property for accessing [nextDFGEdges] that have a
     * [de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity].
     */
    @DoNotPersist
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    val prevFullDFG: List<Node>
        get() {
            return prevDFGEdges
                .filter { it.granularity is FullDataflowGranularity }
                .map { it.start }
        }

    /** Outgoing data flow edges */
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    @Relationship(value = "DFG", direction = Relationship.Direction.OUTGOING)
    var nextDFGEdges: Dataflows<Node> =
        Dataflows<Node>(this, mirrorProperty = Node::prevDFGEdges, outgoing = true)
        protected set

    /** Virtual property for accessing [nextDFGEdges] without property edges. */
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    var nextDFG by unwrapping(Node::nextDFGEdges)

    /**
     * Virtual property for accessing [nextDFGEdges] that have a
     * [de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity].
     */
    @DoNotPersist
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    val nextFullDFG: List<Node>
        get() {
            return nextDFGEdges.filter { it.granularity is FullDataflowGranularity }.map { it.end }
        }

    /** Outgoing Program Dependence Edges. */
    @PopulatedByPass(ProgramDependenceGraphPass::class)
    @Relationship(value = "PDG", direction = Relationship.Direction.OUTGOING)
    var nextPDGEdges: ProgramDependences<Node> =
        ProgramDependences<Node>(this, mirrorProperty = Node::prevPDGEdges, outgoing = false)
        protected set

    var nextPDG by unwrapping(Node::nextPDGEdges)

    /** Incoming Program Dependence Edges. */
    @PopulatedByPass(ProgramDependenceGraphPass::class)
    @Relationship(value = "PDG", direction = Relationship.Direction.INCOMING)
    var prevPDGEdges: ProgramDependences<Node> =
        ProgramDependences<Node>(this, mirrorProperty = Node::nextPDGEdges, outgoing = false)
        protected set

    var prevPDG by unwrapping(Node::prevPDGEdges)

    @DoNotPersist override val assumptions: MutableSet<Assumption> = mutableSetOf()

    /**
     * If a node is marked as being inferred, it means that it was created artificially and does not
     * necessarily have a real counterpart in the scanned source code. However, the nodes
     * represented should have been part of parser output and represents missing code that is
     * inferred by the CPG construction, e.g. missing functions, records, files etc.
     */
    var isInferred = false

    /**
     * Specifies, whether this node is implicit, i.e. is not really existing in source code but only
     * exists implicitly. This mostly relates to implicit casts, return statements or implicit this
     * expressions.
     */
    var isImplicit = false

    /** Required field for object graph mapping. It contains the node id. */
    @DoNotPersist @Id @GeneratedValue var legacyId: Long? = null

    /**
     * A (more or less) unique identifier for this node. It is a [Uuid] derived from
     * [Node.hashCode]. In this sense, it is definitely deterministic and reproducible, however, in
     * theory it is not completely unique, as collisions within [Node.hashCode] could occur.
     */
    val id: Uuid
        get() {
            val parent =
                astParent?.id?.toLongs { mostSignificantBits, leastSignificantBits ->
                    leastSignificantBits
                }
            return Uuid.fromLongs(parent ?: 0, hashCode().toLong())
        }

    /** Index of the argument if this node is used in a function call or parameter list. */
    var argumentIndex = 0

    /** List of annotations associated with that node. */
    @Relationship("ANNOTATIONS") var annotationEdges = astEdgesOf<Annotation>()
    var annotations by unwrapping(Node::annotationEdges)

    /**
     * Additional problem nodes. These nodes represent problems which occurred during processing of
     * a node (i.e. only partially processed).
     */
    val additionalProblems: MutableSet<ProblemNode> = mutableSetOf()

    @Relationship(value = "OVERLAY", direction = Relationship.Direction.OUTGOING)
    val overlayEdges: Overlays =
        Overlays(this, mirrorProperty = OverlayNode::underlyingNodeEdge, outgoing = true)
    var overlays by unwrapping(Node::overlayEdges)

    override fun collectAssumptions(): Set<Assumption> {
        return super.collectAssumptions() + (component?.assumptions ?: emptySet())
    }

    /**
     * If a node should be removed from the graph, just removing it from the AST is not enough (see
     * issue #60). It will most probably be referenced somewhere via DFG or EOG edges. Thus, if it
     * needs to be disconnected completely, we will have to take care of correctly disconnecting
     * these implicit edges.
     *
     * ATTENTION! Please note that this might kill an entire subgraph, if the node to disconnect has
     * further children that have no alternative connection paths to the rest of the graph.
     */
    fun disconnectFromGraph() {
        // Disconnect all AST children first
        astChildren.forEach { it.disconnectFromGraph() }

        nextDFGEdges.clear()
        prevDFGEdges.clear()
        prevCDGEdges.clear()
        nextCDGEdges.clear()
        prevPDGEdges.clear()
        nextPDGEdges.clear()
        nextEOGEdges.clear()
        prevEOGEdges.clear()

        if (this is OverlayNode) {
            underlyingNodeEdge.clear()
        }

        this.overlayEdges.clear()
    }

    override fun toString(): String {
        val builder = ToStringBuilder(this, TO_STRING_STYLE)

        if (name.isNotEmpty()) {
            builder.append("name", name)
        }

        return builder.append("location", location).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Node) {
            return false
        }
        return if (location == null || other.location == null) {
            // we do not know the exact region. Need to rely on Object equality,
            // as a different LOC can have the same name/code/comment/file
            false
        } else
            name == other.name &&
                code == other.code &&
                comment == other.comment &&
                location == other.location &&
                isImplicit == other.isImplicit
    }

    /**
     * Implementation of hash code. We are including the name and the location in this hash code as
     * a compromise between including too few attributes and performance. Please note that this
     * means, that two nodes that might be semantically equal, such as two record declarations with
     * the same name but different location (e.g. because of header files) will be sorted into
     * different hash keys.
     *
     * That means, that you need to be careful, if you use a [Node] as a key in a hash map. You
     * should make sure that the [location] is set before you add it to a hash map. This can be a
     * little tricky, since normally the [Handler] class will set the location after it has
     * "handled" the node. However, most [NodeBuilder] will have an optional parameter to set the
     * location already when creating the node.
     */
    override fun hashCode(): Int {
        return Objects.hash(name, location, this.javaClass)
    }

    companion object {
        @JvmField var TO_STRING_STYLE: ToStringStyle = ToStringStyle.SHORT_PREFIX_STYLE

        @JvmStatic protected val log: Logger = LoggerFactory.getLogger(Node::class.java)

        const val EMPTY_NAME = ""
    }
}

/**
 * Works similar to [apply] but before executing [block], it enters the scope for this object and
 * afterward leaves the scope again.
 */
context(ContextProvider)
inline fun <reified T : Node> T.applyWithScope(block: T.() -> Unit): T {
    return this.apply {
        (this@ContextProvider).ctx.scopeManager.enterScope(this)
        block()
        (this@ContextProvider).ctx.scopeManager.leaveScope(this)
    }
}
