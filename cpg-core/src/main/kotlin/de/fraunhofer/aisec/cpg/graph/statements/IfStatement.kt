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

import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import org.apache.commons.lang3.builder.ToStringBuilder

/** Represents a condition control flow statement, usually indicating by `If`. */
class IfStatement : Statement() {
    /** C++ initializer statement. */
    @field:SubGraph("AST") var initializerStatement: Statement? = null

    /** C++ alternative to the condition. */
    @field:SubGraph("AST") var conditionDeclaration: Declaration? = null

    /** The condition to be evaluated. */
    @field:SubGraph("AST") var condition: Expression? = null

    /** C++ constexpr construct. */
    var isConstExpression = false

    /**
     * The statement that is executed, if the condition is evaluated as true. Usually a
     * [CompoundStatement].
     */
    @field:SubGraph("AST") var thenStatement: Statement? = null

    /**
     * The statement that is executed, if the condition is evaluated as false. Usually a
     * [CompoundStatement].
     */
    @field:SubGraph("AST") var elseStatement: Statement? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("thenStatement", thenStatement)
            .append("elseStatement", elseStatement)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IfStatement) return false
        return super.equals(other) &&
            isConstExpression == other.isConstExpression &&
            initializerStatement == other.initializerStatement &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition &&
            thenStatement == other.thenStatement &&
            elseStatement == other.elseStatement
    }

    override fun hashCode() = super.hashCode()
}
