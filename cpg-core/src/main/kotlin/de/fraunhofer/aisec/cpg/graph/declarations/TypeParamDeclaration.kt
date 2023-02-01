/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.HasDefault
import de.fraunhofer.aisec.cpg.graph.HasType.SecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/** A declaration of a type template parameter */
class TypeParamDeclaration : ValueDeclaration(), SecondaryTypeEdge, HasDefault<Type?> {
    /**
     * TemplateParameters can define a default for the type parameter Since the primary type edge
     * points to the ParameterizedType, the default edge is a secondary type edge. Therefore, the
     * TypeResolver requires to implement the [HasType.SecondaryTypeEdge] to be aware of the edge to
     * be able to merge the type nodes.
     */
    @Relationship(value = "DEFAULT", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    override var default: Type? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as TypeParamDeclaration
        return super.equals(that) && default == that.default
    }

    override fun hashCode() = Objects.hash(super.hashCode(), default)

    override fun updateType(typeState: Collection<Type>) {
        val oldType = default
        if (oldType != null) {
            for (t in typeState) {
                if (t == oldType) {
                    default = t
                }
            }
        }
    }
}
