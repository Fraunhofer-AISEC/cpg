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
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import org.treesitter.TSNode

/**
 * A [Handler] that translates Rust declarations (items) into CPG [Declaration] nodes. It currently
 * supports functions, structs, enums, impl blocks, and modules.
 *
 * It also handles generic type parameters for functions and structs by wrapping them in
 * [FunctionTemplateDeclaration] or [RecordTemplateDeclaration] nodes.
 */
class DeclarationHandler(frontend: RustLanguageFrontend) :
    RustHandler<Declaration, TSNode>(::ProblemDeclaration, frontend) {

    override fun handleNode(node: TSNode): Declaration {
        return when (node.type) {
            "function_item" -> handleFunctionItem(node)
            "struct_item" -> handleStructItem(node)
            "enum_item" -> handleEnumItem(node)
            "impl_item" -> handleImplItem(node)
            "mod_item" -> handleModItem(node)
            "type_item" -> handleTypeItem(node)
            else -> {
                ProblemDeclaration("Unknown declaration type: ${node.type}")
            }
        }
    }

    private fun handleFunctionItem(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        val recordDeclaration =
            (frontend.scopeManager.currentScope as? RecordScope)?.astNode as? RecordDeclaration

        val func =
            if (recordDeclaration != null) {
                newMethodDeclaration(
                    name,
                    isStatic = false,
                    recordDeclaration = recordDeclaration,
                    rawNode = node,
                )
            } else {
                newFunctionDeclaration(name, rawNode = node)
            }

        val typeParameters = node.getChildByFieldName("type_parameters")
        val template =
            if (typeParameters != null && !typeParameters.isNull) {
                val t = newFunctionTemplateDeclaration(name, rawNode = node)
                t.addDeclaration(func)
                frontend.scopeManager.addDeclaration(t)
                frontend.scopeManager.enterScope(t)
                handleTypeParameters(typeParameters, t)
                t
            } else {
                frontend.scopeManager.addDeclaration(func)
                null
            }

        frontend.scopeManager.enterScope(func)

        val parameters = node.getChildByFieldName("parameters")
        if (parameters != null) {
            handleParameters(parameters, func)
        }

        val returnTypeNode = node.getChildByFieldName("return_type")
        if (returnTypeNode != null) {
            func.returnTypes = listOf(frontend.typeOf(returnTypeNode))
        }

        val body = node.getChildByFieldName("body")
        if (body != null) {
            func.body = frontend.statementHandler.handle(body)
        }

        frontend.scopeManager.leaveScope(func)
        if (template != null) {
            frontend.scopeManager.leaveScope(template)
            return template
        }

        return func
    }

    private fun handleTypeParameters(node: TSNode, template: TemplateDeclaration) {
        if (node.isNull) return
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (
                child.type == "type_parameter" ||
                    child.type == "constrained_type_parameter" ||
                    child.type == "type_identifier"
            ) {
                val nameNode = child.getChildByFieldName("name")
                val name = nameNode?.let { frontend.codeOf(it) } ?: frontend.codeOf(child)
                val typeParam = newTypeParameterDeclaration(name, rawNode = child)
                template.addDeclaration(typeParam)
                frontend.scopeManager.addDeclaration(typeParam)
            }
        }
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
            } else if (child.type == "self_parameter") {
                // TODO: Handle self parameter
            }
        }
    }

    private fun handleStructItem(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        val record = newRecordDeclaration(name, "struct", rawNode = node)

        val typeParameters = node.getChildByFieldName("type_parameters")
        val template =
            if (typeParameters != null && !typeParameters.isNull) {
                val t = newRecordTemplateDeclaration(name, rawNode = node)
                t.addDeclaration(record)
                frontend.scopeManager.addDeclaration(t)
                frontend.scopeManager.enterScope(t)
                handleTypeParameters(typeParameters, t)
                t
            } else {
                frontend.scopeManager.addDeclaration(record)
                null
            }

        frontend.scopeManager.enterScope(record)

        val body = node.getChildByFieldName("body")
        if (body != null) {
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.type == "field_declaration") {
                    val fieldNameNode = child.getChildByFieldName("name")
                    val fieldName = fieldNameNode?.let { frontend.codeOf(it) } ?: ""
                    val fieldTypeNode = child.getChildByFieldName("type")

                    val field =
                        newFieldDeclaration(
                            fieldName,
                            frontend.typeOf(fieldTypeNode),
                            rawNode = child,
                        )
                    frontend.scopeManager.addDeclaration(field)
                    record.addDeclaration(field)
                }
            }
        }

        frontend.scopeManager.leaveScope(record)
        if (template != null) {
            frontend.scopeManager.leaveScope(template)
            return template
        }
        return record
    }

    private fun handleEnumItem(node: TSNode): RecordDeclaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        val record = newRecordDeclaration(name, "enum", rawNode = node)
        frontend.scopeManager.addDeclaration(record)
        frontend.scopeManager.enterScope(record)

        val body = node.getChildByFieldName("body")
        if (body != null) {
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.type == "enum_variant") {
                    val variantNameNode = child.getChildByFieldName("name")
                    val variantName = variantNameNode?.let { frontend.codeOf(it) } ?: ""

                    // For now, treat enum variants as fields or special declarations
                    val field = newFieldDeclaration(variantName, record.toType(), rawNode = child)
                    frontend.scopeManager.addDeclaration(field)
                    record.addDeclaration(field)
                }
            }
        }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    private fun handleImplItem(node: TSNode): Declaration {
        val typeNode = node.getChildByFieldName("type")
        val typeNameString = typeNode?.let { frontend.codeOf(it) } ?: ""
        val typeName = Name(typeNameString, null, language.namespaceDelimiter)

        // Try to find an existing record in the current scope
        val existing = frontend.scopeManager.lookupSymbolByName(typeName, language)
        var record = existing.firstOrNull { it is RecordDeclaration } as? RecordDeclaration

        if (record == null) {
            record = newRecordDeclaration(typeNameString, "struct", rawNode = node)
            frontend.scopeManager.addDeclaration(record)
        }

        frontend.scopeManager.enterScope(record)

        val body = node.getChildByFieldName("body")
        if (body != null) {
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.isNamed) {
                    val decl = handle(child)
                    if (decl != null) {
                        record.addDeclaration(decl)
                    }
                }
            }
        }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    private fun handleModItem(node: TSNode): NamespaceDeclaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        val mod = newNamespaceDeclaration(name, rawNode = node)
        frontend.scopeManager.addDeclaration(mod)
        frontend.scopeManager.enterScope(mod)

        val body = node.getChildByFieldName("body")
        if (body != null) {
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.isNamed) {
                    val decl = handle(child)
                    if (decl != null) {
                        mod.addDeclaration(decl)
                    }
                }
            }
        }

        frontend.scopeManager.leaveScope(mod)
        return mod
    }

    private fun handleTypeItem(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""
        val typeNode = node.getChildByFieldName("type")

        val targetType = frontend.typeOf(typeNode)
        val aliasType = objectType(name)

        val decl = newTypedefDeclaration(targetType, aliasType, rawNode = node)
        frontend.scopeManager.addTypedef(decl)
        return decl
    }
}
