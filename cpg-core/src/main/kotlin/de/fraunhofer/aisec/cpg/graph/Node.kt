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
package de.fraunhofer.aisec.cpg.graph

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.neo4j.LocationConverter
import de.fraunhofer.aisec.cpg.helpers.neo4j.NameConverter
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.processing.IVisitable
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.neo4j.ogm.annotation.*
import org.neo4j.ogm.annotation.typeconversion.Convert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** The base class for all graph objects that are going to be persisted in the database. */
open class Node : IVisitable<Node>, Persistable, LanguageProvider, ScopeProvider, ContextProvider {
    /**
     * Because we are updating type information in the properties of the node, we need a reference
     * to managers such as the [TypeManager] instance which is responsible for this particular node.
     * All managers are bundled in [TranslationContext]. It is set in [Node.applyMetadata] when a
     * [ContextProvider] is provided.
     */
    @get:JsonIgnore @Transient override var ctx: TranslationContext? = null

    /**
     * This property holds the full name using our new [Name] class. It is currently not persisted
     * in the graph database.
     */
    @Convert(NameConverter::class) open var name: Name = Name(EMPTY_NAME)

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
    override var language: Language<*>? = null

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

    /** Location of the finding in source code. */
    @Convert(LocationConverter::class) var location: PhysicalLocation? = null

    /**
     * Name of the containing file. It can be null for artificially created nodes or if just
     * analyzing snippets of code without an associated file name.
     */
    @PopulatedByPass(FilenameMapper::class) var file: String? = null

