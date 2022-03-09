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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

@ExperimentalTypeScript
class DeclarationHandler(lang: TypeScriptLanguageFrontend) :
    Handler<Declaration, TypeScriptNode, TypeScriptLanguageFrontend>(::ProblemDeclaration, lang) {
    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    private fun handleNode(node: TypeScriptNode): Declaration {
        when (node.type) {
            "SourceFile" -> return handleSourceFile(node)
            "FunctionDeclaration" -> return handleFunctionDeclaration(node)
            "MethodDeclaration" -> return handleFunctionDeclaration(node)
            "Constructor" -> return handleFunctionDeclaration(node)
            "ArrowFunction" -> return handleFunctionDeclaration(node)
            "FunctionExpression" -> return handleFunctionDeclaration(node)
            "Parameter" -> return handleParameter(node)
            "PropertySignature" -> return handlePropertySignature(node)
            "PropertyDeclaration" -> return handlePropertySignature(node)
            "VariableDeclaration" -> return handleVariableDeclaration(node)
            "InterfaceDeclaration" -> return handleClassDeclaration(node)
            "ClassDeclaration" -> return handleClassDeclaration(node)
        }

        return ProblemDeclaration("No handler was implemented for node of type " + node.type)
    }

    private fun handlePropertySignature(node: TypeScriptNode): FieldDeclaration {
        val name = this.lang.getIdentifierName(node)
        val type =
            node.typeChildNode?.let { this.lang.typeHandler.handle(it) }
                ?: UnknownType.getUnknownType()

        val field =
            NodeBuilder.newFieldDeclaration(
                name,
                type,
                listOf(),
                this.lang.getCodeFromRawNode(node),
                this.lang.getLocationFromRawNode(node),
                null,
                false
            )

        this.lang.processAnnotations(field, node)

        return field
    }

    private fun handleClassDeclaration(node: TypeScriptNode): RecordDeclaration {
        val name = this.lang.getIdentifierName(node)

        val record =
            newRecordDeclaration(
                name,
                if (node.type == "InterfaceDeclaration") {
                    "interface"
                } else {
                    "class"
                },
                this.lang.getCodeFromRawNode(node),
                false
            )

        this.lang.scopeManager.enterScope(record)

        // loop through property signatures aka fields, constructors and methods
        node.children
            ?.filter {
                it.type == "PropertySignature" ||
                    it.type == "PropertyDeclaration" ||
                    it.type == "Constructor" ||
                    it.type == "MethodDeclaration"
            }
            ?.map { this.lang.scopeManager.addDeclaration(this.handle(it)) }

        this.lang.scopeManager.leaveScope(record)

        this.lang.processAnnotations(record, node)

        return record
    }

    private fun handleParameter(node: TypeScriptNode): Declaration {
        val name = this.lang.getIdentifierName(node)
        val type =
            node.typeChildNode?.let { this.lang.typeHandler.handle(it) }
                ?: UnknownType.getUnknownType()

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

        // loop through children
        for (childNode in node.children ?: emptyList()) {
            // filter for statements (not sure if this is really sufficient)
            if (childNode.type.endsWith("Statement")) {
                val statement = this.lang.statementHandler.handle(childNode)

                tu.addStatement(statement)
            } else {
                val decl = this.handle(childNode)

                this.lang.scopeManager.addDeclaration(decl)
            }
        }

        return tu
    }

    private fun handleFunctionDeclaration(node: TypeScriptNode): FunctionDeclaration {
        val name = this.lang.getIdentifierName(node)

        val func: FunctionDeclaration =
            when (node.type) {
                "MethodDeclaration" -> {
                    val record = this.lang.scopeManager.currentRecord

                    NodeBuilder.newMethodDeclaration(
                        name,
                        this.lang.getCodeFromRawNode(node),
                        false,
                        record
                    )
                }
                "Constructor" -> {
                    val record = this.lang.scopeManager.currentRecord

                    NodeBuilder.newConstructorDeclaration(
                        record?.name ?: "",
                        this.lang.getCodeFromRawNode(node),
                        record
                    )
                }
                else -> NodeBuilder.newFunctionDeclaration(name, this.lang.getCodeFromRawNode(node))
            }

        node.typeChildNode?.let { func.type = this.lang.typeHandler.handle(it) }

        this.lang.scopeManager.enterScope(func)

        // gather parameters
        node.children?.filter { it.type == "Parameter" }?.forEach {
            val param = this.lang.declarationHandler.handleNode(it)

            if (func is MethodDeclaration) {
                this.lang.processAnnotations(param, it)
            }

            this.lang.scopeManager.addDeclaration(param)
        }

        // parse body, if it exists
        node.firstChild("Block")?.let { func.body = this.lang.statementHandler.handle(it) }

        // it can also be a JSX element (can it be any expression?)
        node.firstChild("JsxElement")?.let { func.body = this.lang.expressionHandler.handle(it) }

        this.lang.scopeManager.leaveScope(func)

        if (func is MethodDeclaration) {
            this.lang.processAnnotations(func, node)
        }

        return func
    }

    private fun handleVariableDeclaration(node: TypeScriptNode): VariableDeclaration {
        val name = this.lang.getIdentifierName(node)

        // TODO: support ObjectBindingPattern (whatever it is). seems to be multiple assignment

        val `var` =
            NodeBuilder.newVariableDeclaration(
                name,
                UnknownType.getUnknownType(),
                this.lang.getCodeFromRawNode(node),
                false
            )
        `var`.location = this.lang.getLocationFromRawNode(node)

        // the last node that is not an identifier or an object binding pattern is an initializer
        node.children
            ?.lastOrNull { it.type != "Identifier" && it.type != "ObjectBindingPattern" }
            ?.let { `var`.initializer = this.lang.expressionHandler.handle(it) }

        return `var`
    }
}
