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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.descendants
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents a conditional loop statement of the form: `do{...}while(...)`. Where the body, usually
 * a [Block], is executed and re-executed if the [condition] evaluates to true.
 */
class DoStatement : LoopStatement(), ArgumentHolder {
    @Relationship("CONDITION") var conditionEdge = astOptionalEdgeOf<Expression>()
    /**
     * The loop condition that is evaluated after the loop statement and may trigger reevaluation.
     */
    var condition by unwrapping(DoStatement::conditionEdge)

    override fun toString() =
        ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .toString()

    override fun addArgument(expression: Expression) {
        this.condition = expression
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (condition == old) {
            this.condition = new
            return true
        }
        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return condition == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DoStatement) return false
        return super.equals(other) && condition == other.condition
    }

    override fun hashCode() = Objects.hash(super.hashCode(), condition)

    override fun getStartingPrevEOG(): Collection<Node> {
        return statement?.getStartingPrevEOG()?.filter { it != this }
            ?: condition?.getStartingPrevEOG()
            ?: this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        return this.nextEOG.filter { it !in statement.descendants<Node> { true } }
    }
}
