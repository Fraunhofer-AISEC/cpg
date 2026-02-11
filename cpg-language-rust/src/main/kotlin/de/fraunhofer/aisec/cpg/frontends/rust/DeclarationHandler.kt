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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
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
            "trait_item" -> handleTraitItem(node)
            "mod_item" -> handleModItem(node)
            "type_item" -> handleTypeItem(node)
            "macro_definition" -> handleMacroDefinition(node)
            "const_item" -> handleConstItem(node)
            "static_item" -> handleStaticItem(node)
            "use_declaration" -> handleUseDeclaration(node)
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

        // Check for async modifier
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "function_modifiers") {
                for (j in 0 until child.childCount) {
                    val modifier = child.getChild(j)
                    if (modifier.type == "async") {
                        val annotation = newAnnotation("Async", rawNode = modifier)
                        func.annotations += annotation
                        break
                    }
                }
            } else if (child.type == "async") {
                val annotation = newAnnotation("Async", rawNode = child)
                func.annotations += annotation
                break
            }
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

        var whereClause = node.getChildByFieldName("where_clause")
        if (whereClause == null || whereClause.isNull) {
            // Fallback: search for where_clause child by type
            for (i in 0 until node.childCount) {
                val c = node.getChild(i)
                if (c.type == "where_clause") {
                    whereClause = c
                    break
                }
            }
        }
        if (whereClause != null && !whereClause.isNull) {
            handleWhereClause(whereClause)
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

    private fun handleWhereClause(node: TSNode) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "where_predicate") {
                val left = child.getChildByFieldName("left")
                val bounds = child.getChildByFieldName("bounds")
                if (left != null && bounds != null && !left.isNull && !bounds.isNull) {
                    val typeName = frontend.codeOf(left) ?: ""
                    // Look up existing TypeParameterDeclaration to add bounds to its type
                    val lookupName = Name(typeName, null, language.namespaceDelimiter)
                    val typeParamDecl =
                        frontend.scopeManager
                            .lookupSymbolByName(lookupName, language)
                            .filterIsInstance<TypeParameterDeclaration>()
                            .firstOrNull()

                    val type = typeParamDecl?.type ?: frontend.typeHandler.handle(left)
                    parseTraitBounds(bounds, type)
                }
            }
        }
    }

    private fun handleTypeParameters(node: TSNode, template: TemplateDeclaration) {
        if (node.isNull) return
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            // Handle lifetime parameters (e.g., 'a in <'a, T>)
            if (child.type == "lifetime") {
                val name = frontend.codeOf(child) ?: ""
                val typeParam = newTypeParameterDeclaration(name, rawNode = child)
                template.addDeclaration(typeParam)
                frontend.scopeManager.addDeclaration(typeParam)
                continue
            }
            if (
                child.type == "type_parameter" ||
                    child.type == "constrained_type_parameter" ||
                    child.type == "type_identifier"
            ) {
                // tree-sitter returns non-Java-null TSNode for missing fields,
                // so we must check isNull explicitly
                var nameNode = child.getChildByFieldName("left")
                if (nameNode == null || nameNode.isNull) {
                    nameNode = child.getChildByFieldName("name")
                }
                val name =
                    if (nameNode != null && !nameNode.isNull) frontend.codeOf(nameNode)
                    else frontend.codeOf(child) ?: ""
                val typeParam = newTypeParameterDeclaration(name, rawNode = child)

                if (child.type == "constrained_type_parameter") {
                    val boundsNode = child.getChildByFieldName("bounds")
                    if (boundsNode != null && !boundsNode.isNull) {
                        parseTraitBounds(boundsNode, typeParam.type)
                    }
                }

                template.addDeclaration(typeParam)
                frontend.scopeManager.addDeclaration(typeParam)
            }
        }
    }

    private fun parseTraitBounds(node: TSNode, type: Type) {
        // Rust trait bounds can be a single trait or a list separated by +
        // In tree-sitter-rust, trait_bounds contains several children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed) {
                // It's likely a type_identifier or a path
                val boundType = frontend.typeHandler.handle(child)
                type.superTypes.add(boundType)
            }
        }
    }

    private fun handleParameters(node: TSNode, func: FunctionDeclaration) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "parameter") {
                val pattern = child.getChildByFieldName("pattern")
                var name = pattern?.let { frontend.codeOf(it) } ?: ""
                val typeNode = child.getChildByFieldName("type")

                // Check for mut keyword in pattern
                val isMut = pattern != null && !pattern.isNull && pattern.type == "mut_pattern"
                if (isMut) {
                    val inner = pattern.getNamedChild(0)
                    if (inner != null && !inner.isNull) {
                        name = frontend.codeOf(inner) ?: ""
                    } else {
                        name = name.removePrefix("mut ").trim()
                    }
                }

                val param =
                    newParameterDeclaration(name, frontend.typeOf(typeNode), rawNode = child)
                if (isMut) {
                    param.modifiers += "mut"
                }
                frontend.scopeManager.addDeclaration(param)
                func.parameters += param
            } else if (child.type == "self_parameter") {
                handleSelfParameter(child, func)
            }
        }
    }

    private fun handleSelfParameter(node: TSNode, func: FunctionDeclaration) {
        val recordDeclaration =
            (frontend.scopeManager.currentScope as? RecordScope)?.astNode as? RecordDeclaration
                ?: return

        if (func is MethodDeclaration) {
            val selfType = recordDeclaration.toType()
            // Check if it's &self, &mut self, or self
            var isMut = false
            var isRef = false
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "&") isRef = true
                if (child.type == "mutable_specifier") isMut = true
            }
            val paramType = if (isRef) selfType.ref() else selfType
            val selfVar = newVariableDeclaration("self", rawNode = node)
            selfVar.type = paramType
            if (isMut) {
                selfVar.annotations += newAnnotation("mut", rawNode = node)
            }
            func.receiver = selfVar
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
        if (body != null && !body.isNull) {
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
        } else {
            // Check for tuple struct: struct Pair(i32, i32)
            // In this case there is no "body" field but there is an
            // ordered_field_declaration_list child.
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child.type == "ordered_field_declaration_list") {
                    var fieldIdx = 0
                    for (j in 0 until child.childCount) {
                        val fieldChild = child.getChild(j)
                        if (
                            fieldChild.isNamed &&
                                fieldChild.type != "visibility_modifier" &&
                                fieldChild.type != "attribute_item"
                        ) {
                            val field =
                                newFieldDeclaration(
                                    fieldIdx.toString(),
                                    frontend.typeOf(fieldChild),
                                    rawNode = fieldChild,
                                )
                            frontend.scopeManager.addDeclaration(field)
                            record.addDeclaration(field)
                            fieldIdx++
                        }
                    }
                    break
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

    private fun handleTraitItem(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        val record = newRecordDeclaration(name, "trait", rawNode = node)

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
        if (body != null && !body.isNull) {
            val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.type == "attribute_item") {
                    pendingAnnotations += frontend.parseAttribute(child)
                    continue
                }
                if (child.isNamed) {
                    val decl: Declaration? =
                        when (child.type) {
                            "function_item" -> handleFunctionItem(child)
                            "function_signature_item" -> handleFunctionSignatureItem(child)
                            "associated_type" -> {
                                handleAssociatedType(child)
                                null
                            }
                            else -> null
                        }
                    if (decl != null) {
                        if (pendingAnnotations.isNotEmpty()) {
                            decl.annotations += pendingAnnotations
                            pendingAnnotations.clear()
                        }
                        record.addDeclaration(decl)
                    }
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

    private fun handleFunctionSignatureItem(node: TSNode): MethodDeclaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        val recordDeclaration =
            (frontend.scopeManager.currentScope as? RecordScope)?.astNode as? RecordDeclaration

        val func =
            newMethodDeclaration(
                name,
                isStatic = false,
                recordDeclaration = recordDeclaration,
                rawNode = node,
            )

        frontend.scopeManager.enterScope(func)

        val parameters = node.getChildByFieldName("parameters")
        if (parameters != null) {
            handleParameters(parameters, func)
        }

        val returnTypeNode = node.getChildByFieldName("return_type")
        if (returnTypeNode != null) {
            func.returnTypes = listOf(frontend.typeOf(returnTypeNode))
        }

        // Signature has no body
        frontend.scopeManager.leaveScope(func)
        frontend.scopeManager.addDeclaration(func)

        return func
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
        val traitNode = node.getChildByFieldName("trait")

        val typeNameString = typeNode?.let { frontend.codeOf(it) } ?: ""
        val typeName = Name(typeNameString, null, language.namespaceDelimiter)

        // Try to find an existing record in the current scope
        val existing = frontend.scopeManager.lookupSymbolByName(typeName, language)
        var record = existing.firstOrNull { it is RecordDeclaration } as? RecordDeclaration

        if (record == null) {
            record = newRecordDeclaration(typeNameString, "struct", rawNode = node)
            frontend.scopeManager.addDeclaration(record)
        }

        if (traitNode != null && !traitNode.isNull) {
            val traitNameString = frontend.codeOf(traitNode) ?: ""
            record.implementedInterfaces += objectType(traitNameString)
        }

        frontend.scopeManager.enterScope(record)

        val body = node.getChildByFieldName("body")
        if (body != null) {
            val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.type == "attribute_item") {
                    pendingAnnotations += frontend.parseAttribute(child)
                    continue
                }
                if (child.isNamed) {
                    val decl = handle(child)
                    if (pendingAnnotations.isNotEmpty()) {
                        decl.annotations += pendingAnnotations
                        pendingAnnotations.clear()
                    }
                    record.addDeclaration(decl)
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
            val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
            for (i in 0 until body.childCount) {
                val child = body.getChild(i)
                if (child.type == "attribute_item") {
                    pendingAnnotations += frontend.parseAttribute(child)
                    continue
                }
                if (child.isNamed) {
                    val decl = handle(child)
                    if (pendingAnnotations.isNotEmpty()) {
                        decl.annotations += pendingAnnotations
                        pendingAnnotations.clear()
                    }
                    mod.addDeclaration(decl)
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

    private fun handleAssociatedType(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        // Associated types in traits have no concrete type yet (just a declaration).
        // Model as a TypedefDeclaration with unknown target type.
        val aliasType = objectType(name)
        val targetType = unknownType()

        val decl = newTypedefDeclaration(targetType, aliasType, rawNode = node)

        // Parse optional trait bounds on the associated type
        val boundsNode = node.getChildByFieldName("bounds")
        if (boundsNode != null && !boundsNode.isNull) {
            parseTraitBounds(boundsNode, aliasType)
        }

        frontend.scopeManager.addTypedef(decl)
        return decl
    }

    private fun handleMacroDefinition(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = nameNode?.let { frontend.codeOf(it) } ?: ""

        // Model macro_rules! definitions as FunctionDeclarations.
        // The macro body is opaque (token trees), so we just capture the declaration.
        val func = newFunctionDeclaration(name, rawNode = node)
        val annotation = newAnnotation("macro_rules", rawNode = node)
        func.annotations += annotation
        frontend.scopeManager.addDeclaration(func)
        return func
    }

    private fun handleConstItem(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = if (nameNode != null && !nameNode.isNull) frontend.codeOf(nameNode) ?: "" else ""

        val variable = newVariableDeclaration(name, rawNode = node)
        variable.annotations += newAnnotation("const", rawNode = node)

        val typeNode = node.getChildByFieldName("type")
        if (typeNode != null && !typeNode.isNull) {
            variable.type = frontend.typeOf(typeNode)
        }

        val value = node.getChildByFieldName("value")
        if (value != null && !value.isNull) {
            variable.initializer = frontend.expressionHandler.handle(value) as? Expression
        }

        frontend.scopeManager.addDeclaration(variable)
        return variable
    }

    private fun handleStaticItem(node: TSNode): Declaration {
        val nameNode = node.getChildByFieldName("name")
        val name = if (nameNode != null && !nameNode.isNull) frontend.codeOf(nameNode) ?: "" else ""

        val variable = newVariableDeclaration(name, rawNode = node)
        variable.annotations += newAnnotation("static", rawNode = node)

        // Check for mut
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.type == "mutable_specifier") {
                variable.annotations += newAnnotation("mut", rawNode = child)
                break
            }
        }

        val typeNode = node.getChildByFieldName("type")
        if (typeNode != null && !typeNode.isNull) {
            variable.type = frontend.typeOf(typeNode)
        }

        val value = node.getChildByFieldName("value")
        if (value != null && !value.isNull) {
            variable.initializer = frontend.expressionHandler.handle(value) as? Expression
        }

        frontend.scopeManager.addDeclaration(variable)
        return variable
    }

    private fun handleUseDeclaration(node: TSNode): Declaration {
        // Extract the use path as a string
        val argument = node.getNamedChild(0)
        val path = if (argument != null && !argument.isNull) frontend.codeOf(argument) ?: "" else ""

        val include = newIncludeDeclaration(path, rawNode = node)
        frontend.scopeManager.addDeclaration(include)
        return include
    }
}
