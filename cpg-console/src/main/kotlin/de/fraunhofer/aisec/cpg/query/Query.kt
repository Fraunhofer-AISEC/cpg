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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.analysis.SizeEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.graph
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

@ExperimentalGraph
inline fun <reified T : Node> TranslationResult.all(
    sel: (T) -> Boolean,
    mustSatisfy: (T) -> Boolean
): Boolean {
    var nodes = this.graph.nodes.filterIsInstance<T>()

    // filter the nodes according to the selector
    nodes = nodes.filter(sel)

    return nodes.all(mustSatisfy)
}

fun sizeof(n: Node?): Int {
    val eval = SizeEvaluator()
    // TODO(oxisto): This cast could potentially go wrong, but if its not an int, its not really a
    // size
    return eval.evaluate(n) as? Int ?: -1
}

fun const(n: Int): QueryResult {
    return QueryResult(n)
}

class QueryResult(val inner: Any? = null) {
    operator fun compareTo(o: QueryResult): Int {
        // for now assume that its also an int (which is not always the case of course)
        return this.inner as Int - o.inner as Int
    }

    override fun equals(other: Any?): Boolean {
        if (other is Int && this.inner is Int) {
            return other == this.inner
        }

        if (other is QueryResult) {
            return this.inner?.equals(other.inner) ?: false
        }

        return super.equals(other)
    }
}

val Expression.size: QueryResult
    get() {
        return QueryResult(sizeof(this))
    }

val Expression.value: QueryResult
    get() {
        return QueryResult(evaluate())
    }

val Expression.intValue: Int?
    get() {
        return evaluate() as? Int
    }
