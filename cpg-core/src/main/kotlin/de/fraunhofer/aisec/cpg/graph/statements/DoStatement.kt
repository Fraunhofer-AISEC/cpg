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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*

/** Represents a conditional loop statement of the form: `do{...}while(...)`. */
class DoStatement : Statement(), ArgumentHolder {
    /**
     * The loop condition that is evaluated after the loop statement and may trigger reevaluation.
     */
    @AST var condition: Expression? = null

    /**
     * The statement that is going to be executed and re-executed, until the condition evaluates to
     * false for the first time. Usually a [Block].
     */
    @AST var statement: Statement? = null

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DoStatement) return false
        return super.equals(other) && condition == other.condition && statement == other.statement
    }

    override fun hashCode() = Objects.hash(super.hashCode(), condition, statement)
}
