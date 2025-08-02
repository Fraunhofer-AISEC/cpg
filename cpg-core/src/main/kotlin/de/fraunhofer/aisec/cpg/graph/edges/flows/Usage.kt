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

import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList
import org.neo4j.ogm.annotation.RelationshipEntity

/** This edge class denotes the usage of a [ValueDeclaration] in a [Reference]. */
@RelationshipEntity
class Usage(
    start: Node,
    end: Reference,
    /** The type of access (read/write/readwrite) of this usage. */
    var access: AccessValues? = null,
) : Edge<Reference>(start, end) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Usage) return false
        return this.access == other.access && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + access.hashCode()
        return result
    }

    override var labels = setOf("USAGE")
}

/** A container for [Usage] edges. [NodeType] is necessary because of the Neo4J OGM. */
class Usages<NodeType : Reference>(thisRef: ValueDeclaration) :
    EdgeList<Reference, Usage>(thisRef = thisRef, init = ::Usage) {
    override fun handleOnAdd(edge: Usage) {
        edge.access = edge.end.access
    }
}
