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
package de.fraunhofer.aisec.cpg.graph.edge

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TupleDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import org.neo4j.ogm.annotation.RelationshipEntity

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
 * This dataflow granularity denotes that the value or address of a pointer is flowing from
 * [Dataflow.start] to [Dataflow.end].
 */
class PointerDataflowGranularity() : Granularity

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
 * Creates a new [PointerDataflowGranularity]. The [ValueAccess] is specifies if the pointer's value
 * is accessed, or its address.
 */
fun pointer(): PointerDataflowGranularity {
    return PointerDataflowGranularity()
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
    val granularity: Granularity = default()
) : PropertyEdge<Node>(start, end) {
    override val label: String = "DFG"
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
    /** The calling context affecting this dataflow. */
    val callingContext: CallingContext,
    /** The granularity of this dataflow. */
    granularity: Granularity,
) : Dataflow(start, end, granularity) {
    override val label: String = "DFG"
}
