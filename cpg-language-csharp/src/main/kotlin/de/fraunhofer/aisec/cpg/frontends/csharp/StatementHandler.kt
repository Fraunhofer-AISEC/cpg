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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.ProblemNode
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
import de.fraunhofer.aisec.cpg.graph.newEmpty
import de.fraunhofer.aisec.cpg.graph.newExpressionList
import de.fraunhofer.aisec.cpg.graph.newFor
import de.fraunhofer.aisec.cpg.graph.newForEach
import de.fraunhofer.aisec.cpg.graph.newIfElse
import de.fraunhofer.aisec.cpg.graph.newMemberAccess
import de.fraunhofer.aisec.cpg.graph.newMemberCall
import de.fraunhofer.aisec.cpg.graph.newProblemExpression
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.newReturn
import de.fraunhofer.aisec.cpg.graph.newSwitch
import de.fraunhofer.aisec.cpg.graph.newThrow
import de.fraunhofer.aisec.cpg.graph.newTry
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.newWhile
import de.fraunhofer.aisec.cpg.graph.types.Type

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
            is Csharp.AST.CheckedStatementSyntax -> handleCheckedStatement(node)
            is Csharp.AST.UnsafeStatementSyntax -> handleUnsafeStatement(node)
            is Csharp.AST.UsingStatementSyntax -> handleUsing(node)
            is Csharp.AST.EmptyStatementSyntax -> handleEmptyStatement(node)
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
     * Translates a [CheckedStatementSyntax][Csharp.AST.CheckedStatementSyntax] (e.g. `checked { ...
     * }` or `unchecked { ... }`) into the [Block] of its body.
     *
     * Whether arithmetic overflow throws an `OverflowException` or wraps around is not modeled, as
     * it has no direct effect on control or data flow, but it is recorded as a
     * [ProblemNode.ProblemType.TRANSLATION] problem.
     *
     * C# spec:
     * [The checked and unchecked statements](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1312-the-checked-and-unchecked-statements)
     */
    private fun handleCheckedStatement(node: Csharp.AST.CheckedStatementSyntax): Expression {
        val block = handle(node.block)
        // `checked` and `unchecked` share the syntax class, so the kind tells them apart
        block.additionalProblems +=
            newProblemExpression(
                "The overflow behaviour of a ${Csharp.INSTANCE.GetKind(node.pointer)} is not modeled",
                type = ProblemNode.ProblemType.TRANSLATION,
                rawNode = node,
            )
        return block
    }

    /**
     * Translates an [UnsafeStatementSyntax][Csharp.AST.UnsafeStatementSyntax] (e.g. `unsafe { ...
     * }`) into the [Block] of its body.
     *
     * The unsafe context, which only permits pointer types and pointer arithmetic inside the block,
     * is not modeled, but it is recorded as a [ProblemNode.ProblemType.TRANSLATION] problem.
     *
     * C# spec:
     * [Unsafe contexts](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/unsafe-code#232-unsafe-contexts)
     */
    private fun handleUnsafeStatement(node: Csharp.AST.UnsafeStatementSyntax): Expression {
        val block = handle(node.block)
        block.additionalProblems +=
            newProblemExpression(
                "The unsafe context of an UnsafeStatement is not modeled",
                type = ProblemNode.ProblemType.TRANSLATION,
                rawNode = node,
            )
        return block
    }

    /**
     * Translates an [EmptyStatementSyntax][Csharp.AST.EmptyStatementSyntax], i.e. a lone `;`, into
     * an [Empty].
     *
     * C# spec:
     * [The empty statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#134-the-empty-statement)
     */
    private fun handleEmptyStatement(node: Csharp.AST.EmptyStatementSyntax): Empty {
        return newEmpty(rawNode = node)
    }

    /**
     * Translates a [BlockSyntax][Csharp.AST.BlockSyntax] into a [Block].
     *
     * C# spec:
     * [Block](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#133-blocks)
     */
    private fun handleBlock(node: Csharp.AST.BlockSyntax): Block {
        val block = newBlock(rawNode = node)
        block.statements += handleStatements(node.statements)
        return block
    }

    /**
     * Translates the statements of a block.
     *
     * This is more than a loop over [handle], because a using declaration (`using var f = ...;`)
     * disposes its resource at the end of the *enclosing* block and therefore needs the statements
     * that follow it, see [handleUsing]. Blocks without a using declaration are unaffected.
     */
    private fun handleStatements(statements: List<Csharp.AST.StatementSyntax>): List<Expression> {
        val result = mutableListOf<Expression>()

        for ((index, stmt) in statements.withIndex()) {
            if (
                stmt is Csharp.AST.LocalDeclarationStatementSyntax && stmt.usingKeyword.isNotEmpty()
            ) {
                // The using declaration takes the rest of the block as its body, so we are done
                result += handleUsing(stmt, statementsAfter = statements.drop(index + 1))
                break
            }
            result += handle(stmt)
        }

        return result
    }

    /**
     * Translates a `using` into a [Try], both the statement form
     * ([UsingStatementSyntax][Csharp.AST.UsingStatementSyntax]) and the declaration form (a
     * [LocalDeclarationStatementSyntax][Csharp.AST.LocalDeclarationStatementSyntax] carrying a
     * `using` keyword). The two forms differ only in how they delimit the body of the `using`; the
     * declaration form takes the [statementsAfter] it in the enclosing block.
     *
     * Since we do not have a direct representation for a `using`, we destructure it into the
     * `try`/`finally` that C# defines it to be. For example:
     * ```csharp
     * using (var f = File.OpenRead(path))
     * {
     *     Read(f);
     * }
     * ```
     *
     * is equivalent to:
     * ```csharp
     * var f = File.OpenRead(path);
     * try
     * {
     *     Read(f);
     * }
     * finally
     * {
     *     f.Dispose();
     * }
     * ```
     *
     * The [Try] contains:
     * 1. A [DeclarationStatement] as its [Try.resources]: `f = File.OpenRead(path)`
     * 2. The body of the `using` as its [Try.tryBlock]
     * 3. An implicit `f.Dispose()` [MemberCall] as its [Try.finallyBlock]
     *
     * Modeling the [Try.resources] instead of as a statement preceding the [Try] keeps the C#
     * semantics that an exception thrown *while acquiring* a resource skips both the body and the
     * disposal. Putting the disposal in the [Try.finallyBlock] puts it on every path leaving the
     * body, including `return`, `break` and exceptions.
     *
     * The declaration form has no body of its own, it disposes at the end of the *enclosing* block.
     * So everything following it becomes the [Try.tryBlock], see [handleStatements]:
     * ```csharp
     * var path = "log.txt";
     * using var f = File.OpenRead(path);
     * Read(f);
     * ```
     *
     * is equivalent to:
     * ```csharp
     * var path = "log.txt";
     * var f = File.OpenRead(path);
     * try
     * {
     *     Read(f);
     * }
     * finally
     * {
     *     f.Dispose();
     * }
     * ```
     *
     * The expression form disposes an already existing value and therefore has no variable to
     * dispose. Like the C# compiler, we bind the value to an implicit temporary first:
     * ```csharp
     * using (f)
     * {
     *     Read(f);
     * }
     * ```
     *
     * is equivalent to:
     * ```csharp
     * var usingResource_0 = f;
     * try
     * {
     *     Read(f);
     * }
     * finally
     * {
     *     usingResource_0.Dispose();
     * }
     * ```
     *
     * Two details of the lowering specified by C# are deliberately not modeled: the `if (f !=
     * null)` guard around the disposal, because a conditional disposal would make a correctly
     * disposed resource look undisposed on some paths, and the `await` around `DisposeAsync` for an
     * `await using`, because `await` is not modeled at all yet.
     *
     * C# spec:
     * [The using statement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1314-the-using-statement)
     */
    private fun handleUsing(
        node: Csharp.AST.UsingStatementOrDeclaration,
        statementsAfter: List<Csharp.AST.StatementSyntax> = emptyList(),
    ): Expression {
        val awaitKeyword = node.awaitKeyword.isNotEmpty()

        /** Prepares the block the resource is used in, which the two forms delimit differently. */
        val body = {
            if (node is Csharp.AST.UsingStatementSyntax) {
                bodyOf(node.statement)
            } else {
                newBlock().implicit().apply { statements += handleStatements(statementsAfter) }
            }
        }

        /**
         * Prepares the resource declaration of a `using` that declares a variable. Represents the
         * line
         *
         * ```csharp
         * var f = File.OpenRead(path);
         * ```
         */
        fun generateDeclaredResource(
            declarator: Csharp.AST.VariableDeclaratorSyntax,
            type: Type,
        ): Pair<DeclarationStatement, Variable> {
            val variable =
                newVariable(name = declarator.identifier, type = type, rawNode = declarator)
            declarator.initializer?.let {
                variable.initializer = frontend.expressionHandler.handle(it)
            }
            frontend.scopeManager.addDeclaration(variable)

            val declStmt = newDeclarationStatement(rawNode = declarator)
            declStmt.declarations += variable
            return Pair(declStmt, variable)
        }

        /**
         * Prepares the implicit temporary holding the resource of a `using (f)`, which disposes an
         * already existing value instead of declaring a variable. Represents the line
         *
         * ```csharp
         * var usingResource_... = f;
         * ```
         *
         * C# declares the same temporary, called `resource` in the expansion of the spec. It is not
         * in scope for the body, which keeps referring to the original value:
         * ```csharp
         * TextWriter resource = f;
         * try
         * {
         *     Read(f);
         * }
         * finally
         * {
         *     resource.Dispose();
         * }
         * ```
         */
        fun generateTemporaryResource(
            tryStmt: Try,
            expression: Csharp.AST.ExpressionSyntax,
        ): Pair<DeclarationStatement, Variable> {
            val resource = frontend.expressionHandler.handle(expression)
            val tmpName = Name.temporary(prefix = USING_RESOURCE, separatorChar = '_', tryStmt)
            val tmpVar =
                newVariable(name = tmpName, type = resource.type)
                    .implicit(code = resource.code, location = resource.location)
            tmpVar.initializer = resource
            frontend.scopeManager.addDeclaration(tmpVar)

            val declStmt =
                newDeclarationStatement()
                    .implicit(code = resource.code, location = resource.location)
            declStmt.declarations += tmpVar
            return Pair(declStmt, tmpVar)
        }

        /**
         * Prepares the `finally` block disposing [resource]. Represents the line
         *
         * ```csharp
         * f.Dispose();
         * ```
         */
        fun generateDisposeBlock(resource: Variable): Block {
            val code = resource.code
            val location = resource.location

            val base = newReference(name = resource.name).implicit(code = code, location = location)
            base.refersTo = resource
            val dispose =
                newMemberCall(
                        newMemberAccess(
                                name = if (awaitKeyword) "DisposeAsync" else "Dispose",
                                base = base,
                            )
                            .implicit(code = code, location = location)
                    )
                    .implicit(code = code, location = location)

            return newBlock().implicit(code = code, location = location).apply {
                statements += dispose
            }
        }

        /**
         * Builds the [Try] for the first of [declarators] and nests the remaining ones inside it,
         * since a `using` declaring several resources is defined as nested `using` statements:
         * ```csharp
         * using (var a = File.OpenRead("a"), b = File.OpenRead("b"))
         * {
         *     Read(a, b);
         * }
         * ```
         *
         * is equivalent to:
         * ```csharp
         * using (var a = File.OpenRead("a"))
         * {
         *     using (var b = File.OpenRead("b"))
         *     {
         *         Read(a, b);
         *     }
         * }
         * ```
         *
         * The nesting is what gives us the reverse disposal order, `b` before `a`. The resources
         * are declared in the scope of their own [Try], matching C#, where a resource is not
         * visible after its `using`.
         */
        fun generateTry(declarators: List<Csharp.AST.VariableDeclaratorSyntax>, type: Type): Try {
            val tryStmt = newTry(rawNode = node)
            frontend.scopeManager.enterScope(tryStmt)

            val (declStmt, variable) = generateDeclaredResource(declarators.first(), type)
            tryStmt.resources += declStmt
            tryStmt.tryBlock =
                if (declarators.size > 1) {
                    newBlock().implicit().apply {
                        statements += generateTry(declarators.drop(1), type)
                    }
                } else {
                    body()
                }
            tryStmt.finallyBlock = generateDisposeBlock(variable)

            frontend.scopeManager.leaveScope(tryStmt)
            return tryStmt
        }

        val declaration = node.declaration
        // Only the statement form can dispose an already existing value, a using declaration
        // always declares one.
        val expression = (node as? Csharp.AST.UsingStatementSyntax)?.expression

        return when {
            // using (var f = File.OpenRead(path)) { ... } and using var f = File.OpenRead(path);
            declaration != null ->
                generateTry(declaration.variables, frontend.typeOf(declaration.type))

            // using (f) { ... }
            expression != null -> {
                val tryStmt = newTry(rawNode = node)
                frontend.scopeManager.enterScope(tryStmt)

                val (declStmt, variable) = generateTemporaryResource(tryStmt, expression)
                tryStmt.resources += declStmt
                tryStmt.tryBlock = body()
                tryStmt.finallyBlock = generateDisposeBlock(variable)

                frontend.scopeManager.leaveScope(tryStmt)
                tryStmt
            }

            else -> newProblemExpression("using statement without a resource", rawNode = node)
        }
    }

    /**
     * Returns the body of a `using` statement as a [Block]. The body does not have to be a block
     * (e.g. `using (f) Read(f);`), in which case we wrap the single statement in an implicit one,
     * since [Try.tryBlock] requires a [Block].
     */
    private fun bodyOf(node: Csharp.AST.StatementSyntax): Block {
        if (node is Csharp.AST.BlockSyntax) {
            return handleBlock(node)
        }

        val statement = handle(node)
        return newBlock().implicit(code = statement.code, location = statement.location).apply {
            statements += statement
        }
    }
}
