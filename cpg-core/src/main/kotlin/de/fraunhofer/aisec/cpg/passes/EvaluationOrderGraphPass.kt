/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.ProcessedListener
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.StatementHolder
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.scopes.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import java.util.*
import org.slf4j.LoggerFactory

/**
 * Creates an Evaluation Order Graph (EOG) based on AST.
 *
 * An EOG is an intraprocedural directed graph whose vertices are executable AST nodes and edges
 * connect them in the order they would be executed when running the program.
 *
 * An EOG always starts at the header of a method/function and ends in one (virtual) or multiple
 * return statements. A virtual return statement with a code location of (-1,-1) is used if the
 * actual source code does not have an explicit return statement.
 *
 * The EOG is similar to the CFG `ControlFlowGraphPass`, but there are some subtle differences:
 * * For methods without explicit return statement, EOF will have an edge to a virtual return node
 *   with line number -1 which does not exist in the original code. A CFG will always end with the
 *   last reachable statement(s) and not insert any virtual return statements.
 * * EOG considers an opening blocking ("CompoundStatement", indicated by a "{") as a separate node.
 *   A CFG will rather use the first actual executable statement within the block.
 * * For IF statements, EOG treats the "if" keyword and the condition as separate nodes. CFG treats
 *   this as one "if" statement.
 * * EOG considers a method header as a node. CFG will consider the first executable statement of
 *   the methods as a node.
 *
 * Its handleXXX functions are intentionally set as `protected`, in case someone wants to extend
 * this pass and fine-tune it.
 */
@Suppress("MemberVisibilityCanBePrivate")
@DependsOn(CallResolver::class)
open class EvaluationOrderGraphPass : Pass() {
    protected val map = mutableMapOf<Class<out Node>, (Node) -> Unit>()
    private var currentPredecessors = mutableListOf<Node>()
    private val nextEdgeProperties = EnumMap<Properties, Any?>(Properties::class.java)

    /**
     * Allows to register EOG creation logic when a currently visited node can depend on future
     * visited nodes. Currently used to connect goto statements and the target labeled statements.
     * Implemented as listener to connect nodes when the goto appears before the label.
     */
    private val processedListener = ProcessedListener()

    /**
     * Stores all nodes currently handled to add them to the processedListener even if a sub node is
     * the next target of an EOG edge.
     */
    private val intermediateNodes = mutableListOf<Node>()

