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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.newCompoundStatement
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression

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
        val statement = newDeclarationStatement(this.frontend.getCodeFromRawNode(node))

        val decl = this.frontend.declarationHandler.handle(node)

        if (decl != null) {
            statement.addToPropertyEdgeDeclaration(decl)
        }

        this.frontend.scopeManager.addDeclaration(decl)

        return statement
    }

    private fun handleReturnStatement(node: TypeScriptNode): ReturnStatement {
        val returnStmt = newReturnStatement(this.frontend.getCodeFromRawNode(node))

        node.children?.first()?.let {
            returnStmt.returnValue = this.frontend.expressionHandler.handle(it)
        }

        return returnStmt
    }

    private fun handleBlock(node: TypeScriptNode): CompoundStatement {
        val block = newCompoundStatement(this.frontend.getCodeFromRawNode(node))

        node.children?.forEach { this.handle(it)?.let { it1 -> block.addStatement(it1) } }

        return block
    }

    private fun handleExpressionStatement(node: TypeScriptNode): Expression {
        // unwrap it and directly forward it to the expression handler
        // this is possible because in our CPG, expression inherit from statements
        // and can be directly added to a compound statement
        return node.children?.first()?.let { this.frontend.expressionHandler.handle(it) }
            ?: ProblemExpression("problem parsing expression")
    }

    private fun handleVariableStatement(node: TypeScriptNode): DeclarationStatement {
        val statement = newDeclarationStatement(this.frontend.getCodeFromRawNode(node))

        // the declarations are contained in a VariableDeclarationList
        val nodes = node.firstChild("VariableDeclarationList")?.children

        for (variableNode in nodes ?: emptyList()) {
            val decl = this.frontend.declarationHandler.handle(variableNode)

            if (decl != null) {
                statement.addToPropertyEdgeDeclaration(decl)
            }

            this.frontend.scopeManager.addDeclaration(decl)
        }

        return statement
    }
}
