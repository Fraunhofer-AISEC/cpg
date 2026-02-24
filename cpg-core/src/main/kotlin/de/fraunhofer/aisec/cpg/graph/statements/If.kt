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

import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*
import kotlin.collections.ifEmpty
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents a condition control flow statement, usually indicating by `If`. */
class IfStatement : Statement(), BranchingNode, ArgumentHolder {
    @Relationship(value = "INITIALIZER_STATEMENT")
    var initializerStatementEdge = astOptionalEdgeOf<Statement>()
    /** C++ initializer statement. */
    var initializerStatement by unwrapping(IfStatement::initializerStatementEdge)

    @Relationship(value = "CONDITION_DECLARATION")
    var conditionDeclarationEdge = astOptionalEdgeOf<Declaration>()
    /** C++ alternative to the condition. */
    var conditionDeclaration by unwrapping(IfStatement::conditionDeclarationEdge)

    @Relationship(value = "CONDITION") var conditionEdge = astOptionalEdgeOf<Expression>()
    /** The condition to be evaluated. */
    var condition by unwrapping(IfStatement::conditionEdge)

    override val branchedBy
        get() = condition ?: conditionDeclaration

    /** C++ constexpr construct. */
    var isConstExpression = false

    @Relationship(value = "THEN_STATEMENT") var thenStatementEdge = astOptionalEdgeOf<Statement>()
    /** The statement that is executed, if the condition is evaluated as true. Usually a [Block]. */
    var thenStatement by unwrapping(IfStatement::thenStatementEdge)

    @Relationship(value = "ELSE_STATEMENT") var elseStatementEdge = astOptionalEdgeOf<Statement>()
    /**
     * The statement that is executed, if the condition is evaluated as false. Usually a [Block].
     */
    var elseStatement by unwrapping(IfStatement::elseStatementEdge)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("thenStatement", thenStatement)
            .append("elseStatement", elseStatement)
            .toString()
    }

    override fun addArgument(expression: Expression) {
        this.condition = expression
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        this.condition = new
        return true
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.condition == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IfStatement) return false
        return super.equals(other) &&
            isConstExpression == other.isConstExpression &&
            initializerStatement == other.initializerStatement &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition &&
            thenStatement == other.thenStatement &&
            elseStatement == other.elseStatement
    }

    override fun hashCode() =
        Objects.hash(
            super.hashCode(),
            isConstExpression,
            initializerStatement,
            conditionDeclaration,
            condition,
            thenStatement,
            elseStatement,
        )

    override fun getStartingPrevEOG(): Collection<Node> {
        return initializerStatement?.getStartingPrevEOG()
            ?: condition?.getStartingPrevEOG()
            ?: conditionDeclaration?.getStartingPrevEOG()
            ?: this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        return ((this.thenStatement?.getExitNextEOG() ?: setOf()) +
                (this.elseStatement?.getExitNextEOG() ?: this.nextEOG))
            .ifEmpty { this.nextEOG }
    }
}
