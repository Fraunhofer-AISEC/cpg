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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/** Represents a conditional loop statement of the form: `while(...){...}`. */
class WhileStatement : Statement(), BranchingNode, ArgumentHolder {
    /** C++ allows defining a declaration instead of a pure logical expression as condition */
    @AST var conditionDeclaration: Declaration? = null

    /** The condition that decides if the block is executed. */
    @AST var condition: Expression? = null

    /**
     * The statement that is going to be executed, until the condition evaluates to false for the
     * first time. Usually a [Block].
     */
    @AST var statement: Statement? = null

    override val branchedBy: Node?
        get() = condition ?: conditionDeclaration

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("statement", statement)
            .toString()
    }

    override fun addArgument(expression: Expression) {
        this.condition = expression
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        this.condition = new
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WhileStatement) return false

        return super.equals(other) &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition &&
            statement == other.statement
    }

    override fun hashCode() =
        Objects.hash(super.hashCode(), conditionDeclaration, condition, statement)
}
