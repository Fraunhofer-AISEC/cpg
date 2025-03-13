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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.HasOperatorCode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.translationUnit
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName
import kotlin.math.pow

/** An extended version of the [ValueEvaluator] that supports Python-specific operations. */
class PythonValueEvaluator : ValueEvaluator() {
    /**
     * The values of the corresponding symbols as defined on per platform level. Maps the symbol to
     * a map of platform -> value.
     *
     * The file constants (`os.O_...` have to match those defined in the file concept.
     */
    internal val symbolsMap: Map<String, Map<String, Any>> =
        mapOf(
            "os.O_RDONLY" to mapOf("linux" to 0L),
            "os.O_WRONLY" to mapOf("linux" to 1L),
            "os.O_RDWR" to mapOf("linux" to 2L),
            "os.O_CREAT" to mapOf("linux" to 64L),
            "os.O_TRUNC" to mapOf("linux" to 512L),
        )

    override val cannotEvaluate: (Node?, ValueEvaluator) -> Any?
        get() = { node, evaluator ->
            if (node is InitializerListExpression) {
                // We can evaluate initializer lists if all elements are constant
                val values = node.initializers.map { evaluator.evaluate(it) }
                if (values.all { it is Number }) {
                    values
                } else {
                    super.cannotEvaluate(node, evaluator)
                }
            } else {
                super.cannotEvaluate(node, evaluator)
            }
        }

    override fun handleReference(node: Reference, depth: Int): Any? {
        return when (val recName = node.reconstructedImportName.toString()) {
            in symbolsMap.keys ->
                resolveSymbolViaLookup(node, recName) ?: super.handleReference(node, depth)

            // We need to handle sys.platform and sys.version_info specially, since it is often used
            // in a pre-processor macro-style, and we want to replace this with the actual value (if
            // we have it). This allows us to dynamically prune if-branches based on constant
            // evaluation.
            "sys.platform" ->
                node.translationUnit?.sysInfo?.platform ?: super.handleReference(node, depth)
            "sys.version_info" -> {
                return node.translationUnit?.sysInfo?.versionInfo?.toList()
                    ?: super.handleReference(node, depth)
            }
            else -> super.handleReference(node, depth)
        }
    }

    override fun handleCallExpression(call: CallExpression, depth: Int): Any? {
        return when (call.reconstructedImportName.toString()) {
            "os.path.join" -> {
                call.arguments.joinToString(separator = "/") { // TODO separator
                arg ->
                    super.evaluate(arg).toString()
                }
            }
            else -> super.handleCallExpression(call, depth)
        }
    }

    override fun handlePrevDFG(node: Node, depth: Int): Any? {
        return super.handlePrevDFG(node, depth)
    }

    override fun computeBinaryOpEffect(
        lhsValue: Any?,
        rhsValue: Any?,
        has: HasOperatorCode?,
    ): Any? {
        return if (has?.operatorCode == "**") {
            when {
                lhsValue is Number && rhsValue is Number ->
                    lhsValue.toDouble().pow(rhsValue.toDouble())
                else -> cannotEvaluate(has as Node, this)
            }
        } else {
            super.computeBinaryOpEffect(lhsValue, rhsValue, has)
        }
    }

    override fun handleLess(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return handleListComparison(lhsValue, rhsValue, this::handleLess)
            ?: super.handleLess(lhsValue, rhsValue, expr)
    }

    override fun handleGreater(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return handleListComparison(lhsValue, rhsValue, this::handleGreater)
            ?: super.handleGreater(lhsValue, rhsValue, expr)
    }

    override fun handleLEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return handleListComparison(lhsValue, rhsValue, this::handleLEq)
            ?: super.handleLEq(lhsValue, rhsValue, expr)
    }

    override fun handleGEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return handleListComparison(lhsValue, rhsValue, this::handleGEq)
            ?: super.handleGEq(lhsValue, rhsValue, expr)
    }

    override fun handleEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return handleListComparison(lhsValue, rhsValue, this::handleEq)
            ?: super.handleEq(lhsValue, rhsValue, expr)
    }

    override fun handleNEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return handleListComparison(lhsValue, rhsValue, this::handleNEq)
            ?: super.handleNEq(lhsValue, rhsValue, expr)
    }

    /** See https://docs.python.org/3/reference/expressions.html#value-comparisons. */
    private fun handleListComparison(
        lhsValue: Any?,
        rhsValue: Any?,
        compare: (lhsValue: Any?, rhsValue: Any?, expr: Expression?) -> Any?,
    ): Any? {
        return if (lhsValue is List<*> && rhsValue is List<*>) {
            lhsValue.zip(rhsValue).all { (lhs, rhs) ->
                if (lhs != rhs) {
                    compare(lhs, rhs, null) as? Boolean == true
                } else {
                    true
                }
            }
        } else {
            null
        }
    }

    /**
     * Fetches a symbols value from the [symbolsMap] by looking at the [SystemInformation.platform]
     * stored in the [node]. Creates a warning, if no platform is specified.
     *
     * @param node The node being evaluated (used for platform lookup).
     * @param symbol The symbol to evaluate.
     * @return The evaluated symbol or null if it is not specified in the [symbolsMap].
     */
    internal fun resolveSymbolViaLookup(node: Reference, symbol: String): Any? {
        val platform = node.translationUnit?.sysInfo?.platform
        if (platform == null) {
            Util.warnWithFileLocation(node, log, "No platform found. Cannot evaluate symbol.")
        }
        return symbolsMap[symbol]?.get(platform)
    }
}
