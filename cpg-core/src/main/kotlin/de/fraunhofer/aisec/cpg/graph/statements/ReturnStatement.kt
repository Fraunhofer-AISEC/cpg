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
import java.util.Objects

/** Represents a statement that returns out of the current function. */
class ReturnStatement : Statement(), ArgumentHolder {
    /** The expression whose value will be returned. */
    @AST var returnValues: MutableList<Expression> = mutableListOf()

    /**
     * A utility property to handle single-valued return statements. In case [returnValues] contains
     * a single [Expression], it is returned in the getter. The setter can be used to populate
     * [returnValues] with a single entry.
     */
    var returnValue: Expression?
        get() {
            return returnValues.singleOrNull()
        }
        set(value) {
            value?.let { returnValues = mutableListOf(it) }
        }

    override fun toString(): String {
        return "${this::class.simpleName}(returnValues=$returnValues,location=$location)"
    }

    override fun addArgument(expression: Expression) {
        this.returnValues += expression
    }

    override fun removeArgument(expression: Expression): Boolean {
        this.returnValues -= expression
        return true
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        this.returnValue = new
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReturnStatement) return false
        return super.equals(other) && returnValues == other.returnValues
    }

    override fun hashCode() = Objects.hash(super.hashCode(), returnValues)
}
