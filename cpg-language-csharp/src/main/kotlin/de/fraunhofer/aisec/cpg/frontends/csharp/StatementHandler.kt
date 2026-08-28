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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.implicit
import de.fraunhofer.aisec.cpg.graph.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newBreak
import de.fraunhofer.aisec.cpg.graph.newCase
import de.fraunhofer.aisec.cpg.graph.newCatchClause
import de.fraunhofer.aisec.cpg.graph.newContinue
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newDefault
import de.fraunhofer.aisec.cpg.graph.newDoWhile
import de.fraunhofer.aisec.cpg.graph.newExpressionList
import de.fraunhofer.aisec.cpg.graph.newFor
import de.fraunhofer.aisec.cpg.graph.newForEach
import de.fraunhofer.aisec.cpg.graph.newIfElse
import de.fraunhofer.aisec.cpg.graph.newReturn
import de.fraunhofer.aisec.cpg.graph.newSwitch
import de.fraunhofer.aisec.cpg.graph.newThrow
import de.fraunhofer.aisec.cpg.graph.newTry
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.newWhile

class StatementHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Expression, Csharp.AST.StatementSyntax>(
        configConstructor = ::ProblemExpression,
        frontend = frontend,
    ) {
    override fun handleNode(node: Csharp.AST.StatementSyntax): Expression {
        return when (node) {
            is Csharp.AST.BlockSyntax -> handleBlock(node)
            is Csharp.AST.ReturnStatementSyntax -> handleReturn(node)
            is Csharp.AST.IfStatementSyntax -> handleIf(node)
            is Csharp.AST.LocalDeclarationStatementSyntax -> handleLocalDeclaration(node)
            is Csharp.AST.ExpressionStatementSyntax -> handleExpressionStatement(node)
            is Csharp.AST.WhileStatementSyntax -> handleWhile(node)
            is Csharp.AST.DoStatementSyntax -> handleDoWhile(node)
            is Csharp.AST.ForStatementSyntax -> handleFor(node)
            is Csharp.AST.ForEachStatementSyntax -> handleForEach(node)
            is Csharp.AST.SwitchStatementSyntax -> handleSwitch(node)
            is Csharp.AST.BreakStatementSyntax -> handleBreak(node)
            is Csharp.AST.ContinueStatementSyntax -> handleContinue(node)
            is Csharp.AST.TryStatementSyntax -> handleTry(node)
            is Csharp.AST.ThrowStatementSyntax -> handleThrow(node)
            else -> ProblemExpression("Not supported: ${node.csharpType}")
        }
    }

    /**
     * Translates an [IfStatementSyntax][Csharp.AST.IfStatementSyntax] into an [IfElse].
     *
     * C# spec:
     * [IfStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1382-the-if-statement)
     */
    private fun handleIf(node: Csharp.AST.IfStatementSyntax): IfElse {
        return newIfElse(rawNode = node).apply {
            this.condition = frontend.expressionHandler.handle(node.condition)
            this.thenStatement = handle(node.statement)
            node.elseClause?.let { this.elseStatement = handle(it.statement) }
        }
    }

    /**
     * Translates a [ReturnStatementSyntax][Csharp.AST.ReturnStatementSyntax] into a [Return]. The
     * return value expression is optional (e.g. `return;` in void methods).
     *
     * C# spec:
     * [Return statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#13105-the-return-statement)
     */
    private fun handleReturn(node: Csharp.AST.ReturnStatementSyntax): Return {
        val ret = newReturn(rawNode = node)
        node.expression?.let { ret.returnValue = frontend.expressionHandler.handle(it) }
        return ret
    }

    /**
     * Translates a [LocalDeclarationStatementSyntax][Csharp.AST.LocalDeclarationStatementSyntax]
     * into a [DeclarationStatement]. Each
     * [VariableDeclaratorSyntax][Csharp.AST.VariableDeclaratorSyntax] is translated into a
     * [Variable] and added to the current scope.
     *
     * C# spec:
     * [Local variable declarations](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1362-local-variable-declarations)
     */
    private fun handleLocalDeclaration(
        node: Csharp.AST.LocalDeclarationStatementSyntax
    ): DeclarationStatement {
        val declStmt = newDeclarationStatement(rawNode = node)
        val declaration = node.declaration
        val type = frontend.typeOf(declaration.type)

        for (variable in declaration.variables) {
            val v = newVariable(name = variable.identifier, type = type, rawNode = variable)
            variable.initializer?.let { v.initializer = frontend.expressionHandler.handle(it) }
            frontend.scopeManager.addDeclaration(v)
            declStmt.declarations += v
        }

        return declStmt
    }

    /**
     * Translates an [ExpressionStatementSyntax][Csharp.AST.ExpressionStatementSyntax] into an
     * [Expression].
     *
     * C# spec:
     * [Expression statements](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#137-expression-statements)
     */
    private fun handleExpressionStatement(node: Csharp.AST.ExpressionStatementSyntax): Expression {
        return frontend.expressionHandler.handle(node.expression)
    }

    /**
     * Translates a [WhileStatementSyntax][Csharp.AST.WhileStatementSyntax] into a [While].
     *
     * C# spec:
     * [WhileStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1392-the-while-statement)
     */
    private fun handleWhile(node: Csharp.AST.WhileStatementSyntax): While {
        val whileStmt = newWhile(rawNode = node)
        whileStmt.condition = frontend.expressionHandler.handle(node.condition)
        whileStmt.statement = handle(node.statement)
        return whileStmt
    }

    /**
     * Translates a [DoStatementSyntax][Csharp.AST.DoStatementSyntax] into a [DoWhile].
     *
     * C# spec:
     * [DoStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1393-the-do-statement)
     */
    private fun handleDoWhile(node: Csharp.AST.DoStatementSyntax): DoWhile {
        val doStmt = newDoWhile(rawNode = node)
        doStmt.condition = frontend.expressionHandler.handle(node.condition)
        doStmt.statement = handle(node.statement)
        return doStmt
    }

    /**
     * Translates a [ForStatementSyntax][Csharp.AST.ForStatementSyntax] into a [For].
     *
     * C# spec:
     * [ForStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1394-the-for-statement)
     */
    private fun handleFor(node: Csharp.AST.ForStatementSyntax): For {
        val forStmt = newFor(rawNode = node)
        frontend.scopeManager.enterScope(forStmt)

        // The initializer can be either a variable declaration (`for (int i = 0; ...)`)
        // or one/more expressions (`for (i = 0, j = 0; ...)`), so we check for the declaration
        // first and fall back to the expression form.
        val declaration = node.declaration
        if (declaration != null) {
            val declStmt = newDeclarationStatement(rawNode = declaration)
            val type = frontend.typeOf(declaration.type)
            for (variable in declaration.variables) {
                val v = newVariable(name = variable.identifier, type = type, rawNode = variable)
                variable.initializer?.let { v.initializer = frontend.expressionHandler.handle(it) }
                frontend.scopeManager.addDeclaration(v)
                declStmt.declarations += v
            }
            forStmt.initializerStatement = declStmt
        } else if (node.initializerExpressions.size == 1) {
            // Single expression initializer (e.g. `for (i = 0; ...)`)
            forStmt.initializerStatement =
                frontend.expressionHandler.handle(node.initializerExpressions[0])
        } else if (node.initializerExpressions.size > 1) {
            // Multiple expression initializers (e.g. `for (i = 0, j = 0; ...)`)
            val list = newExpressionList()
            for (expr in node.initializerExpressions) {
                list.expressions += frontend.expressionHandler.handle(expr)
            }
            forStmt.initializerStatement = list
        }

        // Condition is optional in C#.
        node.condition?.let { forStmt.condition = frontend.expressionHandler.handle(it) }

        // It can be multiple Incrementors (e.g. 'i++, j--').
        if (node.incrementors.size == 1) {
            forStmt.iterationStatement = frontend.expressionHandler.handle(node.incrementors[0])
        } else if (node.incrementors.size > 1) {
            val list = newExpressionList()
            for (incr in node.incrementors) {
                list.expressions += frontend.expressionHandler.handle(incr)
            }
            forStmt.iterationStatement = list
        }
        forStmt.statement = handle(node.statement)
        frontend.scopeManager.leaveScope(forStmt)
        return forStmt
    }

    /**
     * Translates a [ForEachStatementSyntax][Csharp.AST.ForEachStatementSyntax] into a [ForEach].
     *
     * C# spec:
     * [ForeachStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1395-the-foreach-statement)
     */
    private fun handleForEach(node: Csharp.AST.ForEachStatementSyntax): ForEach {
        val forEachStmt = newForEach(rawNode = node)
        frontend.scopeManager.enterScope(forEachStmt)
        val type = frontend.typeOf(node.type)
        val variable = newVariable(name = node.identifier, type = type, rawNode = node)
        frontend.scopeManager.addDeclaration(variable)
        val declStmt = newDeclarationStatement(rawNode = node)
        declStmt.declarations += variable
        forEachStmt.variable = declStmt
        forEachStmt.iterable = frontend.expressionHandler.handle(node.expression)
        forEachStmt.statement = handle(node.statement)
        frontend.scopeManager.leaveScope(forEachStmt)
        return forEachStmt
    }

    /**
     * Translates a [SwitchStatementSyntax][Csharp.AST.SwitchStatementSyntax] into a [Switch].
     *
     * C# spec:
     * [SwitchStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1383-the-switch-statement)
     */
    private fun handleSwitch(node: Csharp.AST.SwitchStatementSyntax): Switch {
        val switchStmt = newSwitch(rawNode = node)
        frontend.scopeManager.enterScope(switchStmt)
        switchStmt.selector = frontend.expressionHandler.handle(node.expression)

        val block = newBlock()
        for (section in node.sections) {
            for (label in section.labels) {
                when (label) {
                    is Csharp.AST.CaseSwitchLabelSyntax -> {
                        val caseStmt = newCase(rawNode = label)
                        caseStmt.caseExpression = frontend.expressionHandler.handle(label.value)
                        block.statements += caseStmt
                    }
                    is Csharp.AST.CasePatternSwitchLabelSyntax -> {
                        block.statements += handleCasePattern(label)
                    }
                    is Csharp.AST.DefaultSwitchLabelSyntax -> {
                        block.statements += newDefault(rawNode = label)
                    }
                }
            }
            for (stmt in section.statements) {
                block.statements += handle(stmt)
            }
        }
        switchStmt.statement = block

        frontend.scopeManager.leaveScope(switchStmt)
        return switchStmt
    }

    /**
     * Handles a [CasePatternSwitchLabelSyntax][Csharp.AST.CasePatternSwitchLabelSyntax], which
     * represents C# pattern matching in switch statements, e.g. `case var a when condition:`.
     *
     * This is modeled as follows:
     * - A [VarPatternSyntax][Csharp.AST.VarPatternSyntax] is translated into a variable declaration
     *   based on its [VariableDesignationSyntax][Csharp.AST.VariableDesignationSyntax] (e.g.
     *   `SingleVariableDesignationSyntax` for a single variable like `a` in `case var a`).
     * - If a `when` clause is present, the case expression is wrapped in an implicit `and`
     *   [BinaryOperator] with lhs as a declaration and rhs with a condition.
     */
    private fun handleCasePattern(node: Csharp.AST.CasePatternSwitchLabelSyntax): Expression {
        val caseStmt = newCase(rawNode = node)
        val pattern = node.pattern
        val whenClause = node.whenClause

        var patternExpr =
            when (pattern) {
                is Csharp.AST.VarPatternSyntax -> {
                    val identifier =
                        when (val designation = pattern.designation) {
                            is Csharp.AST.SingleVariableDesignationSyntax -> designation.identifier
                            else ->
                                return ProblemExpression(
                                    "Variable designation type not yet supported: ${designation.csharpType}"
                                )
                        }
                    val variable = newVariable(name = identifier, rawNode = pattern)
                    frontend.scopeManager.addDeclaration(variable)
                    val declStmt = newDeclarationStatement(rawNode = pattern)
                    declStmt.declarations += variable
                    declStmt
                }
                else -> {
                    ProblemExpression("Pattern type not yet supported: ${pattern.csharpType}")
                }
            }

        if (whenClause != null) {
            val whenCondition = frontend.expressionHandler.handle(whenClause.condition)
            patternExpr =
                newBinaryOperator(operatorCode = "and").implicit().apply {
                    this.lhs = patternExpr
                    this.rhs = whenCondition
                }
        }

        caseStmt.caseExpression = patternExpr
        return caseStmt
    }

    /**
     * Translates a [BreakStatementSyntax][Csharp.AST.BreakStatementSyntax] into a [Break].
     *
     * C# spec:
     * [Break statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#13101-the-break-statement)
     */
    private fun handleBreak(node: Csharp.AST.BreakStatementSyntax): Break {
        return newBreak(rawNode = node)
    }

    /**
     * Translates a [ContinueStatementSyntax][Csharp.AST.ContinueStatementSyntax] into a [Continue].
     * C# has no labelled `continue`, so [Continue.label] stays `null`.
     *
     * C# spec:
     * [Continue statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#13102-the-continue-statement)
     */
    private fun handleContinue(node: Csharp.AST.ContinueStatementSyntax): Continue {
        return newContinue(rawNode = node)
    }

    /**
     * Translates a [TryStatementSyntax][Csharp.AST.TryStatementSyntax] into a [Try]. C# has no
     * `else` block, so [Try.elseBlock] stays empty, and its resources are declared by a `using`
     * statement rather than by the `try` itself, so [Try.resources] stays empty as well.
     *
     * C# spec:
     * [The try statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1311-the-try-statement)
     */
    private fun handleTry(node: Csharp.AST.TryStatementSyntax): Try {
        val tryStmt = newTry(rawNode = node)
        frontend.scopeManager.enterScope(tryStmt)
        tryStmt.tryBlock = handleBlock(node.block)
        tryStmt.catchClauses = node.catches.map { handleCatchClause(it) }.toMutableList()
        node.finallyBlock?.let { tryStmt.finallyBlock = handleBlock(it) }
        frontend.scopeManager.leaveScope(tryStmt)
        return tryStmt
    }

    /**
     * Translates a [CatchClauseSyntax][Csharp.AST.CatchClauseSyntax] into a [CatchClause].
     *
     * The caught exception becomes the [CatchClause.parameter]. A general catch clause (`catch {
     * }`) has no declaration and no parameter, while `catch (Exception) { }` declares the type but
     * does not bind it to a variable, in which case the parameter has an empty name.
     *
     * We model an exception filter (`catch (Exception e) when (cond)`) by wrapping the body in an
     * implicit [IfElse] guarded by the filter. This keeps the filter expression (and any calls in
     * it).
     *
     * C# spec:
     * [The try statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1311-the-try-statement)
     */
    private fun handleCatchClause(node: Csharp.AST.CatchClauseSyntax): CatchClause {
        val catchClause = newCatchClause(rawNode = node)
        frontend.scopeManager.enterScope(catchClause)

        node.declaration?.let { declaration ->
            val parameter =
                newVariable(
                    name = declaration.identifier,
                    type = frontend.typeOf(declaration.type),
                    rawNode = declaration,
                )
            frontend.scopeManager.addDeclaration(parameter)
            catchClause.parameter = parameter
        }

        val body = handleBlock(node.block)
        // The filter can refer to the parameter, so it has to be handled after the declaration.
        val filterExpression = node.filterExpression
        catchClause.body =
            if (filterExpression == null) {
                body
            } else {
                val guard =
                    newIfElse().implicit(code = body.code, location = body.location).apply {
                        this.condition = frontend.expressionHandler.handle(filterExpression)
                        this.thenStatement = body
                    }
                newBlock().implicit(code = body.code, location = body.location).apply {
                    this.statements += guard
                }
            }

        frontend.scopeManager.leaveScope(catchClause)
        return catchClause
    }

    /**
     * Translates a [ThrowStatementSyntax][Csharp.AST.ThrowStatementSyntax] into a [Throw]. The
     * exception is optional: a `throw;` inside a catch clause rethrows the current exception, so
     * [Throw.exception] stays `null`.
     *
     * C# spec:
     * [The throw statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1310-the-throw-statement)
     */
    private fun handleThrow(node: Csharp.AST.ThrowStatementSyntax): Throw {
        val throwStmt = newThrow(rawNode = node)
        node.expression?.let { throwStmt.exception = frontend.expressionHandler.handle(it) }
        return throwStmt
    }

    /**
     * Translates a [BlockSyntax][Csharp.AST.BlockSyntax] into a [Block].
     *
     * C# spec:
     * [Block](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#133-blocks)
     */
    private fun handleBlock(node: Csharp.AST.BlockSyntax): Block {
        val block = newBlock(rawNode = node)
        for (stmt in node.statements) {
            val statement = handle(stmt)
            statement.let { block.statements += it }
        }
        return block
    }
}
