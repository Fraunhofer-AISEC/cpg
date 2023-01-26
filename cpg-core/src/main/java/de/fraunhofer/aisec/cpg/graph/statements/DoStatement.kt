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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import org.apache.commons.lang3.builder.ToStringBuilder

/** Represents a conditional loop statement of the form: `do{...}while(...)`. */
class DoStatement : Statement() {
    /**
     * The loop condition that is evaluated after the loop statement and may trigger reevaluation.
     */
    @field:SubGraph("AST") var condition: Expression? = null

    /**
     * The statement that is going to be executed and reexecuted, until the condition evaluates to
     * false for the first time. Usually a [CompoundStatement].
     */
    @field:SubGraph("AST") var statement: Statement? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("statement", statement)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DoStatement) {
            return false
        }

        return super.equals(other) && condition == other.condition && statement == other.statement
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