    init {
        map[IncludeDeclaration::class.java] = { doNothing() }
        map[TranslationUnitDeclaration::class.java] = {
            handleTranslationUnitDeclaration(it as TranslationUnitDeclaration)
        }
        map[NamespaceDeclaration::class.java] = {
            handleNamespaceDeclaration(it as NamespaceDeclaration)
        }
        map[RecordDeclaration::class.java] = { handleRecordDeclaration(it as RecordDeclaration) }
        map[FunctionDeclaration::class.java] = {
            handleFunctionDeclaration(it as FunctionDeclaration)
        }
        map[VariableDeclaration::class.java] = {
            handleVariableDeclaration(it as VariableDeclaration)
        }
        map[CallExpression::class.java] = { handleCallExpression(it as CallExpression) }
        map[MemberExpression::class.java] = { handleMemberExpression(it as MemberExpression) }
        map[ArraySubscriptionExpression::class.java] = {
            handleArraySubscriptionExpression(it as ArraySubscriptionExpression)
        }
        map[ArrayCreationExpression::class.java] = {
            handleArrayCreationExpression(it as ArrayCreationExpression)
        }
        map[RangeExpression::class.java] = { handleRangeExpression(it as RangeExpression) }
        map[DeclarationStatement::class.java] = {
            handleDeclarationStatement(it as DeclarationStatement)
        }
        map[ReturnStatement::class.java] = { handleReturnStatement(it as ReturnStatement) }
        map[BinaryOperator::class.java] = { handleBinaryOperator(it as BinaryOperator) }
        map[AssignExpression::class.java] = { handleAssignExpression(it as AssignExpression) }
        map[UnaryOperator::class.java] = { handleUnaryOperator(it as UnaryOperator) }
        map[CompoundStatement::class.java] = { handleCompoundStatement(it as CompoundStatement) }
        map[CompoundStatementExpression::class.java] = {
            handleCompoundStatementExpression(it as CompoundStatementExpression)
        }
        map[IfStatement::class.java] = { handleIfStatement(it as IfStatement) }
        map[AssertStatement::class.java] = { handleAssertStatement(it as AssertStatement) }
        map[WhileStatement::class.java] = { handleWhileStatement(it as WhileStatement) }
        map[DoStatement::class.java] = { handleDoStatement(it as DoStatement) }
        map[ForStatement::class.java] = { handleForStatement(it as ForStatement) }
        map[ForEachStatement::class.java] = { handleForEachStatement(it as ForEachStatement) }
        map[TryStatement::class.java] = { handleTryStatement(it as TryStatement) }
        map[ContinueStatement::class.java] = { handleContinueStatement(it as ContinueStatement) }
        map[DeleteExpression::class.java] = { handleDeleteExpression(it as DeleteExpression) }
        map[BreakStatement::class.java] = { handleBreakStatement(it as BreakStatement) }
        map[SwitchStatement::class.java] = { handleSwitchStatement(it as SwitchStatement) }
        map[LabelStatement::class.java] = { handleLabelStatement(it as LabelStatement) }
        map[GotoStatement::class.java] = { handleGotoStatement(it as GotoStatement) }
        map[CaseStatement::class.java] = { handleCaseStatement(it as CaseStatement) }
        map[SynchronizedStatement::class.java] = {
            handleSynchronizedStatement(it as SynchronizedStatement)
        }
        map[NewExpression::class.java] = { handleNewExpression(it as NewExpression) }
        map[KeyValueExpression::class.java] = { handleKeyValueExpression(it as KeyValueExpression) }
        map[CastExpression::class.java] = { handleCastExpression(it as CastExpression) }
        map[ExpressionList::class.java] = { handleExpressionList(it as ExpressionList) }
        map[ConditionalExpression::class.java] = {
            handleConditionalExpression(it as ConditionalExpression)
        }
        map[InitializerListExpression::class.java] = {
            handleInitializerListExpression(it as InitializerListExpression)
        }
        map[ConstructExpression::class.java] = {
            handleConstructExpression(it as ConstructExpression)
        }
        map[EmptyStatement::class.java] = { handleDefault(it as EmptyStatement) }
        map[Literal::class.java] = { handleDefault(it) }
        map[DefaultStatement::class.java] = { handleDefault(it) }
        map[TypeIdExpression::class.java] = { handleDefault(it) }
        map[DeclaredReferenceExpression::class.java] = { handleDefault(it) }
        map[LambdaExpression::class.java] = { handleLambdaExpression(it as LambdaExpression) }
    }

    private fun doNothing() {
        // Nothing to do for this node type
    }

    override fun cleanup() {
        intermediateNodes.clear()
        currentPredecessors.clear()
    }

    override fun accept(result: TranslationResult) {
        scopeManager = result.scopeManager
        for (tu in result.translationUnits) {
            createEOG(tu)
            removeUnreachableEOGEdges(tu)
            // checkEOGInvariant(tu); To insert when trying to check if the invariant holds
        }
    }

    /**
     * Removes EOG edges by first building the negative set of nodes that cannot be visited and then
     * remove there outgoing edges.In contrast to truncateLooseEdges this also removes cycles.
     */
    private fun removeUnreachableEOGEdges(tu: TranslationUnitDeclaration) {
        val eognodes =
            SubgraphWalker.flattenAST(tu)
                .filter { it.prevEOG.isNotEmpty() || it.nextEOG.isNotEmpty() }
                .toMutableList()
        var validStarts =
            eognodes
                .filter { node ->
                    node is FunctionDeclaration ||
                        node is RecordDeclaration ||
                        node is NamespaceDeclaration ||
                        node is TranslationUnitDeclaration
                }
                .toSet()
        while (validStarts.isNotEmpty()) {
            eognodes.removeAll(validStarts)
            validStarts = validStarts.flatMap { it.nextEOG }.filter { it in eognodes }.toSet()
        }
        // remaining eognodes were not visited and have to be removed from the EOG
        for (unvisitedNode in eognodes) {
            unvisitedNode.nextEOGEdges.forEach { next ->
                next.end.removePrevEOGEntry(unvisitedNode)
            }

            unvisitedNode.nextEOGEdges.clear()
        }
    }

    protected fun handleTranslationUnitDeclaration(node: TranslationUnitDeclaration) {
        handleStatementHolder(node as StatementHolder)

        // loop through functions
        for (child in node.declarations) {
            createEOG(child)
        }
        processedListener.clearProcessed()
    }

    protected fun handleNamespaceDeclaration(node: NamespaceDeclaration) {
        handleStatementHolder(node)

        // loop through functions
        for (child in node.declarations) {
            createEOG(child)
        }
        processedListener.clearProcessed()
    }

