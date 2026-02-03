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

import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.Objects
import kotlin.collections.plus
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents a Java or C++ switch statement of the `switch (selector) {...}` that can include case
 * and default statements. Break statements break out of the switch and labeled breaks in Java are
 * handled properly.
 */
class SwitchStatement : Statement(), BranchingNode {
    @Relationship(value = "SELECTOR") var selectorEdge = astOptionalEdgeOf<Expression>()
    /** Selector that determines the case/default statement of the subsequent execution */
    var selector by unwrapping(SwitchStatement::selectorEdge)

    @Relationship(value = "INITIALIZER_STATEMENT")
    var initializerStatementEdge = astOptionalEdgeOf<Statement>()
    /** C++ can have an initializer statement in a switch */
    var initializerStatement by unwrapping(SwitchStatement::initializerStatementEdge)

    @Relationship(value = "SELECTOR_DECLARATION")
    var selectorDeclarationEdge = astOptionalEdgeOf<Declaration>()
    /** C++ allows to use a declaration instead of an expression as selector */
    var selectorDeclaration by unwrapping(SwitchStatement::selectorDeclarationEdge)

    @Relationship(value = "STATEMENT") var statementEdge = astOptionalEdgeOf<Statement>()
    /**
     * The compound statement that contains break/default statements with regular statements on the
     * same hierarchy
     */
    var statement by unwrapping(SwitchStatement::statementEdge)

    override val branchedBy
        get() = selector

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwitchStatement) return false
        return super.equals(other) &&
            initializerStatement == other.initializerStatement &&
            selectorDeclaration == other.selectorDeclaration &&
            selector == other.selector &&
            statement == other.statement
    }

    override fun hashCode() =
        Objects.hash(
            super.hashCode(),
            initializerStatement,
            selectorDeclaration,
            selector,
            statement,
        )

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.initializerStatement?.getStartingPrevEOG()
            ?: this.selector?.getStartingPrevEOG()
            ?: this.selectorDeclaration?.getStartingPrevEOG()
            ?: this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        return this.statement?.getExitNextEOG() ?: this.nextEOG
    }
}
