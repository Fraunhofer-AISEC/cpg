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
package de.fraunhofer.aisec.cpg.graph.ast.statements.expressions

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.ast.statements.Statement
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Represent a list/set/map comprehension or similar expression. It contains four major components:
 * The statement, the variable, the iterable and a predicate which are combined to something like
 * `[statement(variable) : variable in iterable if predicate(variable)]`.
 *
 * Some languages provide a way to have multiple variables, iterables and predicates. For this
 * reason, we represent the `variable, iterable and predicate in its own class
 * [ComprehensionExpression].
 */
class CollectionComprehension : Expression(), ArgumentHolder {

    @Relationship("COMPREHENSION_EXPRESSIONS")
    var comprehensionExpressionEdges = astEdgesOf<ComprehensionExpression>()
    /**
     * This field contains one or multiple [ComprehensionExpression]s.
     *
     * Note: Instead of having a list here, we could also enforce that the frontend nests the
     * expressions in a meaningful way (in particular this would help us to satisfy dependencies
     * between the comprehensions' variables).
     */
    var comprehensionExpressions by
        unwrapping(CollectionComprehension::comprehensionExpressionEdges)

    @Relationship("STATEMENT")
    var statementEdge =
        astEdgeOf<Statement>(
            ProblemExpression("No statement provided but is required in ${this::class}")
        )
    /**
     * This field contains the statement which is applied to each element of the input for which the
     * predicate returned `true`.
     */
    var statement by unwrapping(CollectionComprehension::statementEdge)

    override fun toString() =
        ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("statement", statement)
            .append("comprehensions", comprehensionExpressions)
            .toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CollectionComprehension) return false
        return super.equals(other) &&
            statement == other.statement &&
            comprehensionExpressions == other.comprehensionExpressions
    }

    override fun hashCode() = Objects.hash(super.hashCode(), statement, comprehensionExpressions)

    override fun addArgument(expression: Expression) {
        if (this.statement is ProblemExpression) {
            this.statement = expression
        } else if (expression is ComprehensionExpression) {
            this.comprehensionExpressions += expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (this.statement == old) {
            this.statement = new
            return true
        }
        if (new !is ComprehensionExpression) return false
        var changedSomething = false
        val newCompExp =
            this.comprehensionExpressions.map {
                if (it == old) {
                    changedSomething = true
                    new
                } else it
            }
        this.comprehensionExpressions.clear()
        this.comprehensionExpressions.addAll(newCompExp)
        return changedSomething
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.statement == expression || expression in this.comprehensionExpressions
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        val allChildren = this.allChildren<Node> { true }
        return comprehensionExpressions.firstOrNull()?.getStartingPrevEOG()?.filter {
            it !in allChildren
        } ?: setOf()
    }
}