    protected fun handleVariableDeclaration(node: VariableDeclaration) {
        // analyze the initializer
        createEOG(node.initializer)
        pushToEOG(node)
    }

    protected fun handleRecordDeclaration(node: RecordDeclaration) {
        scopeManager.enterScope(node)
        handleStatementHolder(node)
        currentPredecessors.clear()
        for (constructor in node.constructors) {
            createEOG(constructor)
        }
        for (method in node.methods) {
            createEOG(method)
        }
        for (records in node.records) {
            createEOG(records)
        }
        scopeManager.leaveScope(node)
    }

    protected fun handleStatementHolder(statementHolder: StatementHolder) {
        // separate code into static and non-static parts as they are executed in different moments,
        // although they can be placed in the same enclosing declaration.
        val code = statementHolder.statements

        val nonStaticCode = code.filter { (it as? CompoundStatement)?.isStaticBlock == false }
        val staticCode = code.filter { it !in nonStaticCode }

        pushToEOG(statementHolder as Node)
        for (staticStatement in staticCode) {
            createEOG(staticStatement)
        }
        currentPredecessors.clear()
        pushToEOG(statementHolder as Node)
        for (nonStaticStatement in nonStaticCode) {
            createEOG(nonStaticStatement)
        }
        currentPredecessors.clear()
    }

    protected fun handleLambdaExpression(node: LambdaExpression) {
        val tmpCurrentEOG = currentPredecessors.toMutableList()
        val tmpCurrentProperties = nextEdgeProperties.toMutableMap()
        val tmpIntermediateNodes = intermediateNodes.toMutableList()

        nextEdgeProperties.clear()
        currentPredecessors.clear()
        intermediateNodes.clear()

        createEOG(node.function)

        nextEdgeProperties.clear()
        currentPredecessors.clear()
        intermediateNodes.clear()

        nextEdgeProperties.putAll(tmpCurrentProperties)
        currentPredecessors.addAll(tmpCurrentEOG)
        intermediateNodes.addAll(tmpIntermediateNodes)

        pushToEOG(node)
    }

    protected fun handleFunctionDeclaration(node: FunctionDeclaration) {
        // reset EOG
        currentPredecessors.clear()
        var needToLeaveRecord = false
        if (
            node is MethodDeclaration &&
                node.recordDeclaration != null &&
                (node.recordDeclaration !== scopeManager.currentRecord)
        ) {
            // This is a method declaration outside the AST of the record, as its possible in
            // languages, such as C++. Therefore, we need to enter the record scope as well
            scopeManager.enterScope(node.recordDeclaration!!)
            needToLeaveRecord = true
        }
        scopeManager.enterScope(node)
        // push the function declaration
        pushToEOG(node)

        // analyze the body
        createEOG(node.body)

        val currentScope = scopeManager.currentScope
        if (currentScope !is FunctionScope) {
            Util.errorWithFileLocation(
                node,
                log,
                "Scope of function declaration is not a function scope. EOG of function might be incorrect."
            )
            // try to recover at least a little bit
            scopeManager.leaveScope(node)
            currentPredecessors.clear()
            return
        }
        val uncaughtEOGThrows = currentScope.catchesOrRelays.values.flatten()
        // Connect uncaught throws to block node
        node.body?.let { addMultipleIncomingEOGEdges(uncaughtEOGThrows, it) }
        scopeManager.leaveScope(node)
        if (node is MethodDeclaration && node.recordDeclaration != null && needToLeaveRecord) {
            scopeManager.leaveScope(node.recordDeclaration!!)
        }

        // Set default argument evaluation nodes
        val funcDeclNextEOG = node.nextEOG
        currentPredecessors.clear()
        currentPredecessors.add(node)
        var defaultArg: Expression? = null
        for (paramVariableDeclaration in node.parameters) {
            if (paramVariableDeclaration.default != null) {
                defaultArg = paramVariableDeclaration.default
                pushToEOG(defaultArg!!)
                currentPredecessors.clear()
                currentPredecessors.add(defaultArg)
                currentPredecessors.add(node)
            }
        }
        if (defaultArg != null) {
            for (nextEOG in funcDeclNextEOG) {
                currentPredecessors.clear()
                currentPredecessors.add(defaultArg)
                pushToEOG(nextEOG)
            }
        }
        currentPredecessors.clear()
    }

