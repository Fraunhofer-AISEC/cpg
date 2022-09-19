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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.Assignment
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util

/** Adds the DFG edges for various types of nodes. */
@DependsOn(VariableUsageResolver::class)
class DFGPass : Pass() {
    override fun accept(tr: TranslationResult) {
        val inferDfgForUnresolvedCalls =
            tr.translationManager.config.inferenceConfiguration.inferDfgForUnresolvedCalls
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit { handle(it, inferDfgForUnresolvedCalls) }
        for (tu in tr.translationUnits) {
            walker.iterate(tu)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    private fun handle(node: Node?, inferDfgForUnresolvedCalls: Boolean) {
        when (node) {
            // Expressions
            is CallExpression -> handleCallExpression(node, inferDfgForUnresolvedCalls)
            is CastExpression -> handleCastExpression(node)
            is BinaryOperator -> handleBinaryOp(node)
            is ArrayCreationExpression -> handleArrayCreationExpression(node)
            is ArraySubscriptionExpression -> handleArraySubscriptionExpression(node)
            is ConditionalExpression -> handleConditionalExpression(node)
            is DeclaredReferenceExpression -> handleDeclaredReferenceExpression(node)
            is ExpressionList -> handleExpressionList(node)
            // We keep the logic for the InitializerListExpression in that class because the
            // performance would decrease too much.
            // is InitializerListExpression -> handleInitializerListExpression(node)
            is KeyValueExpression -> handleKeyValueExpression(node)
            is LambdaExpression -> handleLambdaExpression(node)
            is UnaryOperator -> handleUnaryOperator(node)
            // Statements
            is ReturnStatement -> handleReturnStatement(node)
            // Declarations
            is FieldDeclaration -> handleFieldDeclaration(node)
            is FunctionDeclaration -> handleFunctionDeclaration(node)
            is VariableDeclaration -> handleVariableDeclaration(node)
            // Other
            is Assignment -> handleAssignment(node)
        }
    }

    /**
     * Adds the DFG edge for a [FunctionDeclaration]. The data flows from the return statement(s) to
     * the function.
     */
    private fun handleVariableDeclaration(node: VariableDeclaration) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [FunctionDeclaration]. The data flows from the return statement(s) to
     * the function.
     */
    private fun handleFunctionDeclaration(node: FunctionDeclaration) {
        if (node.body is ReturnStatement) {
            node.addPrevDFG(node.body as ReturnStatement)
        } else if (node.body is CompoundStatement) {
            (node.body as CompoundStatement)
                .statements
                .filterIsInstance<ReturnStatement>()
                .forEach { node.addPrevDFG(it) }
        }
    }

    /**
     * Adds the DFG edge for a [FieldDeclaration]. The data flows from the initializer to the field.
     */
    private fun handleFieldDeclaration(node: FieldDeclaration) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [ReturnStatement]. The data flows from the return value to the
     * statement.
     */
    private fun handleReturnStatement(node: ReturnStatement) {
        node.returnValue?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [UnaryOperator]. The data flow from the input to this node and, in
     * case of the operators "++" and "--" also from the node back to the input.
     */
    private fun handleUnaryOperator(node: UnaryOperator) {
        node.input?.let {
            node.addPrevDFG(it)
            if (node.operatorCode == "++" || node.operatorCode == "--") {
                node.addNextDFG(it)
            }
        }
    }

    /**
     * Adds the DFG edge for a [LambdaExpression]. The data flow from the function representing the
     * lambda to the expression.
     */
    private fun handleLambdaExpression(node: LambdaExpression) {
        node.function?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [KeyValueExpression]. The value flows to this expression. TODO:
     * Check with python and JS implementation
     */
    private fun handleKeyValueExpression(node: KeyValueExpression) {
        node.value?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [InitializerListExpression]. All values in the initializer flow to
     * this expression.
     *
     * TODO: This change seems to have performance issues!
     */
    private fun handleInitializerListExpression(node: InitializerListExpression) {
        node.initializers?.forEach {
            it.registerTypeListener(node)
            node.addPrevDFG(it)
        }
    }

    /**
     * Adds the DFG edge to an [ExpressionList]. The data of the last expression flow to the whole
     * list.
     */
    private fun handleExpressionList(node: ExpressionList) {
        node.expressions.lastOrNull()?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges to a [DeclaredReferenceExpression] as follows:
     * - If the variable is written to, data flows from this node to the variable declaration.
     * - If the variable is read from, data flows from the variable declaration to this node.
     * - For a combined read and write, both edges for data flows are added.
     */
    private fun handleDeclaredReferenceExpression(node: DeclaredReferenceExpression) {
        node.refersTo?.let {
            when (node.access) {
                AccessValues.WRITE -> node.addNextDFG(it)
                AccessValues.READ -> node.addPrevDFG(it)
                else -> {
                    node.addNextDFG(it)
                    node.addPrevDFG(it)
                }
            }
        }
    }

    /**
     * Adds the DFG edge to a [ConditionalExpression]. Data flows from the then and the else
     * expression to the whole expression.
     */
    private fun handleConditionalExpression(node: ConditionalExpression) {
        node.thenExpr?.let { node.addPrevDFG(it) }
        node.elseExpr?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [ArraySubscriptionExpression]. The whole array `x` flows to the
     * result `x[i]`.
     */
    private fun handleArraySubscriptionExpression(node: ArraySubscriptionExpression) {
        node.arrayExpression?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [ArrayCreationExpression]. The initializer flows to the expression.
     */
    private fun handleArrayCreationExpression(node: ArrayCreationExpression) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [BinaryOperator]. The value flows to the target of an assignment or
     * to the whole expression.
     */
    private fun handleBinaryOp(node: BinaryOperator) {
        when (node.operatorCode) {
            "=" -> node.rhs?.let { node.lhs.addPrevDFG(it) }
            "*=",
            "/=",
            "%=",
            "+=",
            "-=",
            "<<=",
            ">>=",
            "&=",
            "^=",
            "|=" -> {
                node.lhs?.let {
                    node.addPrevDFG(it)
                    node.addNextDFG(it)
                }
                node.rhs?.let { node.addPrevDFG(it) }
            }
            else -> {
                node.lhs?.let { node.addPrevDFG(it) }
                node.rhs?.let { node.addPrevDFG(it) }
            }
        }
    }

    /** Adds the DFG edge to an [Assignment]. The value flows to the target. */
    private fun handleAssignment(node: Assignment) {
        node.value?.let { (node.target as? Node)?.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to a [CastExpression]. The inner expression flows to the cast expression.
     */
    private fun handleCastExpression(castExpression: CastExpression) {
        castExpression.expression?.let { castExpression.addPrevDFG(it) }
    }

    /** Adds the DFG edges to a [CallExpression]. */
    fun handleCallExpression(call: CallExpression, inferDfgForUnresolvedCalls: Boolean) {
        // Remove existing DFG edges since they are no longer valid (e.g. after updating the
        // CallExpression with the invokes edges to the called functions)
        call.prevDFG.forEach { it.nextDFG.remove(call) }
        call.prevDFG.clear()

        if (call.invokes.isEmpty() && inferDfgForUnresolvedCalls) {
            // Unresolved call expression
            handleUnresolvedCalls(call)
        } else if (call.invokes.isNotEmpty()) {
            call.invokes.forEach {
                Util.attachCallParameters(it, call.arguments)
                call.addPrevDFG(it)
            }
        }
    }

    /**
     * Adds DFG edges for unresolved function calls as follows:
     * - from base (if available) to the CallExpression
     * - from all arguments to the CallExpression
     */
    private fun handleUnresolvedCalls(call: CallExpression) {
        call.base?.let { call.addPrevDFG(it) }
        call.arguments.forEach { call.addPrevDFG(it) }
    }
}
