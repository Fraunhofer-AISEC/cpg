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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import org.treesitter.TSNode

class DeclarationHandler(frontend: RustLanguageFrontend) :
    RustHandler<Declaration, TSNode>(::ProblemDeclaration, frontend) {

    override fun handleNode(node: TSNode): Declaration {
        return when (node.type) {
            "function_item" -> handleFunctionItem(node)
            else -> {
                ProblemDeclaration("Unknown declaration type: ${node.type}")
            }
        }
    }

    private fun handleFunctionItem(node: TSNode): FunctionDeclaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode.let { frontend.codeOf(it) } ?: ""

        val func = newFunctionDeclaration(name, rawNode = node)
        frontend.scopeManager.enterScope(func)

        val parameters = node.getChildByFieldName("parameters")
        if (parameters != null) {
            handleParameters(parameters, func)
        }

        val body = node.getChildByFieldName("body")
        if (body != null) {
            func.body = frontend.statementHandler.handle(body)
        }

        frontend.scopeManager.leaveScope(func)
        return func
    }

    private fun handleParameters(node: TSNode, func: FunctionDeclaration) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "parameter") {
                val pattern = child.getChildByFieldName("pattern")
                val name = pattern?.let { frontend.codeOf(it) } ?: ""
                val typeNode = child.getChildByFieldName("type")

                val param =
                    newParameterDeclaration(name, frontend.typeOf(typeNode), rawNode = child)
                frontend.scopeManager.addDeclaration(param)
                func.parameters += param
            }
        }
    }
}