    /**
     * Tries to create the necessary EOG edges for the [node] (if it is non-null) by looking up the
     * appropriate handler function of the node's class in [map] and calling it.
     */
    private fun createEOG(node: Node?) {
        if (node == null) {
            // nothing to do
            return
        }

        intermediateNodes.add(node)
        var toHandle: Class<*> = node.javaClass
        var callable = map[toHandle]
        while (callable == null) {
            toHandle = toHandle.superclass
            callable = map[toHandle]
            if (toHandle == Node::class.java || !Node::class.java.isAssignableFrom(toHandle)) break
        }
        if (callable != null) {
            callable(node)
        } else {
            LOGGER.info("Parsing of type ${node.javaClass} is not supported (yet)")
        }
    }

    protected fun handleDefault(node: Node) {
        pushToEOG(node)
    }

    protected fun handleCallExpression(node: CallExpression) {
        // Todo add call as throwexpression to outer scope of call can throw (which is trivial to
        // find out for java, but impossible for c++)

        // evaluate the call target first, optional base should be the callee or in its subtree
        node.callee?.let { createEOG(it) }

        // then the arguments
        for (arg in node.arguments) {
            createEOG(arg)
        }
        // finally the call itself
        pushToEOG(node)
    }

    protected fun handleMemberExpression(node: MemberExpression) {
        createEOG(node.base)
        pushToEOG(node)
    }

    protected fun handleArraySubscriptionExpression(node: ArraySubscriptionExpression) {
        // Connect according to evaluation order, first the array reference, then the contained
        // index.
        createEOG(node.arrayExpression)
        createEOG(node.subscriptExpression)
        pushToEOG(node)
    }

    protected fun handleArrayCreationExpression(node: ArrayCreationExpression) {
        for (dimension in node.dimensions) {
            createEOG(dimension)
        }
        createEOG(node.initializer)
        pushToEOG(node)
    }

    protected fun handleRangeExpression(node: RangeExpression) {
        createEOG(node.floor)
        createEOG(node.ceiling)
        createEOG(node.third)
        pushToEOG(node)
    }

    protected fun handleDeclarationStatement(node: DeclarationStatement) {
        // loop through declarations
        for (declaration in node.declarations) {
            if (declaration is VariableDeclaration) {
                // analyze the initializers if there is one
                createEOG(declaration)
            } else if (declaration is FunctionDeclaration) {
                // save the current EOG stack, because we can have a function declaration within an
                // existing function and the EOG handler for handling function declarations will
                // reset the
                // stack
                val oldEOG = ArrayList(currentPredecessors)

                // analyze the defaults
                createEOG(declaration)

                // reset the oldEOG stack
                currentPredecessors = oldEOG
            }
        }

        // push statement itself
        pushToEOG(node)
    }

    protected fun handleReturnStatement(node: ReturnStatement) {
        // analyze the return value
        createEOG(node.returnValue)

        // push the statement itself
        pushToEOG(node)

        // reset the state afterwards, we're done with this function
        currentPredecessors.clear()
    }

    protected fun handleBinaryOperator(node: BinaryOperator) {
        createEOG(node.lhs)
        val lang = node.language
        // Two operators that don't evaluate the second operator if the first evaluates to a certain
        // value. If the language has the trait of short-circuit evaluation, we check if the
        // operatorCode is amongst the operators that leed such an evaluation.
        if (
            lang != null &&
                lang is HasShortCircuitOperators &&
                (lang.conjunctiveOperators.contains(node.operatorCode) ||
                    lang.disjunctiveOperators.contains(node.operatorCode))
        ) {
            val shortCircuitNodes = mutableListOf<Node>()
            shortCircuitNodes.addAll(currentPredecessors)
            // Adds true or false depending on whether a conjunctive or disjunctive operator is
            // present.
            // If it is not a conjunctive operator, the check above implies it is a disjunctive
            // operator.
            nextEdgeProperties[Properties.BRANCH] =
                lang.conjunctiveOperators.contains(node.operatorCode)
            createEOG(node.rhs)
            pushToEOG(node)
            setCurrentEOGs(shortCircuitNodes)
            // Inverted property to assigne false when true was assigned above.
            nextEdgeProperties[Properties.BRANCH] =
                !lang.conjunctiveOperators.contains(node.operatorCode)
        } else {
            createEOG(node.rhs)
        }
        pushToEOG(node)
    }

    protected fun handleAssignExpression(node: AssignExpression) {
        for (declaration in node.declarations) {
            createEOG(declaration)
        }

        // Handle left hand side(s) first
        node.lhs.forEach { createEOG(it) }

        // Then the right side(s)
        node.rhs.forEach { createEOG(it) }

        pushToEOG(node)
    }

