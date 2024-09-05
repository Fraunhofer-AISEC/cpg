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
package de.fraunhofer.aisec.cpg.graph.statements

import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/** A [Statement] which represents a try/catch block, primarily used for exception handling. */
class TryStatement : Statement() {
    @Relationship(value = "RESOURCES", direction = Relationship.Direction.OUTGOING)
    var resourceEdges = astEdgesOf<Statement>()
    var resources by unwrapping(TryStatement::resourceEdges)

    @Relationship(value = "TRY_BLOCK") var tryBlockEdge = astOptionalEdgeOf<Block>()
    var tryBlock by unwrapping(TryStatement::tryBlockEdge)

    @Relationship(value = "FINALLY_BLOCK") var finallyBlockEdge = astOptionalEdgeOf<Block>()
    var finallyBlock by unwrapping(TryStatement::finallyBlockEdge)

    @Relationship(value = "CATCH_CLAUSES", direction = Relationship.Direction.OUTGOING)
    var catchClauseEdges = astEdgesOf<CatchClause>()
    var catchClauses by unwrapping(TryStatement::catchClauseEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TryStatement) return false
        return (super.equals(other) &&
            resources == other.resources &&
            propertyEqualsList(resourceEdges, other.resourceEdges) &&
            tryBlock == other.tryBlock &&
            finallyBlock == other.finallyBlock &&
            catchClauses == other.catchClauses &&
            propertyEqualsList(catchClauseEdges, other.catchClauseEdges))
    }

    override fun hashCode() =
        Objects.hash(super.hashCode(), resources, tryBlock, finallyBlock, catchClauses)
}
