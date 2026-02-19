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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/** A [Statement] which represents a try/catch block, primarily used for exception handling. */
class TryStatement : Statement() {
    /**
     * This represents some kind of resource which is typically opened (or similar) while entering
     * the [tryBlock]. If this operation fails, we may continue with the [finallyBlock]. However,
     * there is no exception raised but if an exception occurs while opening the resource, we won't
     * enter the [tryBlock].
     */
    @Relationship(value = "RESOURCES", direction = Relationship.Direction.OUTGOING)
    var resourceEdges = astEdgesOf<Statement>()
    var resources by unwrapping(TryStatement::resourceEdges)

    /**
     * This represents a block whose statements can throw exceptions which are handled by the
     * [catchClauses].
     */
    @Relationship(value = "TRY_BLOCK") var tryBlockEdge = astOptionalEdgeOf<Block>()
    var tryBlock by unwrapping(TryStatement::tryBlockEdge)

    /**
     * This represents a block whose statements are only executed if the [tryBlock] finished without
     * exceptions. Note that any exception thrown in this block is no longer caught by the
     * [catchClauses].
     */
    @Relationship(value = "ELSE_BLOCK") var elseBlockEdge = astOptionalEdgeOf<Block>()
    var elseBlock by unwrapping(TryStatement::elseBlockEdge)

    /**
     * This represents a block of statements which is always executed after finishing the [tryBlock]
     * or one of the [catchClauses]. Note that any exception thrown in this block is no longer
     * caught by the [catchClauses].
     */
    @Relationship(value = "FINALLY_BLOCK") var finallyBlockEdge = astOptionalEdgeOf<Block>()
    var finallyBlock by unwrapping(TryStatement::finallyBlockEdge)

    /**
     * This represents a set of blocks whose statements handle the exceptions which are thrown in
     * the [tryBlock]. There can be multiple catch clauses, but it is also possible that none
     * exists.
     */
    @Relationship(value = "CATCH_CLAUSES", direction = Relationship.Direction.OUTGOING)
    var catchClauseEdges = astEdgesOf<CatchClause>()
    var catchClauses by unwrapping(TryStatement::catchClauseEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TryStatement) return false
        return (super.equals(other) &&
            resourceEdges == other.resourceEdges &&
            tryBlock == other.tryBlock &&
            finallyBlock == other.finallyBlock &&
            catchClauseEdges == other.catchClauseEdges &&
            elseBlock == other.elseBlock)
    }

    override fun hashCode() =
        Objects.hash(super.hashCode(), resources, tryBlock, finallyBlock, catchClauses, elseBlock)

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.resources.firstOrNull()?.getStartingPrevEOG()
            ?: this.tryBlock?.getStartingPrevEOG()
            ?: this.finallyBlock?.getStartingPrevEOG()
            ?: this.prevEOG
    }
}