    protected fun handleCompoundStatement(node: CompoundStatement) {
        // not all language handle compound statements as scoping blocks, so we need to avoid
        // creating new scopes here
        scopeManager.enterScopeIfExists(node)

        // analyze the contained statements
        for (child in node.statements) {
            createEOG(child)
        }
        if (scopeManager.currentScope is BlockScope) {
            scopeManager.leaveScope(node)
        }
        pushToEOG(node)
    }

    protected fun handleUnaryOperator(node: UnaryOperator) {
        val input = node.input
        createEOG(input)
        if (node.operatorCode == "throw") {
            val catchingScope =
                scopeManager.firstScopeOrNull { scope ->
                    scope is TryScope || scope is FunctionScope
                }

            val throwType = input.type
            pushToEOG(node)
            if (catchingScope is TryScope) {
                catchingScope.catchesOrRelays[throwType] = ArrayList(currentPredecessors)
            } else if (catchingScope is FunctionScope) {
                catchingScope.catchesOrRelays[throwType] = ArrayList(currentPredecessors)
            }
            currentPredecessors.clear()
        } else {
            pushToEOG(node)
        }
    }

    protected fun handleCompoundStatementExpression(node: CompoundStatementExpression) {
        createEOG(node.statement)
        pushToEOG(node)
    }

    protected fun handleAssertStatement(node: AssertStatement) {
        createEOG(node.condition)
        val openConditionEOGs = ArrayList(currentPredecessors)
        createEOG(node.message)
        setCurrentEOGs(openConditionEOGs)
        pushToEOG(node)
    }

    protected fun handleTryStatement(node: TryStatement) {
        scopeManager.enterScope(node)
        val tryScope = scopeManager.currentScope as TryScope?

        node.resources.forEach { createEOG(it) }

        createEOG(node.tryBlock)
        val tmpEOGNodes = ArrayList(currentPredecessors)
        val catchesOrRelays = tryScope!!.catchesOrRelays
        for (catchClause in node.catchClauses) {
            currentPredecessors.clear()
            // Try to catch all internally thrown exceptions under the catching clause and remove
            // caught ones
            val toRemove = mutableSetOf<Type>()
            for ((throwType, eogEdges) in catchesOrRelays) {
                if (catchClause.parameter == null) { // e.g. catch (...)
                    currentPredecessors.addAll(eogEdges)
                } else if (
                    TypeManager.getInstance()
                        .isSupertypeOf(catchClause.parameter!!.type, throwType, node)
                ) {
                    currentPredecessors.addAll(eogEdges)
                    toRemove.add(throwType)
                }
            }
            toRemove.forEach { catchesOrRelays.remove(it) }
            createEOG(catchClause.body)
            tmpEOGNodes.addAll(currentPredecessors)
        }
        val canTerminateExceptionfree = tmpEOGNodes.any { reachableFromValidEOGRoot(it) }
        currentPredecessors.clear()
        currentPredecessors.addAll(tmpEOGNodes)
        // connect all try-block, catch-clause and uncaught throws eog points to finally start if
        // finally exists
        if (node.finallyBlock != null) {
            // extends current EOG by all value EOG from open throws
            currentPredecessors.addAll(catchesOrRelays.entries.flatMap { (_, value) -> value })
            createEOG(node.finallyBlock)

            //  all current-eog edges , result of finally execution as value List of uncaught
            // catchesOrRelaysThrows
            for ((_, value) in catchesOrRelays) {
                value.clear()
                value.addAll(currentPredecessors)
            }
        }
        // Forwards all open and uncaught throwing nodes to the outer scope that may handle them
        val outerScope =
            scopeManager.firstScopeOrNull(scopeManager.currentScope!!.parent) { scope: Scope? ->
                scope is TryScope || scope is FunctionScope
            }
        if (outerScope != null) {
            val outerCatchesOrRelays =
                if (outerScope is TryScope) outerScope.catchesOrRelays
                else (outerScope as FunctionScope).catchesOrRelays
            for ((key, value) in catchesOrRelays) {
                val catches = outerCatchesOrRelays[key] ?: ArrayList()
                catches.addAll(value)
                outerCatchesOrRelays[key] = catches
            }
        }
        scopeManager.leaveScope(node)
        // To Avoid edges out of the "finally" block to the next regular statement.
        if (!canTerminateExceptionfree) {
            currentPredecessors.clear()
        }
        pushToEOG(node)
    }

