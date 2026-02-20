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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.treesitter.TSNode

/**
 * A [Handler] that translates Rust declarations (items) into CPG [] nodes. It currently supports
 * functions, structs, enums, impl blocks, and modules.
 *
 * It also handles generic type parameters for functions and structs by wrapping them in
 * [FunctionTemplate] or [RecordTemplate] nodes.
 */
class DeclarationHandler(frontend: RustLanguageFrontend) :
    RustHandler<Declaration, TSNode>(::ProblemDeclaration, frontend) {

    override fun handleNode(node: TSNode): Declaration {
        return when (node.type) {
            "function_item",
            "function_signature_item" -> handleFunctionItem(node)
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
            "union_item" -> handleUnionItem(node)
            "foreign_mod_item" -> handleForeignModItem(node)
            "extern_crate_declaration" -> handleExternCrateDeclaration(node)
            "inner_attribute_item" -> handleInnerAttributeItem(node)
            "empty_statement" -> newProblemDeclaration("empty_statement", rawNode = node)
            "macro_invocation" -> handleMacroInvocationDecl(node)
            else -> {
                newProblemDeclaration("Unknown declaration type: ${node.type}", rawNode = node)
            }
        }
    }

    /**
     * Translates a Rust
     * [`function_item`](https://docs.rs/tree-sitter-rust/latest/tree_sitter_rust/) or
     * `function_signature_item` into a [Function] or [Method]. If the function is inside a record
     * scope (e.g., an `impl` or `trait` block), it becomes a [Method]. Associated functions without
     * a `self` parameter are marked as static.
     */
    private fun handleFunctionItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val recordDeclaration = frontend.scopeManager.currentRecord
        val parameters = node["parameters"]
        val hasSelf = parameters?.children?.any { it.type == "self_parameter" } == true

        val func =
            if (recordDeclaration != null) {
                newMethod(
                    name,
                    isStatic = !hasSelf,
                    recordDeclaration = recordDeclaration,
                    rawNode = node,
                )
            } else {
                newFunction(name, rawNode = node)
            }

        // Check for async modifier (only present in function_item, not function_signature_item)
        for (child in node.children) {
            if (child.type == "function_modifiers") {
                for (modifier in child.children) {
                    if (modifier.type == "async") {
                        func.modifiers += "async"
                        break
                    }
                }
            } else if (child.type == "async") {
                func.modifiers += "async"
                break
            }
        }

        val typeParameters = node["type_parameters"]
        val template =
            if (typeParameters != null && !typeParameters.isNull) {
                val t = newFunctionTemplate(name, rawNode = node)
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

        if (parameters != null) {
            handleParameters(parameters, func)
        }

        val returnTypeNode = node["return_type"]
        if (returnTypeNode != null) {
            func.returnTypes = listOf(frontend.typeOf(returnTypeNode))
        }

        val whereClause = node.children.firstOrNull { it.type == "where_clause" }
        if (whereClause != null) {
            handleWhereClause(whereClause)
        }

        val body = node["body"]
        if (body != null && !body.isNull) {
            func.body = frontend.statementHandler.handle(body)
        }

        frontend.scopeManager.leaveScope(func)
        if (template != null) {
            frontend.scopeManager.leaveScope(template)
            return template
        }

        return func
    }

    /**
     * Processes a Rust `where_clause` by resolving each predicate's trait bounds and adding them as
     * super types on the corresponding [TypeParameter].
     */
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
                            .filterIsInstance<TypeParameter>()
                            .firstOrNull()

                    val type = typeParamDecl?.type ?: frontend.typeHandler.handle(left)
                    parseTraitBounds(bounds, type)
                }
            }
        }
    }

    /**
     * Processes generic type parameters (e.g., `<T: Clone, 'a>`) and creates [TypeParameter] nodes
     * for each, adding them to the given [template].
     */
    private fun handleTypeParameters(node: TSNode, template: Template) {
        if (node.isNull) return
        for (child in node.children) {
            // Handle lifetime parameters (e.g., 'a in <'a, T>)
            if (child.type == "lifetime") {
                val name = child.text()
                val typeParam = newTypeParameter(name, rawNode = child)
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
                val typeParam = newTypeParameter(name, rawNode = child)

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

    /** Parses trait bounds (e.g., `Clone + Debug`) and adds them as super types on [type]. */
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

    /**
     * Processes function parameters, creating [Parameter] nodes for each parameter. Also handles
     * `self_parameter` for method receivers and `mut` patterns.
     */
    private fun handleParameters(node: TSNode, func: Function) {
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

                val param = newParameter(name, frontend.typeOf(typeNode), rawNode = child)
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

    /**
     * Processes a `self_parameter` (e.g., `&self`, `&mut self`, `self`) by creating a receiver
     * [Variable] on the [Method].
     */
    private fun handleSelfParameter(node: TSNode, func: Function) {
        if (func is Method) {
            val recordDeclaration = func.recordDeclaration ?: return
            val selfType = recordDeclaration.toType()
            // Check if it's &self, &mut self, or self
            var isMut = false
            var isRef = false
            for (child in node.children) {
                if (child.type == "&") isRef = true
                if (child.type == "mutable_specifier") isMut = true
            }
            val paramType = if (isRef) selfType.ref() else selfType
            val selfVar = newVariable("self", rawNode = node)
            selfVar.type = paramType
            if (isMut) {
                selfVar.modifiers += "mut"
            }
            func.receiver = selfVar
        }
    }

    /**
     * Translates a Rust `struct_item` into a [Record] with kind `"struct"`. Handles both
     * named-field structs (`struct Point { x: i32, y: i32 }`) and tuple structs (`struct Pair(i32,
     * i32)`). Generic type parameters produce a wrapping [RecordTemplate].
     */
    private fun handleStructItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        // Quickly check if we have already defined this record implicitly because of an impl block
        // before the struct declaration. If so, we need to update the existing record instead of
        // creating a new one.
        val recordScope =
            frontend.scopeManager
                .filterScopes { it.name == parseName(name) && it is RecordScope }
                .firstOrNull()
        val implicitRecord = recordScope?.astNode as? Record

        // Update existing implicit record or create new one
        val record =
            if (implicitRecord != null) {
                implicitRecord.isImplicit = false
                implicitRecord.setCodeAndLocation(frontend, node)
                implicitRecord
            } else {
                newRecord(name, "struct", rawNode = node)
            }

        val typeParameters = node["type_parameters"]
        val template =
            if (typeParameters != null && !typeParameters.isNull) {
                val t = newRecordTemplate(name, rawNode = node)
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

                    val field = newField(fieldName, frontend.typeOf(fieldTypeNode), rawNode = child)

                    // Check for visibility modifier (e.g. pub)
                    for (fieldChild in child.children) {
                        if (fieldChild.type == "visibility_modifier") {
                            val vis = fieldChild.text().ifEmpty { "pub" }
                            field.modifiers += vis
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
                                newField(
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

    /**
     * Translates a Rust `trait_item` into a [Record] with kind `"trait"`. Trait method signatures,
     * default method implementations, and associated types are added as declarations to the record.
     */
    private fun handleTraitItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val record = newRecord(name, "trait", rawNode = node)

        val typeParameters = node["type_parameters"]
        val template =
            if (typeParameters != null && !typeParameters.isNull) {
                val t = newRecordTemplate(name, rawNode = node)
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
                            "function_item",
                            "function_signature_item" -> handleFunctionItem(child)
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

    /**
     * Translates a Rust `enum_item` into an [Enum]. Each variant becomes an [EnumConstant] entry.
     */
    private fun handleEnumItem(node: TSNode): Enumeration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val enumDecl = newEnumeration(name, rawNode = node)
        enumDecl.kind = "enum"
        frontend.scopeManager.addDeclaration(enumDecl)
        frontend.scopeManager.enterScope(enumDecl)

        val body = node["body"]
        if (body != null) {
            for (child in body.children) {
                if (child.type == "enum_variant") {
                    val variantNameNode = child["name"]
                    val variantName = variantNameNode.text()

                    val entry = newEnumConstant(variantName, rawNode = child)
                    entry.type = enumDecl.toType()
                    frontend.scopeManager.addDeclaration(entry)
                    enumDecl.entries += entry
                }
            }
        }

        frontend.scopeManager.leaveScope(enumDecl)
        return enumDecl
    }

    /**
     * Translates a Rust `impl_item` by resolving or creating the target [Record] and adding all
     * methods and associated functions to it. Trait implementations add the trait to the record's
     * [RecordDeclaration.implementedInterfaces].
     */
    private fun handleImplItem(node: TSNode): Declaration {
        val typeNode = node["type"]
        val traitNode = node["trait"]

        val typeNameString = typeNode.text()
        val typeName = Name(typeNameString, null, language.namespaceDelimiter)
        val impl = newExtension(typeNameString)

        // Try to find an existing record scope with the type name
        val recordScope =
            frontend.scopeManager
                .filterScopes { it.name == typeName && it is RecordScope }
                .firstOrNull()
        var record = recordScope?.astNode as? Record

        if (record != null) {
            impl.extendedDeclaration = record
        } else {
            // We are running into the situation that we define the impl block BEFORE the struct
            // (which is allowed). But in this case we need to define an implicit record declaration
            // for the struct, so that we have a record to attach the impl block to. We will get rid
            // of this record declaration later.
            record = newRecord(typeNameString, "struct", rawNode = typeNode).implicit()
        }

        // Enter the record's scope, so methods are added to the RecordScope
        // which is where ObjectType.methods looks for them
        frontend.scopeManager.enterScope(record)

        if (traitNode != null && !traitNode.isNull) {
            val traitNameString = traitNode.text()
            record.implementedInterfaces += objectType(traitNameString)
        }

        val body = node["body"]
        handleChildrenWithAnnotations(body, impl)

        frontend.scopeManager.leaveScope(record)
        return impl
    }

    /**
     * Translates a Rust `mod_item` into a [Namespace]. All declarations inside the module body
     * become children of the namespace.
     */
    private fun handleModItem(node: TSNode): Namespace {
        val nameNode = node["name"]
        val name = nameNode.text()

        val mod = newNamespace(name, rawNode = node)
        frontend.scopeManager.addDeclaration(mod)
        frontend.scopeManager.enterScope(mod)

        val body = node["body"]
        handleChildrenWithAnnotations(body, mod)

        frontend.scopeManager.leaveScope(mod)
        return mod
    }

    /**
     * Helper method to process the children of a module, impl block, or trait body, handling inner
     * attributes (annotations) and adding named declarations to the given [holder]. This is used
     * for the bodies of `mod_item` and `impl_item`. For impl blocks, declarations are not added to
     * the holder (Extension) since they're already added to the record scope.
     */
    private fun handleChildrenWithAnnotations(body: TSNode?, holder: DeclarationHolder) {
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
                    // Only add to holder if it's not an ExtensionDeclaration
                    // (for impl blocks, methods are already in the record scope)
                    if (holder !is Extension) {
                        holder.addDeclaration(decl)
                    }
                }
            }
        }
    }

    /** Translates a Rust `type_item` (type alias, e.g., `type Meters = i32`) into a [Typedef]. */
    private fun handleTypeItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()
        val typeNode = node["type"]

        val targetType = frontend.typeOf(typeNode)
        val aliasType = objectType(name)

        val decl = newTypedef(targetType, aliasType, rawNode = node)
        frontend.scopeManager.addTypedef(decl)
        return decl
    }

    /**
     * Translates a Rust `associated_type` declaration inside a trait (e.g., `type Item;`) into a
     * [Typedef] with an unknown target type.
     */
    private fun handleAssociatedType(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        // Associated types in traits have no concrete type yet (just a ).
        // Model as a TypedefDeclaration with unknown target type.
        val aliasType = objectType(name)
        val targetType = unknownType()

        val decl = newTypedef(targetType, aliasType, rawNode = node)

        // Parse optional trait bounds on the associated type
        val boundsNode = node["bounds"]
        if (boundsNode != null && !boundsNode.isNull) {
            parseTraitBounds(boundsNode, aliasType)
        }

        frontend.scopeManager.addTypedef(decl)
        return decl
    }

    /**
     * Translates a Rust `macro_definition` (`macro_rules!`) into a [Function] with a
     * `"macro_rules"` annotation. The macro body is opaque.
     */
    private fun handleMacroDefinition(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        // Model macro_rules! definitions as FunctionDeclarations.
        // The macro body is opaque (token trees), so we just capture the declaration.
        val func = newFunction(name, rawNode = node)
        val annotation = newAnnotation("macro_rules", rawNode = node)
        func.annotations += annotation
        frontend.scopeManager.addDeclaration(func)
        return func
    }

    /**
     * Translates a Rust `const_item` (e.g., `const MAX: i32 = 100`) into a [Variable] with a
     * `"const"` modifier.
     */
    private fun handleConstItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val variable = newVariable(name, rawNode = node)
        variable.modifiers += "const"

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

    /**
     * Translates a Rust `static_item` (e.g., `static mut COUNT: i32 = 0`) into a [Variable] with
     * `"static"` and optionally `"mut"` modifiers.
     */
    private fun handleStaticItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val variable = newVariable(name, rawNode = node)
        variable.modifiers += "static"

        // Check for mut
        for (child in node.children) {
            if (child.type == "mutable_specifier") {
                variable.modifiers += "mut"
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

    /** Translates a Rust `use_declaration` (e.g., `use std::io::Read`) into an [Include]. */
    private fun handleUseDeclaration(node: TSNode): Declaration {
        // Extract the use path as a string
        val argument = node.getNamedChild(0)
        val path = argument.text()

        val include = newInclude(path, rawNode = node)
        frontend.scopeManager.addDeclaration(include)
        return include
    }

    /** Translates a Rust `union_item` into a [Record] with kind `"union"`. */
    private fun handleUnionItem(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val record = newRecord(name, "union", rawNode = node)
        frontend.scopeManager.addDeclaration(record)
        frontend.scopeManager.enterScope(record)

        val body = node["body"]
        if (body != null && !body.isNull) {
            for (child in body.children) {
                if (child.type == "field_declaration") {
                    val fieldNameNode = child["name"]
                    val fieldName = fieldNameNode.text()
                    val fieldTypeNode = child["type"]
                    val field = newField(fieldName, frontend.typeOf(fieldTypeNode), rawNode = child)
                    frontend.scopeManager.addDeclaration(field)
                    record.addDeclaration(field)
                }
            }
        }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    /**
     * Translates a Rust `foreign_mod_item` (e.g., `extern "C" { fn foo(); }`) into a [Namespace]
     * with an extern ABI annotation.
     */
    private fun handleForeignModItem(node: TSNode): Declaration {
        // extern "C" { fn foo(); }
        // Model as NamespaceDeclaration with extern ABI annotation
        var abi = ""
        for (child in node.children) {
            if (child.type == "string_literal") {
                abi = child.text().trim('"')
                break
            }
        }

        val ns = newNamespace("extern", rawNode = node)
        if (abi.isNotEmpty()) {
            ns.annotations += newAnnotation("extern \"$abi\"", rawNode = node)
        }
        frontend.scopeManager.addDeclaration(ns)
        frontend.scopeManager.enterScope(ns)

        val body = node["body"]
        if (body != null && !body.isNull) {
            for (child in body.children) {
                if (child.isNamed) {
                    val decl = handle(child)
                    ns.addDeclaration(decl)
                }
            }
        }

        frontend.scopeManager.leaveScope(ns)
        return ns
    }

    /**
     * Translates a Rust `extern_crate_declaration` (e.g., `extern crate serde`) into an [Include].
     */
    private fun handleExternCrateDeclaration(node: TSNode): Declaration {
        val nameNode = node["name"]
        val name = nameNode.text()

        val include = newInclude(name, rawNode = node)
        frontend.scopeManager.addDeclaration(include)
        return include
    }

    /**
     * Translates a Rust `inner_attribute_item` (e.g., `#![no_std]`) into an annotated declaration.
     */
    private fun handleInnerAttributeItem(node: TSNode): Declaration {
        // #![no_std] — model as an annotated empty declaration
        var attrContent = ""
        for (child in node.children) {
            if (child.type == "attribute") {
                attrContent = child.text()
                break
            }
        }
        val decl = newVariable("", rawNode = node)
        decl.annotations += newAnnotation("#![$attrContent]", rawNode = node)
        frontend.scopeManager.addDeclaration(decl)
        return decl
    }

    /**
     * Translates a `macro_invocation` at declaration level by delegating to the expression handler.
     */
    private fun handleMacroInvocationDecl(node: TSNode): Declaration {
        // Macro invocation at declaration level (e.g., include!("other.rs"))
        // Delegate to expression handler and wrap
        val expr = frontend.expressionHandler.handle(node) as? Expression
        if (expr != null) {
            val decl = newVariable("", rawNode = node)
            decl.initializer = expr
            return decl
        }
        return newProblemDeclaration("Unhandled macro invocation at decl level", rawNode = node)
    }
}
