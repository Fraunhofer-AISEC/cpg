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
package de.fraunhofer.aisec.cpg.frontends.ruby

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Problem
import de.fraunhofer.aisec.cpg.graph.newFunction
import de.fraunhofer.aisec.cpg.graph.newParameter
import de.fraunhofer.aisec.cpg.graph.newReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import org.jruby.ast.ArgumentNode
import org.jruby.ast.DefnNode
import org.jruby.ast.Node

class DeclarationHandler(lang: RubyLanguageFrontend) :
    RubyHandler<Declaration, Node>({ Problem() }, lang) {

    override fun handleNode(node: Node): Declaration {
        return when (node) {
            is ArgumentNode -> handleArgumentNode(node)
            is DefnNode -> handleDefnNode(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "")
        }
    }

    private fun handleArgumentNode(node: ArgumentNode): Declaration {
        return newParameter(node.name.idString(), variadic = false)
    }

    private fun handleDefnNode(node: DefnNode): Function {
        val func = newFunction(node.name.idString())

        frontend.scopeManager.enterScope(func)

        for (arg in node.argsNode.args) {
            val param = this.handle(arg) as? Parameter
            if (param == null) {
                continue
            }

            frontend.scopeManager.addDeclaration(param)
            func.parameters += param
        }

        val body = frontend.statementHandler.handle(node.bodyNode)
        if (body is Block) {
            // get the last statement
            val lastStatement = body.statements.lastOrNull()

            // add an implicit return statement, if there is no return statement
            if (lastStatement !is ReturnStatement) {
                val returnStatement = newReturnStatement()
                returnStatement.isImplicit = true
                body += returnStatement

                // TODO: Ruby returns the last expression, if there is no explicit return
            }
        }
        func.body = body

        frontend.scopeManager.leaveScope(func)

        return func
    }
}
