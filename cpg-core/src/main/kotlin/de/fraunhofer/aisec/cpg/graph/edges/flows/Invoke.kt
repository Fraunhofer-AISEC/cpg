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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Call
import kotlin.reflect.KProperty
import org.neo4j.ogm.annotation.RelationshipEntity

/** This edge class denotes the invocation of a [Function] by a [Call]. */
@RelationshipEntity
class Invoke(
    start: Node,
    end: Function,
    /**
     * True, if this is a "dynamic" invoke, meaning that the call will be resolved during runtime,
     * not during compile-time.
     */
    var dynamicInvoke: Boolean = false,
) : Edge<Function>(start, end) {
    override var labels = setOf("INVOKES")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Invoke) return false
        return this.dynamicInvoke == other.dynamicInvoke && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + dynamicInvoke.hashCode()
        return result
    }
}

/** A container for [Usage] edges. [NodeType] is necessary because of the Neo4J OGM. */
class Invokes<NodeType : Function>(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<Invoke>>,
    outgoing: Boolean = true,
) :
    EdgeList<Function, Invoke>(thisRef = thisRef, init = ::Invoke, outgoing = outgoing),
    MirroredEdgeCollection<Function, Invoke> {
    override fun handleOnAdd(edge: Invoke) {
        super<MirroredEdgeCollection>.handleOnAdd(edge)

        // TODO: Make thisRef generic :(
        if (outgoing) {
            edge.end.registerTypeObserver(thisRef as Call)
        }
    }
}
