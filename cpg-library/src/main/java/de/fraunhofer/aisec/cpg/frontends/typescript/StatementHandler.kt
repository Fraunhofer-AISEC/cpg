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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement

class StatementHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Statement, TypeScriptNode, TypeScriptLanguageFrontend>(::Statement, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    fun handleNode(node: TypeScriptNode): Statement {
        when (node.type) {
            "FirstStatement" -> return handleVariableStatement(node)
            "VariableStatement" -> return handleVariableStatement(node)
        }

        return Statement()
    }

    private fun handleVariableStatement(node: TypeScriptNode): DeclarationStatement {
        val statement = NodeBuilder.newDeclarationStatement(this.lang.getCodeFromRawNode(node))

        // the declarations are contained in a VariableDeclarationList
        var nodes = node.firstChild("VariableDeclarationList")?.children

        for (variableNode in nodes ?: emptyList()) {
            val decl = this.lang.declarationHandler.handleNode(variableNode)

            this.lang.scopeManager.addDeclaration(decl)
        }

        return statement
    }
}
