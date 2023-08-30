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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TupleDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

/** Adds the DFG edges for various types of nodes. */
@DependsOn(VariableUsageResolver::class)
@DependsOn(CallResolver::class)
class DFGPass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(component: Component) {
        val inferDfgForUnresolvedCalls = config.inferenceConfiguration.inferDfgForUnresolvedSymbols
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit2 { node, parent ->
            handle(node, parent, inferDfgForUnresolvedCalls)
        }
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    protected fun handle(node: Node?, parent: Node?, inferDfgForUnresolvedSymbols: Boolean) {
        when (node) {
            // Expressions
            is CallExpression -> handleCallExpression(node, inferDfgForUnresolvedSymbols)
            is CastExpression -> handleCastExpression(node)
            is BinaryOperator -> handleBinaryOp(node, parent)
            is AssignExpression -> handleAssignExpression(node)
            is NewArrayExpression -> handleArrayCreationExpression(node)
            is SubscriptionExpression -> handleArraySubscriptionExpression(node)
            is ConditionalExpression -> handleConditionalExpression(node)
            is MemberExpression -> handleMemberExpression(node, inferDfgForUnresolvedSymbols)
            is Reference -> handleReference(node)
            is ExpressionList -> handleExpressionList(node)
            is NewExpression -> handleNewExpression(node)
            // We keep the logic for the InitializerListExpression in that class because the
            // performance would decrease too much.
            is InitializerListExpression -> handleInitializerListExpression(node)
            is KeyValueExpression -> handleKeyValueExpression(node)
            is LambdaExpression -> handleLambdaExpression(node)
            is UnaryOperator -> handleUnaryOperator(node)
            // Statements
            is ReturnStatement -> handleReturnStatement(node)
            is ForEachStatement -> handleForEachStatement(node)
            is DoStatement -> handleDoStatement(node)
            is WhileStatement -> handleWhileStatement(node)
            is ForStatement -> handleForStatement(node)
            is SwitchStatement -> handleSwitchStatement(node)
            is IfStatement -> handleIfStatement(node)
            // Declarations
            is FieldDeclaration -> handleFieldDeclaration(node)
            is FunctionDeclaration -> handleFunctionDeclaration(node)
            is TupleDeclaration -> handleTupleDeclaration(node)
            is VariableDeclaration -> handleVariableDeclaration(node)
        }
    }

    protected fun handleAssignExpression(node: AssignExpression) {
        // If this is a compound assign, we also need to model a dataflow to the node itself
        if (node.isCompoundAssignment) {
            node.lhs.firstOrNull()?.let {
                node.addPrevDFG(it)
                node.addNextDFG(it)
            }
            node.rhs.firstOrNull()?.let { node.addPrevDFG(it) }
        } else {
            // Find all targets of rhs and connect them
            node.rhs.forEach {
                val targets = node.findTargets(it)
                targets.forEach { target -> it.addNextDFG(target) }
            }
        }

        // If the assignment is used as an expression, we also model a data flow from the (first)
        // rhs to the node itself
        if (node.usedAsExpression) {
            node.expressionValue?.addNextDFG(node)
        }
    }

    /**
     * For a [MemberExpression], the base flows to the expression if the field is not implemented in
     * the code under analysis. Otherwise, it's handled as a [Reference].
     */
    protected fun handleMemberExpression(
        node: MemberExpression,
        inferDfgForUnresolvedCalls: Boolean
    ) {
        if (node.refersTo == null && inferDfgForUnresolvedCalls) {
            node.addPrevDFG(node.base)
        } else {
            handleReference(node)
        }
    }

    /**
     * Adds the DFG edges for a [TupleDeclaration]. The data flows from initializer to the tuple
     * elements.
     */
    protected fun handleTupleDeclaration(node: TupleDeclaration) {
        node.initializer?.let { initializer ->
            node.elements.withIndex().forEach {
                it.value.addPrevDFG(initializer, mutableMapOf(Properties.INDEX to it.index))
            }
        }
    }

