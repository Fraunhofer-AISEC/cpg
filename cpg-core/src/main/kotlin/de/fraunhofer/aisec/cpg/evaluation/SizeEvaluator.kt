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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Simple evaluation of the size of an object. Right now, it can only support a statically given
 * size of arrays and strings.
 */
class SizeEvaluator : ValueEvaluator() {
    override val log: Logger
        get() = LoggerFactory.getLogger(SizeEvaluator::class.java)

    override fun evaluate(node: Any?): Any? {
        if (node is String) {
            return node.length
        }
        return evaluateInternal(node as? Node, 0)
    }

    override fun evaluateInternal(node: Node?, depth: Int): Any? {
        // Add the expression to the current path
        node?.let { this.path += it }

        return when (node) {
            is NewArray ->
                if (node.initializer != null) {
                    evaluateInternal(node.initializer, depth + 1)
                } else {
                    evaluateInternal(node.dimensions.firstOrNull(), depth + 1)
                }
            is Variable -> evaluateInternal(node.initializer, depth + 1)
            is Reference -> evaluateInternal(node.refersTo, depth + 1)
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> if (node.value is String) (node.value as String).length else node.value
            is Subscript -> evaluate(node.arrayExpression)
            else -> cannotEvaluate(node, this)
        }
    }
}
