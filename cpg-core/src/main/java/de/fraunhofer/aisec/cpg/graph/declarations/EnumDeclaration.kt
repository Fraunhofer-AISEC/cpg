/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.transformIntoOutgoingPropertyEdgeList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

class EnumDeclaration : Declaration() {
    @Relationship(value = "ENTRIES", direction = Relationship.Direction.OUTGOING)
    var entryEdges: List<PropertyEdge<EnumConstantDeclaration>> = ArrayList()
        private set

    @Relationship(value = "SUPER_TYPES", direction = Relationship.Direction.OUTGOING)
    var superTypeEdges: List<PropertyEdge<Type>> = ArrayList()
        private set

    @Relationship var superTypeDeclarations: Set<RecordDeclaration> = HashSet()

    @property:SubGraph("AST")
    var entries: List<EnumConstantDeclaration>
        get() = unwrap(entryEdges)
        set(value) {
            entryEdges = transformIntoOutgoingPropertyEdgeList(value, this)
        }

    var superTypes: List<Type>
        get() = unwrap(superTypeEdges)
        set(value) {
            superTypeEdges = transformIntoOutgoingPropertyEdgeList(value, this)
        }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("entries", entryEdges)
            .toString()
    }
}
