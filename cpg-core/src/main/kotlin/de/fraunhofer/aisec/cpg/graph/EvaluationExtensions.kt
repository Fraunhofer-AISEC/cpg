/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.edges.get
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression

fun Node.evaluate(evaluator: ValueEvaluator = this.language.evaluator): Any? {
    return evaluator.evaluate(this)
}

val NewArrayExpression.capacity: Int
    get() {
        return dimensions.first().evaluate() as Int
    }

/**
 * A little helper function to find a [CallExpression]s argument [Node] by argument [name] or
 * argument [position]. The function prioritizes resolution by [name] over the [position].
 *
 * @param this The [CallExpression] to analyze.
 * @param name Optionally: the [CallExpression.arguments] name.
 * @param position Optionally: the [CallExpression.arguments] position.
 * @return The argument [Node] or `null` if not found.
 */
fun CallExpression.argumentByNameOrPosition(name: String? = null, position: Int? = null): Node? {
    return name?.let { this.argumentEdges[it]?.end }
        ?: position?.let { this.arguments.getOrNull(it) }
}

/**
 * A little helper function to find a [CallExpression]s argument first by [name] and if this fails
 * by [position]. The argument ist evaluated and the result is returned if it has the expected type
 * [T].
 *
 * @param this The [CallExpression] to analyze.
 * @param name Optionally: the [CallExpression.arguments] name.
 * @param position Optionally: the [CallExpression.arguments] position.
 * @return The evaluated result (of type [T]) or `null` on failure.
 */
inline fun <reified T> CallExpression.argumentValueByNameOrPosition(
    name: String? = null,
    position: Int? = null,
): T? {
    val arg = this.argumentByNameOrPosition(name, position) ?: return null
    val value = this.language.evaluator.evaluateAs<T>(arg)
    return value
}
