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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*

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
        val name = this.frontend.getIdentifierName(node)
        val type = node.typeChildNode?.let { this.frontend.typeOf(it) } ?: unknownType()

        val field = newFieldDeclaration(name, type, listOf(), null, false, rawNode = node)

        this.frontend.processAnnotations(field, node)

        return field
    }

    private fun handleClassDeclaration(node: TypeScriptNode): RecordDeclaration {
        val name = this.frontend.getIdentifierName(node)

        val record =
            newRecordDeclaration(
                name,
                if (node.type == "InterfaceDeclaration") {
                    "interface"
                } else {
                    "class"
                },
                rawNode = node
            )

        this.frontend.scopeManager.enterScope(record)

        // loop through property signatures aka fields, constructors and methods
        node.children
            ?.filter {
                it.type == "PropertySignature" ||
                    it.type == "PropertyDeclaration" ||
                    it.type == "Constructor" ||
                    it.type == "MethodDeclaration"
            }
            ?.map { this.frontend.scopeManager.addDeclaration(this.handle(it)) }

        this.frontend.scopeManager.leaveScope(record)

        this.frontend.processAnnotations(record, node)

        return record
    }

    private fun handleParameter(node: TypeScriptNode): Declaration {
        val name = this.frontend.getIdentifierName(node)
        val type = node.typeChildNode?.let { this.frontend.typeOf(it) } ?: unknownType()

        return newParameterDeclaration(name, type, false, rawNode = node)
    }

    fun handleSourceFile(node: TypeScriptNode): TranslationUnitDeclaration {
        val tu = newTranslationUnitDeclaration(node.location.file, rawNode = node)

        this.frontend.scopeManager.resetToGlobal(tu)

        // loop through children
        for (childNode in node.children ?: emptyList()) {
            // filter for statements (not sure if this is really sufficient)
            if (childNode.type.endsWith("Statement")) {
                val statement = this.frontend.statementHandler.handle(childNode)

                statement?.let { tu.addStatement(it) }
            } else {
                val decl = this.handle(childNode)

                this.frontend.scopeManager.addDeclaration(decl)
            }
        }

        return tu
    }

    private fun handleFunctionDeclaration(node: TypeScriptNode): FunctionDeclaration {
        val name = this.frontend.getIdentifierName(node)

        val func: FunctionDeclaration =
            when (node.type) {
                "MethodDeclaration" -> {
                    val record = this.frontend.scopeManager.currentRecord

                    newMethodDeclaration(name, false, record, rawNode = node)
                }
                "Constructor" -> {
                    val record = this.frontend.scopeManager.currentRecord

                    newConstructorDeclaration(
                        record?.name?.toString() ?: "",
                        record,
                        rawNode = node
                    )
                }
                else -> newFunctionDeclaration(name, rawNode = node)
            }

        node.typeChildNode?.let { func.type = this.frontend.typeOf(it) }

        this.frontend.scopeManager.enterScope(func)

        // gather parameters
        node.children
            ?.filter { it.type == "Parameter" }
            ?.forEach {
                val param = this.frontend.declarationHandler.handleNode(it)

                if (func is MethodDeclaration) {
                    this.frontend.processAnnotations(param, it)
                }

                this.frontend.scopeManager.addDeclaration(param)
            }

        // parse body, if it exists
        node.firstChild("Block")?.let { func.body = this.frontend.statementHandler.handle(it) }

        // it can also be a JSX element (can it be any expression?)
        node.firstChild("JsxElement")?.let {
            func.body = this.frontend.expressionHandler.handle(it)
        }

        this.frontend.scopeManager.leaveScope(func)

        if (func is MethodDeclaration) {
            this.frontend.processAnnotations(func, node)
        }

        return func
    }

    private fun handleVariableDeclaration(node: TypeScriptNode): VariableDeclaration {
        val name = this.frontend.getIdentifierName(node)

        // TODO: support ObjectBindingPattern (whatever it is). seems to be multiple assignment

        val declaration = newVariableDeclaration(name, unknownType(), false, rawNode = node)
        declaration.location = this.frontend.locationOf(node)

        // the last node that is not an identifier or an object binding pattern is an initializer
        node.children
            ?.lastOrNull { it.type != "Identifier" && it.type != "ObjectBindingPattern" }
            ?.let { declaration.initializer = this.frontend.expressionHandler.handle(it) }

        return declaration
    }
}
