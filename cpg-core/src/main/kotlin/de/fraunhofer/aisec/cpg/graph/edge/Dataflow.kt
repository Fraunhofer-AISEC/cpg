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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import org.neo4j.ogm.annotation.RelationshipEntity

/**
 * The granularity of the data-flow, e.g., whether the flow contains the whole object, or just a
 * part of it, for example a record (class/struct) member. In the latter case, the part can be
 * specified using the [Dataflow.partialTarget], which contains the partial target node.
 */
enum class GranularityType {
    FULL,
    PARTIAL
}

/**
 * This edge class defines a flow of data between [start] and [end]. The flow can have a certain
 * [granularity].
 */
@RelationshipEntity
class Dataflow(
    start: Node,
    end: Node,
    /** The granularity of this dataflow. Must be of type [GranularityType]. */
    val granularity: GranularityType = GranularityType.FULL,
    /**
     * If the granularity is [GranularityType.PARTIAL], this property can be used to store the
     * [Declaration] that is affected by the partial dataflow. Examples include a [FieldDeclaration]
     * for a [MemberExpression] or a [VariableDeclaration] for a [TupleDeclaration].
     */
    val partialTarget: Declaration? = null,
) : PropertyEdge<Node>(start, end) {
    override val label: String = "DFG"
}
