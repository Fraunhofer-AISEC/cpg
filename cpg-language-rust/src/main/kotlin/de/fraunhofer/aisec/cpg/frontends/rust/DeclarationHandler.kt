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

import de.fraunhofer.aisec.cpg.frontends.Handler
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
                newProblemDeclaration("Unknown declaration type: ${node.type}", rawNode = node)
            }
        }
    }

    private fun handleFunctionItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val recordDeclaration = frontend.scopeManager.currentRecord

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
        for (child in node.children) {
            if (child.type == "function_modifiers") {
                for (modifier in child.children) {
                    if (modifier.type == "async") {
                        // We don't fully support async modifiers yet, so we just add an annotation
                        val annotation = newAnnotation("Async", rawNode = modifier)
                        func.annotations += annotation
                        break
                    }
                }
            } else if (child.type == "async") {
                // We don't fully support async modifiers yet, so we just add an annotation
                val annotation = newAnnotation("Async", rawNode = child)
                func.annotations += annotation
                break
            }
        }

        val typeParameters = node["type_parameters"]
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

        val parameters = node["parameters"]
        if (parameters != null) {
            handleParameters(parameters, func)
        }

        val returnTypeNode = node["return_type"]
        if (returnTypeNode != null) {
            func.returnTypes = listOf(frontend.typeOf(returnTypeNode))
        }

        var whereClause = node["where_clause"]
        if (whereClause == null || whereClause.isNull) {
            // Fallback: search for where_clause child by type
            for (c in node.children) {
                if (c.type == "where_clause") {
                    whereClause = c
                    break
                }
            }
        }
        if (whereClause != null && !whereClause.isNull) {
            handleWhereClause(whereClause)
        }

        val body = node["body"]
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
        for (child in node.children) {
            if (child.type == "where_predicate") {
                val left = child["left"]
                val bounds = child["bounds"]
                if (left != null && bounds != null && !left.isNull && !bounds.isNull) {
                    val typeName = left.text()
                    // Look up the existing TypeParameterDeclaration to add bounds to its type
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
        for (child in node.children) {
            // Handle lifetime parameters (e.g., 'a in <'a, T>)
            if (child.type == "lifetime") {
                val name = child.text()
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
                var nameNode = child["left"]
                if (nameNode == null || nameNode.isNull) {
                    nameNode = child["name"]
                }
                val name = nameNode.text().ifEmpty { child.text() }
                val typeParam = newTypeParameterDeclaration(name, rawNode = child)

                if (child.type == "constrained_type_parameter") {
                    val boundsNode = child["bounds"]
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
        for (child in node.children) {
            if (child.isNamed) {
                // It's likely a type_identifier or a path
                val boundType = frontend.typeHandler.handle(child)
                type.superTypes.add(boundType)
            }
        }
    }

    private fun handleParameters(node: TSNode, func: FunctionDeclaration) {
        for (child in node.children) {
            if (child.type == "parameter") {
                val pattern = child["pattern"]
                var name = pattern.text()
                val typeNode = child["type"]

                // Check for mut keyword in pattern
                val isMut = pattern != null && !pattern.isNull && pattern.type == "mut_pattern"
                if (isMut) {
                    val inner = pattern.getNamedChild(0)
                    if (inner != null && !inner.isNull) {
                        name = inner.text()
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
            for (child in node.children) {
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
        val nameNode = node["name"]
        val name = nameNode.text()

        val record = newRecordDeclaration(name, "struct", rawNode = node)

        val typeParameters = node["type_parameters"]
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

        val body = node["body"]
        if (body != null && !body.isNull) {
            for (child in body.children) {
                if (child.type == "field_declaration") {
                    val fieldNameNode = child["name"]
                    val fieldName = fieldNameNode.text()
                    val fieldTypeNode = child["type"]

                    val field =
                        newFieldDeclaration(
                            fieldName,
                            frontend.typeOf(fieldTypeNode),
                            rawNode = child,
                        )

                    // Check for visibility modifier (e.g. pub)
                    for (fieldChild in child.children) {
                        if (fieldChild.type == "visibility_modifier") {
                            val vis = fieldChild.text().ifEmpty { "pub" }
                            field.annotations += newAnnotation(vis, rawNode = fieldChild)
                            break
                        }
                    }

                    frontend.scopeManager.addDeclaration(field)
                    record.addDeclaration(field)
                }
            }
        } else {
            // Check for tuple struct: struct Pair(i32, i32)
            // In this case there is no "body" field but there is an
            // ordered_field_declaration_list child.
            for (child in node.children) {
                if (child.type == "ordered_field_declaration_list") {
                    var fieldIdx = 0
                    for (fieldChild in child.children) {
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
        val nameNode = node["name"]
        val name = nameNode.text()

        val record = newRecordDeclaration(name, "trait", rawNode = node)

        val typeParameters = node["type_parameters"]
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

        val body = node["body"]
        if (body != null && !body.isNull) {
            val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
            for (child in body.children) {
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
        val nameNode = node["name"]
        val name = nameNode.text()

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

        val parameters = node["parameters"]
        if (parameters != null) {
            handleParameters(parameters, func)
        }

        val returnTypeNode = node["return_type"]
        if (returnTypeNode != null) {
            func.returnTypes = listOf(frontend.typeOf(returnTypeNode))
        }

        // Signature has no body
        frontend.scopeManager.leaveScope(func)
        frontend.scopeManager.addDeclaration(func)

        return func
    }

    private fun handleEnumItem(node: TSNode): EnumDeclaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val enumDecl = newEnumDeclaration(name, rawNode = node)
        enumDecl.kind = "enum"
        frontend.scopeManager.addDeclaration(enumDecl)
        frontend.scopeManager.enterScope(enumDecl)

        val body = node["body"]
        if (body != null) {
            for (child in body.children) {
                if (child.type == "enum_variant") {
                    val variantNameNode = child["name"]
                    val variantName = variantNameNode.text()

                    val entry = newEnumConstantDeclaration(variantName, rawNode = child)
                    entry.type = enumDecl.toType()
                    frontend.scopeManager.addDeclaration(entry)
                    enumDecl.entries += entry
                }
            }
        }

        frontend.scopeManager.leaveScope(enumDecl)
        return enumDecl
    }

    private fun handleImplItem(node: TSNode): Declaration {
        val typeNode = node["type"]
        val traitNode = node["trait"]

        val typeNameString = typeNode.text()
        val typeName = Name(typeNameString, null, language.namespaceDelimiter)

        // Try to find an existing record in the current scope
        val existing = frontend.scopeManager.lookupSymbolByName(typeName, language)
        var record = existing.firstOrNull { it is RecordDeclaration } as? RecordDeclaration

        if (record == null) {
            record = newRecordDeclaration(typeNameString, "struct", rawNode = node)
            frontend.scopeManager.addDeclaration(record)
        }

        if (traitNode != null && !traitNode.isNull) {
            val traitNameString = traitNode.text()
            record.implementedInterfaces += objectType(traitNameString)
        }

        frontend.scopeManager.enterScope(record)

        val body = node["body"]
        if (body != null) {
            val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
            for (child in body.children) {
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
        val nameNode = node["name"]
        val name = nameNode.text()

        val mod = newNamespaceDeclaration(name, rawNode = node)
        frontend.scopeManager.addDeclaration(mod)
        frontend.scopeManager.enterScope(mod)

        val body = node["body"]
        if (body != null) {
            val pendingAnnotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
            for (child in body.children) {
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
        val nameNode = node["name"]
        val name = nameNode.text()
        val typeNode = node["type"]

        val targetType = frontend.typeOf(typeNode)
        val aliasType = objectType(name)

        val decl = newTypedefDeclaration(targetType, aliasType, rawNode = node)
        frontend.scopeManager.addTypedef(decl)
        return decl
    }

    private fun handleAssociatedType(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        // Associated types in traits have no concrete type yet (just a declaration).
        // Model as a TypedefDeclaration with unknown target type.
        val aliasType = objectType(name)
        val targetType = unknownType()

        val decl = newTypedefDeclaration(targetType, aliasType, rawNode = node)

        // Parse optional trait bounds on the associated type
        val boundsNode = node["bounds"]
        if (boundsNode != null && !boundsNode.isNull) {
            parseTraitBounds(boundsNode, aliasType)
        }

        frontend.scopeManager.addTypedef(decl)
        return decl
    }

    private fun handleMacroDefinition(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        // Model macro_rules! definitions as FunctionDeclarations.
        // The macro body is opaque (token trees), so we just capture the declaration.
        val func = newFunctionDeclaration(name, rawNode = node)
        val annotation = newAnnotation("macro_rules", rawNode = node)
        func.annotations += annotation
        frontend.scopeManager.addDeclaration(func)
        return func
    }

    private fun handleConstItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val variable = newVariableDeclaration(name, rawNode = node)
        variable.annotations += newAnnotation("const", rawNode = node)

        val typeNode = node["type"]
        if (typeNode != null && !typeNode.isNull) {
            variable.type = frontend.typeOf(typeNode)
        }

        val value = node["value"]
        if (value != null && !value.isNull) {
            variable.initializer = frontend.expressionHandler.handle(value) as? Expression
        }

        frontend.scopeManager.addDeclaration(variable)
        return variable
    }

    private fun handleStaticItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val variable = newVariableDeclaration(name, rawNode = node)
        variable.annotations += newAnnotation("static", rawNode = node)

        // Check for mut
        for (child in node.children) {
            if (child.type == "mutable_specifier") {
                variable.annotations += newAnnotation("mut", rawNode = child)
                break
            }
        }

        val typeNode = node["type"]
        if (typeNode != null && !typeNode.isNull) {
            variable.type = frontend.typeOf(typeNode)
        }

        val value = node["value"]
        if (value != null && !value.isNull) {
            variable.initializer = frontend.expressionHandler.handle(value) as? Expression
        }

        frontend.scopeManager.addDeclaration(variable)
        return variable
    }

    private fun handleUseDeclaration(node: TSNode): Declaration {
        // Extract the use path as a string
        val argument = node.getNamedChild(0)
        val path = argument.text()

        val include = newIncludeDeclaration(path, rawNode = node)
        frontend.scopeManager.addDeclaration(include)
        return include
    }
}
