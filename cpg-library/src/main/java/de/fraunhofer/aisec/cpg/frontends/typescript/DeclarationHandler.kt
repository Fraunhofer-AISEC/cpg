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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

@ExperimentalTypeScript
class DeclarationHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Declaration, TypeScriptNode, TypeScriptLanguageFrontend>(::Declaration, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    fun handleNode(node: TypeScriptNode): Declaration {
        when (node.type) {
            "SourceFile" -> return handleSourceFile(node)
            "FunctionDeclaration" -> return handleFunctionDeclaration(node)
            "Parameter" -> return handleParameter(node)
            "VariableDeclaration" -> return handleVariableDeclaration(node)
        }

        return Declaration()
    }

    private fun handleParameter(node: TypeScriptNode): Declaration {
        val name = this.lang.getIdentifierName(node)
        val type = this.lang.typeHandler.handle(node.typeChildNode)

        val param =
            NodeBuilder.newMethodParameterIn(name, type, false, this.lang.getCodeFromRawNode(node))

        return param
    }

    fun handleSourceFile(node: TypeScriptNode): TranslationUnitDeclaration {
        val tu =
            NodeBuilder.newTranslationUnitDeclaration(
                node.location.file,
                this.lang.getCodeFromRawNode(node)
            )

        this.lang.scopeManager.resetToGlobal(tu)

        // loop through children (for now only look at declarations, but in JS/TS all statements are
        // allowed on global level
        for (childNode in node.children ?: emptyList()) {
            val decl = this.handle(childNode)

            this.lang.scopeManager.addDeclaration(decl)
        }

        return tu
    }

    private fun handleFunctionDeclaration(node: TypeScriptNode): FunctionDeclaration {
        val name = this.lang.getIdentifierName(node)

        val func =
            NodeBuilder.newFunctionDeclaration(name ?: "", this.lang.getCodeFromRawNode(node))

        func.type = this.lang.typeHandler.handle(node.typeChildNode)

        this.lang.scopeManager.enterScope(func)

        // gather parameters
        node.children?.filter { it.type == "Parameter" }?.forEach {
            val param = this.lang.declarationHandler.handleNode(it)

            this.lang.scopeManager.addDeclaration(param)
        }

        // parse body, if it exists
        node.firstChild("Block")?.let { func.body = this.lang.statementHandler.handle(it) }

        this.lang.scopeManager.leaveScope(func)

        return func
    }

    private fun handleVariableDeclaration(node: TypeScriptNode): VariableDeclaration {
        val name = this.lang.getIdentifierName(node)

        val `var` =
            NodeBuilder.newVariableDeclaration(
                name,
                UnknownType.getUnknownType(),
                this.lang.getCodeFromRawNode(node),
                false
            )
        `var`.location = this.lang.getLocationFromRawNode(node)

        // the last node that is not an identifier is an initializer
        `var`.initializer =
            this.lang.expressionHandler.handle(node.children?.last { it.type != "Identifier" })

        return `var`
    }
}
