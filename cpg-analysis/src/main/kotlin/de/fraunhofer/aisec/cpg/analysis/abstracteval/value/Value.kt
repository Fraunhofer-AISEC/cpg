/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.abstracteval.value

import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference

/**
 * The [Value] interface is used by the AbstractEvaluator to store the behaviour of different
 * analysis targets. Each class implementing this interface is expected to define all operations
 * that might affect its internal value in [applyEffect]. When adding new classes remember to add
 * them to AbstractEvaluator.getType and add tests.
 */
interface Value<T> {
    /** Applies the effect of a Node to the interval containing its possible values. */
    fun applyEffect(
        current: T? = null,
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
        node: Node,
        name: String? = null,
        computeWithoutPush: Boolean = false,
    ): T

    companion object {
        fun getInitializer(node: Node?): Node? {
            return when (node) {
                null -> null
                is Reference -> getInitializer(node.refersTo)
                is VariableDeclaration -> node
                else -> getInitializer(node.prevDFG.firstOrNull())
            }
        }
    }
}
