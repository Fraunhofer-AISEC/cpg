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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.python.Python.AST.IsAsync
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
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
import kotlin.collections.plusAssign

class StatementHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Statement, Python.AST.BaseStmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: Python.AST.BaseStmt): Statement {
        return when (node) {
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
            is Python.AST.Def -> {
                val decl = frontend.declarationHandler.handleNode(node)
                frontend.scopeManager.addDeclaration(decl)
                wrapDeclarationToStatement(decl)
            }
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
        fun generateManagerAssignment(
            withItem: Python.AST.withitem,
            currentBlock: Block,
        ): Pair<AssignExpression, Name> {
            // Create a temporary unique reference for the context manager
            val managerName =
                Name.temporary(prefix = CONTEXT_MANAGER, separatorChar = '_', currentBlock)
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
        fun generateExitCallWithNone(managerName: Name): MemberCallExpression {
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
        fun generateExitCallWithSysExcInfo(managerName: Name): IfStatement {
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
            val starOp = newUnaryOperator("*", postfix = false, prefix = false)
            starOp.input =
                newMemberExpression(name = "exec_info", base = newReference("sys").implicit())
                    .implicit()
            exitCallWithSysExec.addArgument(starOp)

            val ifStmt = newIfStatement().implicit()
            ifStmt.thenStatement = newThrowExpression().implicit()
            val neg = newUnaryOperator("not", postfix = false, prefix = false).implicit()
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
            managerAssignment: AssignExpression,
        ): Pair<AssignExpression, Name> {
            val tmpValName =
                Name.temporary(prefix = WITH_TMP_VAL, separatorChar = '_', managerAssignment)
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
                val (managerAssignment, managerName) =
                    generateManagerAssignment(withItem, currentBlock)

                currentBlock.statements.add(managerAssignment)

                val (enterAssignment, tmpValName) =
                    generateEnterCallAndAssignment(managerName, managerAssignment)
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
                                            generateExitCallWithSysExcInfo(managerName)
                                        )
                                    }
                            }
                        )
                        // Add the else-block
                        this.elseBlock =
                            newBlock().implicit().apply {
                                this.statements.add(generateExitCallWithNone(managerName))
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

    /**
     * Translates a Python [`Import`](https://docs.python.org/3/library/ast.html#ast.Import) into a
     * [Statement].
     *
     * For each import, it handles two cases:
     * - If an alias is present (e.g., `import foo.bar.baz as fbb`), only the final module is bound
     *   using the alias.
     * - Without an alias, it iteratively creates declarations for each parent package (e.g., `foo`,
     *   `foo.bar`, and `foo.bar.baz`).
     *
     *   See also the
     *   [`Python specification`](https://docs.python.org/3/reference/simple_stmts.html#the-import-statement)
     *   for details:
     */
    private fun handleImport(node: Python.AST.Import): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        for (imp in node.names) {
            val alias = imp.asname
            if (alias != null) {
                // If we have an alias, we import the package with the alias and do NOT import the
                // parent packages
                val decl =
                    newImportDeclaration(
                        parseName(imp.name),
                        style = ImportStyle.IMPORT_NAMESPACE,
                        parseName(alias),
                        rawNode = imp,
                    )
                conditionallyAddAdditionalSourcesToAnalysis(decl.import)
                frontend.scopeManager.addDeclaration(decl)
                declStmt.declarations += decl
            } else {
                // If we do not have an alias, we import all the packages along the path - unless we
                // already have an import for the package in the scope
                var importName: Name? = parseName(imp.name)
                while (importName != null) {
                    val decl =
                        newImportDeclaration(
                            importName,
                            style = ImportStyle.IMPORT_NAMESPACE,
                            rawNode = imp,
                        )
                    conditionallyAddAdditionalSourcesToAnalysis(decl.import)
                    frontend.scopeManager.addDeclaration(decl)
                    declStmt.declarations += decl
                    importName = importName.parent
                }
            }
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
                    frontend.scopeManager.currentNamespace.fqn(PythonLanguage.IDENTIFIER_INIT)
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
                    conditionallyAddAdditionalSourcesToAnalysis(module)
                    newImportDeclaration(
                        module,
                        style = ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE,
                        rawNode = imp,
                    )
                } else {
                    // If we import an individual symbol, we need to FQN the symbol with our module
                    // name and import that. We also need to take care of any alias
                    val name = module.fqn(imp.name)
                    val alias = imp.asname
                    conditionallyAddAdditionalSourcesToAnalysis(name)
                    if (alias != null) {
                        newImportDeclaration(
                            name,
                            style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                            parseName(alias),
                            rawNode = imp,
                        )
                    } else {
                        newImportDeclaration(
                            name,
                            style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                            rawNode = imp,
                        )
                    }
                }

            // Finally, add our declaration to the scope and the declaration statement
            frontend.scopeManager.addDeclaration(decl)
            declStmt.declarations += decl
        }
        return declStmt
    }

    /**
     * Uses the given name to identify whether one of the files in
     * [TranslationContext.additionalSources] is the target of the import statement. If it is, it is
     * added to [TranslationContext.importedSources] for further analysis by the translation
     * manager.
     */
    private fun conditionallyAddAdditionalSourcesToAnalysis(importName: Name) {
        var currentName: Name? = importName
        while (!currentName.isNullOrEmpty()) {
            // Build a set of candidates how files look like for the current name. They are a set of
            // relative file names, e.g. foo/bar/baz.py and foo/bar/baz/__init__.py
            val candidates = (language as PythonLanguage).nameToLanguageFiles(currentName)

            // Includes a file in the analysis, if relative to its rootpath it matches the import
            // statement. Both possible file endings (.py, pyi) are considered.
            val match =
                ctx.additionalSources.firstOrNull {
                    // Check if the relative file is in our candidates
                    candidates.contains(it.relative)
                }
            if (match != null) {
                // Add the match to the imported sources
                ctx.importedSources += match
            }

            currentName = currentName.parent
        }
    }

    /** Small utility function to check, whether we are inside an __init__ module. */
    private fun isInitModule(): Boolean =
        (frontend.scopeManager.firstScopeIsInstanceOrNull<NameScope>()?.astNode
                as? NamespaceDeclaration)
            ?.path
            ?.endsWith(PythonLanguage.IDENTIFIER_INIT) == true

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
        val tempVarName = Name.temporary(prefix = LOOP_VAR_PREFIX, separatorChar = '_', loopVar)
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
        lhs.assignedTypes += frontend.typeOf(node.annotation)
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
            lhs.forEach { it.assignedTypes += tpe }
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

    /**
     * Translates a Python [`Global`](https://docs.python.org/3/library/ast.html#ast.Global) into a
     * [LookupScopeStatement].
     */
    private fun handleGlobal(global: Python.AST.Global): LookupScopeStatement {
        // Technically, our global scope is not identical to the python "global" scope. The reason
        // behind that is that we wrap each file in a namespace (as defined in the python spec). So
        // the "global" scope is actually our current namespace scope.
        val pythonGlobalScope =
            frontend.scopeManager.globalScope.children.firstOrNull { it is NamespaceScope }

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
        // We need to find the first outer function scope
        val outerFunctionScope =
            frontend.scopeManager.firstScopeOrNull {
                it is FunctionScope && it != frontend.scopeManager.currentScope
            }

        return newLookupScopeStatement(
            global.names.map { parseName(it).localName },
            outerFunctionScope,
            rawNode = global,
        )
    }

    /**
     * This function "wraps" a list of [Python.AST.BaseStmt] nodes into a [Block]. Since the list
     * itself does not have a code/location, we need to employ [codeAndLocationFromChildren] on the
     * [parentNode].
     */
    internal fun makeBlock(
        statements: List<Python.AST.BaseStmt>,
        parentNode: Python.AST.WithLocation,
    ): Block {
        val result = newBlock()

        for (stmt in statements) {
            result.statements += handle(stmt)
        }

        // We need to scope the call to codeAndLocationFromChildren to our frontend, so that
        // all Python.AST.AST nodes are accepted, otherwise it would be scoped to the handler
        // and only Python.AST.BaseStmt nodes would be accepted. This would cause issues with
        // other nodes that are not "statements", but also handled as part of this handler,
        // e.g., the Python.AST.ExceptHandler.
        with(frontend) { result.codeAndLocationFromChildren(parentNode, frontend.lineSeparator) }

        return result
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
    internal fun addAsyncWarning(mightBeAsync: Python.AST.AsyncOrNot, parentNode: Node) {
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
