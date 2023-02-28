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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

// TODO: Document this class!
class DesignatedInitializerExpression : Expression() {
    @field:SubGraph("AST") var rhs: Expression? = null

    @Relationship(value = "LHS", direction = Relationship.Direction.OUTGOING)
    var lhsEdges: MutableList<PropertyEdge<Expression>> = mutableListOf()

    @property:SubGraph("AST")
    var lhs: List<Expression> by PropertyEdgeDelegate(DesignatedInitializerExpression::lhsEdges)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("lhr", lhsEdges)
            .append("rhs", rhs)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DesignatedInitializerExpression) return false
        return super.equals(other) &&
            rhs == other.rhs &&
            lhs == other.lhs &&
            lhsEdges == other.lhsEdges
    }

    override fun hashCode() = Objects.hash(super.hashCode(), rhs, lhs)
}
