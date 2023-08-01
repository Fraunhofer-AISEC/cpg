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
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import java.util.Objects

class ForEachStatement : Statement(), BranchingNode, StatementHolder {
    /**
     * This field contains the iteration variable of the loop. It can be either a new variable
     * declaration or a reference to an existing variable.
     */
    @AST
    var variable: Statement? = null
        set(value) {
            if (value is DeclaredReferenceExpression) {
                value.access = AccessValues.WRITE
            }
            field = value
        }

    /** This field contains the iteration subject of the loop. */
    @AST var iterable: Statement? = null

    /** This field contains the body of the loop. */
    @AST var statement: Statement? = null

    override val branchedBy: Node?
        get() = iterable

    override var statementEdges: MutableList<PropertyEdge<Statement>>
        get() {
            val statements = mutableListOf<PropertyEdge<Statement>>()
            variable?.let { statements.add(PropertyEdge(this, it)) }
            iterable?.let { statements.add(PropertyEdge(this, it)) }
            statement?.let { statements.add(PropertyEdge(this, it)) }
            return statements
        }
        set(value) {
            // Nothing to do here
        }

    override fun addStatement(s: Statement) {
        if (variable == null) {
            variable = s
        } else if (iterable == null) {
            iterable = s
        } else if (statement == null) {
            statement = s
        } else if (statement !is CompoundStatement) {
            val newStmt = newCompoundStatement()
            statement?.let { newStmt.addStatement(it) }
            newStmt.addStatement(s)
            statement = newStmt
        } else {
            (statement as? CompoundStatement)?.addStatement(s)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ForEachStatement) return false
        return super.equals(other) &&
            variable == other.variable &&
            iterable == other.iterable &&
            statement == other.statement
    }

    override fun hashCode() = Objects.hash(super.hashCode(), variable, iterable, statement)
}
