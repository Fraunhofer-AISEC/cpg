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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import java.util.Objects
import kotlin.collections.plusAssign
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** This class holds the variable, iterable and predicate of the [CollectionComprehension]. */
class ComprehensionExpression : Expression(), ArgumentHolder {
    @Relationship("VARIABLE")
    var variableEdge =
        astOptionalEdgeOf<Statement>(
            onChanged = { _, new ->
                val end = new?.end
                if (end is Reference) {
                    end.access = AccessValues.WRITE
                }
            }
        )

    /**
     * This field contains the iteration variable of the comprehension. It can be either a new
     * variable declaration or a reference (probably to a new variable).
     */
    var variable by unwrapping(ComprehensionExpression::variableEdge)

    @Relationship("ITERABLE") var iterableEdge = astOptionalEdgeOf<Statement>()

    /** This field contains the iteration subject of the loop. */
    var iterable by unwrapping(ComprehensionExpression::iterableEdge)

    @Relationship("PREDICATE") var predicateEdges = astEdgesOf<Statement>()

    /**
     * This field contains the predicates which have to hold to evaluate `statement(variable)` and
     * include it in the result.
     */
    var predicates by unwrapping(ComprehensionExpression::predicateEdges)

    override fun toString() =
        ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("variable", variable)
            .append("iterable", iterable)
            .append("predicate", predicates)
            .toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComprehensionExpression) return false
        return super.equals(other) &&
            variable == other.variable &&
            iterable == other.iterable &&
            predicates == other.predicates
    }

    override fun hashCode() = Objects.hash(super.hashCode(), variable, iterable, predicates)

    override fun addArgument(expression: Expression) {
        if (this.variable == null) {
            this.variable = expression
        } else if (this.iterable == null) {
            this.iterable = expression
        } else {
            this.predicates += expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (this.variable == old) {
            this.variable = new
            return true
        }

        if (this.iterable == old) {
            this.iterable = new
            return true
        }

        var changedSomething = false
        val newPredicates =
            this.predicates.map {
                if (it == old) {
                    changedSomething = true
                    new
                } else it
            }
        this.predicates.clear()
        this.predicates.addAll(newPredicates)
        return changedSomething
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.variable == expression ||
            this.iterable == expression ||
            expression in this.predicates
    }
}
