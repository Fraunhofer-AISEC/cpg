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
package de.fraunhofer.aisec.cpg.graph.statements

import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/** Represents a `throw` or `raise` statement. */
class RaiseStatement : Statement(), ArgumentHolder {

    /** The exception object to be raised. */
    @Relationship(value = "EXCEPTION") var exceptionEdge = astOptionalEdgeOf<Expression>()
    var exception by unwrapping(RaiseStatement::exceptionEdge)

    /**
     * Some languages (Python) can add a cause to indicate that an exception was raised while
     * handling another exception. This is stored in the graph, but has no further implications like
     * EOG or DFG connections, as it is only of informational purpose, but it doesn't change the
     * program behavior.
     */
    @Relationship(value = "CAUSE") var causeEdge = astOptionalEdgeOf<Expression>()
    var cause by unwrapping(RaiseStatement::causeEdge)

    override fun addArgument(expression: Expression) {
        when {
            exception == null -> exception = expression
            cause == null -> cause = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        return when {
            exception == old -> {
                exception = new
                true
            }
            cause == old -> {
                cause = new
                true
            }
            else -> false
        }
    }

    override fun hasArgument(expression: Expression): Boolean {
        return when {
            exception == expression -> true
            cause == expression -> true
            else -> false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RaiseStatement) return false
        return super.equals(other) && exception == other.exception && cause == other.cause
    }

    override fun hashCode() = Objects.hash(super.hashCode(), exception, cause)
}
