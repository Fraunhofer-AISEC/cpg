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
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.DoWhile
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.For
import de.fraunhofer.aisec.cpg.graph.expressions.ForEach
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.expressions.While
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newDoWhile
import de.fraunhofer.aisec.cpg.graph.newExpressionList
import de.fraunhofer.aisec.cpg.graph.newFor
import de.fraunhofer.aisec.cpg.graph.newForEach
import de.fraunhofer.aisec.cpg.graph.newIfElse
import de.fraunhofer.aisec.cpg.graph.newReturn
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