    /**
     * Adds the DFG edge for a [VariableDeclaration]. The data flows from initializer to the
     * variable.
     */
    protected fun handleVariableDeclaration(node: VariableDeclaration) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [FunctionDeclaration]. The data flows from the return statement(s) to
     * the function.
     */
    protected fun handleFunctionDeclaration(node: FunctionDeclaration) {
        node.allChildren<ReturnStatement>().forEach { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [FieldDeclaration]. The data flows from the initializer to the field.
     */
    protected fun handleFieldDeclaration(node: FieldDeclaration) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [ReturnStatement]. The data flows from the return value to the
     * statement.
     */
    protected fun handleReturnStatement(node: ReturnStatement) {
        node.returnValues.forEach { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [ForEachStatement]. The data flows from the
     * [ForEachStatement.iterable] to the [ForEachStatement.variable]. However, since the
     * [ForEachStatement.variable] is a [Statement], we have to identify the variable which is used
     * in the loop. In most cases, we should have a [DeclarationStatement] which means that we can
     * unwrap the [VariableDeclaration]. If this is not the case, we assume that the last
     * [VariableDeclaration] in the statement is the one we care about.
     */
    protected fun handleForEachStatement(node: ForEachStatement) {
        node.iterable?.let { iterable ->
            if (node.variable is DeclarationStatement) {
                (node.variable as DeclarationStatement).declarations.forEach {
                    it.addPrevDFG(iterable)
                }
            } else {
                node.variable.variables.lastOrNull()?.addPrevDFG(iterable)
            }
        }
        node.variable?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge from [ForEachStatement.variable] to the [ForEachStatement] to show the
     * dependence between data and the branching node.
     */
    protected fun handleDoStatement(node: DoStatement) {
        node.condition?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge from [ForStatement.condition] or [ForStatement.conditionDeclaration] to the
     * [ForStatement] to show the dependence between data and the branching node. Usage of one or
     * the other in the statement is mutually exclusive.
     */
    protected fun handleForStatement(node: ForStatement) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.condition,
            node.conditionDeclaration
        )
    }

    /**
     * Adds the DFG edge from [IfStatement.condition] or [IfStatement.conditionDeclaration] to the
     * [IfStatement] to show the dependence between data and the branching node. Usage of one or the
     * other in the statement is mutually exclusive.
     */
    protected fun handleIfStatement(node: IfStatement) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.condition,
            node.conditionDeclaration
        )
    }

    /**
     * Adds the DFG edge from [SwitchStatement.selector] or [SwitchStatement.selectorDeclaration] to
     * the [SwitchStatement] to show the dependence between data and the branching node. Usage of
     * one or the other in the statement is mutually exclusive.
     */
    protected fun handleSwitchStatement(node: SwitchStatement) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.selector,
            node.selectorDeclaration
        )
    }

