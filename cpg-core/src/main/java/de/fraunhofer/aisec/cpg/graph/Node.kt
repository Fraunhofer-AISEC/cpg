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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.helpers.LocationConverter
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.IVisitable
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** The base class for all graph objects that are going to be persisted in the database. */
open class Node : IVisitable<Node>, Persistable {
    /** A human readable name. */
    open var name = EMPTY_NAME // initialize it with an empty string

    /**
     * Original code snippet of this node. Most nodes will have a corresponding "code", but in cases
     * where nodes are created artificially, it may be null.
     */
    var code: String? = null

    /** Optional comment of this node. */
    var comment: String? = null

    /** Location of the finding in source code. */
    @Convert(LocationConverter::class) var location: PhysicalLocation? = null

    /**
     * Name of the containing file. It can be null for artificially created nodes or if just
     * analyzing snippets of code without an associated file name.
     */
    var file: String? = null

    /** Incoming control flow edges. */
    @field:Relationship(value = "EOG", direction = "INCOMING")
    var prevEOGEdges: MutableList<PropertyEdge<Node>> = ArrayList()
        protected set

    /** outgoing control flow edges. */
    @field:Relationship(value = "EOG", direction = "OUTGOING")
    var nextEOGEdges: MutableList<PropertyEdge<Node>> = ArrayList()
        protected set

    /**
     * Virtual property to return a list of the node's children. Uses the [SubgraphWalker] to
     * retrieve the appropriate nodes.
     */
    val astChildren: List<Node>
        get() = SubgraphWalker.getAstChildren(this)

    /** Virtual property for accessing [prevEOGEdges] without property edges. */
    var prevEOG: List<Node>
        get() = unwrap(prevEOGEdges, false)
        set(value) {
            val propertyEdgesEOG: MutableList<PropertyEdge<Node>> = ArrayList()

            for ((idx, prev) in value.withIndex()) {
                val propertyEdge = PropertyEdge(prev, this)
                propertyEdge.addProperty(Properties.INDEX, idx)
                propertyEdgesEOG.add(propertyEdge)
            }

            this.prevEOGEdges = propertyEdgesEOG
        }

    /** Virtual property for accessing [nextEOGEdges] without property edges. */
    var nextEOG: List<Node>
        get() = unwrap(nextEOGEdges)
        set(value) {
            this.nextEOGEdges = PropertyEdge.transformIntoOutgoingPropertyEdgeList(value, this)
        }

    @field:Relationship(value = "DFG", direction = "INCOMING")
    var prevDFG: MutableSet<Node> = HashSet()

    @field:Relationship(value = "DFG") var nextDFG: MutableSet<Node> = HashSet()

    var typedefs: MutableSet<TypedefDeclaration> = HashSet()

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
    @field:Id @field:GeneratedValue var id: Long? = null

    /** Index of the argument if this node is used in a function call or parameter list. */
    var argumentIndex = 0

    /** List of annotations associated with that node. */
    @field:SubGraph("AST") var annotations: MutableList<Annotation> = ArrayList()

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

    fun addNextDFG(next: Node) {
        nextDFG.add(next)
        next.prevDFG.add(this)
    }

    fun removeNextDFG(next: Node?) {
        if (next != null) {
            nextDFG.remove(next)
            next.prevDFG.remove(this)
        }
    }

    fun addPrevDFG(prev: Node) {
        prevDFG.add(prev)
        prev.nextDFG.add(this)
    }

    fun removePrevDFG(prev: Node?) {
        if (prev != null) {
            prevDFG.remove(prev)
            prev.nextDFG.remove(this)
        }
    }

    fun addTypedef(typedef: TypedefDeclaration) {
        typedefs.add(typedef)
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
        for (n in nextDFG) {
            n.prevDFG.remove(this)
        }
        nextDFG.clear()

        for (n in prevDFG) {
            n.nextDFG.remove(this)
        }
        prevDFG.clear()

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

        if (name != "") {
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
            // we do not know the exact region. Need to rely on Object equalness,
            // as a different LOC can have the same name/code/comment/file
            false
        } else
            name == other.name &&
                code == other.code &&
                comment == other.comment &&
                location == other.location &&
                file == other.file &&
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

        protected val log: Logger = LoggerFactory.getLogger(Node::class.java)

        const val EMPTY_NAME = ""
    }
}
