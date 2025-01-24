/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.frontends.HasOperatorOverloading
import de.fraunhofer.aisec.cpg.frontends.isKnownOperatorName
import de.fraunhofer.aisec.cpg.frontends.python.Python.AST.IsAsync
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_KEYWORD_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.AssertStatement
import de.fraunhofer.aisec.cpg.graph.statements.CatchClause
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.TryStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.helpers.Util
import kotlin.collections.plusAssign

class StatementHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Statement, Python.AST.BaseStmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: Python.AST.BaseStmt): Statement {
        return when (node) {
            is Python.AST.ClassDef -> handleClassDef(node)
            is Python.AST.FunctionDef -> handleFunctionDef(node)
            is Python.AST.AsyncFunctionDef -> handleFunctionDef(node)
            is Python.AST.Pass -> return newEmptyStatement(rawNode = node)
            is Python.AST.ImportFrom -> handleImportFrom(node)
            is Python.AST.Assign -> handleAssign(node)
            is Python.AST.AugAssign -> handleAugAssign(node)
            is Python.AST.Return -> handleReturn(node)
            is Python.AST.If -> handleIf(node)
            is Python.AST.AnnAssign -> handleAnnAssign(node)
            is Python.AST.Expr -> handleExpressionStatement(node)
            is Python.AST.For -> handleFor(node)
            is Python.AST.AsyncFor -> handleFor(node)
            is Python.AST.While -> handleWhile(node)
            is Python.AST.Import -> handleImport(node)
            is Python.AST.Break -> newBreakStatement(rawNode = node)
            is Python.AST.Continue -> newContinueStatement(rawNode = node)
            is Python.AST.Assert -> handleAssert(node)
            is Python.AST.Try -> handleTryStatement(node)
            is Python.AST.Delete -> handleDelete(node)
            is Python.AST.With,
            is Python.AST.AsyncWith -> handleWithStatement(node)
            is Python.AST.Global -> handleGlobal(node)
            is Python.AST.Nonlocal -> handleNonLocal(node)
            is Python.AST.Raise -> handleRaise(node)
            is Python.AST.Match -> handleMatch(node)
            is Python.AST.TryStar ->
                newProblemExpression(
                    problem = "The statement of class ${node.javaClass} is not supported yet",
                    rawNode = node,
                )
        }
    }

    /**
     * Translates a pattern which can be used by a `match_case`. There are various options available
     * and all of them are translated to traditional comparisons and logical expressions which could
     * also be seen in the condition of an if-statement.
     */
    private fun handlePattern(node: Python.AST.BasePattern, subject: String): Expression {
        return when (node) {
            is Python.AST.MatchValue ->
                newBinaryOperator(operatorCode = "==", rawNode = node).implicit().apply {
                    this.lhs = newReference(name = subject)
                    this.rhs = frontend.expressionHandler.handle(ctx = node.value)
                }
            is Python.AST.MatchSingleton ->
                newBinaryOperator(operatorCode = "===", rawNode = node).implicit().apply {
                    this.lhs = newReference(name = subject)
                    this.rhs =
                        when (val value = node.value) {
                            is Python.AST.BaseExpr -> frontend.expressionHandler.handle(ctx = value)
                            null -> newLiteral(value = null, rawNode = node)
                            else ->
                                newProblemExpression(
                                    problem =
                                        "Can't handle ${value::class} in value of Python.AST.MatchSingleton yet"
                                )
                        }
                }
            is Python.AST.MatchOr ->
                frontend.expressionHandler.joinListWithBinOp(
                    operatorCode = "or",
                    nodes = node.patterns.map { handlePattern(node = it, subject = subject) },
                    rawNode = node,
                    isImplicit = false,
                )
            is Python.AST.MatchSequence,
            is Python.AST.MatchMapping,
            is Python.AST.MatchClass,
            is Python.AST.MatchStar,
            is Python.AST.MatchAs ->
                newProblemExpression(
                    problem = "Cannot handle of type ${node::class} yet",
                    rawNode = node,
                )
            else ->
                newProblemExpression(
                    problem = "Cannot handle of type ${node::class} yet",
                    rawNode = node,
                )
        }
    }

    /**
     * Translates a [`match_case`](https://docs.python.org/3/library/ast.html#ast.match_case) to a
     * [Block] which holds the [CaseStatement] and then all other statements of the
     * [Python.AST.match_case.body].
     *
     * The [CaseStatement] is generated by the [Python.AST.match_case.pattern] and, if available,
     * [Python.AST.match_case.guard]. A `guard` is modeled with an `AND` BinaryOperator in the
     * [CaseStatement.caseExpression]. Its `lhs` is the normal pattern and the `rhs` is the guard.
     * This is in line with [PEP 634](https://peps.python.org/pep-0634/).
     */
    private fun handleMatchCase(node: Python.AST.match_case, subject: String): List<Statement> {
        val statements = mutableListOf<Statement>()
        // First, we add the CaseStatement. A `MatchAs` without a `pattern` implies
        // it's a default statement.
        // We have to handle this here since we do not want to generate the CaseStatement in this
        // case.
        val pattern = node.pattern
        val guard = node.guard
        statements +=
            if (pattern is Python.AST.MatchAs && pattern.pattern == null) {
                newDefaultStatement(rawNode = pattern)
            } else if (guard != null) {
                newCaseStatement(rawNode = node).apply {
                    this.caseExpression =
                        newBinaryOperator(operatorCode = "and")
                            .implicit(
                                code = frontend.codeOf(astNode = node),
                                location = frontend.locationOf(astNode = node),
                            )
                            .apply {
                                this.lhs = handlePattern(node = node.pattern, subject = subject)
                                this.rhs = frontend.expressionHandler.handle(ctx = guard)
                            }
                }
            } else {
                newCaseStatement(rawNode = node).apply {
                    this.caseExpression = handlePattern(node = node.pattern, subject = subject)
                }
            }
        // Now, we add the remaining body.
        statements += node.body.map(::handle)
        // Currently, the EOG pass requires a break statement to work as expected. For this reason,
        // we insert an implicit break statement at the end of the block.
        statements +=
            newBreakStatement()
                .implicit(
                    code = frontend.codeOf(astNode = node),
                    location = frontend.locationOf(astNode = node),
                )
        return statements
    }

    /**
     * Translates a Python [`Match`](https://docs.python.org/3/library/ast.html#ast.Match) into a
     * [SwitchStatement].
     */
    private fun handleMatch(node: Python.AST.Match): SwitchStatement =
        newSwitchStatement(rawNode = node).apply {
            val subject = frontend.expressionHandler.handle(ctx = node.subject)
            this.selector = subject

            this.statement =
                node.cases.fold(initial = newBlock().implicit()) { block, case ->
                    block.statements +=
                        handleMatchCase(node = case, subject = subject.name.localName)
                    block
                }
        }

    /**
     * Translates a Python [`Raise`](https://docs.python.org/3/library/ast.html#ast.Raise) into a
     * [ThrowExpression].
     */
    private fun handleRaise(node: Python.AST.Raise): ThrowExpression {
        val ret = newThrowExpression(rawNode = node)
        node.exc?.let { ret.exception = frontend.expressionHandler.handle(it) }
        node.cause?.let { ret.parentException = frontend.expressionHandler.handle(it) }
        return ret
    }

    /**
     * Translates a Python [`With`](https://docs.python.org/3/library/ast.html#ast.With) into a
     * [Block].
     *
     * We return a Block to handle the with statement, following
     * [python's documentation](https://docs.python.org/3/reference/compound_stmts.html#the-with-statement).
     * The context manager's `__enter__` and `__exit__` methods should be identified before entering
     * the try-block. However, we simplify the code from the documentation as follows:
     * * `__enter__()` is called before the try-block.We make the identification and the call in the
     *   same step.
     * * `__exit__()` is either called in the `except`-block or in the `finally`-block but not
     *   identified separately.
     * * In fact, the `finally` is used like an `else`-block, so we use this construction.
     *
     * Example: We will translate the code
     *
     * ```python
     * with ContextManager() as cm:
     *     cm.doSomething()
     * ```
     *
     * to something like
     *
     * ```python
     * manager = ContextManager()
     * tmpVal = manager.__enter__()
     * try:
     *     cm = tmpVal # Doesn't exist if no variable is used
     *     cm.doSomething()
     * except:
     *     if not manager.__exit__(*sys.exc_info()):
     *         raise
     * else:
     *     manager.__exit__(None, None, None)
     * ```
     */
    private fun handleWithStatement(node: Python.AST.NormalOrAsyncWith): Block {
        /**
         * Prepares the `manager = ContextManager()` and returns the random name for the "manager"
         * as well as the assignment.
         */
        fun generateManagerAssignment(withItem: Python.AST.withitem): Pair<AssignExpression, Name> {
            // Create a temporary reference for the context manager
            val managerName = Name.random(prefix = CONTEXT_MANAGER)
            val manager = newReference(name = managerName).implicit()

            // Handle the 'context expression' (the part before 'as') and assign to tempRef
            // Represents the line `manager = ContextManager()`
            val contextExpr = frontend.expressionHandler.handle(withItem.context_expr)
            val managerAssignment =
                newAssignExpression(
                        operatorCode = "=",
                        lhs = listOf(manager),
                        rhs = listOf(contextExpr),
                    )
                    .implicit()
            return Pair(managerAssignment, managerName)
        }

        /** Prepares the `manager.__exit__(None, None, None)` call for the else-block. */
        fun generateExitCallWithNone(
            managerName: Name,
            withItem: Python.AST.withitem,
        ): MemberCallExpression {
            val exitCallWithNone =
                newMemberCallExpression(
                        callee =
                            newMemberExpression(
                                    name = "__exit__",
                                    base = newReference(name = managerName).implicit(),
                                )
                                .implicit(),
                        rawNode = node,
                    )
                    .implicit()
            exitCallWithNone.addArgument(newLiteral(null).implicit())
            exitCallWithNone.addArgument(newLiteral(null).implicit())
            exitCallWithNone.addArgument(newLiteral(null).implicit())
            return exitCallWithNone
        }

        /**
         * Prepares the if-statement which is the body of the catch block. This includes the call of
         * `manager.__exit__(*sys.exc_info())`, the negation and the throw statement.
         */
        fun generateExitCallWithSysExcInfo(
            managerName: Name,
            withItem: Python.AST.withitem,
        ): IfStatement {
            val exitCallWithSysExec =
                newMemberCallExpression(
                        callee =
                            newMemberExpression(
                                    name = "__exit__",
                                    base = newReference(name = managerName).implicit(),
                                )
                                .implicit(),
                        rawNode = node,
                    )
                    .implicit()
            val starOp = newUnaryOperator("*", false, false)
            starOp.input =
                newMemberExpression(name = "exec_info", base = newReference("sys").implicit())
                    .implicit()
            exitCallWithSysExec.addArgument(starOp)

            val ifStmt = newIfStatement().implicit()
            ifStmt.thenStatement = newThrowExpression().implicit()
            val neg = newUnaryOperator("not", false, false).implicit()
            neg.input = exitCallWithSysExec
            ifStmt.condition = neg
            return ifStmt
        }

        /**
         * calls __enter__() and assign to another random variable. Represents the line
         *
         * ```python
         * tmpVal = manager.__enter__()
         * ```
         */
        fun generateEnterCallAndAssignment(
            managerName: Name,
            withItem: Python.AST.withitem,
        ): Pair<AssignExpression, Name> {
            val tmpValName = Name.random(prefix = WITH_TMP_VAL)
            val enterVar = newReference(name = tmpValName).implicit()
            val enterCall =
                newMemberCallExpression(
                        callee =
                            newMemberExpression(
                                    name = "__enter__",
                                    base = newReference(name = managerName).implicit(),
                                )
                                .implicit(),
                        rawNode = node,
                    )
                    .implicit()

            return Pair(
                newAssignExpression(
                        operatorCode = "=",
                        lhs = listOf(enterVar),
                        rhs = listOf(enterCall),
                    )
                    .implicit(),
                tmpValName,
            )
        }
        val result =
            newBlock().codeAndLocationFromOtherRawNode(node as? Python.AST.BaseStmt).implicit()

        addAsyncWarning(node, result)

        // If there are multiple elements in node.items, we have to nest the try statements.
        // We start with a generic block for the outer context manager.
        // For i > 1, we add context_manager[i] to the try-block of item[i-1]
        val currentBlock =
            node.items.fold(result) { currentBlock, withItem ->
                val (managerAssignment, managerName) = generateManagerAssignment(withItem)

                currentBlock.statements.add(managerAssignment)

                val (enterAssignment, tmpValName) =
                    generateEnterCallAndAssignment(managerName, withItem)
                currentBlock.statements.add(enterAssignment)

                // Create the try statement with __exit__ calls in the finally block
                val tryStatement =
                    newTryStatement(rawNode = node).apply {
                        // We set it as implicit below because there we also have a code and
                        // location.
                        this.tryBlock =
                            newBlock()
                                .apply {
                                    withItem.optional_vars?.let {
                                        val optionalVar = frontend.expressionHandler.handle(it)
                                        node.type_comment?.let {
                                            optionalVar.type = frontend.typeOf(it)
                                        }

                                        // Assign the result of __enter__() to `optionalVar`
                                        // Represents the line "cm = tmpVal # Doesn't exist if no
                                        // variable is used"
                                        this.statements.add(
                                            newAssignExpression(
                                                    operatorCode = "=",
                                                    lhs = listOf(optionalVar),
                                                    rhs =
                                                        listOf(
                                                            newReference(name = tmpValName)
                                                                .implicit()
                                                        ),
                                                )
                                                .implicit()
                                        )
                                    }
                                }
                                .codeAndLocationFromOtherRawNode(node as? Python.AST.BaseStmt)
                                .implicit()
                        // Add the catch block
                        this.catchClauses.add(
                            newCatchClause().implicit().apply {
                                this.body =
                                    newBlock().implicit().apply {
                                        this.statements.add(
                                            generateExitCallWithSysExcInfo(managerName, withItem)
                                        )
                                    }
                            }
                        )
                        // Add the else-block
                        this.elseBlock =
                            newBlock().implicit().apply {
                                this.statements.add(generateExitCallWithNone(managerName, withItem))
                            }
                    }
                currentBlock.statements.add(tryStatement)

                tryStatement.tryBlock ?: throw NullPointerException("This should never happen!")
            }

        // Create the block of the with statement and add it to the inner try-block
        val bodyBlock = makeBlock(node.body, parentNode = node) // represents `cm.doSomething()`
        currentBlock.statements.addAll(bodyBlock.statements)
        currentBlock.implicit(code = bodyBlock.code, location = bodyBlock.location)

        // The result is the outer block
        return result
    }

    /**
     * Translates an [`excepthandler`] which can only be a
     * [`ExceptHandler`](https://docs.python.org/3/library/ast.html#ast.ExceptHandler) to a
     * [CatchClause].
     *
     * It adds all the statements to the body and will set a parameter if it exists. For the
     * catch-all clause, we do not set the [CatchClause.parameter].
     */
    private fun handleBaseExcepthandler(node: Python.AST.BaseExcepthandler): CatchClause {
        return when (node) {
            is Python.AST.ExceptHandler -> {
                val catchClause = newCatchClause(rawNode = node)
                catchClause.body = makeBlock(node.body, node)
                // The parameter can have a type but if the type is None/null, it's the "catch-all"
                // clause.
                // In this case, it also cannot have a name, so we can skip the variable
                // declaration.
                if (node.type != null) {
                    // the parameter can have a name, or we use the anonymous identifier _
                    catchClause.parameter =
                        newVariableDeclaration(
                            name = node.name ?: "",
                            type = frontend.typeOf(node.type),
                            rawNode = node,
                        )
                }
                catchClause
            }
        }
    }

    /**
     * Translates a Python [`Try`](https://docs.python.org/3/library/ast.html#ast.Try) into a
     * [TryStatement].
     */
    private fun handleTryStatement(node: Python.AST.Try): TryStatement {
        val tryStatement = newTryStatement(rawNode = node)
        tryStatement.tryBlock = makeBlock(node.body, node)
        tryStatement.catchClauses.addAll(node.handlers.map { handleBaseExcepthandler(it) })

        if (node.orelse.isNotEmpty()) {
            tryStatement.elseBlock = makeBlock(node.orelse, node)
        }

        if (node.finalbody.isNotEmpty()) {
            tryStatement.finallyBlock = makeBlock(node.finalbody, node)
        }

        return tryStatement
    }

    /**
     * Translates a Python [`Delete`](https://docs.python.org/3/library/ast.html#ast.Delete) into a
     * [DeleteExpression].
     */
    private fun handleDelete(node: Python.AST.Delete): DeleteExpression {
        val delete = newDeleteExpression(rawNode = node)
        node.targets.forEach { target ->
            delete.operands.add(frontend.expressionHandler.handle(target))

            if (target !is Python.AST.Subscript) {
                delete.additionalProblems +=
                    newProblemExpression(
                        problem =
                            "handleDelete: 'Name' and 'Attribute' deletions are not fully supported, as they remove variables from the scope.",
                        rawNode = target,
                    )
            }
        }

        return delete
    }

    /**
     * Translates a Python [`Assert`](https://docs.python.org/3/library/ast.html#ast.Assert) into a
     * [AssertStatement].
     */
    private fun handleAssert(node: Python.AST.Assert): AssertStatement {
        val assertStatement = newAssertStatement(rawNode = node)
        val testExpression = frontend.expressionHandler.handle(node.test)
        assertStatement.condition = testExpression
        node.msg?.let { assertStatement.message = frontend.expressionHandler.handle(it) }

        return assertStatement
    }

    private fun handleImport(node: Python.AST.Import): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        for (imp in node.names) {
            val alias = imp.asname
            val decl =
                if (alias != null) {
                    newImportDeclaration(
                        parseName(imp.name),
                        false,
                        parseName(alias),
                        rawNode = imp,
                    )
                } else {
                    newImportDeclaration(parseName(imp.name), false, rawNode = imp)
                }
            frontend.scopeManager.addDeclaration(decl)
            declStmt.declarationEdges += decl
        }
        return declStmt
    }

    private fun handleImportFrom(node: Python.AST.ImportFrom): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        val level = node.level
        var module = parseName(node.module ?: "")

        if (level != null && level > 0L) {
            // Because the __init__ module is omitted from our current namespace, we need to check
            // for its existence and add __init__, otherwise the relative path would be off by one
            // level.
            var parent =
                if (isInitModule()) {
                    frontend.scopeManager.currentNamespace.fqn("__init__")
                } else {
                    frontend.scopeManager.currentNamespace
                }

            // If the level is specified, we need to relative the module path. We basically need to
            // move upwards in the parent namespace in the amount of dots
            for (i in 0 until level) {
                parent = parent?.parent
                if (parent == null) {
                    break
                }
            }

            module =
                if (module.localName != "") {
                    parent.fqn(module.localName)
                } else {
                    parent ?: Name("")
                }
        }

        for (imp in node.names) {
            // We need to differentiate between a wildcard import and an individual symbol.
            // Wildcards luckily do not have aliases
            val decl =
                if (imp.name == "*") {
                    // In the wildcard case, our "import" is the module name, and we set "wildcard"
                    // to true
                    newImportDeclaration(module, true, rawNode = imp)
                } else {
                    // If we import an individual symbol, we need to FQN the symbol with our module
                    // name and import that. We also need to take care of any alias
                    val name = module.fqn(imp.name)
                    val alias = imp.asname
                    if (alias != null) {
                        newImportDeclaration(name, false, parseName(alias), rawNode = imp)
                    } else {
                        newImportDeclaration(name, false, rawNode = imp)
                    }
                }

            // Finally, add our declaration to the scope and the declaration statement
            frontend.scopeManager.addDeclaration(decl)
            declStmt.declarationEdges += decl
        }
        return declStmt
    }

    /** Small utility function to check, whether we are inside an __init__ module. */
    private fun isInitModule(): Boolean =
        (frontend.scopeManager.firstScopeIsInstanceOrNull<NameScope>()?.astNode
                as? NamespaceDeclaration)
            ?.path
            ?.endsWith("__init__") == true

    private fun handleWhile(node: Python.AST.While): Statement {
        val ret = newWhileStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.statement = makeBlock(node.body, parentNode = node)
        if (node.orelse.isNotEmpty()) {
            ret.elseStatement = makeBlock(node.orelse, parentNode = node)
        }
        return ret
    }

    /**
     * Translates a Python [`For`](https://docs.python.org/3/library/ast.html#ast.For) into an
     * [ForEachStatement].
     *
     * Python supports implicit unpacking of multiple loop variables. To map this to CPG node, we
     * translate the following implicit unpacking of code like this:
     * ```python
     * for a, b, c in someNestedList:
     *     pass
     * ```
     *
     * to have only one loop variable and add the unpacking statement to the top of the loop body
     * like this:
     * ```python
     * for tempVar in someNestedList:
     *     a, b, c = tempVar
     *     pass
     * ```
     */
    private fun handleFor(node: Python.AST.NormalOrAsyncFor): ForEachStatement {
        val ret = newForEachStatement(rawNode = node)
        addAsyncWarning(node, ret)

        ret.iterable = frontend.expressionHandler.handle(node.iter)

        when (val loopVar = frontend.expressionHandler.handle(node.target)) {
            is InitializerListExpression -> { // unpacking
                val (tempVarRef, unpackingAssignment) = getUnpackingNodes(loopVar)

                ret.variable = tempVarRef

                val body = makeBlock(node.body, parentNode = node)
                body.statements.add(
                    0,
                    unpackingAssignment,
                ) // add the unpacking instruction to the top of the loop body
                ret.statement = body
            }
            is Reference -> { // only one var
                ret.variable = loopVar
                ret.statement = makeBlock(node.body, parentNode = node)
            }
            else -> {
                ret.variable =
                    newProblemExpression(
                        problem =
                            "handleFor: cannot handle loop variable of type ${loopVar::class.simpleName}.",
                        rawNode = node.target,
                    )
                ret.statement = makeBlock(node.body, parentNode = node)
            }
        }

        if (node.orelse.isNotEmpty()) {
            ret.elseStatement = makeBlock(node.orelse, parentNode = node)
        }
        return ret
    }

    /**
     * This function creates two things:
     * - A [Reference] to a variable with a random [Name]
     * - An [AssignExpression] assigning the reference above to the [loopVar] input
     *
     * This is used in [handleFor] when loops have multiple loop variables to iterate over with
     * automatic unpacking. We translate this implicit unpacking to multiple CPG nodes, as the CPG
     * does not support automatic unpacking.
     */
    private fun getUnpackingNodes(
        loopVar: InitializerListExpression
    ): Pair<Reference, AssignExpression> {
        val tempVarName = Name.random(prefix = LOOP_VAR_PREFIX)
        val tempRef = newReference(name = tempVarName).implicit().codeAndLocationFrom(loopVar)
        val assign =
            newAssignExpression(
                    operatorCode = "=",
                    lhs = (loopVar).initializers,
                    rhs = listOf(tempRef),
                )
                .implicit()
                .codeAndLocationFrom(loopVar)
        return Pair(tempRef, assign)
    }

    private fun handleExpressionStatement(node: Python.AST.Expr): Statement {
        return frontend.expressionHandler.handle(node.value)
    }

    /**
     * Translates a Python [`AnnAssign`](https://docs.python.org/3/library/ast.html#ast.AnnAssign)
     * into an [AssignExpression].
     */
    private fun handleAnnAssign(node: Python.AST.AnnAssign): AssignExpression {
        val lhs = frontend.expressionHandler.handle(node.target)
        lhs.type = frontend.typeOf(node.annotation)
        val rhs = node.value?.let { listOf(frontend.expressionHandler.handle(it)) } ?: emptyList()
        return newAssignExpression(lhs = listOf(lhs), rhs = rhs, rawNode = node)
    }

    private fun handleIf(node: Python.AST.If): Statement {
        val ret = newIfStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.thenStatement =
            if (node.body.isNotEmpty()) {
                makeBlock(node.body, parentNode = node)
            } else {
                null
            }
        ret.elseStatement =
            if (node.orelse.isNotEmpty()) {
                makeBlock(node.orelse, parentNode = node)
            } else {
                null
            }
        return ret
    }

    private fun handleReturn(node: Python.AST.Return): Statement {
        val ret = newReturnStatement(rawNode = node)
        node.value?.let { ret.returnValue = frontend.expressionHandler.handle(it) }
        return ret
    }

    /**
     * Translates a Python [`Assign`](https://docs.python.org/3/library/ast.html#ast.Assign) into an
     * [AssignExpression].
     */
    private fun handleAssign(node: Python.AST.Assign): AssignExpression {
        val lhs = node.targets.map { frontend.expressionHandler.handle(it) }
        node.type_comment?.let { typeComment ->
            val tpe = frontend.typeOf(typeComment)
            lhs.forEach { it.type = tpe }
        }
        val rhs = frontend.expressionHandler.handle(node.value)
        if (rhs is List<*>)
            newAssignExpression(
                lhs = lhs,
                rhs =
                    rhs.map {
                        (it as? Expression)
                            ?: newProblemExpression(
                                "There was an issue with an argument.",
                                rawNode = node,
                            )
                    },
                rawNode = node,
            )
        return newAssignExpression(lhs = lhs, rhs = listOf(rhs), rawNode = node)
    }

    private fun handleAugAssign(node: Python.AST.AugAssign): Statement {
        val lhs = frontend.expressionHandler.handle(node.target)
        val rhs = frontend.expressionHandler.handle(node.value)
        val op = frontend.operatorToString(node.op) + "="
        return newAssignExpression(
            operatorCode = op,
            lhs = listOf(lhs),
            rhs = listOf(rhs),
            rawNode = node,
        )
    }

    private fun handleClassDef(stmt: Python.AST.ClassDef): Statement {
        val cls = newRecordDeclaration(stmt.name, "class", rawNode = stmt)
        stmt.bases.map { cls.superClasses.add(frontend.typeOf(it)) }

        frontend.scopeManager.enterScope(cls)

        stmt.keywords.forEach {
            cls.additionalProblems +=
                newProblemExpression(problem = "could not parse keyword $it in class", rawNode = it)
        }

        for (s in stmt.body) {
            when (s) {
                is Python.AST.FunctionDef -> handleFunctionDef(s, cls)
                else -> cls.statements += handleNode(s)
            }
        }

        frontend.scopeManager.leaveScope(cls)
        frontend.scopeManager.addDeclaration(cls)

        return wrapDeclarationToStatement(cls)
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [Python.AST.FunctionDef] is a [Statement] from Python's point of view. The CPG sees it as
     *   a declaration -> we have to wrap the result in a [DeclarationStatement].
     * - A [Python.AST.FunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodeDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ConstructorDeclaration] or[MethodDeclaration]: the first argument is the
     * `receiver` (most often called `self`).
     */
    private fun handleFunctionDef(
        s: Python.AST.NormalOrAsyncFunctionDef,
        recordDeclaration: RecordDeclaration? = null,
    ): DeclarationStatement {
        val language = language
        val result =
            if (recordDeclaration != null) {
                if (s.name == "__init__") {
                    newConstructorDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        rawNode = s,
                    )
                } else if (language is HasOperatorOverloading && s.name.isKnownOperatorName) {
                    var decl =
                        newOperatorDeclaration(
                            name = s.name,
                            recordDeclaration = recordDeclaration,
                            operatorCode = language.operatorCodeFor(s.name) ?: "",
                            rawNode = s,
                        )
                    if (decl.operatorCode == "") {
                        Util.warnWithFileLocation(
                            decl,
                            log,
                            "Could not find operator code for operator {}. This will most likely result in a failure",
                            s.name,
                        )
                    }
                    decl
                } else {
                    newMethodDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        isStatic = false,
                        rawNode = s,
                    )
                }
            } else {
                newFunctionDeclaration(name = s.name, rawNode = s)
            }
        frontend.scopeManager.enterScope(result)

        addAsyncWarning(s, result)

        // Handle decorators (which are translated into CPG "annotations")
        result.annotations += handleAnnotations(s)

        // Handle return type and calculate function type
        if (result is ConstructorDeclaration) {
            // Return type of the constructor is always its record declaration type
            result.returnTypes = listOf(recordDeclaration?.toType() ?: unknownType())
        } else {
            result.returnTypes = listOf(frontend.typeOf(s.returns))
        }
        result.type = FunctionType.computeType(result)

        handleArguments(s.args, result, recordDeclaration)

        if (s.body.isNotEmpty()) {
            // Make sure we open a new (block) scope for the function body. This is not a 1:1
            // mapping to python scopes, since python only has a "function scope", but in the CPG
            // the function scope only comprises the function arguments, and we need a block scope
            // to hold all local variables within the function body.
            result.body = makeBlock(s.body, parentNode = s, enterScope = true)
        }

        frontend.scopeManager.leaveScope(result)
        frontend.scopeManager.addDeclaration(result)

        return wrapDeclarationToStatement(result)
    }

    /**
     * Translates a Python [`Global`](https://docs.python.org/3/library/ast.html#ast.Global) into a
     * [LookupScopeStatement].
     */
    private fun handleGlobal(global: Python.AST.Global): LookupScopeStatement {
        // Technically, our global scope is not identical to the python "global" scope. The reason
        // behind that is that we wrap each file in a namespace (as defined in the python spec). So
        // the "global" scope is actually our current namespace scope.
        var pythonGlobalScope =
            frontend.scopeManager.globalScope?.children?.firstOrNull { it is NamespaceScope }

        return newLookupScopeStatement(
            global.names.map { parseName(it).localName },
            pythonGlobalScope,
            rawNode = global,
        )
    }

    /**
     * Translates a Python [`Nonlocal`](https://docs.python.org/3/library/ast.html#ast.Nonlocal)
     * into a [LookupScopeStatement].
     */
    private fun handleNonLocal(global: Python.AST.Nonlocal): LookupScopeStatement {
        // We need to find the first outer function scope, or rather the block scope belonging to
        // the function
        var outerFunctionScope =
            frontend.scopeManager.firstScopeOrNull {
                it is LocalScope && it != frontend.scopeManager.currentScope
            }

        return newLookupScopeStatement(
            global.names.map { parseName(it).localName },
            outerFunctionScope,
            rawNode = global,
        )
    }

    /** Adds the arguments to [result] which might be located in a [recordDeclaration]. */
    private fun handleArguments(
        args: Python.AST.arguments,
        result: FunctionDeclaration,
        recordDeclaration: RecordDeclaration?,
    ) {
        // We can merge posonlyargs and args because both are positional arguments. We do not
        // enforce that posonlyargs can ONLY be used in a positional style, whereas args can be used
        // both in positional and keyword style.
        var positionalArguments = args.posonlyargs + args.args

        // Handle receiver if it exists and if it is not a static method
        if (
            recordDeclaration != null &&
                result.annotations.none { it.name.localName == "staticmethod" }
        ) {
            handleReceiverArgument(positionalArguments, args, result, recordDeclaration)
            // Skip the receiver argument for further processing
            positionalArguments = positionalArguments.drop(1)
        }

        // Handle remaining arguments
        handlePositionalArguments(positionalArguments, args)

        args.vararg?.let { handleArgument(it, isPosOnly = false, isVariadic = true) }
        args.kwarg?.let { handleArgument(it, isPosOnly = false, isVariadic = false) }

        handleKeywordOnlyArguments(args)
    }

    /**
     * This method retrieves the first argument of the [positionalArguments], which is typically the
     * receiver object.
     *
     * A receiver can also have a default value. However, this case is not handled and is therefore
     * passed with a problem expression.
     */
    private fun handleReceiverArgument(
        positionalArguments: List<Python.AST.arg>,
        args: Python.AST.arguments,
        result: FunctionDeclaration,
        recordDeclaration: RecordDeclaration,
    ) {
        // first argument is the receiver
        val recvPythonNode = positionalArguments.firstOrNull()
        if (recvPythonNode == null) {
            result.additionalProblems += newProblemExpression("Expected a receiver", rawNode = args)
        } else {
            val tpe = recordDeclaration.toType()
            val recvNode =
                newVariableDeclaration(
                    name = recvPythonNode.arg,
                    type = tpe,
                    implicitInitializerAllowed = false,
                    rawNode = recvPythonNode,
                )

            // If the number of defaults equals the number of positional arguments, the receiver has
            // a default value
            if (args.defaults.size == positionalArguments.size) {
                val defaultValue =
                    args.defaults.getOrNull(0)?.let { frontend.expressionHandler.handle(it) }
                defaultValue?.let {
                    frontend.scopeManager.addDeclaration(recvNode)
                    result.additionalProblems +=
                        newProblemExpression("Receiver with default value", rawNode = args)
                }
            }

            when (result) {
                is ConstructorDeclaration,
                is MethodDeclaration -> result.receiver = recvNode
                else ->
                    result.additionalProblems +=
                        newProblemExpression(
                            problem =
                                "Expected a constructor or method declaration. Got something else.",
                            rawNode = result,
                        )
            }
        }
    }

    /**
     * This method extracts the [positionalArguments] including those that may have default values.
     *
     * In Python only the arguments with default values are stored in `args.defaults`.
     * https://docs.python.org/3/library/ast.html#ast.arguments
     *
     * For example: `def my_func(a, b=1, c=2): pass`
     *
     * In this case, `args.defaults` contains only the defaults for `b` and `c`, while `args.args`
     * includes all arguments (`a`, `b` and `c`). The number of arguments without defaults is
     * determined by subtracting the size of `args.defaults` from the total number of arguments.
     * This matches each default to its corresponding argument.
     *
     * From the Python docs: "If there are fewer defaults, they correspond to the last n arguments."
     */
    private fun handlePositionalArguments(
        positionalArguments: List<Python.AST.arg>,
        args: Python.AST.arguments,
    ) {
        val nonDefaultArgsCount = positionalArguments.size - args.defaults.size

        for (idx in positionalArguments.indices) {
            val arg = positionalArguments[idx]
            val defaultIndex = idx - nonDefaultArgsCount
            val defaultValue =
                if (defaultIndex >= 0) {
                    args.defaults.getOrNull(defaultIndex)?.let {
                        frontend.expressionHandler.handle(it)
                    }
                } else {
                    null
                }
            handleArgument(arg, isPosOnly = arg in args.posonlyargs, defaultValue = defaultValue)
        }
    }

    /**
     * This method extracts the keyword-only arguments from [args] and maps them to the
     * corresponding function parameters.
     */
    private fun handleKeywordOnlyArguments(args: Python.AST.arguments) {
        for (idx in args.kwonlyargs.indices) {
            val arg = args.kwonlyargs[idx]
            val default = args.kw_defaults.getOrNull(idx)
            handleArgument(
                arg,
                isPosOnly = false,
                isKwoOnly = true,
                defaultValue = default?.let { frontend.expressionHandler.handle(it) },
            )
        }
    }

    private fun handleAnnotations(
        node: Python.AST.NormalOrAsyncFunctionDef
    ): Collection<Annotation> {
        return handleDeclaratorList(node, node.decorator_list)
    }

    fun handleDeclaratorList(
        node: Python.AST.WithLocation,
        decoratorList: List<Python.AST.BaseExpr>,
    ): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        for (decorator in decoratorList) {
            var annotation =
                when (decorator) {
                    is Python.AST.Name -> {
                        val parsedDecorator = frontend.expressionHandler.handle(decorator)
                        newAnnotation(name = parsedDecorator.name, rawNode = decorator)
                    }
                    is Python.AST.Attribute -> {
                        val parsedDecorator = frontend.expressionHandler.handle(decorator)
                        val name =
                            if (parsedDecorator is MemberExpression) {
                                parsedDecorator.base.name.fqn(parsedDecorator.name.localName)
                            } else {
                                parsedDecorator.name
                            }
                        newAnnotation(name = name, rawNode = decorator)
                    }
                    is Python.AST.Call -> {
                        val parsedDecorator = frontend.expressionHandler.handle(decorator.func)
                        val name =
                            if (parsedDecorator is MemberExpression) {
                                parsedDecorator.base.name.fqn(parsedDecorator.name.localName)
                            } else {
                                parsedDecorator.name
                            }
                        val annotation = newAnnotation(name = name, rawNode = decorator)
                        for (arg in decorator.args) {
                            val argParsed = frontend.expressionHandler.handle(arg)
                            annotation.members +=
                                newAnnotationMember(
                                    "annotationArg" + decorator.args.indexOf(arg), // TODO
                                    argParsed,
                                    rawNode = arg,
                                )
                        }
                        for (keyword in decorator.keywords) {
                            annotation.members +=
                                newAnnotationMember(
                                    name = keyword.arg,
                                    value = frontend.expressionHandler.handle(keyword.value),
                                    rawNode = keyword,
                                )
                        }
                        annotation
                    }
                    else -> {
                        Util.warnWithFileLocation(
                            frontend,
                            decorator,
                            log,
                            "Decorator is of type ${decorator::class}, cannot handle this (yet).",
                        )
                        continue
                    }
                }

            annotations += annotation
        }

        return annotations
    }

    /**
     * This function "wraps" a list of [Python.AST.BaseStmt] nodes into a [Block]. Since the list
     * itself does not have a code/location, we need to employ [codeAndLocationFromChildren] on the
     * [parentNode].
     *
     * Optionally, a new scope will be opened when [enterScope] is specified. This should be done
     * VERY carefully, as Python has a very limited set of scopes and is most likely only to be used
     * by [handleFunctionDef].
     */
    private fun makeBlock(
        stmts: List<Python.AST.BaseStmt>,
        parentNode: Python.AST.WithLocation,
        enterScope: Boolean = false,
    ): Block {
        val result = newBlock()
        if (enterScope) {
            frontend.scopeManager.enterScope(result)
        }

        for (stmt in stmts) {
            result.statements += handle(stmt)
        }

        if (enterScope) {
            frontend.scopeManager.leaveScope(result)
        }

        // Try to retrieve the code and location from the parent node, if it is a base stmt
        val ast = parentNode as? Python.AST.AST
        if (ast != null) {
            // We need to scope the call to codeAndLocationFromChildren to our frontend, so that
            // all Python.AST.AST nodes are accepted, otherwise it would be scoped to the handler
            // and only Python.AST.BaseStmt nodes would be accepted. This would cause issues with
            // other nodes that are not "statements", but also handled as part of this handler,
            // e.g., the Python.AST.ExceptHandler.
            with(frontend) { result.codeAndLocationFromChildren(ast, frontend.lineSeparator) }
        }

        return result
    }

    /**
     * This function creates a [newParameterDeclaration] for the argument, setting any modifiers
     * (like positional-only or keyword-only) and [defaultValue] if applicable.
     */
    internal fun handleArgument(
        node: Python.AST.arg,
        isPosOnly: Boolean = false,
        isVariadic: Boolean = false,
        isKwoOnly: Boolean = false,
        defaultValue: Expression? = null,
    ) {
        val type = frontend.typeOf(node.annotation)
        val arg =
            newParameterDeclaration(
                name = node.arg,
                type = type,
                variadic = isVariadic,
                rawNode = node,
            )
        defaultValue?.let { arg.default = it }
        if (isPosOnly) {
            arg.modifiers += MODIFIER_POSITIONAL_ONLY_ARGUMENT
        }

        if (isKwoOnly) {
            arg.modifiers += MODIFIER_KEYWORD_ONLY_ARGUMENT
        }

        frontend.scopeManager.addDeclaration(arg)
    }

    /**
     * Wrap a declaration in a [DeclarationStatement]
     *
     * @param decl The [Declaration] to be wrapped
     * @return The wrapped [decl]
     */
    private fun wrapDeclarationToStatement(decl: Declaration): DeclarationStatement {
        val declStmt = newDeclarationStatement().codeAndLocationFrom(decl)
        declStmt.addDeclaration(decl)
        return declStmt
    }

    /**
     * Checks whether [mightBeAsync] implements the [IsAsync] interface and adds a warning to the
     * corresponding [parentNode] stored in [Node.additionalProblems].
     */
    private fun addAsyncWarning(mightBeAsync: Python.AST.AsyncOrNot, parentNode: Node) {
        if (mightBeAsync is IsAsync) {
            parentNode.additionalProblems +=
                newProblemDeclaration(
                    problem = "The \"async\" keyword is not yet supported.",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = mightBeAsync,
                )
        }
    }
}
