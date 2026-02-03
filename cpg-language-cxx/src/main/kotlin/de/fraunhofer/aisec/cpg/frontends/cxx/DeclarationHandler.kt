/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.function.Supplier
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit.IDependencyTree.IASTInclusionNode
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

/**
 * This class is a [CXXHandler] which takes care of translating C/C++
 * [declarations](https://en.cppreference.com/w/cpp/language/declarations) into [Declaration] nodes.
 * It heavily relies on its sibling handler, the [DeclaratorHandler].
 *
 * In the C/C++ language, the meaning of a declaration depends on two major factors: First, a list
 * of so-called declaration
 * [specifiers](https://en.cppreference.com/w/cpp/language/declarations#Specifiers), which specify
 * the type of declaration. Prominent examples include `class` or `enum`. Second, a list of
 * [declarators](https://en.cppreference.com/w/cpp/language/declarations#Declarators), which
 * describe the actual "content" of the declaration, such as its name or parameters (in case of a
 * function).
 *
 * Since the logic behind a declarator is quite complex, this logic is extracted into its own
 * handler. In fact, in most cases the [DeclaratorHandler] actually creates the CPG [Declaration]
 * and the [DeclarationHandler] modifies the declaration depending on the declaration specifiers.
 */
class DeclarationHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Declaration, IASTNode>(Supplier(::ProblemDeclaration), lang) {

