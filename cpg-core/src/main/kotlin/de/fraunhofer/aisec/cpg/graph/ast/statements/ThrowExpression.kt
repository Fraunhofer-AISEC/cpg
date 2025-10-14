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
package de.fraunhofer.aisec.cpg.graph.ast.statements

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents a `throw` or `raise` statement/expression. */
class ThrowExpression : Expression(), ArgumentHolder {

    /** The exception object to be raised. */
    @Relationship(value = "EXCEPTION") var exceptionEdge = astOptionalEdgeOf<Expression>()
    var exception by unwrapping(ThrowExpression::exceptionEdge)

    /**
     * Some languages (Python) can add a parent exception (or `cause`) to indicate that an exception
     * was raised while handling another exception.
     */
    @Relationship(value = "PARENT_EXCEPTION")
    var parentExceptionEdge = astOptionalEdgeOf<Expression>()
    var parentException by unwrapping(ThrowExpression::parentExceptionEdge)

    override fun addArgument(expression: Expression) {
        when {
            exception == null -> exception = expression
            parentException == null -> parentException = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        return when {
            exception == old -> {
                exception = new
                true
            }
            parentException == old -> {
                parentException = new
                true
            }
            else -> false
        }
    }

    override fun hasArgument(expression: Expression): Boolean {
        return exception == expression || parentException == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThrowExpression) return false
        return super.equals(other) &&
            exception == other.exception &&
            parentException == other.parentException
    }

    override fun hashCode() = Objects.hash(super.hashCode(), exception, parentException)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("exception", exception)
            .append("parentException", parentException)
            .toString()
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.exception?.getStartingPrevEOG()
            ?: this.parentException?.getStartingPrevEOG()
            ?: this.prevEOG
    }
}
