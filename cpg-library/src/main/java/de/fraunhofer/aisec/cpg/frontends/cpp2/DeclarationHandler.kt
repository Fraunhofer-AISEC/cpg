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
package de.fraunhofer.aisec.cpg.frontends.cpp2

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.cpp2.CXXLanguageFrontend2.Companion.ts_node_child_by_field_name
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import org.bytedeco.treesitter.TSNode
import org.bytedeco.treesitter.global.treesitter
import org.bytedeco.treesitter.global.treesitter.*

class DeclarationHandler(lang: CXXLanguageFrontend2) :
    Handler<Declaration, TSNode, CXXLanguageFrontend2>(::Declaration, lang) {
    init {
        map.put(TSNode::class.java, ::handleDeclaration)
    }

    private fun handleDeclaration(node: TSNode): Declaration {
        return when (val type = node.type) {
            "function_definition" -> handleFunctionDefinition(node)
            "parameter_declaration" -> handleParameterDeclaration(node)
            "field_declaration" -> handleFieldDeclaration(node)
            "class_specifier" -> handleClassSpecifier(node)
            else -> {
                LanguageFrontend.log.error("Not handling type {} yet.", type)
                Declaration()
            }
        }
    }

    private fun handleClassSpecifier(node: TSNode): Declaration {
        val name = lang.getCodeFromRawNode(ts_node_child_by_field_name(node, "name"))

        val recordDeclaration =
            NodeBuilder.newRecordDeclaration(name, "class", lang.getCodeFromRawNode(node))

        lang.scopeManager.enterScope(recordDeclaration)

        val classBody = ts_node_child_by_field_name(node, "body")
        if (!ts_node_is_null(classBody)) {
            // loop through fields
            for (i in 0 until ts_node_named_child_count(classBody)) {
                var childNode = ts_node_named_child(classBody, i)

                // skip access_specifier for now
                if (ts_node_type(childNode).string == "access_specifier") {
                    continue
                }

                val declaration = handle(childNode)

                lang.scopeManager.addDeclaration(declaration)
            }
        }

        lang.scopeManager.leaveScope(recordDeclaration)

        return recordDeclaration
    }

    private fun handleParameterDeclaration(node: TSNode): ParamVariableDeclaration {
        val startType = lang.handleType(ts_node_child_by_field_name(node, "type"))
        val param =
            NodeBuilder.newMethodParameterIn("", startType, false, lang.getCodeFromRawNode(node))

        // process the declarator to adjust name and type of this declaration
        processDeclarator(
            treesitter.ts_node_child_by_field_name(node, "declarator", "declarator".length),
            param
        )

        return param
    }

    private fun handleFieldDeclaration(node: TSNode): Declaration {
        val startType = lang.handleType(ts_node_child_by_field_name(node, "type"))

        // peek into the declarator
        val declarator = ts_node_child_by_field_name(node, "declarator")
        val declaration =
            if (isReallyAFunctionDeclaration(declarator)) {
                newFunctionDeclaration("", lang.getCodeFromRawNode(node))
            } else {
                newFieldDeclaration(
                    "",
                    startType,
                    listOf(),
                    lang.getCodeFromRawNode(node),
                    lang.getLocationFromRawNode(node),
                    null,
                    false
                )
            }

        // process the declarator to adjust name and type of this declaration
        processDeclarator(
            treesitter.ts_node_child_by_field_name(node, "declarator", "declarator".length),
            declaration
        )

        // TODO: initialiezers

        return declaration
    }

    /**
     * Pure function declarations within a class are showing up as field_declaration. However, we
     * need to peek into the (nested) declarators to find out if this is really a function
     * declaration.
     */
    private fun isReallyAFunctionDeclaration(node: TSNode): Boolean {
        // TODO: we can probably do this with a query

        var declarator = ts_node_child_by_field_name(node, "declarator")
        if (!ts_node_is_null(declarator)) {
            if (ts_node_type(declarator).string == "function_declarator") {
                return true
            } else {
                if (isReallyAFunctionDeclaration(declarator)) {
                    return true
                }
            }
        }

        return false
    }

    private fun handleFunctionDefinition(node: TSNode): FunctionDeclaration {
        println(treesitter.ts_node_string(node).string)

        val nonPointerType = lang.handleType(ts_node_child_by_field_name(node, "type"))

        // name will be filled later by handleDeclarator
        val func = NodeBuilder.newFunctionDeclaration("", lang.getCodeFromRawNode(node))
        func.type = nonPointerType

        lang.scopeManager.enterScope(func)

        processDeclarator(ts_node_child_by_field_name(node, "declarator"), func)

        // update code to include the whole function
        func.code = lang.getCodeFromRawNode(node)

        println(func)

        func.body = lang.statementHandler.handle(ts_node_child_by_field_name(node, "body"))

        lang.scopeManager.leaveScope(func)

        return func
    }

    internal fun processDeclarator(node: TSNode, declaration: ValueDeclaration) {
        when (node.type) {
            "identifier" -> {
                declaration.name = lang.getCodeFromRawNode(node) ?: ""
            }
            "field_identifier" -> {
                declaration.name = lang.getCodeFromRawNode(node) ?: ""
            }
            "init_declarator" -> {
                processInitDeclarator(node, declaration)
            }
            "pointer_declarator" -> {
                processPointerDeclarator(node, declaration)
            }
            "function_declarator" -> processFunctionDeclarator(node, declaration)
            else -> {
                LanguageFrontend.log.error("Not handling declarator of type {} yet", node.type)
            }
        }
    }

    private fun processInitDeclarator(node: TSNode, declaration: ValueDeclaration) {
        // going forward in the declarator chain
        processDeclarator(ts_node_child_by_field_name(node, "declarator"), declaration)

        val hasInitializer = declaration as? HasInitializer
        hasInitializer?.let {
            // the value is nested in the init declarator
            val expression =
                lang.expressionHandler.handle(ts_node_child_by_field_name(node, "value"))

            hasInitializer.initializer = expression
        }
    }

    private fun processPointerDeclarator(node: TSNode, declaration: ValueDeclaration) {
        processDeclarator(ts_node_child_by_field_name(node, "declarator"), declaration)

        // reference the type using a pointer
        declaration.type = declaration.type.reference(PointerType.PointerOrigin.POINTER)
    }

    private fun processFunctionDeclarator(node: TSNode, declaration: ValueDeclaration) {
        processDeclarator(ts_node_child_by_field_name(node, "declarator"), declaration)

        val parameterList = ts_node_child_by_field_name(node, "parameters")
        for (i in 0 until ts_node_named_child_count(parameterList)) {
            val param = handle(ts_node_named_child(parameterList, i))

            lang.scopeManager.addDeclaration(param)
        }
    }
}
