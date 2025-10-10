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
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents a conditional loop statement of the form: `while(...){...}`. The loop body is executed
 * until condition evaluates to false for the first time.
 */
class WhileStatement : LoopStatement(), BranchingNode, ArgumentHolder {
    @Relationship(value = "CONDITION_DECLARATION")
    var conditionDeclarationEdge = astOptionalEdgeOf<Declaration>()
    /** C++ allows defining a declaration instead of a pure logical expression as condition */
    var conditionDeclaration by unwrapping(WhileStatement::conditionDeclarationEdge)

    @Relationship(value = "CONDITION") var conditionEdge = astOptionalEdgeOf<Expression>()
    /** The condition that decides if the block is executed. */
    var condition by unwrapping(WhileStatement::conditionEdge)

    override val branchedBy
        get() = condition ?: conditionDeclaration

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("conditionDeclaration", conditionDeclaration)
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
        if (other !is WhileStatement) return false

        return super.equals(other) &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition
    }

    override fun hashCode() = Objects.hash(super.hashCode(), conditionDeclaration, condition)

    override fun startingPrevEOG(): Collection<Node> {
        val astChildren = this.allChildren<Node> { true }
        return condition?.startingPrevEOG()?.filter { it !in astChildren }
            ?: conditionDeclaration?.startingPrevEOG()
            ?: setOf()
    }

    override fun exitNextEOG(): Collection<Node> {
        return this.nextEOG.filter { it !in statement.allChildren<Node> { true } }
    }
}
