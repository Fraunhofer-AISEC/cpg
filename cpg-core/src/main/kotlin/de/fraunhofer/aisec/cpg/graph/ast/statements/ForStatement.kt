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
import de.fraunhofer.aisec.cpg.graph.ast.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents an iterating loop statement of the form `for(initializer; condition; iteration){...}`
 * that declares variables, can change them in an iteration statement and is executed until the
 * condition evaluates to false.
 */
class ForStatement : LoopStatement(), BranchingNode, StatementHolder {

    @Relationship("INITIALIZER_STATEMENT")
    var initializerStatementEdge = astOptionalEdgeOf<Statement>()
    var initializerStatement by unwrapping(ForStatement::initializerStatementEdge)

    @Relationship("CONDITION_DECLARATION")
    var conditionDeclarationEdge = astOptionalEdgeOf<Declaration>()
    var conditionDeclaration by unwrapping(ForStatement::conditionDeclarationEdge)

    @Relationship("CONDITION") var conditionEdge = astOptionalEdgeOf<Expression>()
    var condition by unwrapping(ForStatement::conditionEdge)

    @Relationship("ITERATION_STATEMENT") var iterationStatementEdge = astOptionalEdgeOf<Statement>()
    var iterationStatement by unwrapping(ForStatement::iterationStatementEdge)

    override val branchedBy
        get() = condition ?: conditionDeclaration

    override var statementEdges: AstEdges<Statement, AstEdge<Statement>>
        get() {
            val statements = astEdgesOf<Statement>()
            statements += initializerStatementEdge
            statements += iterationStatementEdge
            statements += statementEdge
            statements += elseStatementEdge
            return statements
        }
        set(_) {
            // Nothing to do here
        }

    override var statements: MutableList<Statement>
        get() = unwrapping(ForStatement::statementEdges)
        set(value) {}

    override fun toString() =
        ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("initializer", initializerStatement)
            .append("condition", condition)
            .append("conditionDeclaration", conditionDeclaration)
            .append("iteration", iterationStatement)
            .toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ForStatement) {
            return false
        }

        return super.equals(other) &&
            initializerStatement == other.initializerStatement &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition &&
            iterationStatement == other.iterationStatement
    }

    override fun hashCode(): Int {
        return Objects.hash(
            this.condition,
            this.initializerStatement,
            this.conditionDeclaration,
            this.iterationStatement,
        )
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        val astChildren = this.allChildren<Node> { true }
        return initializerStatement?.getStartingPrevEOG()
            ?: this.condition?.getStartingPrevEOG()?.filter { it !in astChildren }
            ?: this.conditionDeclaration?.getStartingPrevEOG()?.filter { it !in astChildren }
            ?: this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        return this.nextEOG.filter { it !in statement.allChildren<Node> { true } }
    }
}
