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

import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.newBlock

class StatementHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Expression, Csharp.AST.StatementSyntax>(
        configConstructor = ::ProblemExpression,
        frontend = frontend,
    ) {
    override fun handleNode(node: Csharp.AST.StatementSyntax): Expression {
        return when (node) {
            is Csharp.AST.BlockSyntax -> handleBlock(node)
            else -> ProblemExpression("Not supported: ${node.csharpType}")
        }
    }

    /**
     * Translates a C#
     * [`BlockSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.blocksyntax?view=roslyn-dotnet-5.0.0)
     * into a [Block].
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
