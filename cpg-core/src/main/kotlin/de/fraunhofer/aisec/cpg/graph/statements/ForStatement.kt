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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*
import org.neo4j.ogm.annotation.Relationship

class ForStatement : Statement(), BranchingNode {
    @Relationship("STATEMENT") var statementEdge = astOptionalEdgeOf<Statement>()
    var statement by unwrapping(ForStatement::statementEdge)

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

    override val branchedBy: Node?
        get() = condition ?: conditionDeclaration

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ForStatement) {
            return false
        }

        return (super.equals(other) &&
            statement == other.statement &&
            initializerStatement == other.initializerStatement &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition &&
            iterationStatement == other.iterationStatement)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            this.statement,
            this.condition,
            this.initializerStatement,
            this.conditionDeclaration,
            this.iterationStatement
        )
    }
}
