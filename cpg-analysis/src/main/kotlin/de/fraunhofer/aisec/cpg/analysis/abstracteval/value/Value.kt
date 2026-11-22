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
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice

/**
 * The [Value] interface is used by the AbstractEvaluator to store the behaviour of different
 * analysis targets. Each class implementing this interface is expected to define all operations
 * that might affect its internal value in [applyEffect]. When adding new classes remember to add
 * them to AbstractEvaluator.getType and add tests.
 *
 * [E] is the inner [Lattice.Element] type stored by the [TupleState]/[TupleStateElement] this value
 * is evaluated against (e.g.
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.NewIntervalLattice.Element], a wrapper around
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval]). [T] is the logical value type
 * [applyEffect] computes and returns. These two are deliberately independent type parameters: some
 * domains need a mutable wrapper class as their [Lattice.Element] ([E]) while exposing an immutable
 * value type ([T]) to callers, whereas domains whose value type already implements
 * [Lattice.Element] directly (e.g. [de.fraunhofer.aisec.cpg.analysis.string.StringPattern], see
 * design decision D8 in `docs/docs/CPG/impl/string-analysis.md`) simply instantiate `E == T` with
 * no wrapper at all.
 */
interface Value<E : Lattice.Element, T> {
    /** Applies the effect of a Node to the interval containing its possible values. */
    fun applyEffect(
        lattice: TupleState<Any, E>,
        state: TupleStateElement<Any, E>,
        node: Node,
        edge: EvaluationOrder? = null,
        computeWithoutPush: Boolean = false,
    ): T

    companion object {
        fun getInitializer(node: Node?): Node? {
            return when (node) {
                null -> null
                is Reference -> getInitializer(node.refersTo)
                is Variable -> node
                else -> getInitializer(node.prevDFG.firstOrNull())
            }
        }
    }
}
