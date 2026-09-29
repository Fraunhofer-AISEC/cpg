/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.Value
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [Value] implementation for the flow-sensitive [AbstractStringEvaluator] (Phase 5, see
 * `docs/docs/CPG/impl/string-analysis.md`). Tracks how a local string variable's [StringPattern]
 * evolves across the EOG, mirroring
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.value.IntegerValue]'s `applyEffect` structure -
 * with `E == T == StringPattern` (design decision D8: [StringPattern] is its own
 * [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.Element], no wrapper needed).
 *
 * Handles, at minimum:
 * - [Literal] initialization (`Const`, or `Bottom` for a `null` literal, mirroring
 *   [StringEvaluator.handleLiteral]).
 * - [Variable] (re-)declaration, seeding the declaration state from the initializer.
 * - [BinaryOperator] `+`/`+=`, i.e. concatenation, via the domain's [concat] smart constructor.
 * - [Assign], including compound `+=` assignment.
 * - Plain reference reads, via the shared fallback at the end of [applyEffect] (mirrors
 *   [de.fraunhofer.aisec.cpg.analysis.abstracteval.value.IntegerValue] not special-casing
 *   `Reference` either: [de.fraunhofer.aisec.cpg.passes.objectIdentifier] already resolves a
 *   `Reference` to the underlying variable's declaration-state key).
 */
class StringValue : Value<StringPattern, StringPattern> {
    companion object {
        val log: Logger = LoggerFactory.getLogger(StringValue::class.java)
    }

    override fun applyEffect(
        lattice: TupleState<Any, StringPattern>,
        state: TupleStateElement<Any, StringPattern>,
        node: Node,
        edge: EvaluationOrder?,
        computeWithoutPush: Boolean,
    ): StringPattern {
        if (node is Literal<*>) {
            val value = node.value
            val pattern = if (value == null) StringPattern.Bottom else const(value.toString())
            if (!computeWithoutPush) {
                lattice.pushToDeclarationState(state, node, pattern)
                lattice.pushToGeneralState(state, node, pattern)
            }
            return pattern
        } // (Re-)Declarations of the Variable
        else if (node is Variable) {
            val initializerValue =
                node.initializer?.let {
                    this.applyEffect(lattice, state, it, null, computeWithoutPush = true)
                } ?: StringPattern.Unknown()
            lattice.pushToDeclarationState(state, node, initializerValue)
            lattice.pushToGeneralState(state, node, initializerValue)
            return initializerValue
        } // Concatenation
        else if (node is BinaryOperator) {
            val newValue =
                when (node.operatorCode) {
                    "+",
                    "+=" -> {
                        val lhsValue = state.patternOf(node.lhs)
                        val rhsValue = state.patternOf(node.rhs)
                        concat(lhsValue, rhsValue)
                    }
                    else -> {
                        log.info("Unsupported binary operator: ${node.operatorCode}")
                        StringPattern.Unknown(origin = node)
                    }
                }
            lattice.pushToGeneralState(state, node, newValue)
            lattice.pushToDeclarationState(state, node, newValue)
            return newValue
        } // Assignments and combined assign expressions
        else if (node is Assign) {
            if (node.lhs.size == 1 && node.rhs.size == 1) {
                // The lhs and rhs must already have been evaluated before reaching the operator.
                // This should be guaranteed by the evaluation order graph.
                val rhsValue = state.patternOf(node.rhs[0])
                val lhsValue = state.patternOf(node.lhs[0])
                val newValue =
                    when (node.operatorCode) {
                        "=" -> rhsValue
                        "+=" -> concat(lhsValue, rhsValue)
                        else -> {
                            log.info("Unsupported assignment operator: ${node.operatorCode}")
                            StringPattern.Unknown(origin = node)
                        }
                    }
                // Push the new value to the declaration state of the variable. Overwrites rather
                // than joins, mirroring IntegerValue.changeDeclarationState: a plain assignment
                // replaces the previous value on this path, it does not merge with it.
                lattice.changeDeclarationState(state, node.lhs.first(), newValue)
                return newValue
            } else {
                log.info(
                    "Unsupported: multiple lhs or rhs (${node.lhs.size} / ${node.rhs.size}) in " +
                        "Assign node $node"
                )
            }
        }

        lattice.pushToGeneralState(state, node, state.patternOf(node))
        return state.patternOf(node)
    }
}