    /**
     * Adds the DFG edge from [WhileStatement.condition] or [WhileStatement.conditionDeclaration] to
     * the [WhileStatement] to show the dependence between data and the branching node. Usage of one
     * or the other in the statement is mutually exclusive.
     */
    protected fun handleWhileStatement(node: WhileStatement) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.condition,
            node.conditionDeclaration
        )
    }

    /**
     * Adds the DFG edges for an [UnaryOperator]. The data flow from the input to this node and, in
     * case of the operators "++" and "--" also from the node back to the input.
     */
    protected fun handleUnaryOperator(node: UnaryOperator) {
        node.input.let {
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
    protected fun handleLambdaExpression(node: LambdaExpression) {
        node.function?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [KeyValueExpression]. The value flows to this expression. TODO:
     * Check with python and JS implementation
     */
    protected fun handleKeyValueExpression(node: KeyValueExpression) {
        node.value?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [InitializerListExpression]. All values in the initializer flow to
     * this expression.
     */
    protected fun handleInitializerListExpression(node: InitializerListExpression) {
        node.initializers.forEach { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [ExpressionList]. The data of the last expression flow to the whole
     * list.
     */
    protected fun handleExpressionList(node: ExpressionList) {
        node.expressions.lastOrNull()?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [NewExpression]. The data of the initializer flow to the whole
     * expression.
     */
    protected fun handleNewExpression(node: NewExpression) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges to a [Reference] as follows:
     * - If the variable is written to, data flows from this node to the variable declaration.
     * - If the variable is read from, data flows from the variable declaration to this node.
     * - For a combined read and write, both edges for data flows are added.
     */
    protected fun handleReference(node: Reference) {
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
    protected fun handleConditionalExpression(node: ConditionalExpression) {
        node.thenExpr?.let { node.addPrevDFG(it) }
        node.elseExpr?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [SubscriptionExpression]. The whole array `x` flows to the result
     * `x[i]`.
     */
    protected fun handleArraySubscriptionExpression(node: SubscriptionExpression) {
        node.addPrevDFG(node.arrayExpression)
    }

    /** Adds the DFG edge to an [NewArrayExpression]. The initializer flows to the expression. */
    protected fun handleArrayCreationExpression(node: NewArrayExpression) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [BinaryOperator]. The value flows to the target of an assignment or
     * to the whole expression.
     */
    protected fun handleBinaryOp(node: BinaryOperator, parent: Node?) {
        when (node.operatorCode) {
            "=" -> {
                node.rhs.let { node.lhs.addPrevDFG(it) }
                // There are cases where we explicitly want to connect the rhs to the =.
                // E.g., this is the case in C++ where subexpressions can make the assignment.
                // Examples: a + (b = 1)  or  a = a == b ? b = 2: b = 3
                // When the parent is a compound statement (or similar block of code), we can safely
                // assume that we're not in such a sub-expression
                if (parent == null || parent !is BlockStatement) {
                    node.rhs.addNextDFG(node)
                }
            }
            in node.language?.compoundAssignmentOperators ?: setOf() -> {
                node.lhs.let {
                    node.addPrevDFG(it)
                    node.addNextDFG(it)
                }
                node.rhs.let { node.addPrevDFG(it) }
            }
            else -> {
                node.lhs.let { node.addPrevDFG(it) }
                node.rhs.let { node.addPrevDFG(it) }
            }
        }
    }

    /**
     * Adds the DFG edge to a [CastExpression]. The inner expression flows to the cast expression.
     */
    protected fun handleCastExpression(castExpression: CastExpression) {
        castExpression.expression.let { castExpression.addPrevDFG(it) }
    }

    /** Adds the DFG edges to a [CallExpression]. */
    fun handleCallExpression(call: CallExpression, inferDfgForUnresolvedSymbols: Boolean) {
        // Remove existing DFG edges since they are no longer valid (e.g. after updating the
        // CallExpression with the invokes edges to the called functions)
        call.prevDFG.forEach { it.nextDFG.remove(call) }
        call.prevDFG.clear()

        if (call.invokes.isEmpty() && inferDfgForUnresolvedSymbols) {
            // Unresolved call expression
            handleUnresolvedCalls(call, call)
        } else if (call.invokes.isNotEmpty()) {
            call.invokes.forEach {
                if (it.isInferred && inferDfgForUnresolvedSymbols) {
                    handleUnresolvedCalls(call, it)
                } else {
                    Util.attachCallParameters(it, call.arguments)
                    call.addPrevDFG(it)
                }
            }
        }
    }

    /**
     * Adds DFG edges for unresolved function calls as follows:
     * - from base (if available) to the CallExpression
     * - from all arguments to the CallExpression
     */
    protected fun handleUnresolvedCalls(call: CallExpression, dfgTarget: Node) {
        if (call is MemberCallExpression && !call.isStatic) {
            call.base?.let { dfgTarget.addPrevDFG(it) }
        }

        call.arguments.forEach { dfgTarget.addPrevDFG(it) }
    }
}
