/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.statements.ReturnStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOp
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/**
 * This interfaces denotes that [Node] can accept arguments. The most famous example would be a
 * [CallExpr] to populate [CallExpr.arguments] or the [ReturnStmt.returnValue] of a return
 * statement.
 *
 * We do have some use-cases where we are a little "relaxed" about what is an argument. For example,
 * we also consider the [BinaryOp.lhs] and [BinaryOp.rhs] of a binary operator as arguments, so we
 * can use node builders in the Node Fluent DSL.
 */
interface ArgumentHolder : Holder<Expression> {

    /** Adds the [expression] to the list of arguments. */
    fun addArgument(expression: Expression)

    /**
     * Removes the [expression] from the list of arguments.
     *
     * An indication whether this operation was successful needs to be returned.
     */
    fun removeArgument(expression: Expression): Boolean {
        return false
    }

    /**
     * Replaces the existing argument specified in [old] with the one in [new]. Implementation how
     * to do that might be specific to the argument holder.
     *
     * An indication whether this operation was successful needs to be returned.
     */
    fun replaceArgument(old: Expression, new: Expression): Boolean

    override operator fun plusAssign(node: Expression) {
        addArgument(node)
    }

    operator fun minusAssign(node: Expression) {
        removeArgument(node)
    }
}
