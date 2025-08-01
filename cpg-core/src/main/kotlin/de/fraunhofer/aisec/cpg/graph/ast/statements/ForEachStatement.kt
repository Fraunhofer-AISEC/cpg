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
package de.fraunhofer.aisec.cpg.graph.ast.statements

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.BranchingNode
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Represent a for statement of the form `for(variable ... iterable){...}` that executes the loop
 * body for each instance of an element in `iterable` that is temporarily stored in `variable`.
 */
class ForEachStatement : LoopStatement(), BranchingNode, StatementHolder {

    @Relationship("VARIABLE")
    var variableEdge =
        astOptionalEdgeOf<Statement>(
            onChanged = { _, new -> (new?.end as? Expression)?.access = AccessValues.WRITE }
        )

    /**
     * This field contains the iteration variable of the loop. It can be either a new variable
     * declaration or a reference to an existing variable.
     */
    var variable by unwrapping(ForEachStatement::variableEdge)

    @Relationship("ITERABLE") var iterableEdge = astOptionalEdgeOf<Statement>()
    /** This field contains the iteration subject of the loop. */
    var iterable by unwrapping(ForEachStatement::iterableEdge)

    override val branchedBy
        get() = iterable

    override var statementEdges: AstEdges<Statement, AstEdge<Statement>>
        get() {
            val statements = astEdgesOf<Statement>()
            statements += variableEdge
            statements += iterableEdge
            statements += statementEdge
            statements += elseStatementEdge
            return statements
        }
        set(_) {
            // Nothing to do here
        }

    override var statements: MutableList<Statement>
        get() = unwrapping(ForEachStatement::statementEdges)
        set(value) {}

    override fun toString() =
        ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("variable", variable)
            .append("iterable", iterable)
            .toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ForEachStatement) return false
        return super.equals(other) && variable == other.variable && iterable == other.iterable
    }

    override fun hashCode() = Objects.hash(super.hashCode(), variable, iterable)

    override fun getStartingPrevEOG(): Collection<Node> {
        val astChildren = this.allChildren<Node> { true }
        return iterable?.getStartingPrevEOG()?.filter { it !in astChildren }
            ?: variable?.getStartingPrevEOG()
            ?: this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        return this.nextEOG.filter { it !in statement.allChildren<Node> { true } }
    }
}