    protected fun handleContinueStatement(node: ContinueStatement) {
        pushToEOG(node)
        scopeManager.addContinueStatement(node)
        currentPredecessors.clear()
    }

    protected fun handleDeleteExpression(node: DeleteExpression) {
        createEOG(node.operand)
        pushToEOG(node)
    }

    protected fun handleBreakStatement(node: BreakStatement) {
        pushToEOG(node)
        scopeManager.addBreakStatement(node)
        currentPredecessors.clear()
    }

    protected fun handleLabelStatement(node: LabelStatement) {
        scopeManager.addLabelStatement(node)
        createEOG(node.subStatement)
    }

    protected fun handleGotoStatement(node: GotoStatement) {
        pushToEOG(node)
        if (node.targetLabel != null) {
            processedListener.registerObjectListener(node.targetLabel!!) { _: Any?, to: Any? ->
                addEOGEdge(node, to as Node)
            }
        }
        currentPredecessors.clear()
    }

    protected fun handleCaseStatement(node: CaseStatement) {
        createEOG(node.caseExpression)
        pushToEOG(node)
    }

    protected fun handleNewExpression(node: NewExpression) {
        createEOG(node.initializer)
        pushToEOG(node)
    }

    protected fun handleKeyValueExpression(node: KeyValueExpression) {
        createEOG(node.key)
        createEOG(node.value)
        pushToEOG(node)
    }

    protected fun handleCastExpression(node: CastExpression) {
        createEOG(node.expression)
        pushToEOG(node)
    }

    protected fun handleExpressionList(node: ExpressionList) {
        for (expr in node.expressions) {
            createEOG(expr)
        }
        pushToEOG(node)
    }

    protected fun handleInitializerListExpression(node: InitializerListExpression) {
        // first the arguments
        for (inits in node.initializers) {
            createEOG(inits)
        }
        pushToEOG(node)
    }

    protected fun handleConstructExpression(node: ConstructExpression) {
        // first the arguments
        for (arg in node.arguments) {
            createEOG(arg)
        }
        pushToEOG(node)
    }

    /**
     * Creates an EOG-edge between the given argument node and the saved currentEOG Edges.
     *
     * @param node node that gets the incoming edge
     */
    fun pushToEOG(node: Node) {
        LOGGER.trace("Pushing ${node.javaClass.simpleName} $node to EOG")
        for (intermediate in intermediateNodes) {
            processedListener.process(intermediate, node)
        }
        addMultipleIncomingEOGEdges(currentPredecessors, node)
        intermediateNodes.clear()
        currentPredecessors.clear()
        nextEdgeProperties.clear()
        currentPredecessors.add(node)
    }

    fun setCurrentEOGs(nodes: List<Node>) {
        LOGGER.trace("Setting $nodes to EOGs")
        currentPredecessors = ArrayList(nodes)
    }

    /**
     * Connects the current EOG leaf nodes to the last stacked node, e.g. loop head, and removes the
     * nodes.
     *
     * @param loopStatement the loop statement
     * @param loopScope the loop scope
     */
    protected fun exitLoop(loopStatement: Statement, loopScope: LoopScope) {
        // Breaks are connected to the NEXT EOG node and therefore temporarily stored after the loop
        // context is destroyed
        currentPredecessors.addAll(loopScope.breakStatements)
        val continues = ArrayList(loopScope.continueStatements)
        if (continues.isNotEmpty()) {
            val conditions =
                loopScope.conditions.map { SubgraphWalker.getEOGPathEdges(it).entries }.flatten()
            conditions.forEach { node -> addMultipleIncomingEOGEdges(continues, node) }
        }
    }

    /**
     * Connects current EOG nodes to the previously saved loop start to mimic control flow of loops
     */
    protected fun connectCurrentToLoopStart() {
        val loopScope = scopeManager.firstScopeOrNull { it is LoopScope } as? LoopScope
        if (loopScope == null) {
            LOGGER.error("I am unexpectedly not in a loop, cannot add edge to loop start")
            return
        }
        loopScope.starts.forEach { node -> addMultipleIncomingEOGEdges(currentPredecessors, node) }
    }

