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

import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal

interface ArgumentHolder {
    /**
     * Creates a new binary operator with operator code + out of two expressions and assigns them to
     * this expression as an argument.
     */
    operator fun Expression.plus(rhs: Expression): BinaryOperator {
        return binOp("+") {
            this.lhs = this@plus
            this.rhs = rhs
        }
    }

    /**
     * Creates a new binary operator with operator code - out of two expressions and assigns them to
     * this expression as an argument.
     */
    operator fun Expression.minus(rhs: Expression): BinaryOperator {
        return binOp("-") {
            this.lhs = this@minus
            this.rhs = rhs
        }
    }

    fun addArgument(expression: Expression)
}

/** Creates a new reference and assigns it to the [ArgumentHolder]. */
fun ArgumentHolder.ref(
    to: String,
    init: (DeclaredReferenceExpression.() -> Unit)? = null
): DeclaredReferenceExpression {
    val node = new(to, parent = this as? Node, init = init)

    // this.addArgument(node)

    return node
}

/** Creates a new literal and assigns it to the [ArgumentHolder]. */
fun <T> ArgumentHolder.literal(value: T, init: (Literal<T>.() -> Unit)? = null): Literal<T> {
    val node = new(parent = this as? Node, init = init)
    node.value = value

    this.addArgument(node)

    return node
}

/** Creates a new binary operator and assigns it to the [ArgumentHolder]. */
fun ArgumentHolder.binOp(operatorCode: String, init: BinaryOperator.() -> Unit): BinaryOperator {
    val node = new(parent = this as? Node, init = init)
    node.operatorCode = operatorCode

    this.addArgument(node)

    // a little bit hacky but works for now
    node.lhs?.parent = node
    node.rhs?.parent = node

    return node
}