    /** Incoming control flow edges. */
    @Relationship(value = "EOG", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(EvaluationOrderGraphPass::class)
    var prevEOGEdges: MutableList<PropertyEdge<Node>> = ArrayList()
        protected set

    /** Outgoing control flow edges. */
    @Relationship(value = "EOG", direction = Relationship.Direction.OUTGOING)
    @PopulatedByPass(EvaluationOrderGraphPass::class)
    var nextEOGEdges: MutableList<PropertyEdge<Node>> = ArrayList()
        protected set

    /**
     * The nodes which are control-flow dominated, i.e., the children of the Control Dependence
     * Graph (CDG).
     */
    @PopulatedByPass(ControlDependenceGraphPass::class)
    @Relationship(value = "CDG", direction = Relationship.Direction.OUTGOING)
    var nextCDGEdges: MutableList<PropertyEdge<Node>> = ArrayList()
        protected set

    var nextCDG by PropertyEdgeDelegate(Node::nextCDGEdges, true)

    /**
     * The nodes which dominate this node via the control-flow, i.e., the parents of the Control
     * Dependence Graph (CDG).
     */
    @PopulatedByPass(ControlDependenceGraphPass::class)
    @Relationship(value = "CDG", direction = Relationship.Direction.INCOMING)
    var prevCDGEdges: MutableList<PropertyEdge<Node>> = ArrayList()
        protected set

    var prevCDG by PropertyEdgeDelegate(Node::prevCDGEdges, false)

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
    var astChildren: List<Node> = listOf()
        get() = SubgraphWalker.getAstChildren(this)

    /** Virtual property for accessing [prevEOGEdges] without property edges. */
    @PopulatedByPass(EvaluationOrderGraphPass::class)
    var prevEOG: List<Node> by PropertyEdgeDelegate(Node::prevEOGEdges, false)

    /** Virtual property for accessing [nextEOGEdges] without property edges. */
    @PopulatedByPass(EvaluationOrderGraphPass::class)
    var nextEOG: List<Node> by PropertyEdgeDelegate(Node::nextEOGEdges)

    /** Incoming data flow edges */
    @Relationship(value = "DFG", direction = Relationship.Direction.INCOMING)
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    var prevDFGEdges: MutableList<Dataflow> = mutableListOf()
        protected set

    /** Virtual property for accessing [prevDFGEdges] without property edges. */
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    var prevDFG: MutableSet<Node> by PropertyEdgeSetDelegate(Node::prevDFGEdges, false)

    /** Outgoing data flow edges */
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    @Relationship(value = "DFG", direction = Relationship.Direction.OUTGOING)
    var nextDFGEdges: MutableList<Dataflow> = mutableListOf()
        protected set

    /** Virtual property for accessing [nextDFGEdges] without property edges. */
    @PopulatedByPass(DFGPass::class, ControlFlowSensitiveDFGPass::class)
    var nextDFG: MutableSet<Node> by PropertyEdgeSetDelegate(Node::nextDFGEdges, true)

    /** Outgoing Program Dependence Edges. */
    @PopulatedByPass(ProgramDependenceGraphPass::class)
    @Relationship(value = "PDG", direction = Relationship.Direction.OUTGOING)
    var nextPDGEdges: MutableSet<PropertyEdge<Node>> = mutableSetOf()
        protected set

    /** Virtual property for accessing the children of the Program Dependence Graph (PDG). */
    var nextPDG: MutableSet<Node> by PropertyEdgeSetDelegate(Node::nextPDGEdges, true)

    /** Incoming Program Dependence Edges. */
    @PopulatedByPass(ProgramDependenceGraphPass::class)
    @Relationship(value = "PDG", direction = Relationship.Direction.INCOMING)
    var prevPDGEdges: MutableSet<PropertyEdge<Node>> = mutableSetOf()
        protected set

    /** Virtual property for accessing the parents of the Program Dependence Graph (PDG). */
    var prevPDG: MutableSet<Node> by PropertyEdgeSetDelegate(Node::prevPDGEdges, false)

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
    @Id @GeneratedValue var id: Long? = null

    /** Index of the argument if this node is used in a function call or parameter list. */
    var argumentIndex = 0

    /** List of annotations associated with that node. */
    @AST var annotations: MutableList<Annotation> = ArrayList()

    fun removePrevEOGEntry(eog: Node) {
        removePrevEOGEntries(listOf(eog))
    }

    private fun removePrevEOGEntries(prevEOGs: List<Node>) {
        for (n in prevEOGs) {
            val remove = PropertyEdge.findPropertyEdgesByPredicate(prevEOGEdges) { it.start === n }
            prevEOGEdges.removeAll(remove)
        }
    }

    fun addPrevEOG(propertyEdge: PropertyEdge<Node>) {
        prevEOGEdges.add(propertyEdge)
    }

    fun addNextEOG(propertyEdge: PropertyEdge<Node>) {
        nextEOGEdges.add(propertyEdge)
    }

    fun clearNextEOG() {
        nextEOGEdges.clear()
    }

    /** Adds a [Dataflow] edge from this node to [next], with the given [Granularity]. */
    fun addNextDFG(
        next: Node,
        granularity: Granularity = default(),
    ) {
        val edge = Dataflow(this, next, granularity)
        nextDFGEdges.add(edge)
        next.prevDFGEdges.add(edge)
    }

    /**
     * Adds a [Dataflow] edge from this node to [next], with the given [CallingContext] and
     * [Granularity].
     */
    fun addNextDFGWithContext(
        next: Node,
        callingContext: CallingContext,
        granularity: Granularity = default(),
    ) {
        val edge = ContextSensitiveDataflow(this, next, callingContext, granularity)
        nextDFGEdges.add(edge)
        next.prevDFGEdges.add(edge)
    }

    fun removeNextDFG(next: Node?) {
        if (next != null) {
            val thisRemove =
                PropertyEdge.findPropertyEdgesByPredicate(nextDFGEdges) { it.end === next }
            nextDFGEdges.removeAll(thisRemove)

            val nextRemove =
                PropertyEdge.findPropertyEdgesByPredicate(next.prevDFGEdges) { it.start == this }
            next.prevDFGEdges.removeAll(nextRemove)
        }
    }

    /** Adds a [Dataflow] edge from [prev] node to this node, with the given [Granularity]. */
    open fun addPrevDFG(
        prev: Node,
        granularity: Granularity = default(),
    ) {
        val edge = Dataflow(prev, this, granularity)
        prevDFGEdges.add(edge)
        prev.nextDFGEdges.add(edge)
    }

    /**
     * Adds a [Dataflow] edge from [prev] node to this node, with the given [CallingContext] and
     * [Granularity].
     */
    open fun addPrevDFGWithContext(
        prev: Node,
        callingContext: CallingContext,
        granularity: Granularity = default(),
    ) {
        val edge = ContextSensitiveDataflow(prev, this, callingContext, granularity)
        prevDFGEdges.add(edge)
        prev.nextDFGEdges.add(edge)
    }

    fun addPrevCDG(
        prev: Node,
        properties: MutableMap<Properties, Any?> = EnumMap(Properties::class.java)
    ) {
        val edge = PropertyEdge(prev, this, properties)
        prevCDGEdges.add(edge)
        prev.nextCDGEdges.add(edge)
    }

    /** Adds a [Dataflow] edge from all [prev] nodes to this node, with the given [Granularity]. */
    fun addAllPrevDFG(
        prev: Collection<Node>,
        granularity: Granularity = full(),
    ) {
        prev.forEach { addPrevDFG(it, granularity) }
    }

    /**
     * Adds a [Dataflow] edge from all [prev] nodes to this node, with the given [CallingContext]
     * and [Granularity].
     */
    fun addAllPrevDFGWithContext(
        prev: Collection<Node>,
        callingContext: CallingContext,
        granularity: Granularity = full(),
    ) {
        prev.forEach { addPrevDFGWithContext(it, callingContext, granularity) }
    }

    fun addAllPrevPDG(prev: Collection<Node>, dependenceType: DependenceType) {
        addAllPrevPDGEdges(prev.map { PropertyEdge(it, this) }, dependenceType)
    }

    fun addAllPrevPDGEdges(prev: Collection<PropertyEdge<Node>>, dependenceType: DependenceType) {
        prev.forEach {
            val edge = PropertyEdge(it).apply { addProperty(Properties.DEPENDENCE, dependenceType) }
            this.prevPDGEdges.add(edge)
            val other = if (it.start != this) it.start else it.end
            other.nextPDGEdges.add(edge)
        }
    }

    fun removePrevDFG(prev: Node?) {
        if (prev != null) {
            val thisRemove =
                PropertyEdge.findPropertyEdgesByPredicate(prevDFGEdges) { it.start === prev }
            prevDFGEdges.removeAll(thisRemove)

            val prevRemove =
                PropertyEdge.findPropertyEdgesByPredicate(prev.nextDFGEdges) { it.end === this }
            prev.nextDFGEdges.removeAll(prevRemove)
        }
    }

    fun clearPrevDFG() {
        for (prev in ArrayList(prevDFG)) {
            removePrevDFG(prev)
        }
    }

    fun clearNextDFG() {
        for (prev in ArrayList(nextDFG)) {
            removeNextDFG(prev)
        }
    }

    fun addAnnotations(annotations: Collection<Annotation>) {
        this.annotations.addAll(annotations)
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
        for (n in nextDFGEdges) {
            val remove =
                PropertyEdge.findPropertyEdgesByPredicate(n.end.prevDFGEdges) { it.start == this }
            n.end.prevDFGEdges.removeAll(remove)
        }
        nextDFGEdges.clear()

        for (n in prevDFGEdges) {
            val remove =
                PropertyEdge.findPropertyEdgesByPredicate(n.start.nextDFGEdges) { it.end == this }
            n.start.nextDFGEdges.removeAll(remove)
        }
        prevDFGEdges.clear()

        for (n in nextEOGEdges) {
            val remove =
                PropertyEdge.findPropertyEdgesByPredicate(n.end.prevEOGEdges) { it.start == this }
            n.end.prevEOGEdges.removeAll(remove)
        }
        nextEOGEdges.clear()

        for (n in prevEOGEdges) {
            val remove =
                PropertyEdge.findPropertyEdgesByPredicate(n.start.nextEOGEdges) { it.end == this }
            n.start.nextEOGEdges.removeAll(remove)
        }
        prevEOGEdges.clear()
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
                // We need to exclude "file" here, because in C++ the same header node can be
                // imported in two different files and in this case, the "file" property will be
                // different. Since want to squash those equal nodes, we will only consider all the
                // other attributes, including "location" (which contains the *original* file
                // location in the header file), but not "file".
                // file == other.file &&
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