    /**
     * Builds an EOG edge from prev to next. 'eogDirection' defines how the node instances save the
     * references constituting the edge. 'FORWARD': only the nodes nextEOG member contains
     * references, an points to the next nodes. 'BACKWARD': only the nodes prevEOG member contains
     * references and points to the previous nodes. 'BIDIRECTIONAL': nextEOG and prevEOG contain
     * references and point to the previous and the next nodes.
     *
     * @param prev the previous node
     * @param next the next node
     */
    private fun addEOGEdge(prev: Node, next: Node) {
        val propertyEdge = PropertyEdge(prev, next)
        propertyEdge.addProperties(nextEdgeProperties)
        propertyEdge.addProperty(Properties.INDEX, prev.nextEOG.size)
        propertyEdge.addProperty(Properties.UNREACHABLE, false)
        prev.addNextEOG(propertyEdge)
        next.addPrevEOG(propertyEdge)
    }

    private fun addMultipleIncomingEOGEdges(prevs: List<Node>, next: Node) {
        prevs.forEach { prev -> addEOGEdge(prev, next) }
    }

    protected fun handleSynchronizedStatement(node: SynchronizedStatement) {
        createEOG(node.expression)
        pushToEOG(node)
        createEOG(node.blockStatement)
    }

    protected fun handleConditionalExpression(node: ConditionalExpression) {
        val openBranchNodes = mutableListOf<Node>()
        createEOG(node.condition)
        // To have semantic information after the condition evaluation
        pushToEOG(node)
        val openConditionEOGs = ArrayList(currentPredecessors)
        nextEdgeProperties[Properties.BRANCH] = true
        createEOG(node.thenExpr)
        openBranchNodes.addAll(currentPredecessors)
        setCurrentEOGs(openConditionEOGs)
        nextEdgeProperties[Properties.BRANCH] = false
        createEOG(node.elseExpr)
        openBranchNodes.addAll(currentPredecessors)
        setCurrentEOGs(openBranchNodes)
    }

    protected fun handleDoStatement(node: DoStatement) {
        scopeManager.enterScope(node)
        createEOG(node.statement)
        createEOG(node.condition)
        node.condition?.let { node.addPrevDFG(it) }
        pushToEOG(node) // To have semantic information after the condition evaluation
        nextEdgeProperties[Properties.BRANCH] = true
        connectCurrentToLoopStart()
        nextEdgeProperties[Properties.BRANCH] = false
        val currentLoopScope = scopeManager.leaveScope(node) as LoopScope?
        if (currentLoopScope != null) {
            exitLoop(node, currentLoopScope)
        } else {
            LOGGER.error("Trying to exit do loop, but no loop scope: $node")
        }
    }

    protected fun handleForEachStatement(node: ForEachStatement) {
        scopeManager.enterScope(node)
        createEOG(node.iterable)
        createEOG(node.variable)
        node.variable?.let { node.addPrevDFG(it) }
        pushToEOG(node) // To have semantic information after the variable declaration
        nextEdgeProperties[Properties.BRANCH] = true
        val tmpEOGNodes = ArrayList(currentPredecessors)
        createEOG(node.statement)
        connectCurrentToLoopStart()
        currentPredecessors.clear()
        val currentLoopScope = scopeManager.leaveScope(node) as LoopScope?
        if (currentLoopScope != null) {
            exitLoop(node, currentLoopScope)
        } else {
            LOGGER.error("Trying to exit foreach loop, but not in loop scope: $node")
        }
        currentPredecessors.addAll(tmpEOGNodes)
        nextEdgeProperties[Properties.BRANCH] = false
    }

    protected fun handleForStatement(node: ForStatement) {
        scopeManager.enterScope(node)
        createEOG(node.initializerStatement)
        createEOG(node.conditionDeclaration)
        createEOG(node.condition)

        pushToEOG(node) // To have semantic information after the condition evaluation
        nextEdgeProperties[Properties.BRANCH] = true
        val tmpEOGNodes = ArrayList(currentPredecessors)

        createEOG(node.statement)
        createEOG(node.iterationStatement)

        connectCurrentToLoopStart()

        currentPredecessors.clear()
        val currentLoopScope = scopeManager.leaveScope(node) as LoopScope?
        if (currentLoopScope != null) {
            exitLoop(node, currentLoopScope)
        } else {
            LOGGER.error("Trying to exit for loop, but no loop scope: $node")
        }
        currentPredecessors.addAll(tmpEOGNodes)
        nextEdgeProperties[Properties.BRANCH] = false
    }