    override fun handleNode(node: IASTNode): Declaration {
        return when (node) {
            is IASTSimpleDeclaration -> handleSimpleDeclaration(node)
            is IASTFunctionDefinition -> handleFunctionDefinition(node)
            is IASTProblemDeclaration -> handleProblem(node)
            is CPPASTTemplateDeclaration -> handleTemplateDeclaration(node)
            is CPPASTNamespaceDefinition -> handleNamespace(node)
            is CPPASTUsingDirective -> handleUsingDirective(node)
            is CPPASTUsingDeclaration -> handleUsingDeclaration(node)
            is CPPASTAliasDeclaration -> handleAliasDeclaration(node)
            is CPPASTNamespaceAlias -> handleNamespaceAlias(node)
            is CPPASTLinkageSpecification -> handleLinkageSpecification(node)
            else -> {
                handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    /**
     * Translates a C++
     * [namespace alias](https://en.cppreference.com/w/cpp/language/namespace_alias) into an alias
     * handled by an [ImportDeclaration].
     */
    private fun handleNamespaceAlias(ctx: CPPASTNamespaceAlias): ImportDeclaration {
        val from = parseName(ctx.mappingName.toString())
        val to = parseName(ctx.alias.toString())

        val import =
            newImportDeclaration(from, style = ImportStyle.IMPORT_NAMESPACE, to, rawNode = ctx)

        return import
    }

    /**
     * Translates a C++
     * [using directive](https://en.cppreference.com/w/cpp/language/namespace#Using-directives) into
     * a [ImportDeclaration].
     */
    private fun handleUsingDirective(ctx: CPPASTUsingDirective): Declaration {
        val import = parseName(ctx.qualifiedName.toString())
        val declaration =
            newImportDeclaration(
                import,
                style = ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE,
                rawNode = ctx,
            )

        return declaration
    }

    /**
     * Translates a C++
     * [using declaration](https://en.cppreference.com/w/cpp/language/using_declaration) into a
     * [ImportDeclaration].
     */
    private fun handleUsingDeclaration(ctx: CPPASTUsingDeclaration): Declaration {
        val import = parseName(ctx.name.toString())
        val declaration =
            newImportDeclaration(
                import,
                style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                rawNode = ctx,
            )

        return declaration
    }

    /**
     * Translates a C++ [namespace](https://en.cppreference.com/w/cpp/language/namespace#Namespaces)
     * into a [NamespaceDeclaration].
     */
    private fun handleNamespace(ctx: CPPASTNamespaceDefinition): NamespaceDeclaration {
        val nsd = newNamespaceDeclaration(ctx.name.toString(), rawNode = ctx)

        // Enter the namespace scope
        frontend.scopeManager.enterScope(nsd)

        // Finally, handle all declarations within that namespace
        for (child in ctx.declarations) {
            val decl = handle(child) ?: continue

            frontend.scopeManager.addDeclaration(decl)
            nsd.declarations += decl
        }

        frontend.scopeManager.leaveScope(nsd)

        return nsd
    }

    private fun handleProblem(ctx: IASTProblemDeclaration): Declaration {
        Util.errorWithFileLocation(frontend, ctx, log, ctx.problem.message)

        val problem = newProblemDeclaration(ctx.problem.message)

        return problem
    }

    /**
     * Translates a C/C++ (function)[https://en.cppreference.com/w/cpp/language/functions]
     * definition into a [FunctionDeclaration]. A definition, in contrast to a declaration also has
     * a function body. Function declarations are most likely handled by [handleSimpleDeclaration].
     * However, in both cases, the majority of the function is described by a declarator, which gets
     * parsed by [DeclaratorHandler.handleFunctionDeclarator].
     */
    private fun handleFunctionDefinition(ctx: IASTFunctionDefinition): Declaration {
        // TODO: A problem with cpp functions is that we cannot know if they may throw an exception
        //  as throw(...) is not compiler enforced (Problem for TryStatement)
        val declaration = frontend.declaratorHandler.handle(ctx.declarator)

        if (declaration !is FunctionDeclaration) {
            return ProblemDeclaration(
                "declarator of function definition is not a function declarator"
            )
        }

        // Retrieve the type. This should parse as a function type, otherwise it is unknown.
        val type = frontend.typeOf(ctx.declarator, ctx.declSpecifier, declaration) as? FunctionType
        declaration.type = type ?: unknownType()
        declaration.isDefinition = true

        // We also need to set the return type, based on the function type.
        declaration.returnTypes = type?.returnTypes ?: listOf(incompleteType())

        // We want to determine, whether this is a function definition that is external to its
        // scope. This is a usual case in C++, where the named scope, such as a record or namespace
        // only includes the AST element for a function declaration and the definition is outside.
        val outsideOfScope = frontend.scopeManager.currentScope != declaration.scope

        // Store the reference to a declaration holder of a named scope.
        val holder = (declaration.scope as? NameScope)?.astNode

        if (holder != null && outsideOfScope) {
            // everything inside the method is within the scope of its record or namespace
            frontend.scopeManager.enterScope(holder)
        }

        // Enter the scope of our function
        frontend.scopeManager.enterScope(declaration)

        // Since this is a definition, the body should always be there, but we need to make sure in
        // case of parsing errors.
        if (ctx.body != null) {
            // Let the statement handler take care of the function body. The outcome should (always)
            // be a compound statement, holding all other statements.
            val bodyStatement = frontend.statementHandler.handle(ctx.body)
            if (bodyStatement is Block) {
                val statements = bodyStatement.statementEdges

                // get the last statement
                var lastStatement: Statement? = null
                if (statements.isNotEmpty()) {
                    lastStatement = statements[statements.size - 1].end
                }

                // add an implicit return statement, if there is none
                if (lastStatement !is ReturnStatement) {
                    val returnStatement = newReturnStatement()
                    returnStatement.isImplicit = true
                    bodyStatement.statements += returnStatement
                }
                declaration.body = bodyStatement
            }
        }

        frontend.processAttributes(declaration, ctx)

        frontend.scopeManager.leaveScope(declaration)

        if (holder != null && outsideOfScope) {
            frontend.scopeManager.leaveScope(holder)
        }

        return declaration
    }

    /**
     * A virtual property on a [IASTSimpleDeclaration] that determines whether this is a typedef or
     * not. For some reason it seems that Eclipse CDT has no other means of differentiating a
     * typedef declaration from a regular one, except looking at the raw code
     */
    private val IASTSimpleDeclaration.isTypedef: Boolean
        get() {
            return if (this.declSpecifier is IASTCompositeTypeSpecifier) {
                if (this.declSpecifier.rawSignature.contains("typedef")) {
                    // This is very stupid. For composite type specifiers, we need to make sure that
                    // we do not match simply because our declarations contain a typedef.
                    // The problem is that we cannot correctly detect the case where both our "main"
                    // declaration and our sub declarations contain a typedef :(
                    (this.declSpecifier as IASTCompositeTypeSpecifier).getDeclarations(true).none {
                        it.rawSignature.contains("typedef")
                    }
                } else {
                    false
                }
            } else {
                this.declSpecifier.rawSignature.contains("typedef")
            }
        }

    private fun handleTemplateDeclaration(ctx: CPPASTTemplateDeclaration): TemplateDeclaration {
        val name = ctx.rawSignature.split("{").toTypedArray()[0].replace('\n', ' ').trim()

        val templateDeclaration: TemplateDeclaration =
            if (ctx.declaration is CPPASTFunctionDefinition) {
                newFunctionTemplateDeclaration(name, rawNode = ctx)
            } else {
                newRecordTemplateDeclaration(name, rawNode = ctx)
            }

        templateDeclaration.location = frontend.locationOf(ctx)
        frontend.scopeManager.enterScope(templateDeclaration)
        addTemplateParameters(ctx, templateDeclaration)

        // Handle Template
        val innerDeclaration = frontend.declarationHandler.handle(ctx.declaration)
        // Add it to the realization
        if (innerDeclaration != null) {
            templateDeclaration.addDeclaration(innerDeclaration)
        }

        frontend.scopeManager.leaveScope(templateDeclaration)
        if (templateDeclaration is FunctionTemplateDeclaration) {
            // Fix typeName
            templateDeclaration.name = templateDeclaration.realizations[0].name.clone()
        } else
            (innerDeclaration as? RecordDeclaration)?.let {
                addParameterizedTypesToRecord(templateDeclaration, it)
            }

        addRealizationToScope(templateDeclaration)

        return templateDeclaration
    }

    /**
     * Add Template Parameters to the TemplateDeclaration
     *
     * @param ctx
     * @param templateDeclaration
     */
    private fun addTemplateParameters(
        ctx: CPPASTTemplateDeclaration,
        templateDeclaration: TemplateDeclaration,
    ) {
        for (templateParameter in ctx.templateParameters) {
            if (templateParameter is CPPASTSimpleTypeTemplateParameter) {
                // Handle Type Parameters
                val typeParamDecl =
                    frontend.declaratorHandler.handle(templateParameter) as TypeParameterDeclaration
                val parameterizedType =
                    frontend.typeManager.createOrGetTypeParameter(
                        templateDeclaration,
                        templateParameter.name.toString(),
                        language,
                    )
                typeParamDecl.type = parameterizedType
                if (templateParameter.defaultType != null) {
                    val defaultType = frontend.typeOf(templateParameter.defaultType)
                    typeParamDecl.default = newTypeExpression(defaultType.name, type = defaultType)
                }
                templateDeclaration.parameters += typeParamDecl
            } else if (templateParameter is CPPASTParameterDeclaration) {
                // Handle Value Parameters
                val nonTypeTemplateParamDeclaration =
                    frontend.parameterDeclarationHandler.handle(
                        templateParameter as IASTParameterDeclaration
                    )
                if (nonTypeTemplateParamDeclaration is ParameterDeclaration) {
                    if (templateParameter.declarator.initializer != null) {
                        val defaultExpression =
                            frontend.initializerHandler.handle(
                                templateParameter.declarator.initializer
                            )
                        nonTypeTemplateParamDeclaration.default = defaultExpression
                        defaultExpression?.let {
                            nonTypeTemplateParamDeclaration.prevDFGEdges += it
                            it.nextDFGEdges += nonTypeTemplateParamDeclaration
                        }
                    }
                    frontend.scopeManager.addDeclaration(nonTypeTemplateParamDeclaration)
                    templateDeclaration.parameters += nonTypeTemplateParamDeclaration
                }
            }
        }
    }

    /**
     * Adds the generic realization of the template to the scope
     *
     * @param templateDeclaration
     */
    private fun addRealizationToScope(templateDeclaration: TemplateDeclaration) {
        for (declaration in templateDeclaration.realizations) {
            frontend.scopeManager.addDeclaration(declaration)
        }
    }

    /**
     * Adjusts the type created in a [RecordDeclaration] to include the parametrized types of the
     * [templateDeclaration]. This is necessary because templates are being parsed after all record
     * types (e.g. used in receivers) are created.
     *
     * @param templateDeclaration the template
     * @param innerDeclaration the record
     */
    private fun addParameterizedTypesToRecord(
        templateDeclaration: TemplateDeclaration,
        innerDeclaration: RecordDeclaration,
    ) {
        val parameterizedTypes = frontend.typeManager.getAllParameterizedType(templateDeclaration)

        // Loop through all the methods and adjust their receiver types
        for (method in innerDeclaration.methods) {
            // Add ParameterizedTypes to type
            method.receiver?.let {
                it.type = addParameterizedTypesToType(it.type, parameterizedTypes)
            }
        }

        // Add parameterizedTypes to ConstructorDeclaration type and adjust their receiver types
        for (constructor in innerDeclaration.constructors) {
            constructor.receiver?.let {
                it.type = addParameterizedTypesToType(it.type, parameterizedTypes)
            }

            // We need to add the type to (first) return type as well as the function type
            constructor.returnTypes =
                constructor.returnTypes.map { addParameterizedTypesToType(it, parameterizedTypes) }
            constructor.type =
                FunctionType(
                    constructor.type.typeName,
                    (constructor.type as? FunctionType)?.parameters ?: listOf(),
                    constructor.returnTypes,
                    this.language,
                )
        }
    }

    /**
     * Connects the ObjectType node to the ParameterizedType nodes with the generics edge
     *
     * @param type
     * @param parameterizedTypes
     */
    private fun addParameterizedTypesToType(
        type: Type,
        parameterizedTypes: List<ParameterizedType>,
    ): Type {
        if (type is ObjectType) {
            // Because we cannot mutate the existing type (otherwise this will affect ALL usages of
            // it), we need to create a new type with the correct generics
            return objectType(type.name, parameterizedTypes)
        } else if (type is PointerType) {
            return addParameterizedTypesToType(type.elementType, parameterizedTypes).pointer()
        }

        return type
    }

    private fun handleSimpleDeclaration(ctx: IASTSimpleDeclaration): Declaration {
        val sequence = DeclarationSequence()
        val declSpecifier = ctx.declSpecifier

        // check, whether the declaration specifier also contains declarations, e.g. class
        // definitions or enums
        val (primaryDeclaration, useNameOfDeclarator) =
            handleDeclarationSpecifier(declSpecifier, ctx, sequence)

        // Fill template params, if needed
        val templateParams = extractTemplateParams(ctx, declSpecifier)

        // Loop through all declarators, as we can potentially have multiple declarations here
        for (declarator in ctx.declarators) {
            // If a previous step informed us that we should take the name of the primary
            // declaration, we do so here. This most likely is the case of a typedef struct.
            val declSpecifierToUse =
                if (useNameOfDeclarator && declSpecifier is IASTCompositeTypeSpecifier) {
                    val copy = declSpecifier.copy()
                    copy.name = CPPASTName(primaryDeclaration?.name?.toString()?.toCharArray())
                    copy
                } else {
                    declSpecifier
                }

            var type: Type

            if (ctx.isTypedef) {
                type = frontend.typeOf(declarator, declSpecifierToUse)

                val (nameDecl, _) = declarator.realName()

                // Handle typedefs.
                val declaration = handleTypedef(nameDecl.name, type)

                sequence.addDeclaration(declaration)
            } else {
                // Parse the declaration first, so we can supply the declaration as a hint to
                // the typeOf function.
                val declaration =
                    frontend.declaratorHandler.handle(declarator) as? ValueDeclaration ?: continue

                // Parse the type (with some hints)
                type = frontend.typeOf(declarator, declSpecifierToUse, declaration)

                // For function *declarations*, we need to update the return types based on the
                // function type. For function *definitions*, this is done in
                // [handleFunctionDefinition].
                if (declaration is FunctionDeclaration) {
                    declaration.returnTypes =
                        (type as? FunctionType)?.returnTypes ?: listOf(incompleteType())
                }

                // We also need to set the type, based on the declarator type.
                declaration.type = type

                // process attributes
                frontend.processAttributes(declaration, ctx)
                sequence.addDeclaration(declaration)

                // We want to make sure that we parse the initializer *after* we have set the
                // type. This has several advantages:
                // * This way we can deduce, whether our initializer needs to have the
                //   declared type (in case of a ConstructExpression);
                // * or if the declaration needs to have the same type as the initializer (when
                //   an auto-type is used). The latter case is done internally by the
                //   VariableDeclaration class and its type observer.
                // * Additionally, it makes sure that the type is known before parsing the
                //   initializer. This allows us to guess cast vs. call expression in the
                //   initializer.
                if (declaration is VariableDeclaration) {
                    // Set template parameters of the variable (if any)
                    if (templateParams != null) {
                        declaration.templateParameters = templateParams
                    }

                    // Parse the initializer, if we have one
                    declarator.initializer?.let {
                        val initializer = frontend.initializerHandler.handle(it)
                        when {
                            // We need to set a resolution "helper" for function pointers, so that a
                            // reference to this declaration can resolve the function pointer (using
                            // the type of this declaration). The typical (and only) scenario we
                            // support here is the assignment of a `&ref` as initializer.
                            initializer is UnaryOperator && type is FunctionPointerType -> {
                                val ref = initializer.input as? Reference
                                ref?.resolutionHelper = declaration
                            }
                        }

                        declaration.initializer = initializer
                    }
                }
            }
        }

        return simplifySequence(sequence)
    }

    /**
     * In C++, a [IASTDeclSpecifier] can potentially also contain declarations, e.g. records or
     * enums. This function gathers these [Declaration] nodes, before processing the remainder of
     * declarations within [IASTSimpleDeclaration.getDeclarators].
     */
    private fun handleDeclarationSpecifier(
        declSpecifier: IASTDeclSpecifier?,
        ctx: IASTSimpleDeclaration,
        sequence: DeclarationSequence,
    ): Pair<Declaration?, Boolean> {
        var primaryDeclaration: Declaration? = null
        var useNameOfDeclarator = false

        when (declSpecifier) {
            is IASTCompositeTypeSpecifier -> {
                primaryDeclaration =
                    frontend.declaratorHandler.handle(
                        ctx.declSpecifier as IASTCompositeTypeSpecifier
                    )

                if (primaryDeclaration != null) {
                    // handle typedef
                    if (
                        primaryDeclaration.name.isEmpty() &&
                            ctx.rawSignature.trim().startsWith("typedef")
                    ) {
                        // This is a special case, which is a common idiom in C, to typedef an
                        // unnamed struct into a type. For example `typedef struct { int a; } S`. In
                        // this case the record declaration actually has no name and only the
                        // typedef'd name is called S. However, to make things a little bit easier
                        // we also transfer the name to the record declaration.
                        ctx.declarators.firstOrNull()?.name?.toString()?.let {
                            primaryDeclaration.name = parseName(it)
                            useNameOfDeclarator = false
                        }
                    }
                    frontend.processAttributes(primaryDeclaration, ctx)

                    sequence.addDeclaration(primaryDeclaration)
                }
            }
            is IASTElaboratedTypeSpecifier -> {
                // In the future, we might want to have declaration chains, but for now, there is
                // nothing to do
            }
            is IASTEnumerationSpecifier -> {
                // Handle it as an enum
                primaryDeclaration = handleEnum(ctx, declSpecifier)

                sequence.addDeclaration(primaryDeclaration)

                // process attributes
                frontend.processAttributes(primaryDeclaration, ctx)
            }
        }

        // TODO `useNameOfDeclarator` is always `false` -> issue?
        return Pair(primaryDeclaration, useNameOfDeclarator)
    }

    /**
     * Extracts template parameters (used for [VariableDeclaration.templateParameters]) out of the
     * declaration (if it has any), otherwise null is returned.
     */
    private fun extractTemplateParams(
        ctx: IASTSimpleDeclaration,
        declSpecifier: IASTDeclSpecifier,
    ): MutableList<AstNode>? {
        if (
            !ctx.isTypedef &&
                declSpecifier is CPPASTNamedTypeSpecifier &&
                declSpecifier.name is CPPASTTemplateId
        ) {
            val templateParams = mutableListOf<AstNode>()
            val templateId = declSpecifier.name as CPPASTTemplateId
            for (templateArgument in templateId.templateArguments) {
                if (templateArgument is CPPASTTypeId) {
                    val genericInstantiation = frontend.typeOf(templateArgument)
                    templateParams.add(
                        newTypeExpression(
                            genericInstantiation.name.toString(),
                            genericInstantiation,
                        )
                    )
                } else if (templateArgument is IASTExpression) {
                    val expression = frontend.expressionHandler.handle(templateArgument)
                    expression?.let { templateParams.add(it) }
                }
            }

            return templateParams
        }

        return null
    }

    /**
     * @param aliasName is an [IASTName] that describes the new type name.
     * @param type is the original type.
     */
    private fun handleTypedef(aliasName: IASTName, type: Type): TypedefDeclaration {
        // C/C++ behaves slightly different when it comes to typedefs in a function or in a record,
        // such as a class or struct. A typedef in a function (or actually in any block scope) is
        // scoped to the current block. A  typedef in a record declaration is scoped to the global
        // scope,
        // but its alias name is FQN'd.
        val (scope, doFqn) =
            if (frontend.scopeManager.currentScope is RecordScope) {
                Pair(frontend.scopeManager.globalScope, true)
            } else {
                Pair(frontend.scopeManager.currentScope, false)
            }
        // TODO(oxisto): What about namespaces?

        val declaration =
            frontend.newTypedefDeclaration(type, frontend.typeOf(aliasName, doFqn = doFqn))

        frontend.scopeManager.addTypedef(declaration, scope)

        return declaration
    }

    private fun handleEnum(
        ctx: IASTSimpleDeclaration,
        declSpecifier: IASTEnumerationSpecifier,
    ): EnumDeclaration {
        val entries = mutableListOf<EnumConstantDeclaration>()
        val enum = newEnumDeclaration(name = declSpecifier.name.toString(), rawNode = ctx)

        // Loop through its members
        for (enumerator in declSpecifier.enumerators) {
            // Enums are a bit complicated. Their fully qualified name (in C++) includes the enum
            // class, so e.g. `MyEnum::THIS'. In order to do that, we need to be in the `MyEnum`
            // scope when we create it. But, the symbol of the enum can be resolved using just
            // the enum constant `THIS` as well as `MyEnum::THIS` (at least in C++11). So we need to
            // put the symbol in the outer scope as well as the enum's scope.
            frontend.scopeManager.enterScope(enum)
            val enumConst =
                newEnumConstantDeclaration(enumerator.name.toString(), rawNode = enumerator)
            frontend.scopeManager.addDeclaration(enumConst)
            entries += enumConst

            frontend.scopeManager.leaveScope(enum)

            // In C/C++, default enums are of type int
            enumConst.type = primitiveType("int")

            // Also put the symbol in the outer scope (but do not add AST nodes)
            frontend.scopeManager.addDeclaration(enumConst)
        }

        enum.entries = entries

        return enum
    }

    /**
     * Translates a C++ (linkage
     * specification)[https://en.cppreference.com/w/cpp/language/language_linkage]. Actually, we do
     * not care about the linkage specification per-se, but we need to parse the declaration(s) it
     * contains.
     */
    private fun handleLinkageSpecification(ctx: CPPASTLinkageSpecification): Declaration {
        val sequence = DeclarationSequence()

        // Just forward its declaration(s) to our handler
        for (decl in ctx.declarations) {
            handle(decl)?.let { sequence += it }
        }

        return simplifySequence(sequence)
    }

    /**
     * @param sequence
     * @return First Element of DeclarationSequence if the Sequence consist of only one element,
     *   full sequence if it contains more than one element
     */
    private fun simplifySequence(sequence: DeclarationSequence): Declaration {
        return if (sequence.isSingle) {
            sequence.first()
        } else {
            sequence
        }
    }

    private fun parseInclusions(
        includes: Array<IASTInclusionNode>?,
        allIncludes: HashMap<String, HashSet<String?>>,
    ) {
        if (includes != null) {
            for (n in includes) {
                val strings =
                    allIncludes.computeIfAbsent(n.includeDirective.containingFilename) { HashSet() }
                strings.add(n.includeDirective.path)
                parseInclusions(n.nestedInclusions, allIncludes)
            }
        }
    }

    fun handleTranslationUnit(translationUnit: IASTTranslationUnit): TranslationUnitDeclaration {
        val node =
            newTranslationUnitDeclaration(translationUnit.filePath, rawNode = translationUnit)

        // There might have been errors in the previous translation unit and in any case
        // we need to reset the scope manager scope to global scope to avoid spilling scope errors
        // into other translation units
        frontend.scopeManager.resetToGlobal(node)
        frontend.currentTU = node
        val problematicIncludes = HashMap<String?, HashSet<ProblemDeclaration>>()

        for (declaration in translationUnit.declarations) {
            val decl = handle(declaration) ?: continue
            if (decl is DeclarationSequence) {
                decl.declarations.forEach {
                    frontend.scopeManager.addDeclaration(it)
                    node.addDeclaration(it)
                }
            } else {
                frontend.scopeManager.addDeclaration(decl)
                node.addDeclaration(decl)
            }
        }

        if (frontend.config.addIncludesToGraph) {
            addIncludes(translationUnit, problematicIncludes, node)
        }

        return node
    }

    /**
     * Translates a C++ (type alias)[https://en.cppreference.com/w/cpp/language/type_alias] into a
     * [TypedefDeclaration].
     */
    private fun handleAliasDeclaration(ctx: CPPASTAliasDeclaration): TypedefDeclaration {
        val type = frontend.typeOf(ctx.mappingTypeId)

        return handleTypedef(ctx.alias, type)
    }

    private fun addIncludes(
        translationUnit: IASTTranslationUnit,
        problematicIncludes: Map<String?, Set<ProblemDeclaration>>,
        node: TranslationUnitDeclaration,
    ) {
        // TODO: Remark CB: I am not quite sure, what the point of the code beyond this line is.
        // Probably needs to be refactored

        // this tree is a bit problematic: If a file was already included before, it will not be
        // shown connecting to other leaves.
        // I.e. if FileA includes FileB and FileC, and FileC also includes FileB, _no_
        // connection
        // between FileC and FileB will be shown.
        val dependencyTree = translationUnit.dependencyTree
        val allIncludes = HashMap<String, HashSet<String?>>()
        parseInclusions(dependencyTree.inclusions, allIncludes)

        if (allIncludes.isEmpty()) {
            return
        }

        // create all include nodes, potentially attach problemdecl
        val includeMap = HashMap<String?, IncludeDeclaration>()

        for (includesStrings in allIncludes.values) {
            for (includeString in includesStrings) {
                if (includeString in includeMap) {
                    continue
                }

                val problems = problematicIncludes[includeString]
                val includeDeclaration = newIncludeDeclaration(includeString ?: "")
                problems?.forEach { includeDeclaration.problems += it }
                includeMap[includeString] = includeDeclaration
            }
        }

        // attach to root note
        for (incl in allIncludes[translationUnit.filePath] ?: listOf()) {
            includeMap[incl]?.let { node.addDeclaration(it) }
        }
        allIncludes.remove(translationUnit.filePath)
        // attach to remaining nodes
        for ((key, value) in allIncludes) {
            val includeDeclaration = includeMap[key] ?: continue
            for (s in value) {
                includeMap[s]?.let { includeDeclaration.includes += it }
            }
        }
    }
}
