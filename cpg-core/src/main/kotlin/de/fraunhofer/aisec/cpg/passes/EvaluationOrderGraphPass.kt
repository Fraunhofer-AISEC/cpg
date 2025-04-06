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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.CastNotPossible
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.ProcessedListener
import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.StatementHolder
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.tryCast
import java.util.*
import org.slf4j.LoggerFactory

/**
 * Creates an Evaluation Order Graph (EOG) based on the CPG's version of the AST. The expected
 * outcomes are specified in the
 * [Specification](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog).
 *
 * Note: If you changed this file, make sure the specification is still in-line with the
 * implementation. If you support new nodes, add a section to the specification.
 *
 * An EOG is an intra-procedural directed graph whose vertices are executable AST nodes and edges
 * connect them in the order they would be executed when running the program.
 *
 * An EOG always starts at the header of a method/function and ends in one (virtual) or multiple
 * return statements. A virtual return statement with a code location of (-1,-1) is used if the
 * actual source code does not have an explicit return statement.
 *
 * How to use: When constructing the eog for a new CPG AST-node, first the EOG should be constructed
 * for its subtrees, in the order the subtrees are evaluated. This is done by invoking the handler
 * function [handleEOG] on the children of the current node in the appropriate order. After the
 * AST-subtrees of the children are attached to the EOG, the current node has to be attached with
 * [attachToEOG], which simply constructs an EOG-edge from the [currentPredecessors] to the node,
 * and saves the node as the new [currentPredecessors]. Note that some handlers deviate from this
 * order and attach the current root node after a condition and before the other subtrees
 * constituted by its child nodes to represent branching. Nodes that manipulate the control flow of
 * a program have to be handled with more care, by adding and removing nodes from
 * [currentPredecessors] or even temporarily save and restore the valid eog exits of an ast subtree,
 * e.g. [IfStatement].
 *
 * The EOG is similar to the CFG `ControlFlowGraphPass`, but there are some subtle differences:
 * * For methods without explicit return statement, EOF will have an edge to a virtual return node
 *   with line number -1 which does not exist in the original code. A CFG will always end with the
 *   last reachable statement(s) and not insert any virtual return statements.
 * * EOG considers an opening blocking ("Block", indicated by a "{") as a separate node. A CFG will
 *   rather use the first actual executable statement within the block.
 * * For IF statements, EOG treats the "if" keyword and the condition as separate nodes. CFG treats
 *   this as one "if" statement.
 * * EOG considers a method header as a node. CFG will consider the first executable statement of
 *   the methods as a node.
 *
 * Its handleXXX functions are intentionally set as `protected`, in case someone wants to extend
 * this pass and fine-tune it.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class EvaluationOrderGraphPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    protected var currentPredecessors = mutableListOf<Node>()
    protected var nextEdgeBranch: Boolean? = null

    /**
     * This maps nodes that have to handle throws, i.e. [TryStatement] and [FunctionDeclaration], to
     * the [Type]s of errors that were thrown and the EOG exits of the throwing statements. Entries
     * to the outer map will only be created if the node was identified to handle or relay a throw.
     * Entries to the inner throw will only be created when the mapping type was thrown.
     */
    val nodesToInternalThrows = mutableMapOf<Node, MutableMap<Type, MutableList<Node>>>()

    /**
     * This maps nodes that have to handle [BreakStatement]s and [ContinueStatement]s, i.e.
     * [LoopStatement]s and [SwitchStatement]s to the EOG exits of the node they have to handle. An
     * entry will only be created if the statement was identified to handle the above-mentioned
     * control flow statements.
     */
    val nodesWithContinuesAndBreaks = mutableMapOf<Node, MutableList<Node>>()

    /**
     * Allows to register EOG creation logic when a currently visited node can depend on future
     * visited nodes. Currently used to connect goto statements and the target labeled statements.
     * Implemented as listener to connect nodes when the goto appears before the label.
     */
    protected val processedListener = ProcessedListener()
    /**
     * Stores all nodes currently handled to add them to the processedListener even if a sub node is
     * the next target of an EOG edge.
     */
    protected val intermediateNodes = mutableListOf<Node>()

    protected fun doNothing() {
        // Nothing to do for this node type
    }

    override fun cleanup() {
        intermediateNodes.clear()
        currentPredecessors.clear()
    }

    override fun accept(tu: TranslationUnitDeclaration) {
        handleEOG(tu)
        removeUnreachableEOGEdges(tu)
    }

    /**
     * Removes EOG edges by first building the negative set of nodes that cannot be visited and then
     * remove their outgoing edges. This also removes cycles.
     */
    protected fun removeUnreachableEOGEdges(tu: TranslationUnitDeclaration) {
        // All nodes which have an eog edge
        val eogNodes = IdentitySet<Node>()
        eogNodes.addAll(
            SubgraphWalker.flattenAST(tu).filter {
                it.prevEOGEdges.isNotEmpty() || it.nextEOGEdges.isNotEmpty()
            }
        )
        // only eog entry points
        var validStarts =
            eogNodes.filter { it is EOGStarterHolder || it is VariableDeclaration }.toSet()
        // Remove all nodes from eogNodes which are reachable from validStarts and transitively.
        val alreadySeen = IdentitySet<Node>()
        while (validStarts.isNotEmpty()) {
            eogNodes.removeAll(validStarts)
            validStarts =
                validStarts
                    .flatMap { it.nextEOGEdges }
                    .filter { it.end !in alreadySeen }
                    .map { it.end }
                    .toSet()
            alreadySeen.addAll(validStarts)
        }
        // The remaining nodes are unreachable from the entry points. We delete their outgoing EOG
        // edges.
        for (unvisitedNode in eogNodes) {
            unvisitedNode.nextEOGEdges.clear()
        }
    }

    /**
     * See
     * [Specification for StatementHolder](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#statementholder)
     */
    protected fun handleTranslationUnitDeclaration(node: TranslationUnitDeclaration) {
        handleStatementHolder(node as StatementHolder)

        // loop through functions
        for (child in node.declarations) {
            currentPredecessors.clear()
            handleEOG(child)
        }
        processedListener.clearProcessed()
    }

    /**
     * See
     * [Specification for StatementHolder](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#statementholder)
     */
    protected fun handleNamespaceDeclaration(node: NamespaceDeclaration) {
        handleStatementHolder(node)

        // loop through functions
        for (child in node.declarations) {
            currentPredecessors.clear()
            handleEOG(child)
        }
        processedListener.clearProcessed()
    }

    /**
     * See
     * [Specification for VariableDeclaration](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#variabledeclaration)
     */
    protected fun handleVariableDeclaration(node: VariableDeclaration) {
        attachToEOG(node)
        // analyze the initializer
        handleEOG(node.initializer)
    }

    /**
     * See
     * [Specification for TupleDeclaration](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#tupledeclaration)
     */
    protected fun handleTupleDeclaration(node: TupleDeclaration) {
        attachToEOG(node)
        // analyze the initializer
        handleEOG(node.initializer)
    }

    /**
     * See
     * [Specification for StatementHolder](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#statementholder)
     */
    protected open fun handleRecordDeclaration(node: RecordDeclaration) {
        handleStatementHolder(node)
        currentPredecessors.clear()
        for (constructor in node.constructors) {
            handleEOG(constructor)
        }
        for (method in node.methods) {
            handleEOG(method)
        }
        for (fields in node.fields) {
            handleEOG(fields)
        }
        for (records in node.records) {
            handleEOG(records)
        }
    }

    /**
     * See
     * [Specification for StatementHolder](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#statementholder)
     */
    protected fun handleStatementHolder(statementHolder: StatementHolder) {
        // separate code into static and non-static parts as they are executed in different moments,
        // although they can be placed in the same enclosing declaration.
        val code = statementHolder.statements

        val staticCode = code.filter { (it as? Block)?.isStaticBlock == true }
        val nonStaticCode = code.filter { it !in staticCode }

        attachToEOG(statementHolder as Node)
        for (staticStatement in staticCode) {
            handleEOG(staticStatement)
        }
        currentPredecessors.clear()
        attachToEOG(statementHolder as Node)
        for (nonStaticStatement in nonStaticCode) {
            handleEOG(nonStaticStatement)
        }
        currentPredecessors.clear()
    }

    /**
     * See
     * [Specification for LambdaExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#lambdaexpression)
     */
    protected fun handleLambdaExpression(node: LambdaExpression) {
        val tmpCurrentEOG = currentPredecessors.toMutableList()
        val tmpCurrentProperties = nextEdgeBranch
        val tmpIntermediateNodes = intermediateNodes.toMutableList()

        nextEdgeBranch = null
        currentPredecessors.clear()
        intermediateNodes.clear()

        handleEOG(node.function)

        nextEdgeBranch = null
        currentPredecessors.clear()
        intermediateNodes.clear()

        nextEdgeBranch = tmpCurrentProperties
        currentPredecessors.addAll(tmpCurrentEOG)
        intermediateNodes.addAll(tmpIntermediateNodes)

        attachToEOG(node)
    }

    /**
     * See
     * [Specification for FunctionDeclaration](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#functiondeclaration)
     */
    protected open fun handleFunctionDeclaration(node: FunctionDeclaration) {
        // reset EOG
        currentPredecessors.clear()
        // push the function declaration
        attachToEOG(node)

        // analyze the body
        handleEOG(node.body)

        val uncaughtEOGThrows = nodesToInternalThrows[node]?.values?.flatten() ?: listOf()
        // Connect uncaught throws to block node
        node.body?.let { addMultipleIncomingEOGEdges(uncaughtEOGThrows, it) }

        // Set default argument evaluation nodes
        val funcDeclNextEOG = node.nextEOG
        currentPredecessors.clear()
        currentPredecessors.add(node)
        var defaultArg: Expression? = null
        for (paramVariableDeclaration in node.parameters) {
            paramVariableDeclaration.default?.let {
                defaultArg = it
                attachToEOG(it)
                currentPredecessors.clear()
                currentPredecessors.add(it)
                currentPredecessors.add(node)
            }
        }
        defaultArg?.let {
            for (nextEOG in funcDeclNextEOG) {
                currentPredecessors.clear()
                currentPredecessors.add(it)
                attachToEOG(nextEOG)
            }
        }
        currentPredecessors.clear()
    }

    /**
     * Tries to create the necessary EOG edges for the [node] (if it is non-null) by looking up the
     * appropriate handler function of the node's class in [map] and calling it. The EOG is build
     * for the entire ast subtree represented by node, and when this function returns the EOG in
     * this subtree can be connected to other trees by invoking this function on them. The nodes
     * stored in [currentPredecessors] contain the valid EOG exits of the subtree that will be
     * connected to the next handled subtree. Adding or removing nodes from the list allows for
     * custom adaptation of control flow behavior when handling nodes that influence control flow,
     * e.g. [LoopStatement]s or [BreakStatement].
     */
    protected fun handleEOG(node: Node?) {
        if (node == null) {
            return
        }
        intermediateNodes.add(node)

        when (node) {
            is TranslationUnitDeclaration -> handleTranslationUnitDeclaration(node)
            is NamespaceDeclaration -> handleNamespaceDeclaration(node)
            is RecordDeclaration -> handleRecordDeclaration(node)
            is FunctionDeclaration -> handleFunctionDeclaration(node)
            is TupleDeclaration -> handleTupleDeclaration(node)
            is VariableDeclaration -> handleVariableDeclaration(node)
            is ConstructExpression -> handleConstructExpression(node)
            is CallExpression -> handleCallExpression(node)
            is MemberExpression -> handleMemberExpression(node)
            is SubscriptExpression -> handleSubscriptExpression(node)
            is NewArrayExpression -> handleNewArrayExpression(node)
            is RangeExpression -> handleRangeExpression(node)
            is DeclarationStatement -> handleDeclarationStatement(node)
            is ReturnStatement -> handleReturnStatement(node)
            is BinaryOperator -> handleBinaryOperator(node)
            is AssignExpression -> handleAssignExpression(node)
            is UnaryOperator -> handleUnaryOperator(node)
            is Block -> handleBlock(node)
            is IfStatement -> handleIfStatement(node)
            is AssertStatement -> handleAssertStatement(node)
            is WhileStatement -> handleWhileStatement(node)
            is DoStatement -> handleDoStatement(node)
            is ForStatement -> handleForStatement(node)
            is ForEachStatement -> handleForEachStatement(node)
            is TypeExpression -> handleTypeExpression(node)
            is TryStatement -> handleTryStatement(node)
            is ContinueStatement -> handleContinueStatement(node)
            is DeleteExpression -> handleDeleteExpression(node)
            is BreakStatement -> handleBreakStatement(node)
            is SwitchStatement -> handleSwitchStatement(node)
            is LabelStatement -> handleLabelStatement(node)
            is GotoStatement -> handleGotoStatement(node)
            is CaseStatement -> handleCaseStatement(node)
            is SynchronizedStatement -> handleSynchronizedStatement(node)
            is NewExpression -> handleNewExpression(node)
            is KeyValueExpression -> handleKeyValueExpression(node)
            is CastExpression -> handleCastExpression(node)
            is ExpressionList -> handleExpressionList(node)
            is ConditionalExpression -> handleConditionalExpression(node)
            is InitializerListExpression -> handleInitializerListExpression(node)
            is CollectionComprehension -> handleCollectionComprehension(node)
            is ComprehensionExpression -> handleComprehensionExpression(node)
            is LambdaExpression -> handleLambdaExpression(node)
            is LookupScopeStatement -> handleLookupScopeStatement(node)
            is ThrowExpression -> handleThrowExpression(node)
            // For templates, we will just handle the declarations and not the realizations (for
            // now)
            is TemplateDeclaration -> handleTemplate(node)
            // These nodes will be added to the eog graph but no children will be handled
            is EmptyStatement -> handleDefault(node)
            is Literal<*> -> handleDefault(node)
            is DefaultStatement -> handleDefault(node)
            is TypeIdExpression -> handleDefault(node)
            is PointerDereference -> handlePointerDereference(node)
            is PointerReference -> handlePointerReference(node)
            is Reference -> handleDefault(node)
            is ImportDeclaration -> handleDefault(node)
            // These nodes are not added to the EOG
            is IncludeDeclaration -> doNothing()
            else -> LOGGER.info("Parsing of type ${node.javaClass} is not supported (yet)")
        }
    }

    protected fun handleTemplate(template: TemplateDeclaration) {
        // Handle the declarations
        for (decl in template.declarations) {
            handleEOG(decl)
        }

        // Finally the template itself
        attachToEOG(template)
    }

    protected fun handlePointerReference(node: PointerReference) {
        handleEOG(node.input)

        attachToEOG(node)
    }

    protected fun handlePointerDereference(node: PointerDereference) {
        handleEOG(node.input)

        attachToEOG(node)
    }

    /**
     * Default handler for nodes. The node is simply attached to the EOG and the ast subtree is
     * ignored.
     */
    protected fun handleDefault(node: Node) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for EmptyStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#emptystatement)
     */
    private fun handleEmptyStatement(node: EmptyStatement) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for Literal](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#literal)
     */
    private fun handleLiteral(node: Literal<*>) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for DefaultStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#defaultstatement)
     */
    private fun handleDefaultStatement(node: DefaultStatement) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for TypeIdExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#typeidexpression)
     */
    private fun handleTypeIdExpression(node: TypeIdExpression) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for Reference](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#reference)
     */
    private fun handleReference(node: Reference) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for IncludeDeclaration](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#includedeclaration)
     */
    protected fun handleIncludeDeclaration() {
        doNothing()
    }

    /**
     * See
     * [Specification for CallExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#callexpression)
     */
    protected fun handleCallExpression(node: CallExpression) {
        // Todo add call as throwexpression to outer scope of call can throw (which is trivial to
        // find out for java, but impossible for c++)

        // evaluate the call target first, optional base should be the callee or in its subtree
        handleEOG(node.callee)

        // then the arguments
        for (arg in node.arguments) {
            handleEOG(arg)
        }
        // finally the call itself
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for MemberExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#memberexpression)
     */
    protected fun handleMemberExpression(node: MemberExpression) {
        handleEOG(node.base)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for SubscriptExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#subscriptexpression)
     */
    protected fun handleSubscriptExpression(node: SubscriptExpression) {
        // Connect according to evaluation order, first the array reference, then the contained
        // index.
        handleEOG(node.arrayExpression)
        handleEOG(node.subscriptExpression)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for NewArrayExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#newarrayexpression)
     */
    protected fun handleNewArrayExpression(node: NewArrayExpression) {
        for (dimension in node.dimensions) {
            handleEOG(dimension)
        }
        handleEOG(node.initializer)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for RangeExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#rangeexpression)
     */
    protected fun handleRangeExpression(node: RangeExpression) {
        handleEOG(node.floor)
        handleEOG(node.ceiling)
        handleEOG(node.third)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for DeclarationStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#declarationexpression)
     */
    protected fun handleDeclarationStatement(node: DeclarationStatement) {
        // loop through declarations
        for (declaration in node.declarations) {
            if (declaration is ImportDeclaration) {
                handleEOG(declaration)
            } else if (declaration is VariableDeclaration) {
                // analyze the initializers if there is one
                handleEOG(declaration)
            } else if (declaration is FunctionDeclaration) {
                // save the current EOG stack, because we can have a function declaration within an
                // existing function and the EOG handler for handling function declarations will
                // reset the
                // stack
                val oldEOG = currentPredecessors.toMutableList()

                // analyze the defaults
                handleEOG(declaration)

                // reset the oldEOG stack
                currentPredecessors = oldEOG
            }
        }

        // push statement itself
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for ReturnStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#returnstatement)
     */
    protected fun handleReturnStatement(node: ReturnStatement) {
        // analyze the return value
        handleEOG(node.returnValue)

        // push the statement itself
        attachToEOG(node)

        // reset the state afterward, we're done with this function
        currentPredecessors.clear()
    }

    /**
     * See
     * [Specification for BinaryOperator](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#binaryoperator)
     */
    protected fun handleBinaryOperator(node: BinaryOperator) {
        handleEOG(node.lhs)
        val lang = node.language
        // Two operators that don't evaluate the second operator if the first evaluates to a certain
        // value. If the language has the trait of short-circuit evaluation, we check if the
        // operatorCode is amongst the operators that lead to such an evaluation.
        if (
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
            nextEdgeBranch = lang.conjunctiveOperators.contains(node.operatorCode)
            handleEOG(node.rhs)
            attachToEOG(node)
            setCurrentEOGs(shortCircuitNodes)
            // Inverted property to assign false when true was assigned above.
            nextEdgeBranch = !lang.conjunctiveOperators.contains(node.operatorCode)
        } else {
            handleEOG(node.rhs)
        }
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for AssignExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#assignexpression)
     */
    protected fun handleAssignExpression(node: AssignExpression) {
        for (declaration in node.declarations) {
            handleEOG(declaration)
        }

        // Handle left hand side(s) first
        node.lhs.forEach { handleEOG(it) }

        // Then, handle the right side(s). Avoid creating the EOG twice if it's already part of the
        // initializer of a declaration
        node.rhs.forEach {
            if (it !in node.declarations.map { decl -> decl.initializer }) {
                handleEOG(it)
            }
        }

        attachToEOG(node)
    }

    /**
     * See [Specification for Block](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#block)
     */
    protected fun handleBlock(node: Block) {

        // analyze the contained statements
        for (child in node.statements) {
            handleEOG(child)
        }
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for UnaryOperator](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#unaryoperator)
     */
    protected fun handleUnaryOperator(node: UnaryOperator) {
        handleUnspecificUnaryOperator(node)
    }

    /**
     * This function handles all regular unary operators that do not receive any special handling
     * (such as [handleThrowOperator]). This gives language frontends a chance to override this
     * function using [de.fraunhofer.aisec.cpg.passes.configuration.ReplacePass], handle specific
     * operators on their own and delegate the rest to this function.
     */
    protected open fun handleUnspecificUnaryOperator(node: UnaryOperator) {
        val input = node.input
        handleEOG(input)

        attachToEOG(node)
    }

    /**
     * See
     * [Specification fir AssertStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#assertstatement)
     */
    protected fun handleAssertStatement(node: AssertStatement) {
        handleEOG(node.condition)
        val openConditionEOGs = currentPredecessors.toMutableList()
        handleEOG(node.message)
        setCurrentEOGs(openConditionEOGs)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for TypeExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#typeexpression)
     */
    protected fun handleTypeExpression(node: TypeExpression) {
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for TryStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#trystatement)
     */
    protected fun handleTryStatement(node: TryStatement) {

        node.resources.forEach { handleEOG(it) }

        handleEOG(node.tryBlock)
        val tmpEOGNodes = currentPredecessors.toMutableList()
        val catchEnds = mutableListOf<Node>()
        val catchesOrRelays = nodesToInternalThrows[node]
        for (catchClause in node.catchClauses) {
            currentPredecessors.clear()
            // Try to catch all internally thrown exceptions under the catching clause and remove
            // caught ones
            val toRemove = mutableSetOf<Type>()
            for ((throwType, eogEdges) in catchesOrRelays ?: mapOf()) {
                val catchParam = catchClause.parameter
                if (catchParam == null) { // e.g. catch (...)
                    currentPredecessors.addAll(eogEdges)
                } else if (throwType.tryCast(catchParam.type) != CastNotPossible) {
                    // If the thrown type can be cast to the type of the catch clause, a valid
                    // handling of the throw can be assumed
                    currentPredecessors.addAll(eogEdges)
                    toRemove.add(throwType)
                }
            }
            toRemove.forEach { catchesOrRelays?.remove(it) }
            attachToEOG(catchClause)
            handleEOG(catchClause.body)
            catchEnds.addAll(currentPredecessors)
        }

        // We need to handle the else block after the catch clauses, as the else could contain a
        // throw itself that should not be caught be the catch clauses.
        if (node.elseBlock != null) {
            currentPredecessors.clear()
            currentPredecessors.addAll(tmpEOGNodes)
            handleEOG(node.elseBlock)
            // All valid try ends got through the else block.
            tmpEOGNodes.clear()
            tmpEOGNodes.addAll(currentPredecessors)
        }
        tmpEOGNodes.addAll(catchEnds)

        val canTerminateExceptionfree = tmpEOGNodes.any { reachableFromValidEOGRoot(it) }
        currentPredecessors.clear()
        currentPredecessors.addAll(tmpEOGNodes)
        // connect all try-block, catch-clause and uncaught throws eog points to finally start if
        // finally exists
        if (node.finallyBlock != null) {
            // extends current EOG by all value EOG from open throws
            catchesOrRelays
                ?.entries
                ?.flatMap { (_, value) -> value }
                ?.let { currentPredecessors.addAll(it) }
            handleEOG(node.finallyBlock)

            //  all current-eog edges , result of finally execution as value List of uncaught
            // catchesOrRelaysThrows
            for ((_, value) in catchesOrRelays ?: mapOf()) {
                value.clear()
                value.addAll(currentPredecessors)
            }
        }
        // Forwards all open and uncaught throwing nodes to the outer scope that may handle them
        val outerCatchingNode =
            node.firstParentOrNull<Node> { parent ->
                parent is TryStatement || parent is LoopStatement
            }
        if (outerCatchingNode != null) {
            // Forwarding is done by merging the currently associated throws to a type with the new
            // throws based on their type
            val outerCatchesOrRelays =
                nodesToInternalThrows.getOrPut(outerCatchingNode) { mutableMapOf() }
            for ((exceptionType, exceptionSources) in catchesOrRelays ?: mapOf()) {
                val catches = outerCatchesOrRelays.getOrPut(exceptionType) { mutableListOf() }
                catches.addAll(exceptionSources)
            }
        }
        // To Avoid edges out of the try or finally block to the next regular statement if the try
        // can not be exited without a throw
        if (!canTerminateExceptionfree) {
            currentPredecessors.clear()
        }
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for ContinueStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#continuestatement)
     */
    protected fun handleContinueStatement(node: ContinueStatement) {
        attachToEOG(node)
        val label = node.label
        val continuableNode =
            if (label == null) {
                node.firstParentOrNull { it.isContinuable() }
            } else {
                // If a label was specified, the continue statement is associated to a node
                // explicitly labeled with the same label
                getLabeledASTNode(node, label)
            }
        if (continuableNode != null) {
            val cfNodesList =
                nodesWithContinuesAndBreaks.getOrPut(continuableNode) { mutableListOf() }
            cfNodesList.add(node)
        } else {
            LOGGER.error(
                "I am unexpectedly not in a continuable subtree, cannot add continue statement"
            )
        }

        currentPredecessors.clear()
    }

    /**
     * See
     * [Specification for DeleteExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#deleteexpression)
     */
    protected fun handleDeleteExpression(node: DeleteExpression) {
        for (operand in node.operands) {
            handleEOG(operand)
        }
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for BreakStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#breakstatement)
     */
    protected fun handleBreakStatement(node: BreakStatement) {
        attachToEOG(node)
        val label = node.label
        val breakableNode =
            if (label == null) {
                node.firstParentOrNull { it.isBreakable() }
            } else {
                getLabeledASTNode(node, label)
            }
        if (breakableNode != null) {
            val cfNodesList =
                nodesWithContinuesAndBreaks.getOrPut(breakableNode) { mutableListOf() }
            cfNodesList.add(node)
        } else {
            LOGGER.error("I am unexpectedly not in a breakable subtree, cannot add break statement")
        }

        currentPredecessors.clear()
    }

    /**
     * See
     * [Specification for LabelStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#labelstatement)
     */
    protected fun handleLabelStatement(node: LabelStatement) {
        node.scope?.addLabelStatement(node)
        handleEOG(node.subStatement)
    }

    /**
     * See
     * [Specification for GotoStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#gotostatement)
     */
    protected fun handleGotoStatement(node: GotoStatement) {
        attachToEOG(node)
        node.targetLabel?.let {
            processedListener.registerObjectListener(it) { _, to -> addEOGEdge(node, to) }
        }
        currentPredecessors.clear()
    }

    /**
     * See
     * [Specification for CaseStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#casestatement)
     */
    protected fun handleCaseStatement(node: CaseStatement) {
        handleEOG(node.caseExpression)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for NewExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#newexpression)
     */
    protected fun handleNewExpression(node: NewExpression) {
        handleEOG(node.initializer)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for KeyValueExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#keyvalueexpression)
     */
    protected fun handleKeyValueExpression(node: KeyValueExpression) {
        handleEOG(node.key)
        handleEOG(node.value)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for CastExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#castexpression)
     */
    protected fun handleCastExpression(node: CastExpression) {
        handleEOG(node.expression)
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for ExpressionList](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#expressionlist)
     */
    protected fun handleExpressionList(node: ExpressionList) {
        for (expr in node.expressions) {
            handleEOG(expr)
        }
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for InitializerListExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#initializerlistexpression)
     */
    protected fun handleInitializerListExpression(node: InitializerListExpression) {
        // first the arguments
        for (inits in node.initializers) {
            handleEOG(inits)
        }
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for ConstructExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#constructexpression)
     */
    protected fun handleConstructExpression(node: ConstructExpression) {
        // first the arguments
        for (arg in node.arguments) {
            handleEOG(arg)
        }
        attachToEOG(node)

        if (node.anonymousClass != null) {
            // Generate the EOG inside the anonymous class. It's not linked to the EOG of the outer
            // part.
            val tmpCurrentEOG = currentPredecessors.toMutableList()
            val tmpCurrentProperties = nextEdgeBranch
            val tmpIntermediateNodes = intermediateNodes.toMutableList()

            nextEdgeBranch = null
            currentPredecessors.clear()
            intermediateNodes.clear()

            handleEOG(node.anonymousClass)

            nextEdgeBranch = null
            currentPredecessors.clear()
            intermediateNodes.clear()

            nextEdgeBranch = tmpCurrentProperties
            currentPredecessors.addAll(tmpCurrentEOG)
            intermediateNodes.addAll(tmpIntermediateNodes)
        }
    }

    /**
     * Creates an EOG-edge between the given argument node and the saved currentEOG Nodes stored in
     * [currentPredecessors].
     *
     * @param node node that gets the incoming edge
     */
    fun attachToEOG(node: Node) {
        LOGGER.trace("Pushing {} {} to EOG", node.javaClass.simpleName, node)
        for (intermediate in intermediateNodes) {
            processedListener.process(intermediate, node)
        }
        addMultipleIncomingEOGEdges(currentPredecessors, node)
        intermediateNodes.clear()
        currentPredecessors.clear()
        nextEdgeBranch = null
        currentPredecessors.add(node)
    }

    fun setCurrentEOGs(nodes: List<Node>) {
        LOGGER.trace("Setting {} to EOGs", nodes)
        currentPredecessors = nodes.toMutableList()
    }

    /**
     * Connects the current EOG leaf nodes to the last stacked node, e.g. loop head, and removes the
     * nodes.
     *
     * @param loopStatement the loop statement
     */
    protected fun handleContainedBreaksAndContinues(loopStatement: LoopStatement) {
        // Breaks are connected to the NEXT EOG node and therefore temporarily stored after the loop
        // context is destroyed
        val cfNode = nodesWithContinuesAndBreaks[loopStatement]
        cfNode?.let {
            // All [BreakStatement]s are added to the current predecessors to attach them to the
            // nodes following the loop
            currentPredecessors.addAll(cfNode.filterIsInstance<BreakStatement>())
            // [ContinueStatement]s are attached to the start of loops
            val continues = cfNode.filterIsInstance<ContinueStatement>().toMutableList()
            if (continues.isNotEmpty()) {
                val conditions =
                    loopStatement.conditions
                        .map { SubgraphWalker.getEOGPathEdges(it).entries }
                        .flatten()
                conditions.forEach { node -> addMultipleIncomingEOGEdges(continues, node) }
            }
        }
    }

    /**
     * Connects current EOG nodes to the previously saved loop start to mimic control flow of loops
     */
    protected fun connectCurrentEOGToLoopStart(loopStatement: LoopStatement) {
        loopStatement.starts.forEach { node ->
            addMultipleIncomingEOGEdges(currentPredecessors, node)
        }
    }

    /**
     * Builds an EOG edge from prev to next.
     *
     * @param prev the previous node
     * @param next the next node
     */
    protected fun addEOGEdge(prev: Node, next: Node) {
        val propertyEdge = EvaluationOrder(prev, next, unreachable = false)
        propertyEdge.branch = nextEdgeBranch

        prev.nextEOGEdges += propertyEdge
    }

    protected fun addMultipleIncomingEOGEdges(prevs: List<Node>, next: Node) {
        prevs.forEach { prev -> addEOGEdge(prev, next) }
    }

    /**
     * See
     * [Specification for SynchronizedStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#synchronizedstatement)
     */
    protected fun handleSynchronizedStatement(node: SynchronizedStatement) {
        handleEOG(node.expression)
        attachToEOG(node)
        handleEOG(node.block)
    }

    /**
     * See
     * [Specification for ConditionalExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#conditionalexpression)
     */
    protected fun handleConditionalExpression(node: ConditionalExpression) {
        val openBranchNodes = mutableListOf<Node>()
        handleEOG(node.condition)
        // To have semantic information after the condition evaluation
        attachToEOG(node)
        val openConditionEOGs = currentPredecessors.toMutableList()
        nextEdgeBranch = true
        handleEOG(node.thenExpression)
        openBranchNodes.addAll(currentPredecessors)
        setCurrentEOGs(openConditionEOGs)
        nextEdgeBranch = false
        handleEOG(node.elseExpression)
        openBranchNodes.addAll(currentPredecessors)
        setCurrentEOGs(openBranchNodes)
    }

    /**
     * See
     * [Specification for DoStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#dostatement)
     */
    protected fun handleDoStatement(node: DoStatement) {
        handleEOG(node.statement)
        handleEOG(node.condition)
        // TODO(oxisto): Do we really want to set DFG edges here?
        node.condition?.let { node.prevDFGEdges += it }
        attachToEOG(node) // To have semantic information after the condition evaluation
        nextEdgeBranch = true
        connectCurrentEOGToLoopStart(node)
        nextEdgeBranch = false
        node.elseStatement?.let { handleEOG(it) }
        handleContainedBreaksAndContinues(node)
    }

    /**
     * See
     * [Specification for ComprehensionExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#comprehensionexpression)
     */
    private fun handleComprehensionExpression(node: ComprehensionExpression) {
        handleEOG(node.iterable)
        // When the iterable contains another element, the variable is evaluated with the
        // nextElement. Therefore, we add a "true" edge.
        nextEdgeBranch = true
        handleEOG(node.variable)
        handleEOG(node.predicate)
        attachToEOG(node)

        // If the conditions evaluated to false, we need to retrieve the next element, therefore
        // evaluating the iterable
        drawEOGToEntriesOf(currentPredecessors, node.iterable, branchLabel = false)

        // If an element was found that fulfills the condition, we move forward
        nextEdgeBranch = true
    }

    /**
     * See
     * [Specification for CollectionComprehension](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#collectioncomprehension)
     */
    private fun handleCollectionComprehension(node: CollectionComprehension) {
        // Process the comprehension expressions from 0 to n and connect the EOG of i to i+1.
        var prevComprehensionExpression: ComprehensionExpression? = null
        var noMoreElementsInCollection = listOf<Node>()
        node.comprehensionExpressions.forEach {
            handleEOG(it)

            val noMoreElements = SubgraphWalker.getEOGPathEdges(it.iterable).exits

            // [ComprehensionExpression] yields no more elements => EOG:false
            val prevComp = prevComprehensionExpression
            if (prevComp == null) {
                // We handle the EOG:false edges of the outermost comprehensionExpression later,
                // they continue the
                // path of execution when no more elements are yielded
                noMoreElementsInCollection = noMoreElements
            } else {
                drawEOGToEntriesOf(noMoreElements, prevComp.iterable, branchLabel = false)
            }
            prevComprehensionExpression = it

            // [ComprehensionExpression] yields and element => EOG:true
            nextEdgeBranch = true
        }

        handleEOG(node.statement)
        // After evaluating the statement we
        node.comprehensionExpressions.last().let {
            drawEOGToEntriesOf(currentPredecessors, it.iterable)
        }
        currentPredecessors.clear()
        currentPredecessors.addAll(noMoreElementsInCollection)
        nextEdgeBranch =
            false // This path is followed when the comprehensions yield no more elements
        attachToEOG(node)
    }

    /**
     * See
     * [Specification for ForEachStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#foreachstatement)
     */
    protected fun handleForEachStatement(node: ForEachStatement) {
        handleEOG(node.iterable)
        handleEOG(node.variable)
        // TODO(oxisto): Do we really want to set DFG edges here?
        node.variable?.let { node.prevDFGEdges += it }
        attachToEOG(node) // To have semantic information after the variable declaration
        nextEdgeBranch = true
        val tmpEOGNodes = currentPredecessors.toMutableList()
        handleEOG(node.statement)
        connectCurrentEOGToLoopStart(node)
        currentPredecessors.clear()
        currentPredecessors.addAll(tmpEOGNodes)
        node.elseStatement?.let { handleEOG(it) }
        handleContainedBreaksAndContinues(node)
        nextEdgeBranch = false
    }

    /**
     * See
     * [Specification for ForStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#forstatement)
     */
    protected fun handleForStatement(node: ForStatement) {
        handleEOG(node.initializerStatement)
        handleEOG(node.conditionDeclaration)
        handleEOG(node.condition)

        attachToEOG(node) // To have semantic information after the condition evaluation
        nextEdgeBranch = true
        val tmpEOGNodes = currentPredecessors.toMutableList()

        handleEOG(node.statement)
        handleEOG(node.iterationStatement)

        connectCurrentEOGToLoopStart(node)

        currentPredecessors.clear()
        currentPredecessors.addAll(tmpEOGNodes)
        node.elseStatement?.let { handleEOG(it) }
        handleContainedBreaksAndContinues(node)
        nextEdgeBranch = false
    }

    /**
     * See
     * [Specification for IfStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#ifstatement)
     */
    protected fun handleIfStatement(node: IfStatement) {
        val openBranchNodes = mutableListOf<Node>()
        handleEOG(node.initializerStatement)
        handleEOG(node.conditionDeclaration)
        handleEOG(node.condition)
        attachToEOG(node) // To have semantic information after the condition evaluation
        val openConditionEOGs = currentPredecessors.toMutableList()
        nextEdgeBranch = true
        handleEOG(node.thenStatement)
        openBranchNodes.addAll(currentPredecessors)
        if (node.elseStatement != null) {
            setCurrentEOGs(openConditionEOGs)
            nextEdgeBranch = false
            handleEOG(node.elseStatement)
            openBranchNodes.addAll(currentPredecessors)
        } else {
            openBranchNodes.addAll(openConditionEOGs)
        }
        setCurrentEOGs(openBranchNodes)
    }

    /**
     * See
     * [Specification for SwitchStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#switchstatement)
     */
    protected fun handleSwitchStatement(node: SwitchStatement) {
        handleEOG(node.initializerStatement)
        handleEOG(node.selectorDeclaration)
        handleEOG(node.selector)
        attachToEOG(node) // To have semantic information after the condition evaluation
        val tmp = currentPredecessors.toMutableList()
        val compound =
            if (node.statement is DoStatement) {
                handleEOG(node.statement)
                (node.statement as DoStatement).statement as Block
            } else {
                node.statement as Block
            }
        currentPredecessors = mutableListOf()
        for (subStatement in compound.statements) {
            if (subStatement is CaseStatement || subStatement is DefaultStatement) {
                currentPredecessors.addAll(tmp)
            }
            handleEOG(subStatement)
        }

        // If we do not have default statement, we also need to put the switch statement into the
        // currentPredecessors, otherwise we will completely ignore everything that is "beyond" the
        // switch statement
        if (compound.statements.none { it is DefaultStatement }) {
            currentPredecessors.add(node)
        }

        attachToEOG(compound)
        currentPredecessors.addAll(nodesWithContinuesAndBreaks[node] ?: mutableListOf())
    }

    /**
     * See
     * [Specification for WhileStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#whilestatement)
     */
    protected fun handleWhileStatement(node: WhileStatement) {
        handleEOG(node.conditionDeclaration)
        handleEOG(node.condition)
        attachToEOG(node) // To have semantic information after the condition evaluation
        nextEdgeBranch = true
        val tmpEOGNodes = currentPredecessors.toMutableList()
        handleEOG(node.statement)
        connectCurrentEOGToLoopStart(node)

        // Replace current EOG nodes without triggering post setEOG ... processing
        currentPredecessors.clear()
        currentPredecessors.addAll(tmpEOGNodes)
        nextEdgeBranch = false
        node.elseStatement?.let { handleEOG(it) }
        handleContainedBreaksAndContinues(node)
    }

    /**
     * See
     * [Specification for LookupScopeStatement](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#lookupScopestatement)
     */
    private fun handleLookupScopeStatement(stmt: LookupScopeStatement) {
        // Include the node as part of the EOG itself, but we do not need to go into any children or
        // properties here
        attachToEOG(stmt)
    }

    /** We use the scope where the current [node] is in, to find a statement labeled with [label] */
    fun getLabeledASTNode(node: Node, label: String): Node? {
        scopeManager.jumpTo(node.scope)
        val labelStatement = scopeManager.getLabelStatement(label)
        labelStatement?.subStatement?.let {
            return it
        }
        return null
    }

    /**
     * Calls [handleThrowOperator].
     *
     * See
     * [Specification for ThrowExpression](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/#throwexpression)
     */
    protected fun handleThrowExpression(throwExpression: ThrowExpression) {
        handleThrowOperator(
            throwExpression,
            throwExpression.exception?.type,
            throwExpression.exception,
            throwExpression.parentException,
        )
    }

    /**
     * Generates the EOG for a [throwExpression] which represents a statement/expression which
     * throws an exception. Since some languages may accept different inputs to a throw statement
     * (typically 1, sometimes 2, 0 is also possible), we have collect these in [inputs]. The input
     * which is evaluated first, must be the first item in the vararg! Any `null` object in `inputs`
     * will be filtered. We connect the throw statement internally, i.e., the inputs are evaluated
     * from index 0 to n and then the whole node is evaluated.
     */
    protected fun handleThrowOperator(
        throwExpression: Node,
        throwType: Type?,
        vararg inputs: Expression?,
    ) {
        inputs.filterNotNull().forEach { handleEOG(it) }
        attachToEOG(throwExpression)

        if (throwType != null) {
            // Here, we identify the encapsulating ast node that can handle or relay a throw
            val handlingOrRelayingParent =
                throwExpression.firstParentOrNull<Node> { parent ->
                    parent is TryStatement || parent is FunctionDeclaration
                }
            if (handlingOrRelayingParent != null) {
                val throwByTypeMap =
                    nodesToInternalThrows.getOrPut(handlingOrRelayingParent) { mutableMapOf() }
                val throwEOGExits = throwByTypeMap.getOrPut(throwType) { mutableListOf() }
                throwEOGExits.addAll(currentPredecessors.toMutableList())
            } else {
                LOGGER.error(
                    "Cannot attach throw to a parent node, throw is neither in a try statement nor in a relaying function."
                )
            }
        }
        // After a throw, the eog is not progressing in the following ast subtrees
        currentPredecessors.clear()
    }

    companion object {
        protected val LOGGER = LoggerFactory.getLogger(EvaluationOrderGraphPass::class.java)

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
            val workList = node.prevEOG.toMutableList()
            while (workList.isNotEmpty()) {
                val toProcess = workList[0]
                workList.remove(toProcess)
                passedBy.add(toProcess)
                if (toProcess is EOGStarterHolder) {
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

    /**
     * Statements that constitute the start of the Loop depending on the used pass, mostly of
     * size 1. THis list has to be extended if new structures are added that allow for looping.
     */
    val LoopStatement.starts: List<Node>
        get() =
            when (this) {
                is WhileStatement -> {
                    if (this.conditionDeclaration != null)
                        SubgraphWalker.getEOGPathEdges(this.conditionDeclaration).entries
                    else if (this.condition != null)
                        SubgraphWalker.getEOGPathEdges(this.condition).entries
                    else SubgraphWalker.getEOGPathEdges(this.statement).entries
                }
                is ForStatement -> {
                    if (this.conditionDeclaration != null)
                        SubgraphWalker.getEOGPathEdges(this.conditionDeclaration).entries
                    else if (this.condition != null)
                        SubgraphWalker.getEOGPathEdges(this.condition).entries
                    else SubgraphWalker.getEOGPathEdges(this.statement).entries
                }
                is ForEachStatement -> {
                    SubgraphWalker.getEOGPathEdges(this).entries
                }
                is DoStatement -> {
                    SubgraphWalker.getEOGPathEdges(this.statement).entries
                }
                else -> {
                    LOGGER.error(
                        "Currently the component {} does not have a defined loop start.",
                        this.javaClass,
                    )
                    ArrayList()
                }
            }

    /**
     * Statements that constitute the start of the condition evaluation, mostly of size 1. This has
     * to be extended if new nodes are added that have a condition relevant as entry points when
     * looping.
     */
    val Node.conditions: List<Node>
        get() =
            when (this) {
                is WhileStatement ->
                    mutableListOf(this.condition, this.conditionDeclaration).filterNotNull()
                is ForStatement -> mutableListOf(this.condition).filterNotNull()
                is ForEachStatement -> mutableListOf(this.variable).filterNotNull()
                is DoStatement -> mutableListOf(this.condition).filterNotNull()
                is AssertStatement -> mutableListOf(this.condition).filterNotNull()
                else -> {
                    LOGGER.error(
                        "Currently the component {} does not have defined conditions",
                        this.javaClass,
                    )
                    mutableListOf()
                }
            }

    /** Can be exited via [BreakStatement]. */
    fun Node.isBreakable(): Boolean {
        return when (this) {
            is LoopStatement -> true
            is TryStatement -> true
            is SwitchStatement -> true
            else -> false
        }
    }

    /** Can be rerun from the beginning via [ContinueStatement]. */
    fun Node.isContinuable(): Boolean {
        return when (this) {
            is LoopStatement -> true
            else -> false
        }
    }

    fun drawEOGToEntriesOf(from: List<Node>, toEntriesOf: Node?, branchLabel: Boolean? = null) {
        val tmpBranchLabel = nextEdgeBranch
        branchLabel?.let { nextEdgeBranch = it }
        SubgraphWalker.getEOGPathEdges(toEntriesOf).entries.forEach { entrance ->
            addMultipleIncomingEOGEdges(from, entrance)
        }
        nextEdgeBranch = tmpBranchLabel
    }
}
