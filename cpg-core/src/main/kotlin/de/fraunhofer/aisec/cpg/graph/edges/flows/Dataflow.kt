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
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.persistence.Convert
import de.fraunhofer.aisec.cpg.persistence.converters.DataflowGranularityConverter
import java.util.Objects
import kotlin.reflect.KProperty

/**
 * The granularity of the data-flow, e.g., whether the flow contains the whole object, or just a
 * part of it, for example a record (class/struct) member.
 *
 * The helper functions [full] and [field] can be used to construct either full or partial dataflow
 * granularity.
 */
sealed interface Granularity

/**
 * This dataflow granularity is the default. The "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end].
 */
data object FullDataflowGranularity : Granularity

/**
 * This dataflow granularity denotes that not the "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end] but only parts of it. Common examples include [MemberExpression]s, array or tuple
 * accesses. This class should allow
 */
open class PartialDataflowGranularity<T>(
    /** The target that is affected by this partial dataflow. */
    val partialTarget: T
) : Granularity {
    override fun equals(other: Any?): Boolean {
        return this.partialTarget == (other as? PartialDataflowGranularity<T>)?.partialTarget
    }

    override fun hashCode(): Int {
        return Objects.hash(partialTarget)
    }
}

/**
 * This dataflow granularity denotes that not the "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end] but only parts of it, where the part is identified by a (known) [Field]. Common
 * examples include [MemberExpression] nodes, where we model a dataflow to the base, but only
 * partially scoped to a particular field.
 */
class FieldDataflowGranularity(partialTarget: Field) :
    PartialDataflowGranularity<Field>(partialTarget)

/**
 * This dataflow granularity denotes that not the "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end] but only parts of it, where the part is identified by a (constant) integer. Common
 * examples include tuples or array indices.
 */
class IndexedDataflowGranularity(
    /** The index that is affected by this partial dataflow. */
    partialTarget: Number
) : PartialDataflowGranularity<Number>(partialTarget)

/**
 * This dataflow granularity denotes that not the "whole" object is flowing from [Dataflow.start] to
 * [Dataflow.end] but only parts of it, where the part is identified by a (constant) String. Common
 * examples include access to map entries or similar.
 */
class StringIndexedDataflowGranularity(
    /** The index that is affected by this partial dataflow. */
    partialTarget: String
) : PartialDataflowGranularity<String>(partialTarget)

/** Creates a new [FullDataflowGranularity]. */
fun full(): Granularity {
    return FullDataflowGranularity
}

/** Creates a new default [Granularity]. Currently, this defaults to [FullDataflowGranularity]. */
fun default() = full()

/**
 * Creates a new [FieldDataflowGranularity]. The [target] is the [Declaration] that is affected by
 * the partial dataflow. Examples include a [Field] for a [MemberExpression].
 */
fun field(target: Field): FieldDataflowGranularity {
    return FieldDataflowGranularity(target)
}

/**
 * Creates a new [PartialDataflowGranularity]. The [identifier] is used to access the specific part
 * of the whole object.
 */
fun <T> partial(identifier: T): PartialDataflowGranularity<T> {
    return PartialDataflowGranularity<T>(identifier)
}

/**
 * Creates a new [IndexedDataflowGranularity]. The [idx] is the index that is used for the partial
 * dataflow. An example is the access to an array or tuple element, or a [Variable] for a [Tuple].
 */
fun indexed(idx: Number): IndexedDataflowGranularity {
    return IndexedDataflowGranularity(idx)
}

/**
 * Creates a new [IndexedDataflowGranularity]. The [idx] is the index that is used for the partial
 * dataflow. An example is the access to a map entry.
 */
fun indexed(idx: String): StringIndexedDataflowGranularity {
    return StringIndexedDataflowGranularity(idx)
}

/**
 * This edge class defines a flow of data between [start] and [end]. The flow can have a certain
 * [granularity].
 */
open class Dataflow(
    start: Node,
    end: Node,
    /** The granularity of this dataflow. */
    @Convert(DataflowGranularityConverter::class)
    @JsonIgnore
    var granularity: Granularity = default(),
) : ProgramDependence(start, end, DependenceType.DATA) {
    override var labels = super.labels.plus("DFG")

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

sealed interface CallingContext {
    /** The call expression that affects this dataflow edge. */
    val call: CallExpression
}

class CallingContextIn(override val call: CallExpression) : CallingContext

class CallingContextOut(
    /** The call expression that affects this dataflow edge. */
    override val call: CallExpression
) : CallingContext

/**
 * This edge class defines a flow of data between [start] and [end]. The flow must have a
 * [callingContext] which allows for a context-sensitive dataflow analysis. This edge can also have
 * a certain [granularity].
 */
class ContextSensitiveDataflow(
    start: Node,
    end: Node,
    /** The granularity of this dataflow. */
    granularity: Granularity = default(),
    val callingContext: CallingContext,
) : Dataflow(start, end, granularity) {

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
        callingContext: CallingContext,
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
     * nextDFG, we add our thisRef to the "prev" of "next" and vice versa.
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
