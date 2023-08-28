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
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDecl
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDecl
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
            is CallExpr -> handleCallExpression(node, inferDfgForUnresolvedSymbols)
            is CastExpr -> handleCastExpression(node)
            is BinaryOp -> handleBinaryOp(node, parent)
            is AssignExpr -> handleAssignExpression(node)
            is ArrayExpr -> handleArrayCreationExpression(node)
            is SubscriptionExpr -> handleArraySubscriptionExpression(node)
            is ConditionalExpr -> handleConditionalExpression(node)
            is MemberExpr -> handleMemberExpression(node, inferDfgForUnresolvedSymbols)
            is Reference -> handleDeclaredReferenceExpression(node)
            is ExprList -> handleExpressionList(node)
            is NewExpr -> handleNewExpression(node)
            // We keep the logic for the InitializerListExpression in that class because the
            // performance would decrease too much.
            is InitializerListExpr -> handleInitializerListExpression(node)
            is KeyValueExpr -> handleKeyValueExpression(node)
            is LambdaExpr -> handleLambdaExpression(node)
            is UnaryOp -> handleUnaryOperator(node)
            // Statements
            is ReturnStmt -> handleReturnStatement(node)
            is ForEachStmt -> handleForEachStatement(node)
            is DoStmt -> handleDoStatement(node)
            is WhileStmt -> handleWhileStatement(node)
            is ForStmt -> handleForStatement(node)
            is SwitchStmt -> handleSwitchStatement(node)
            is IfStmt -> handleIfStatement(node)
            // Declarations
            is FieldDecl -> handleFieldDeclaration(node)
            is FunctionDecl -> handleFunctionDeclaration(node)
            is VariableDecl -> handleVariableDeclaration(node)
        }
    }

    protected fun handleAssignExpression(node: AssignExpr) {
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
     * For a [MemberExpr], the base flows to the expression if the field is not implemented in the
     * code under analysis. Otherwise, it's handled as a [Reference].
     */
    protected fun handleMemberExpression(node: MemberExpr, inferDfgForUnresolvedCalls: Boolean) {
        if (node.refersTo == null && inferDfgForUnresolvedCalls) {
            node.addPrevDFG(node.base)
        } else {
            handleDeclaredReferenceExpression(node)
        }
    }

    /** Adds the DFG edge for a [VariableDecl]. The data flows from initializer to the variable. */
    protected fun handleVariableDeclaration(node: VariableDecl) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [FunctionDecl]. The data flows from the return statement(s) to the
     * function.
     */
    protected fun handleFunctionDeclaration(node: FunctionDecl) {
        node.allChildren<ReturnStmt>().forEach { node.addPrevDFG(it) }
    }

    /** Adds the DFG edge for a [FieldDecl]. The data flows from the initializer to the field. */
    protected fun handleFieldDeclaration(node: FieldDecl) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [ReturnStmt]. The data flows from the return value to the statement.
     */
    protected fun handleReturnStatement(node: ReturnStmt) {
        node.returnValues.forEach { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge for a [ForEachStmt]. The data flows from the [ForEachStmt.iterable] to the
     * [ForEachStmt.variable]. However, since the [ForEachStmt.variable] is a [Statement], we have
     * to identify the variable which is used in the loop. In most cases, we should have a
     * [DeclarationStmt] which means that we can unwrap the [VariableDecl]. If this is not the case,
     * we assume that the last [VariableDecl] in the statement is the one we care about.
     */
    protected fun handleForEachStatement(node: ForEachStmt) {
        node.iterable?.let { iterable ->
            if (node.variable is DeclarationStmt) {
                (node.variable as DeclarationStmt).declarations.forEach { it.addPrevDFG(iterable) }
            } else {
                node.variable.variables.lastOrNull()?.addPrevDFG(iterable)
            }
        }
        node.variable?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge from [ForEachStmt.variable] to the [ForEachStmt] to show the dependence
     * between data and the branching node.
     */
    protected fun handleDoStatement(node: DoStmt) {
        node.condition?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge from [ForStmt.condition] or [ForStmt.conditionDeclaration] to the [ForStmt]
     * to show the dependence between data and the branching node. Usage of one or the other in the
     * statement is mutually exclusive.
     */
    protected fun handleForStatement(node: ForStmt) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.condition,
            node.conditionDeclaration
        )
    }

    /**
     * Adds the DFG edge from [IfStmt.condition] or [IfStmt.conditionDeclaration] to the [IfStmt] to
     * show the dependence between data and the branching node. Usage of one or the other in the
     * statement is mutually exclusive.
     */
    protected fun handleIfStatement(node: IfStmt) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.condition,
            node.conditionDeclaration
        )
    }

    /**
     * Adds the DFG edge from [SwitchStmt.selector] or [SwitchStmt.selectorDeclaration] to the
     * [SwitchStmt] to show the dependence between data and the branching node. Usage of one or the
     * other in the statement is mutually exclusive.
     */
    protected fun handleSwitchStatement(node: SwitchStmt) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.selector,
            node.selectorDeclaration
        )
    }

    /**
     * Adds the DFG edge from [WhileStmt.condition] or [WhileStmt.conditionDeclaration] to the
     * [WhileStmt] to show the dependence between data and the branching node. Usage of one or the
     * other in the statement is mutually exclusive.
     */
    protected fun handleWhileStatement(node: WhileStmt) {
        Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
            node,
            node.condition,
            node.conditionDeclaration
        )
    }

    /**
     * Adds the DFG edges for an [UnaryOp]. The data flow from the input to this node and, in case
     * of the operators "++" and "--" also from the node back to the input.
     */
    protected fun handleUnaryOperator(node: UnaryOp) {
        node.input.let {
            node.addPrevDFG(it)
            if (node.operatorCode == "++" || node.operatorCode == "--") {
                node.addNextDFG(it)
            }
        }
    }

    /**
     * Adds the DFG edge for a [LambdaExpr]. The data flow from the function representing the lambda
     * to the expression.
     */
    protected fun handleLambdaExpression(node: LambdaExpr) {
        node.function?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [KeyValueExpr]. The value flows to this expression. TODO: Check
     * with python and JS implementation
     */
    protected fun handleKeyValueExpression(node: KeyValueExpr) {
        node.value?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges for an [InitializerListExpr]. All values in the initializer flow to this
     * expression.
     */
    protected fun handleInitializerListExpression(node: InitializerListExpr) {
        node.initializers.forEach { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [ExprList]. The data of the last expression flow to the whole list.
     */
    protected fun handleExpressionList(node: ExprList) {
        node.expressions.lastOrNull()?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [NewExpr]. The data of the initializer flow to the whole expression.
     */
    protected fun handleNewExpression(node: NewExpr) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edges to a [Reference] as follows:
     * - If the variable is written to, data flows from this node to the variable declaration.
     * - If the variable is read from, data flows from the variable declaration to this node.
     * - For a combined read and write, both edges for data flows are added.
     */
    protected fun handleDeclaredReferenceExpression(node: Reference) {
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
     * Adds the DFG edge to a [ConditionalExpr]. Data flows from the then and the else expression to
     * the whole expression.
     */
    protected fun handleConditionalExpression(node: ConditionalExpr) {
        node.thenExpr?.let { node.addPrevDFG(it) }
        node.elseExpr?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [SubscriptionExpr]. The whole array `x` flows to the result `x[i]`.
     */
    protected fun handleArraySubscriptionExpression(node: SubscriptionExpr) {
        node.addPrevDFG(node.arrayExpression)
    }

    /** Adds the DFG edge to an [ArrayExpr]. The initializer flows to the expression. */
    protected fun handleArrayCreationExpression(node: ArrayExpr) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [BinaryOp]. The value flows to the target of an assignment or to the
     * whole expression.
     */
    protected fun handleBinaryOp(node: BinaryOp, parent: Node?) {
        when (node.operatorCode) {
            "=" -> {
                node.rhs.let { node.lhs.addPrevDFG(it) }
                // There are cases where we explicitly want to connect the rhs to the =.
                // E.g., this is the case in C++ where subexpressions can make the assignment.
                // Examples: a + (b = 1)  or  a = a == b ? b = 2: b = 3
                // When the parent is a compound statement (or similar block of code), we can safely
                // assume that we're not in such a sub-expression
                if (parent == null || parent !is CompoundStmt) {
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

    /** Adds the DFG edge to a [CastExpr]. The inner expression flows to the cast expression. */
    protected fun handleCastExpression(castExpr: CastExpr) {
        castExpr.expression.let { castExpr.addPrevDFG(it) }
    }

    /** Adds the DFG edges to a [CallExpr]. */
    fun handleCallExpression(call: CallExpr, inferDfgForUnresolvedSymbols: Boolean) {
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
    protected fun handleUnresolvedCalls(call: CallExpr, dfgTarget: Node) {
        if (call is MemberCallExpr && !call.isStatic) {
            call.base?.let { dfgTarget.addPrevDFG(it) }
        }

        call.arguments.forEach { dfgTarget.addPrevDFG(it) }
    }
}
