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
package de.fraunhofer.aisec.cpg.graph.statements

import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * This [Node] is a generalization of all looping statements and serves duplication reduction. All
 * Looping statements can be identified by if they inherit from this class. Loops deviate from other
 * nods in the way they change a programs control flow, and do so in combination with other nodes,
 * e.g. [BreakStatement].
 *
 * The looping criterion can be a condition or the iteration over all elements in a list and is
 * defined by the subclass.
 */
abstract class LoopStatement : Statement() {

    @Relationship("STATEMENT") var statementEdge = astOptionalEdgeOf<Statement>()
    /** This field contains the body of the loop, e.g. a [Block] or single [Statement]. */
    var statement by unwrapping(LoopStatement::statementEdge)

    /**
     * This represents a single or block of statements that are executed when the loop terminates
     * regularly, e.g. the loop finishes iterating over all elements or the loop-condition evaluates
     * to false. The exact situation when this is happening depends on the language that supports an
     * `else`-Statement at loop level. E.g. in Python the [elseStatement] is executed when the loop
     * was not left through a break.
     */
    @Relationship(value = "ELSE_STATEMENT") var elseStatementEdge = astOptionalEdgeOf<Statement>()
    var elseStatement by unwrapping(LoopStatement::elseStatementEdge)

    override fun toString() =
        ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("statement", statement)
            .append("else", elseStatement)
            .toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is LoopStatement) {
            return false
        }

        return (super.equals(other) &&
            statement == other.statement &&
            elseStatement == other.elseStatement)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), statement, elseStatement)
}