    protected fun handleIfStatement(node: IfStatement) {
        val openBranchNodes = mutableListOf<Node>()
        scopeManager.enterScopeIfExists(node)
        createEOG(node.initializerStatement)
        createEOG(node.conditionDeclaration)
        createEOG(node.condition)
        pushToEOG(node) // To have semantic information after the condition evaluation
        val openConditionEOGs = ArrayList(currentPredecessors)
        nextEdgeProperties[Properties.BRANCH] = true
        createEOG(node.thenStatement)
        openBranchNodes.addAll(currentPredecessors)
        if (node.elseStatement != null) {
            setCurrentEOGs(openConditionEOGs)
            nextEdgeProperties[Properties.BRANCH] = false
            createEOG(node.elseStatement)
            openBranchNodes.addAll(currentPredecessors)
        } else {
            openBranchNodes.addAll(openConditionEOGs)
        }
        scopeManager.leaveScope(node)
        setCurrentEOGs(openBranchNodes)
    }

    protected fun handleSwitchStatement(node: SwitchStatement) {
        scopeManager.enterScopeIfExists(node)
        createEOG(node.initializerStatement)
        createEOG(node.selectorDeclaration)
        createEOG(node.selector)
        pushToEOG(node) // To have semantic information after the condition evaluation
        val tmp = ArrayList(currentPredecessors)
        val compound =
            if (node.statement is DoStatement) {
                createEOG(node.statement)
                (node.statement as DoStatement).statement as CompoundStatement
            } else {
                node.statement as CompoundStatement
            }
        currentPredecessors = ArrayList()
        for (subStatement in compound.statements) {
            if (subStatement is CaseStatement || subStatement is DefaultStatement) {
                currentPredecessors.addAll(tmp)
            }
            createEOG(subStatement)
        }
        pushToEOG(compound)
        val switchScope = scopeManager.leaveScope(node) as SwitchScope?
        if (switchScope != null) {
            currentPredecessors.addAll(switchScope.breakStatements)
        } else {
            LOGGER.error("Handling switch statement, but not in switch scope: $node")
        }
    }

    protected fun handleWhileStatement(node: WhileStatement) {
        scopeManager.enterScope(node)
        createEOG(node.conditionDeclaration)
        createEOG(node.condition)
        pushToEOG(node) // To have semantic information after the condition evaluation
        nextEdgeProperties[Properties.BRANCH] = true
        val tmpEOGNodes = ArrayList(currentPredecessors)
        createEOG(node.statement)
        connectCurrentToLoopStart()

        // Replace current EOG nodes without triggering post setEOG ... processing
        currentPredecessors.clear()
        val currentLoopScope = scopeManager.leaveScope(node) as LoopScope?
        if (currentLoopScope != null) {
            exitLoop(node, currentLoopScope)
        } else {
            LOGGER.error("Trying to exit while loop, but no loop scope: $node")
        }
        currentPredecessors.addAll(tmpEOGNodes)
        nextEdgeProperties[Properties.BRANCH] = false
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EvaluationOrderGraphPass::class.java)

        /**
         * Searches backwards in the EOG on whether there is a path from a function declaration to
         * the given node. After the construction phase, some unreachable nodes may have EOG edges.
         * This function also serves to truncate the EOG graph by unreachable paths.
         *
         * @param node
         * - That lies on the reachable or unreachable path
         *
         * @return true if the node can bea reached from a function declaration
         */
        protected fun reachableFromValidEOGRoot(node: Node): Boolean {
            val passedBy = mutableSetOf<Node>()
            val workList = ArrayList(node.prevEOG)
            while (workList.isNotEmpty()) {
                val toProcess = workList[0]
                workList.remove(toProcess)
                passedBy.add(toProcess)
                if (toProcess is FunctionDeclaration) {
                    return true
                }
                for (pred in toProcess.prevEOG) {
                    if (pred !in passedBy && pred !in workList) {
                        workList.add(pred)
                    }
                }
            }
            return false
        }

        /**
         * Checks if every node that has another node in its next or previous EOG List is also
         * contained in that nodes previous or next EOG list to ensure the bidirectionality of the
         * relation in both lists.
         *
         * @param n
         * @return
         */
        fun checkEOGInvariant(n: Node): Boolean {
            val allNodes = SubgraphWalker.flattenAST(n)
            var ret = true
            for (node in allNodes) {
                for (next in node.nextEOG) {
                    if (node !in next.prevEOG) {
                        LOGGER.warn(
                            "Violation to EOG invariant found: Node $node does not have a back-reference from his EOG-successor $next."
                        )
                        ret = false
                    }
                }
                for (prev in node.prevEOG) {
                    if (node !in prev.nextEOG) {
                        LOGGER.warn(
                            "Violation to EOG invariant found: Node $node does not have a reference from his EOG-predecessor $prev."
                        )
                        ret = false
                    }
                }
            }
            return ret
        }
    }
}
