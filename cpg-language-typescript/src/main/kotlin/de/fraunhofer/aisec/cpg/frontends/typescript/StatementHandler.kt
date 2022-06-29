/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.ExperimentalTypeScript
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression

@ExperimentalTypeScript
class StatementHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Statement, TypeScriptNode, TypeScriptLanguageFrontend>(::ProblemExpression, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    private fun handleNode(node: TypeScriptNode): Statement {
        when (node.type) {
            "Block" -> return handleBlock(node)
            "FirstStatement" -> return handleVariableStatement(node)
            "VariableStatement" -> return handleVariableStatement(node)
            "ExpressionStatement" -> return handleExpressionStatement(node)
            "ReturnStatement" -> return handleReturnStatement(node)
            "FunctionDeclaration" -> return handleFunctionDeclaration(node)
        }

        return ProblemExpression("No handler was implemented for nodes of type " + node.type)
    }

    private fun handleFunctionDeclaration(node: TypeScriptNode): Statement {
        // typescript allows to declare function on a statement level, e.g. within a compound
        // statement. We can wrap it into a declaration statement
        val statement = NodeBuilder.newDeclarationStatement(this.lang.getCodeFromRawNode(node))

        val decl = this.lang.declarationHandler.handle(node)

        statement.addToPropertyEdgeDeclaration(decl)

        this.lang.scopeManager.addDeclaration(decl)

        return statement
    }

    private fun handleReturnStatement(node: TypeScriptNode): ReturnStatement {
        val returnStmt = NodeBuilder.newReturnStatement(this.lang.getCodeFromRawNode(node))

        node.children?.first()?.let {
            returnStmt.returnValue = this.lang.expressionHandler.handle(it)
        }

        return returnStmt
    }

    private fun handleBlock(node: TypeScriptNode): CompoundStatement {
        val block = NodeBuilder.newCompoundStatement(this.lang.getCodeFromRawNode(node))

        node.children?.forEach { block.addStatement(this.handle(it)) }

        return block
    }

    private fun handleExpressionStatement(node: TypeScriptNode): Expression {
        // unwrap it and directly forward it to the expression handler
        // this is possible because in our CPG, expression inherit from statements
        // and can be directly added to a compound statement
        return this.lang.expressionHandler.handle(node.children?.first())
    }

    private fun handleVariableStatement(node: TypeScriptNode): DeclarationStatement {
        val statement = NodeBuilder.newDeclarationStatement(this.lang.getCodeFromRawNode(node))

        // the declarations are contained in a VariableDeclarationList
        val nodes = node.firstChild("VariableDeclarationList")?.children

        for (variableNode in nodes ?: emptyList()) {
            val decl = this.lang.declarationHandler.handle(variableNode)

            statement.addToPropertyEdgeDeclaration(decl)

            this.lang.scopeManager.addDeclaration(decl)
        }

        return statement
    }
}
