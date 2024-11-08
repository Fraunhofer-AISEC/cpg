/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edges.flows

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.helpers.neo4j.DataflowGranularityConverter
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.*
import org.neo4j.ogm.annotation.typeconversion.Convert

/**
 * The granularity of the data-flow, e.g., whether the flow contains the whole object, or just a
 * part of it, for example a record (class/struct) member.
 *
 * The helper functions [full] and [partial] can be used to construct either full or partial
 * dataflow granularity.
 */
sealed interface Granularity

/**
 * This dataflow granularity is the default. The "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end].
 */
data object FullDataflowGranularity : Granularity

/**
 * This dataflow granularity denotes that not the "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end] but only parts of it. Common examples include [MemberExpression] nodes, where we
 * model a dataflow to the base, but only partially scoped to a particular field.
 */
class PartialDataflowGranularity(
    /** The target that is affected by this partial dataflow. */
    val partialTarget: Declaration?
) : Granularity

/** Creates a new [FullDataflowGranularity]. */
fun full(): Granularity {
    return FullDataflowGranularity
}

/** Creates a new default [Granularity]. Currently, this defaults to [FullDataflowGranularity]. */
fun default() = full()

/**
 * Creates a new [PartialDataflowGranularity]. The [target] is the [Declaration] that is affected by
 * the partial dataflow. Examples include a [FieldDeclaration] for a [MemberExpression] or a
 * [VariableDeclaration] for a [TupleDeclaration].
 */
fun partial(target: Declaration?): PartialDataflowGranularity {
    return PartialDataflowGranularity(target)
}

/**
 * This edge class defines a flow of data between [start] and [end]. The flow can have a certain
 * [granularity].
 */
@RelationshipEntity
open class Dataflow(
    start: Node,
    end: Node,
    /** The granularity of this dataflow. */
    @Convert(DataflowGranularityConverter::class)
    @JsonIgnore
    var granularity: Granularity = default()
) : Edge<Node>(start, end) {
    override val label: String = "DFG"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Dataflow) return false
        return this.granularity == other.granularity && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + granularity.hashCode()
        return result
    }
}

sealed interface CallingContext

class CallingContextIn(
    /** The call expression that affects this dataflow edge. */
    val call: CallExpression
) : CallingContext

class CallingContextOut(
    /** The call expression that affects this dataflow edge. */
    val call: CallExpression
) : CallingContext

/**
 * This edge class defines a flow of data between [start] and [end]. The flow must have a
 * [callingContext] which allows for a context-sensitive dataflow analysis. This edge can also have
 * a certain [granularity].
 */
@RelationshipEntity
class ContextSensitiveDataflow(
    start: Node,
    end: Node,
    /** The granularity of this dataflow. */
    granularity: Granularity = default(),
    val callingContext: CallingContext
) : Dataflow(start, end, granularity) {

    override val label: String = "DFG"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContextSensitiveDataflow) return false
        return this.callingContext == other.callingContext && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + callingContext.hashCode()
        return result
    }
}

/** This class represents a container of [Dataflow] property edges in a [thisRef]. */
class Dataflows<T : Node>(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<Dataflow>>,
    outgoing: Boolean,
) :
    EdgeSet<Node, Dataflow>(thisRef = thisRef, init = ::Dataflow, outgoing = outgoing),
    MirroredEdgeCollection<Node, Dataflow> {

    /**
     * Adds a [ContextSensitiveDataflow] edge from/to (depending on [outgoing]) the node which
     * contains this edge container to/from [node], with the given [Granularity].
     */
    fun addContextSensitive(
        node: T,
        granularity: Granularity = default(),
        callingContext: CallingContext
    ) {
        val edge =
            if (outgoing) {
                ContextSensitiveDataflow(thisRef, node, granularity, callingContext)
            } else {
                ContextSensitiveDataflow(node, thisRef, granularity, callingContext)
            }

        this.add(edge)
    }

    /**
     * This connects our dataflow to our "mirror" property. Meaning that if we add a node to
     * nextDFG, we add our thisRef to the "prev" of "next" and vice-versa.
     */
    override fun handleOnAdd(edge: Dataflow) {
        super<MirroredEdgeCollection>.handleOnAdd(edge)
        val start = edge.start
        val thisRef = this.thisRef

        // For references, we want to propagate assigned types all through the previous DFG nodes.
        // Therefore, we add a type observer to the previous node (if it is not ourselves)
        if (thisRef is Reference && !outgoing && start != thisRef && start is HasType) {
            start.registerTypeObserver(thisRef)
        }
    }
}
