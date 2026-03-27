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
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newIfElse
import de.fraunhofer.aisec.cpg.graph.newReturn
import de.fraunhofer.aisec.cpg.graph.newVariable

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
     * [ReturnStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#13105-the-return-statement)
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
     * [LocalVariableDeclaration](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1364-local-variable-declarations)
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
     * [ExpressionStatement](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1372-expression-statements)
     */
    private fun handleExpressionStatement(node: Csharp.AST.ExpressionStatementSyntax): Expression {
        return frontend.expressionHandler.handle(node.expression)
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
