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
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodDeclaration
import de.fraunhofer.aisec.cpg.graph.ResolveInFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope
import io.github.oxisto.kotlintree.jvm.*

class DeclarationHandler(lang: CXXLanguageFrontend2) :
    Handler<Declaration, Node, CXXLanguageFrontend2>(::Declaration, lang) {
    init {
        map.put(Node::class.java, ::handleDeclaration)
    }

    private fun handleDeclaration(node: Node): Declaration {
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

    private fun handleClassSpecifier(node: Node): Declaration {
        val name = lang.getCodeFromRawNode(node.childByFieldName("name")) ?: ""

        val recordDeclaration =
            NodeBuilder.newRecordDeclaration(name, "class", lang.getCodeFromRawNode(node))

        lang.scopeManager.enterScope(recordDeclaration)

        val classBody = node.childByFieldName("body")
        if (!classBody.isNull) {
            // loop through fields
            for (i in 0 until classBody.namedChildCount) {
                val childNode = classBody.namedChild(i)

                // skip access_specifier for now
                if (childNode.type == "access_specifier") {
                    continue
                }

                val declaration = handle(childNode)

                lang.scopeManager.addDeclaration(declaration)
            }
        }

        lang.scopeManager.leaveScope(recordDeclaration)

        return recordDeclaration
    }

    private fun handleParameterDeclaration(node: Node): ParamVariableDeclaration {
        val startType = lang.handleType(node.childByFieldName("type"))
        val param =
            NodeBuilder.newMethodParameterIn("", startType, false, lang.getCodeFromRawNode(node))

        // process the declarator to adjust name and type of this declaration
        processDeclarator(node.childByFieldName("declarator"), param)

        return param
    }

    /**
     * This function handles field declarations. However, it is important to know that in the
     * tree-sitter syntax a method declaration inside a class will be considered a "field" with a
     * "function_declarator". Therefore, this function either returns a [FieldDeclaration] or a
     * [MethodDeclaration].
     */
    private fun handleFieldDeclaration(node: Node): Declaration {
        val startType = lang.handleType(node.childByFieldName("type"))

        // Peek into the declarator to determine the type
        val declarator = node.childByFieldName("declarator")
        val declaration =
            if (isReallyAFunctionDeclaration(declarator)) {
                val method =
                    newMethodDeclaration(
                        "",
                        lang.getCodeFromRawNode(node),
                        false,
                        lang.scopeManager.currentRecord
                    )
                method.type = startType
                method
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

        // If this is a method declaration, make sure to set it as active scope, so that its
        // parameters are correctly associated when processing the declarators
        if (declaration is MethodDeclaration) {
            lang.scopeManager.enterScope(declaration)
        }

        // Process the declarator to adjust name and type of this declaration and add the parameters
        // (if this is a method).
        processDeclarator(node.childByFieldName("declarator"), declaration)

        // Leave the method scope if it exists
        if (declaration is MethodDeclaration) {
            lang.scopeManager.leaveScope(declaration)
        }

        // TODO: initializers
        // TODO: check for static modifiers

        return declaration
    }

    /**
     * Pure function declarations within a class are showing up as field_declaration. However, we
     * need to peek into the (nested) declarators to find out if this is really a function
     * declaration.
     */
    private fun isReallyAFunctionDeclaration(node: Node): Boolean {
        // TODO: we can probably do this with a query

        val declarator = node.childByFieldName("declarator")
        if (!declarator.isNull) {
            if (declarator.type == "function_declarator") {
                return true
            } else {
                if (isReallyAFunctionDeclaration(declarator)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * This function checks whether the function declaration is really a method declaration, e.g.,
     * if it contains a scoped_identifier.
     */
    private fun isMethodDeclaration(node: Node): Boolean {
        // TODO: we can probably do this with a query

        val declarator = node.childByFieldName("declarator")
        if (!declarator.isNull) {
            if (declarator.type == "scoped_identifier") {
                return true
            } else {
                if (isReallyAFunctionDeclaration(declarator)) {
                    return true
                }
            }
        }

        return false
    }

    private fun createMethodOrConstructor(
        name: String,
        code: String,
        recordDeclaration: RecordDeclaration?
    ): MethodDeclaration {
        return if (recordDeclaration != null && name.equals(recordDeclaration.name))
            newConstructorDeclaration(name, code, recordDeclaration)
        else newMethodDeclaration(name, code, false, recordDeclaration)
    }

    /**
     * Handles function definitions. A *definition* is similar to a *declaration*, expect it also
     * contains the actual body of the function (or method).
     *
     * There are three occurrences where this handler might get called and we are handling them a
     * little bit differently depending on the environment:
     * * A non-scoped function definition *outside* of any record will be parsed as a
     * [FunctionDeclaration].
     * * A scoped function definition, e.g. containing a `::`, *outside* of a record will be parsed
     * as a [MethodDeclaration] (or [ConstructorDeclaration]) and associated to its record.
     * * A non-scoped function definition *inside* a record will be parsed as a [MethodDeclaration]
     * (or [ConstructorDeclaration]) and associated to its record.
     *
     * In all cases the returning object SHOULD contain a [FunctionDeclaration.body] and
     * [FunctionDeclaration.isDefinition] should be set to `true`.
     */
    private fun handleFunctionDefinition(node: Node): FunctionDeclaration {
        log.debug("Handling function: {}", node.string)

        // Retrieve the non-pointer result type of the function. Any pointers or other array
        // modifiers will be part of the declarator.
        val nonPointerType = lang.handleType(node.childByFieldName("type"))

        // It is important to know whether we are within a record scope or outside. If we are
        // inside, every function is automatically a method. If we are outside, we need to check if
        // the function name is scoped, then it is also a method.
        val insideRecord = lang.scopeManager.currentRecord != null
        val func =
            when {
                isMethodDeclaration(node) -> {
                    // name and record declaration will be filled later by processDeclarator
                    newMethodDeclaration("", lang.getCodeFromRawNode(node), false, null)
                }
                insideRecord -> {
                    // if we are inside a record, we can directly set the record declaration link
                    createMethodOrConstructor(
                        lang.getCodeFromRawNode(
                                node.childByFieldName("declarator").childByFieldName("declarator")
                            )
                            .orEmpty(),
                        lang.getCodeFromRawNode(node).orEmpty(),
                        lang.scopeManager.currentRecord
                    )
                }
                else -> {
                    // `name` will be filled later by handleDeclarator
                    newFunctionDeclaration("", lang.getCodeFromRawNode(node))
                }
            }
        func.type = nonPointerType

        // If we are outside of a record and are dealing with a method declaration, we need to
        // temporarily enter the record scope (and leave it later). This allows us to resolve member
        // variables later
        if (!insideRecord && func is MethodDeclaration) {
            func.recordDeclaration?.let { lang.scopeManager.enterScope(it) }
        }

        // Establish a function scope
        lang.scopeManager.enterScope(func)

        // Process the declrator, this will set name and the record declaration in case of outside
        // methods
        processDeclarator(node.childByFieldName("declarator"), func)

        // Update code to include the whole function
        func.code = lang.getCodeFromRawNode(node)

        // Parse the method body. It SHOULD exist since we are parsing this as a *definition*.
        val body = node.childByFieldName("body")
        if (body.isNull) {
            log.error(
                "We encountered a function definition '{}' that has no body. This was probably the result of a syntax error.",
                func.name
            )
        } else {
            func.body = lang.statementHandler.handle(body)
            func.setIsDefinition(func.body != null)
        }

        // Link the method declaration to a definition which is outside. Note that we should extract
        // this into a pass later on (see https://github.com/Fraunhofer-AISEC/cpg/issues/194)
        // because currently this will
        // a) only work if this happens in the same translation unit
        // b) we actually want to do this for regular function calls as well
        if (func.isDefinition && func is MethodDeclaration) {
            updateDefinition(func, func.recordDeclaration)
        }

        // Leave the function scope
        lang.scopeManager.leaveScope(func)

        // Leave the record scope, if any
        if (!insideRecord && func is MethodDeclaration) {
            func.recordDeclaration?.let { lang.scopeManager.leaveScope(it) }
        }

        return func
    }

    internal fun processDeclarator(node: Node, declaration: ValueDeclaration) {
        when (node.type) {
            "identifier" -> {
                declaration.name = lang.getCodeFromRawNode(node) ?: ""
            }
            "field_identifier" -> {
                declaration.name = lang.getCodeFromRawNode(node) ?: ""
            }
            "scoped_identifier" -> {
                processScopedIdentifier(node, declaration)
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

    @ResolveInFrontend
    private fun processScopedIdentifier(node: Node, declaration: ValueDeclaration) {
        // we are interested in the namespace part first, because this points to our class
        var namespace = lang.getCodeFromRawNode(node.childByFieldName("namespace"))

        if (namespace == null) {
            log.error(
                "Could not determine the namespace name in a scoped identifier. Trying to continue, but this will produce errors"
            )
            namespace = ""
        }

        if (declaration is MethodDeclaration) {
            // try to find the record this belongs to
            lang.scopeManager.currentScope?.let {
                val record = lang.scopeManager.getRecordForName(it, namespace)
                declaration.recordDeclaration = record
            }
        }

        val name = lang.getCodeFromRawNode(node.childByFieldName("name")) ?: ""

        declaration.name = name
    }

    private fun updateDefinition(
        functionDeclaration: FunctionDeclaration,
        recordDeclaration: RecordDeclaration?
    ) {
        if (recordDeclaration == null) {
            return
        }

        // update the definition
        var candidates: List<MethodDeclaration> =
            if (functionDeclaration is ConstructorDeclaration) {
                recordDeclaration?.constructors
            } else {
                recordDeclaration?.methods
            }
                ?: listOf()

        // look for the method or constructor
        candidates =
            candidates.filter { m: MethodDeclaration ->
                m.signature == functionDeclaration.signature
            }
        if (candidates.isEmpty() && lang.scopeManager.currentScope !is TemplateScope) {
            log.warn(
                "Could not find declaration of method {} in record {}",
                functionDeclaration.name,
                recordDeclaration.name
            )
        } else if (candidates.size > 1) {
            log.warn(
                "Found more than one candidate to connect definition of method {} in record {} to its declaration. We will comply, but this is suspicious.",
                functionDeclaration.name,
                recordDeclaration.name
            )
        }
        for (candidate in candidates) {
            candidate.definition = functionDeclaration
        }
    }

    private fun processInitDeclarator(node: Node, declaration: ValueDeclaration) {
        // going forward in the declarator chain
        processDeclarator(node.childByFieldName("declarator"), declaration)

        val hasInitializer = declaration as? HasInitializer
        hasInitializer?.let {
            // the value is nested in the init declarator
            val expression = lang.expressionHandler.handle(node.childByFieldName("value"))

            hasInitializer.initializer = expression
        }
    }

    private fun processPointerDeclarator(node: Node, declaration: ValueDeclaration) {
        processDeclarator(node.childByFieldName("declarator"), declaration)

        // reference the type using a pointer
        declaration.type = declaration.type.reference(PointerType.PointerOrigin.POINTER)
    }

    private fun processFunctionDeclarator(node: Node, declaration: ValueDeclaration) {
        processDeclarator(node.childByFieldName("declarator"), declaration)

        val parameterList = node.childByFieldName("parameters")
        for (i in 0 until parameterList.namedChildCount) {
            val param = handle(parameterList.namedChild(i))

            lang.scopeManager.addDeclaration(param)
        }
    }
}
