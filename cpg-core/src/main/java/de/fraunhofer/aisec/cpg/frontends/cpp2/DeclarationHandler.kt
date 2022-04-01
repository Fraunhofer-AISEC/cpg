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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope
import io.github.oxisto.kotlintree.jvm.Node
import io.github.oxisto.kotlintree.jvm.of
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Because of the way the CPP AST is designed, we first need to gather certain information in a
 * declarator before we can build the actual [Declaration].
 */
class Declarator(
    var name: String,
    var type: Type,
    var kind: String? = null,
    var namespace: String? = null,
    var parameters: MutableList<Declaration> = mutableListOf(),
    var initializer: Expression? = null
) {

    override fun toString(): String {
        val builder = ToStringBuilder(this, de.fraunhofer.aisec.cpg.graph.Node.TO_STRING_STYLE)

        builder.append("name", name)
        builder.append("type", type)
        builder.append("kind", kind)

        if (namespace != null) {
            builder.append("namespace", namespace)
        }

        return builder.toString()
    }
}

class DeclarationHandler(lang: CXXLanguageFrontend2) :
    Handler<Declaration, Node, CXXLanguageFrontend2>(::Declaration, lang) {
    init {
        map.put(Node::class.java, ::handleDeclaration)
    }

    private fun handleDeclaration(node: Node): Declaration? {
        for (i in 0 until node.namedChildCount) {
            if (node.namedChild(i).type.equals("attribute") && node.type.equals("declaration")) {
                val decl = handleDeclaration(node.childByFieldName("type"))
                if (decl != null) {
                    lang.processAttributes(decl, node)
                    return decl
                }
            }
        }

        val declaration =
            when (val type = node.type) {
                "function_definition" -> handleFunctionDefinition(node)
                "parameter_declaration" -> handleParameterDeclaration(node)
                "optional_parameter_declaration" -> handleParameterDeclaration(node)
                "field_declaration" -> handleFieldDeclaration(node)
                "class_specifier" -> handleRecordSpecifier(node, "class")
                "struct_specifier" -> handleRecordSpecifier(node, "struct")
                "namespace_definition" -> handleNamespaceDefinition(node)
                "declaration_list" -> handleDeclarationList(node)
                // "preproc_include" -> handleInclude(node) TODO resolve should includes be added as
                // declarations? (handleTranslationUnit with UnityBuild Issue)
                ";" -> null
                else -> {
                    val declarator = node.childByFieldName("declarator")
                    if (!declarator.isNull) {
                        return when (val declaratorType = declarator.type) {
                            "identifier" -> handleVariableDeclaration(node)
                            "pointer_declarator" -> handlePointerDeclaration(node)
                            "init_declarator" -> handleVariableDeclaration(node)
                            "array_declarator" -> handleVariableDeclaration(node)
                            "function_declarator" -> handleFunctionDefinition(node)
                            else -> {
                                LanguageFrontend.log.error(
                                    "Not handling declarator {} yet.",
                                    declarator.type
                                )
                                Declaration()
                            }
                        }
                    } else {
                        // Otherwise return empty declaration
                        LanguageFrontend.log.error("Not handling declaration type {} yet.", type)
                        Declaration()
                    }
                }
            }
        return declaration
    }

    private fun handleInclude(node: Node): Declaration {
        // TODO resolve should includes be added as declarations? (handleTranslationUnit with
        // UnityBuild Issue)
        var name = lang.getCodeFromRawNode(node.childByFieldName("path"))!!.drop(1)!!.dropLast(1)
        return NodeBuilder.newIncludeDeclaration(name)
    }

    private fun handleDeclarationList(node: Node): Declaration {
        val sequence = DeclarationSequence()

        for (i in 0 until node.namedChildCount) {
            val declaration = handle(node.namedChild(i))
            sequence.addDeclaration(declaration)
        }

        if (sequence.isSingle) {
            return sequence.first()
        }
        return sequence
    }

    private fun handleNamespaceDefinition(node: Node): Declaration {
        val fqn =
            lang.scopeManager.currentNamePrefixWithDelimiter +
                lang.getCodeFromRawNode(node.childByFieldName("name"))
        val declaration = NodeBuilder.newNamespaceDeclaration(fqn, lang.getCodeFromRawNode(node))

        lang.scopeManager.addDeclaration(declaration)
        // enter the namespace scope
        lang.scopeManager.enterScope(declaration)

        val childDeclarations = handle(node.childByFieldName("body"))
        lang.scopeManager.addDeclaration(childDeclarations)

        lang.scopeManager.leaveScope(declaration)

        return declaration
    }

    private fun handleRecordSpecifier(node: Node, kind: String): Declaration {
        val name =
            (lang.scopeManager.currentNamePrefixWithDelimiter +
                lang.getCodeFromRawNode(node.childByFieldName("name")))
                ?: ""

        val recordDeclaration =
            NodeBuilder.newRecordDeclaration(name, kind, lang.getCodeFromRawNode(node), true, lang)

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

        if (recordDeclaration.constructors.isEmpty()) {
            val constructorDeclaration =
                NodeBuilder.newConstructorDeclaration(
                    recordDeclaration.name,
                    recordDeclaration.name,
                    recordDeclaration
                )

            // set this as implicit
            constructorDeclaration.isImplicit = true

            // and set the type, constructors always have implicitly the return type of their class
            constructorDeclaration.type = TypeParser.createFrom(recordDeclaration.name, true, lang)
            recordDeclaration.addConstructor(constructorDeclaration)
            lang.scopeManager.addDeclaration(constructorDeclaration)
        }

        lang.scopeManager.leaveScope(recordDeclaration)

        return recordDeclaration
    }

    private fun handleParameterDeclaration(node: Node): ParamVariableDeclaration? {
        val startType = lang.handleTypeWithQualifier(node)

        if (startType is IncompleteType) {
            return null
        }

        val declarator = handleDeclarator("declarator" of node, startType)

        val param =
            NodeBuilder.newMethodParameterIn(
                declarator.name,
                declarator.type,
                false,
                lang.getCodeFromRawNode(node)
            )

        if (node.type.equals("optional_parameter_declaration") &&
                !node.childByFieldName("default_value").isNull
        ) {
            // The parameter has a default value
            param.default = lang.expressionHandler.handle(node.childByFieldName("default_value"))
        }

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
        var sequence = DeclarationSequence()

        for (i in 0 until node.namedChildCount) {
            if (node.namedChild(i).type.equals("field_identifier")) {
                val declaratorNode = node.namedChild(i)
                val declarator = handleDeclarator(declaratorNode, startType)

                if (isReallyAFunctionDeclaration(declaratorNode)) {
                    return declareFunction(declarator, node)
                }

                val declaration = declareVariable(declarator, node)

                (declaration as? HasInitializer)?.let {
                    if (!node.childByFieldName("default_value").isNull) {
                        it.initializer = lang.expressionHandler.handle("default_value" of node)
                    }
                }
                lang.processAttributes(declaration, node)
                sequence.addDeclaration(declaration)
            }
        }

        if (sequence.isSingle) {
            return sequence.first()
        }
        return sequence
    }

    private fun handlePointerDeclaration(node: Node): Declaration {
        val startType = lang.handleTypeWithQualifier(node)
        val declarator = handlePointerDeclarator(node, startType)
        return when (declarator.kind) {
            "function" -> {
                declareFunction(declarator, node)
            }
            else -> {
                LanguageFrontend.log.error(
                    "Not handling pointer_declarator of kind {} yet.",
                    declarator.kind
                )
                Declaration()
            }
        }
    }

    /**
     * This function handles regular variable declarations. Usually we get more information from the
     * declarator which is wrapped in a generic declaration, so we process the declarator and
     * generate the correct type.
     */
    private fun handleVariableDeclaration(node: Node): Declaration {
        val startType = lang.handleTypeWithQualifier(node)
        val sequence = DeclarationSequence()
        for (i in 0 until node.namedChildCount) {
            if (node.namedChild(i).type.equals("identifier") ||
                    node.namedChild(i).type.equals("init_declarator")
            ) {
                val declarator = handleDeclarator(node.namedChild(i), startType)
                sequence.addDeclaration(declareVariable(declarator, node))
            }
        }
        if (sequence.isSingle) {
            return sequence.first()
        }

        return sequence
    }

    /**
     * Sometimes function declarations are nested in other nodes. This function @return the nested
     * function declaration if there is any. Otherwise null.
     */
    private fun getNestedFunctionDeclaration(node: Node): Node? {
        val declarator = "declarator" of node
        if (!declarator.isNull) {
            return if (declarator.type == "function_declarator") {
                declarator
            } else {
                getNestedFunctionDeclaration(declarator)
            }
        }

        return null
    }

    /**
     * Pure function declarations within a class are showing up as field_declaration. However, we
     * need to peek into the (nested) declarators to find out if this is really a function
     * declaration.
     */
    private fun isReallyAFunctionDeclaration(node: Node): Boolean {
        if (getNestedFunctionDeclaration(node) != null) {
            return true
        }

        return false
    }

    /**
     * This function checks whether the function declaration is really a method declaration, e.g.,
     * if it contains a scoped_identifier.
     */
    private fun isMethodDeclaration(node: Node): Boolean {
        // TODO: we can probably do this with a query

        val declarator = "declarator" of node
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

    /**
     * Handles function definitions. A *definition* is similar to a *declaration*, expect it also
     * contains the actual body of the function (or method).
     *
     * There are three occurrences where this handler might get called and we are handling them a
     * little bit differently depending on the environment:
     * * A non-scoped function definition *outside* of any record will be parsed as a
     * [FunctionDeclaration].
     * * A scoped function definition, e.g. containing a `::`, *outside* of a record will be parsed
     * as a [MethodDeclaration] and associated to its record.
     * * A scoped function definition, e.g. containing a `::`, *outside* of a record with the same
     * name as the record will be parsed [ConstructorDeclaration] and associated to its record.
     * * A non-scoped function definition *inside* a record will be parsed as a [MethodDeclaration]
     * (or [ConstructorDeclaration]) and associated to its record.
     *
     * In all cases the returning object SHOULD contain a [FunctionDeclaration.body] and
     * [FunctionDeclaration.isDefinition] should be set to `true`.
     */
    private fun handleFunctionDefinition(node: Node): Declaration {
        log.debug("Handling function: {}", node.string)

        // Retrieve the non-pointer result type of the function. Any pointers or other array
        // modifiers will be part of the declarator.
        val nonPointerType = lang.handleType(node.childByFieldName("type"))

        val declarator = handleDeclarator(node.childByFieldName("declarator"), nonPointerType)

        return declareFunction(declarator, node)
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
                recordDeclaration.constructors
            } else {
                recordDeclaration.methods
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

    private fun createMethodOrConstructor(
        name: String,
        code: String,
        recordDeclaration: RecordDeclaration?
    ): MethodDeclaration {
        return if (recordDeclaration != null && name == recordDeclaration.name)
            newConstructorDeclaration(name, code, recordDeclaration)
        else newMethodDeclaration(name, code, false, recordDeclaration)
    }

    /**
     * Parses a declarator which specifies the name and kind (method, function, etc.) of the
     * declaration.
     */
    fun handleDeclarator(node: Node, startType: Type): Declarator {
        return when (node.type) {
            "identifier" -> {
                Declarator(lang.getCodeFromRawNode(node) ?: "", startType)
            }
            "field_identifier" -> {
                Declarator(lang.getCodeFromRawNode(node) ?: "", startType, "field")
            }
            "scoped_identifier" -> {
                handleScopedIdentifier(node, startType)
            }
            "init_declarator" -> {
                handleInitDeclarator(node, startType)
            }
            "array_declarator" -> {
                handleArrayDeclarator(node, startType)
            }
            "pointer_declarator" -> {
                handlePointerDeclarator(node, startType)
            }
            "reference_declarator" -> {
                handleReferenceDeclarator(node, startType)
            }
            "abstract_reference_declarator" -> {
                handleReferenceDeclarator(node, startType)
            }
            "function_declarator" -> handleFunctionDeclarator(node, startType)
            else -> {
                LanguageFrontend.log.error("Not handling declarator of type {} yet", node.type)
                Declarator("", startType)
            }
        }
    }

    private fun handleScopedIdentifier(node: Node, type: Type): Declarator {
        // we are interested in the namespace part first, because this points to our class
        val namespace = lang.getCodeFromRawNode(node.childByFieldName("namespace"))

        if (namespace == null) {
            log.error(
                "Could not determine the namespace name in a scoped identifier. Trying to continue, but this will produce errors"
            )
        }

        val name = lang.getCodeFromRawNode(node.childByFieldName("name")) ?: ""

        return Declarator(name, type, null, namespace)
    }

    private fun handleInitDeclarator(node: Node, type: Type): Declarator {
        // going forward in the declarator chain
        var declarator = handleDeclarator(node.childByFieldName("declarator"), type)

        // the value is nested in the init declarator
        val value = node.childByFieldName("value")
        val expression: Expression
        if (value.type.equals("argument_list")) {
            expression =
                de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructExpression(
                    lang.getCodeFromRawNode(value)
                )
            for (i in 0 until value.namedChildCount) {
                val arg = lang.expressionHandler.handle(value.namedChild(i))
                (expression as ConstructExpression).addArgument(arg)
            }
        } else {
            expression = lang.expressionHandler.handle(node.childByFieldName("value"))
        }

        declarator.initializer = expression

        return declarator
    }

    private fun handleArrayDeclarator(node: Node, type: Type): Declarator {
        var declarator = handleDeclarator(node.childByFieldName("declarator"), type)

        declarator.type = declarator.type.reference(PointerType.PointerOrigin.ARRAY)
        return declarator
    }

    private fun handlePointerDeclarator(node: Node, type: Type): Declarator {
        var declarator = handleDeclarator(node.childByFieldName("declarator"), type)

        // reference the type using a pointer
        declarator.type = type.reference(PointerType.PointerOrigin.POINTER)

        return declarator
    }

    private fun handleReferenceDeclarator(node: Node, type: Type): Declarator {
        val declarator = handleDeclarator(node.child(1), type)

        // reference the type using a pointer
        declarator.type = ReferenceType(declarator.type)

        return declarator
    }

    /**
     * Handles a function declarator. It primarily takes care of gathering parameters, which we will
     * add to the [Declaration] later as [ParamVariableDeclaration].
     */
    private fun handleFunctionDeclarator(node: Node, type: Type): Declarator {
        val declarator = handleDeclarator(node.childByFieldName("declarator"), type)

        // All functions inside a record are automatically methods (or constructors)
        declarator.kind =
            when {
                lang.scopeManager.isInRecord -> {
                    if (declarator.name == lang.scopeManager.currentRecord?.name) {
                        "constructor"
                    } else {
                        "method"
                    }
                }
                declarator.namespace != null -> {
                    if (declarator.name == declarator.namespace) {
                        "constructor"
                    } else {
                        "method"
                    }
                }
                else -> {
                    "function"
                }
            }

        val parameterList = node.childByFieldName("parameters")
        for (i in 0 until parameterList.namedChildCount) {
            val param = handle(parameterList.namedChild(i))
            if (param != null) {
                declarator.parameters += param
            }
        }

        return declarator
    }

    fun declareFunction(declarator: Declarator, node: Node): FunctionDeclaration {
        val insideRecord = lang.scopeManager.currentRecord != null
        var recordDeclaration: RecordDeclaration?

        // It is important to know whether we are within a record scope or outside. If we are
        // inside, every function is automatically a method. If we are outside, we need to check if
        // the function name is scoped, then it is also a method.
        val func =
            when {
                insideRecord -> {
                    // if it is inside a record scope, it is a method
                    recordDeclaration = lang.scopeManager.currentRecord
                    createMethodOrConstructor(
                        declarator.name,
                        lang.getCodeFromRawNode(node) ?: "",
                        recordDeclaration
                    )
                }
                declarator.kind == "method" -> {
                    newMethodDeclaration(
                        declarator.name,
                        lang.getCodeFromRawNode(node),
                        false,
                        null
                    )
                }
                declarator.kind == "constructor" -> {
                    newConstructorDeclaration(declarator.name, lang.getCodeFromRawNode(node), null)
                }
                else -> {
                    newFunctionDeclaration(declarator.name, lang.getCodeFromRawNode(node))
                }
            }
        lang.scopeManager.addDeclaration(func)

        func.type = declarator.type

        // If we are outside of a record and are dealing with a method declaration, we need to
        // temporarily enter the record scope (and leave it later). This allows us to resolve member
        // variables later
        if (!insideRecord && func is MethodDeclaration) {
            func.recordDeclaration?.let { lang.scopeManager.enterScope(it) }
        }

        // Establish a function scope
        lang.scopeManager.enterScope(func)

        // Add the parameters we have gathered in the declarator
        declarator.parameters.forEach { lang.scopeManager.addDeclaration(it) }
        declarator.parameters.forEach {
            if (it is ParamVariableDeclaration && !func.parameters.contains(it)) {
                func.addParameter(it)
            }
        }

        if (func is MethodDeclaration && func.recordDeclaration == null) {
            // try to find the record this belongs to
            lang.scopeManager.currentScope?.let {
                recordDeclaration =
                    declarator.namespace?.let { it1 -> lang.scopeManager.getRecordForName(it, it1) }
                func.recordDeclaration = recordDeclaration
            }
        }

        // Update code to include the whole function
        func.code = lang.getCodeFromRawNode(node)

        // Parse the method body. It SHOULD exist since we are parsing this as a *definition*.
        val body = node.childByFieldName("body")
        if (body.isNull) {
            /*log.error(
                "We encountered a function definition '{}' that has no body. This was probably the result of a syntax error.",
                func.name
            )*/
        } else {
            func.body = lang.statementHandler.handle(body)

            // add an implicit return statement, if there is none
            if (func.body is CompoundStatement) {
                if ((func.body as CompoundStatement).statements.size == 0 ||
                        (func.body as CompoundStatement).statements.last() !is ReturnStatement
                ) {
                    val returnStatement = NodeBuilder.newReturnStatement("return;")
                    returnStatement.isImplicit = true
                    (func.body as CompoundStatement).addStatement(returnStatement)
                }
            }

            func.setIsDefinition(func.body != null)
        }

        // Link the method declaration to a definition which is outside. Note that we should extract
        // this into a pass later on (see https://github.com/Fraunhofer-AISEC/cpg/issues/194)
        // because currently this will
        // a) only work if this happens in the same translation unit
        // b) we actually want to do this for regular function calls as well
        if (func.isDefinition && func is MethodDeclaration && !insideRecord) {
            updateDefinition(func, func.recordDeclaration)
        }

        // Handle attribute
        lang.processAttributes(func, node)

        // Leave the function scope
        lang.scopeManager.leaveScope(func)

        // Leave the record scope, if any
        if (!insideRecord && func is MethodDeclaration) {
            func.recordDeclaration?.let { lang.scopeManager.leaveScope(it) }
        }

        return func
    }

    fun declareVariable(declarator: Declarator, node: Node): ValueDeclaration {
        val declaration =
            if (declarator.kind == "field") {
                newFieldDeclaration(
                    declarator.name,
                    declarator.type,
                    listOf(),
                    lang.getCodeFromRawNode(node),
                    lang.getLocationFromRawNode(node),
                    declarator.initializer,
                    true
                )
            } else {
                newVariableDeclaration(
                    declarator.name,
                    declarator.type,
                    lang.getCodeFromRawNode(node),
                    true
                )
            }
        declaration.location = lang.getLocationFromRawNode(node)
        declaration.code = lang.getCodeFromRawNode(node)
        declaration.initializer = declarator.initializer

        return declaration
    }
}
