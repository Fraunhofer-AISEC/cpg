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
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.field
import de.fraunhofer.aisec.cpg.graph.edges.flows.indexed
import de.fraunhofer.aisec.cpg.graph.edges.flows.partial
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries

/** Adds the DFG edges for various types of nodes. */
@DependsOn(SymbolResolver::class)
class DFGPass(ctx: TranslationContext) : ComponentPass(ctx) {
    private val callsInferredFunctions = mutableListOf<CallExpression>()

    override fun accept(component: Component) {
        log.info(
            "Function summaries database has {} entries",
            config.functionSummaries.functionToDFGEntryMap.size,
        )

        val inferDfgForUnresolvedCalls = config.inferenceConfiguration.inferDfgForUnresolvedSymbols
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit { node, parent ->
            handle(node, parent, inferDfgForUnresolvedCalls, config.functionSummaries)
        }
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
        if (config.registeredPasses.all { ControlFlowSensitiveDFGPass::class !in it }) {
            connectInferredCallArguments(config.functionSummaries)
        }
    }

    /**
     * For inferred functions which have function summaries encoded, we connect the arguments to
     * modified parameter to propagate the changes to the arguments out of the [FunctionDeclaration]
     * again.
     */
    private fun connectInferredCallArguments(functionSummaries: DFGFunctionSummaries) {
        for (call in callsInferredFunctions) {
            for (invoked in call.invokes.filter { it.isInferred }) {
                val changedParams =
                    functionSummaries.functionToChangedParameters[invoked] ?: mapOf()
                for ((param, _) in changedParams) {
                    if (param == (invoked as? MethodDeclaration)?.receiver) {
                        (call as? MemberCallExpression)
                            ?.base
                            ?.prevDFGEdges
                            ?.addContextSensitive(param, callingContext = CallingContextOut(call))
                    } else if (param is ParameterDeclaration) {
                        val arg = call.arguments[param.argumentIndex]
                        arg.prevDFGEdges.addContextSensitive(
                            param,
                            callingContext = CallingContextOut(call),
                        )
                        arg.access = AccessValues.READWRITE
                        (arg as? Reference)?.let {
                            it.refersTo?.let { it1 -> it.nextDFGEdges += it1 }
                        }
                    }
                }
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    protected fun handle(
        node: Node?,
        parent: Node?,
        inferDfgForUnresolvedSymbols: Boolean,
        functionSummaries: DFGFunctionSummaries,
    ) {
        when (node) {
            // Expressions
            is CollectionComprehension -> handleCollectionComprehension(node)
            is ComprehensionExpression -> handleComprehensionExpression(node)
            is CallExpression -> handleCallExpression(node, inferDfgForUnresolvedSymbols)
            is CastExpression -> handleCastExpression(node)
            is BinaryOperator -> handleBinaryOp(node, parent)
            is AssignExpression -> handleAssignExpression(node)
            is NewArrayExpression -> handleNewArrayExpression(node)
            is SubscriptExpression -> handleSubscriptExpression(node)
            is ConditionalExpression -> handleConditionalExpression(node)
            is MemberExpression -> handleMemberExpression(node)
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
            is ThrowExpression -> handleThrowExpression(node)
            // Declarations
            is FieldDeclaration -> handleFieldDeclaration(node)
            is FunctionDeclaration -> handleFunctionDeclaration(node, functionSummaries)
            is TupleDeclaration -> handleTupleDeclaration(node)
            is VariableDeclaration -> handleVariableDeclaration(node)
        }
    }

    /**
     * Handles a collection comprehension. The data flow from
     * `comprehension.comprehensionExpressions[i]` to `comprehension.comprehensionExpressions[i+1]`
     * and for the last `comprehension.comprehensionExpressions[i]`, it flows to the
     * `comprehension.statement`.
     */
    protected fun handleCollectionComprehension(comprehension: CollectionComprehension) {
        if (comprehension.comprehensionExpressions.isNotEmpty()) {
            comprehension.comprehensionExpressions
                .subList(0, comprehension.comprehensionExpressions.size - 1)
                .forEachIndexed { i, expr ->
                    expr.nextDFG += comprehension.comprehensionExpressions[i + 1]
                }
            comprehension.comprehensionExpressions.last().nextDFG += comprehension.statement
        }
        comprehension.prevDFG += comprehension.statement
    }

    /**
     * The iterable flows to the variable which flows into the whole expression together with the
     * predicate(s).
     */
    protected fun handleComprehensionExpression(comprehension: ComprehensionExpression) {
        comprehension.iterable.nextDFG += comprehension.variable
        comprehension.prevDFG += comprehension.variable
        comprehension.predicate?.let { comprehension.prevDFG += it }
    }

    /** Handle a [ThrowExpression]. The exception and parent exception flow into the node. */
    protected fun handleThrowExpression(node: ThrowExpression) {
        node.exception?.let { node.prevDFGEdges += it }
        node.parentException?.let { node.prevDFGEdges += it }
    }

    protected fun handleAssignExpression(node: AssignExpression) {
        // If this is a compound assign, we also need to model a dataflow to the node itself
        if (node.isCompoundAssignment) {
            node.lhs.firstOrNull()?.let {
                node.prevDFGEdges += it
                node.nextDFGEdges += it
            }
            node.rhs.firstOrNull()?.let { node.prevDFGEdges += it }
        } else {
            // Find all targets of rhs and connect them
            node.rhs.forEach {
                val targets = node.findTargets(it)
                targets.forEach { target -> it.nextDFGEdges += target }
            }
        }

        // If the assignment is used as an expression, we also model a data flow from the (first)
        // rhs to the node itself
        if (node.usedAsExpression) {
            node.expressionValue?.nextDFGEdges += node
        }
    }

    /**
     * For a [MemberExpression], the base flows from/to the expression, depending on the
     * [MemberExpression.access].
     */
    protected fun handleMemberExpression(node: MemberExpression) {
        when (node.access) {
            AccessValues.WRITE -> {
                node.nextDFGEdges.add(node.base) {
                    (node.refersTo as? FieldDeclaration)?.let { granularity = field(it) }
                }
            }
            AccessValues.READWRITE -> {
                node.nextDFGEdges.add(node.base) {
                    (node.refersTo as? FieldDeclaration)?.let { granularity = field(it) }
                }
                // We do not make an edge in the other direction on purpose as a workaround for
                // nested field accesses on the lhs of an assignment.
            }
            else -> {
                node.prevDFGEdges.add(node.base) {
                    (node.refersTo as? FieldDeclaration)?.let { granularity = field(it) }
                }
            }
        }
    }

    /**
     * Adds the DFG edges for a [TupleDeclaration]. The data flows from initializer to the tuple
     * elements.
     */
    protected fun handleTupleDeclaration(node: TupleDeclaration) {
        node.initializer?.let { initializer ->
            node.prevDFG += initializer
            node.elements.forEachIndexed { idx, variable ->
                variable.prevDFGEdges.add(node) { granularity = indexed(idx) }
            }
        }
    }

    /**
     * Adds the DFG edge for a [VariableDeclaration]. The data flows from initializer to the
     * variable.
     */
    protected fun handleVariableDeclaration(node: VariableDeclaration) {
        node.initializer?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edge for a [FunctionDeclaration]. The data flows from the return statement(s) to
     * the function.
     */
    protected fun handleFunctionDeclaration(
        node: FunctionDeclaration,
        functionSummaries: DFGFunctionSummaries,
    ) {
        if (node.isInferred) {
            val summaryExists = with(functionSummaries) { addFlowsToFunctionDeclaration(node) }

            if (!summaryExists) {
                // If the function is inferred, we connect all parameters to the function
                // declaration.  The condition should make sure that we don't add edges multiple
                // times, i.e., we only handle the declaration exactly once.
                node.prevDFGEdges.addAll(node.parameters)
                // If it's a method with a receiver, we connect that one too.
                if (node is MethodDeclaration) {
                    node.receiver?.let { node.prevDFGEdges += it }
                }
            }
        } else {
            node.allChildren<ReturnStatement>().forEach { node.prevDFGEdges += it }
        }
    }

    /**
     * Adds the DFG edge for a [FieldDeclaration]. The data flows from the initializer to the field.
     */
    protected fun handleFieldDeclaration(node: FieldDeclaration) {
        node.initializer?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edge for a [ReturnStatement]. The data flows from the return value to the
     * statement.
     */
    protected fun handleReturnStatement(node: ReturnStatement) {
        node.returnValues.forEach { node.prevDFGEdges += it }
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
                    it.prevDFGEdges += iterable
                }
            } else {
                node.variable.variables.lastOrNull()?.prevDFGEdges += iterable
            }
        }
        assume(AssumptionType.AmbiguityAssumption, node) {
            "If this is not the case, we assume that the last VariableDeclaration in the statement is the one we care about."
        }
        node.variable?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edge from [ForEachStatement.variable] to the [ForEachStatement] to show the
     * dependence between data and the branching node.
     */
    protected fun handleDoStatement(node: DoStatement) {
        node.condition?.let { node.prevDFGEdges += it }
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
            node.conditionDeclaration,
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
            node.conditionDeclaration,
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
            node.selectorDeclaration,
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
            node.conditionDeclaration,
        )
    }

    /**
     * Adds the DFG edges for an [UnaryOperator]. The data flow from the input to this node and, in
     * case of the operators "++" and "--" also from the node back to the input.
     */
    protected fun handleUnaryOperator(node: UnaryOperator) {
        node.input.let {
            node.prevDFGEdges += it
            if (node.operatorCode == "++" || node.operatorCode == "--") {
                node.nextDFGEdges += it
            }
        }
    }

    /**
     * Adds the DFG edge for a [LambdaExpression]. The data flow from the function representing the
     * lambda to the expression.
     */
    protected fun handleLambdaExpression(node: LambdaExpression) {
        node.function?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edges for an [KeyValueExpression]. The value flows to this expression. TODO:
     * Check with python and JS implementation
     */
    protected fun handleKeyValueExpression(node: KeyValueExpression) {
        // TODO: Doesn't the node also contain the key?? Should the value be "partial" or "full"?
        node.prevDFGEdges += node.value
    }

    /**
     * Adds the DFG edges for an [InitializerListExpression]. All values in the initializer flow to
     * this expression.
     */
    protected fun handleInitializerListExpression(node: InitializerListExpression) {
        node.initializers.forEachIndexed { idx, it ->
            val astParent = node.astParent
            if (
                astParent is AssignExpression && node in astParent.lhs ||
                    astParent is ComprehensionExpression && node == astParent.variable
            ) {
                // If we're the target of an assignment or the variable of a comprehension
                // expression, the DFG flows from the node to the initializers.
                node.nextDFGEdges.add(it) { granularity = indexed(idx) }
            } else {
                node.prevDFGEdges.add(it) { granularity = indexed(idx) }
            }
        }
    }

    /**
     * Adds the DFG edge to an [ExpressionList]. The data of the last expression flow to the whole
     * list.
     */
    protected fun handleExpressionList(node: ExpressionList) {
        node.expressions.lastOrNull()?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edge to an [NewExpression]. The data of the initializer flow to the whole
     * expression.
     */
    protected fun handleNewExpression(node: NewExpression) {
        node.initializer?.let { node.prevDFGEdges += it }
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
                AccessValues.WRITE -> node.nextDFGEdges += it
                AccessValues.READ -> node.prevDFGEdges += it
                else -> {
                    node.nextDFGEdges += it
                    node.prevDFGEdges += it
                }
            }
        }
    }

    /**
     * Adds the DFG edge to a [ConditionalExpression]. Data flows from the then and the else
     * expression to the whole expression.
     */
    protected fun handleConditionalExpression(node: ConditionalExpression) {
        node.thenExpression?.let { node.prevDFGEdges += it }
        node.elseExpression?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edge to an [SubscriptExpression]. The whole array `x` flows to the result `x[i]`
     * or vice versa depending on the access value.
     */
    protected fun handleSubscriptExpression(node: SubscriptExpression) {
        if (node.access == AccessValues.WRITE) {
                node.nextDFGEdges
            } else {
                node.prevDFGEdges
            }
            .add(node.arrayExpression) {
                val literalValue = (node.subscriptExpression as? Literal<*>)?.value
                granularity =
                    when (literalValue) {
                        is Number -> indexed(literalValue)
                        is String -> indexed(literalValue)
                        else -> partial(node.subscriptExpression)
                    }
            }
    }

    /** Adds the DFG edge to an [NewArrayExpression]. The initializer flows to the expression. */
    protected fun handleNewArrayExpression(node: NewArrayExpression) {
        node.initializer?.let { node.prevDFGEdges += it }
    }

    /**
     * Adds the DFG edge to an [BinaryOperator]. The value flows to the target of an assignment or
     * to the whole expression.
     */
    protected fun handleBinaryOp(node: BinaryOperator, parent: Node?) {
        when (node.operatorCode) {
            "=" -> {
                node.rhs.let { node.lhs.prevDFGEdges += it }
                // There are cases where we explicitly want to connect the rhs to the =.
                // E.g., this is the case in C++ where subexpressions can make the assignment.
                // Examples: a + (b = 1)  or  a = a == b ? b = 2: b = 3
                // When the parent is a compound statement (or similar block of code), we can safely
                // assume that we're not in such a sub-expression
                if (parent == null || parent !is Block) {
                    node.rhs.nextDFGEdges += node
                }
            }
            in node.language.compoundAssignmentOperators -> {
                node.lhs.let {
                    node.prevDFGEdges += it
                    node.nextDFGEdges += it
                }
                node.rhs.let { node.prevDFGEdges += it }
            }
            else -> {
                node.lhs.let { node.prevDFGEdges += it }
                node.rhs.let { node.prevDFGEdges += it }
            }
        }
    }

    /**
     * Adds the DFG edge to a [CastExpression]. The inner expression flows to the cast expression.
     */
    protected fun handleCastExpression(castExpression: CastExpression) {
        castExpression.expression.let { castExpression.prevDFGEdges += it }
    }

    /** Adds the DFG edges to a [CallExpression]. */
    fun handleCallExpression(call: CallExpression, inferDfgForUnresolvedSymbols: Boolean) {
        // Remove existing DFG edges since they are no longer valid (e.g. after updating the
        // CallExpression with the invokes edges to the called functions)
        call.prevDFGEdges.clear()

        if (call.invokes.isEmpty() && inferDfgForUnresolvedSymbols) {
            // Unresolved call expression
            handleUnresolvedCalls(call, call)
        } else if (call.invokes.isNotEmpty()) {
            call.invokes.forEach {
                Util.attachCallParameters(it, call)
                call.prevDFGEdges.addContextSensitive(it, callingContext = CallingContextOut(call))
                if (it.isInferred) {
                    callsInferredFunctions.add(call)
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
            call.base?.let { dfgTarget.prevDFGEdges += it }
        }

        call.arguments.forEach { dfgTarget.prevDFGEdges += it }
    }
}
