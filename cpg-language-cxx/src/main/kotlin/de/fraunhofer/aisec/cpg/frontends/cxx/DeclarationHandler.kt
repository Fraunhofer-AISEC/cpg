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
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.TemplateScope
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
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
    CXXHandler<Declaration, IASTDeclaration>(Supplier(::ProblemDeclaration), lang) {

    override fun handleNode(node: IASTDeclaration): Declaration {
        return when (node) {
            is IASTSimpleDeclaration -> handleSimpleDeclaration(node)
            is IASTFunctionDefinition -> handleFunctionDefinition(node)
            is IASTProblemDeclaration -> handleProblem(node)
            is CPPASTTemplateDeclaration -> handleTemplateDeclaration(node)
            is CPPASTNamespaceDefinition -> handleNamespace(node)
            is CPPASTUsingDirective -> handleUsingDirective(node)
            else -> {
                return handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    /**
     * Translates a C++ (using
     * directive)[https://en.cppreference.com/w/cpp/language/namespace#Using-directives] into a
     * [UsingDeclaration]. However, currently, no actual adjustment of available names / scopes is
     * done yet.
     */
    private fun handleUsingDirective(using: CPPASTUsingDirective): Declaration {
        return newUsingDeclaration(qualifiedName = using.qualifiedName.toString(), rawNode = using)
    }

    /**
     * Translates a C++ [namespace](https://en.cppreference.com/w/cpp/language/namespace#Namespaces)
     * into a [NamespaceDeclaration].
     */
    private fun handleNamespace(ctx: CPPASTNamespaceDefinition): NamespaceDeclaration {
        val declaration = newNamespaceDeclaration(ctx.name.toString(), frontend.codeOf(ctx))

        frontend.scopeManager.addDeclaration(declaration)

        // Enter the namespace scope
        frontend.scopeManager.enterScope(declaration)

        // Finally, handle all declarations within that namespace
        for (child in ctx.declarations) {
            handle(child)
        }

        frontend.scopeManager.leaveScope(declaration)

        return declaration
    }

    private fun handleProblem(ctx: IASTProblemDeclaration): Declaration {
        val problem = newProblemDeclaration(ctx.problem.message)

        frontend.scopeManager.addDeclaration(problem)

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
        declaration.returnTypes = type?.returnTypes ?: listOf(IncompleteType())

        // We want to determine, whether this is a function definition that is external to its
        // scope. This is a usual case in C++, where the named scope, such as a record or namespace
        // only includes the AST element for a function declaration and the definition is outside.
        val outsideOfScope = frontend.scopeManager.currentScope != declaration.scope

        // Store the reference to a declaration holder of a named scope.
        val holder = (declaration.scope as? NameScope)?.astNode

        if (holder != null) {
            if (outsideOfScope) {
                // everything inside the method is within the scope of its record or namespace
                frontend.scopeManager.enterScope(holder)
            }

            // Update the definition
            // TODO: This should be extracted into a separate pass and done for all function
            //  declarations, also global ones
            var candidates =
                (holder as? DeclarationHolder)
                    ?.declarations
                    ?.filterIsInstance<FunctionDeclaration>()
                    ?: listOf()

            // Look for the method or constructor
            candidates =
                candidates.filter {
                    it::class == declaration::class && it.signature == declaration.signature
                }
            if (candidates.isEmpty() && frontend.scopeManager.currentScope !is TemplateScope) {
                log.warn(
                    "Could not find declaration of method {} in record {}",
                    declaration.name,
                    holder.name
                )
            } else if (candidates.size > 1) {
                log.warn(
                    "Found more than one candidate to connect definition of method {} in record {} to its declaration. We will comply, but this is suspicious.",
                    declaration.name,
                    holder.name
                )
            }
            for (candidate in candidates) {
                candidate.definition = declaration
            }
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
                    val returnStatement = newReturnStatement("return;")
                    returnStatement.isImplicit = true
                    bodyStatement.addStatement(returnStatement)
                }
                declaration.body = bodyStatement
            }
        }

        frontend.processAttributes(declaration, ctx)

        frontend.scopeManager.leaveScope(declaration)

        if (holder != null && outsideOfScope) {
            frontend.scopeManager.leaveScope(holder)
        }

        // Check for declarations of the same function within the same translation unit to connect
        // definitions and declarations.
        // TODO: Extract this into a pass
        val declarationCandidates =
            frontend.currentTU
                ?.declarations
                ?.filterIsInstance(FunctionDeclaration::class.java)
                ?.filter { !it.isDefinition && it.hasSameSignature(declaration) }
                ?: listOf()
        for (candidate in declarationCandidates) {
            candidate.definition = declaration
            // Do some additional magic with default parameters, which I do not really understand
            for (i in declaration.parameters.indices) {
                if (candidate.parameters[i].default != null) {
                    declaration.parameters[i].default = candidate.parameters[i].default
                }
            }
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
            return if (this.rawSignature.contains("typedef")) {
                if (this.declSpecifier is CPPASTCompositeTypeSpecifier) {
                    // we need to make a difference between structs that have typedefs and structs
                    // that are typedefs themselves
                    this.declSpecifier.toString() == "struct" &&
                        this.rawSignature.trim().startsWith("typedef")
                } else {

                    true
                }
            } else {
                false
            }
        }

    private fun handleTemplateDeclaration(ctx: CPPASTTemplateDeclaration): TemplateDeclaration {
        val name = ctx.rawSignature.split("{").toTypedArray()[0].replace('\n', ' ').trim()

        val templateDeclaration: TemplateDeclaration =
            if (ctx.declaration is CPPASTFunctionDefinition) {
                newFunctionTemplateDeclaration(name, frontend.codeOf(ctx))
            } else {
                newRecordTemplateDeclaration(name, frontend.codeOf(ctx))
            }

        templateDeclaration.location = frontend.locationOf(ctx)
        frontend.scopeManager.addDeclaration(templateDeclaration)
        frontend.scopeManager.enterScope(templateDeclaration)
        addTemplateParameters(ctx, templateDeclaration)

        // Handle Template
        val innerDeclaration = frontend.declarationHandler.handle(ctx.declaration)
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
        templateDeclaration: TemplateDeclaration
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
                        language
                    )
                typeParamDecl.type = parameterizedType
                if (templateParameter.defaultType != null) {
                    val defaultType = frontend.typeOf(templateParameter.defaultType)
                    typeParamDecl.default = defaultType
                }
                templateDeclaration.addParameter(typeParamDecl)
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
                            nonTypeTemplateParamDeclaration.addPrevDFG(it)
                            it.addNextDFG(nonTypeTemplateParamDeclaration)
                        }
                    }
                    templateDeclaration.addParameter(nonTypeTemplateParamDeclaration)
                    frontend.scopeManager.addDeclaration(nonTypeTemplateParamDeclaration)
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
        innerDeclaration: RecordDeclaration
    ) {
        val parameterizedTypes = frontend.typeManager.getAllParameterizedType(templateDeclaration)

        // Loop through all the methods and adjust their receiver types
        for (method in (innerDeclaration as? RecordDeclaration)?.methods ?: listOf()) {
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
        parameterizedTypes: List<ParameterizedType>
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
        val templateParams: List<Node>? = extractTemplateParams(ctx, declSpecifier)

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

                // Handle typedefs.
                val declaration = handleTypedef(declarator, ctx, type)

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
                        (type as? FunctionType)?.returnTypes ?: listOf(IncompleteType())
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
                    declaration.templateParameters = templateParams

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
        sequence: DeclarationSequence
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
                        // This is a special case, which is a common idiom in C, to typedef a
                        // unnamed struct into a type. For example `typedef struct { int a; } S`. In
                        // this case the record declaration actually has no name and only the
                        // typedef'd name is called S. However, to make things a little bit easier
                        // we also transfer the name to the record declaration.
                        ctx.declarators.firstOrNull()?.name?.toString()?.let {
                            primaryDeclaration?.name = parseName(it)
                            // We need to inform the later steps that we want to take the name
                            // of this declaration as the basis for the result type of the typedef
                            useNameOfDeclarator = true
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
                frontend.scopeManager.addDeclaration(primaryDeclaration)

                // process attributes
                frontend.processAttributes(primaryDeclaration, ctx)
            }
        }

        return Pair(primaryDeclaration, useNameOfDeclarator)
    }

    /**
     * Extracts template parameters (used for [VariableDeclaration.templateParameters] out of the
     * declaration (if it has any), otherwise null is returned.
     */
    private fun extractTemplateParams(
        ctx: IASTSimpleDeclaration,
        declSpecifier: IASTDeclSpecifier,
    ): MutableList<Node>? {
        if (
            !ctx.isTypedef &&
                declSpecifier is CPPASTNamedTypeSpecifier &&
                declSpecifier.name is CPPASTTemplateId
        ) {
            val templateParams = mutableListOf<Node>()
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

    private fun handleTypedef(
        declarator: IASTDeclarator,
        ctx: IASTSimpleDeclaration,
        type: Type
    ): Declaration {
        val (nameDecl: IASTDeclarator, _) = declarator.realName()

        val declaration =
            frontend.typeManager.createTypeAlias(
                frontend,
                frontend.codeOf(ctx),
                type,
                frontend.typeOf(nameDecl.name)
            )

        // Add the declaration to the current scope
        frontend.scopeManager.addDeclaration(declaration)

        return declaration
    }

    private fun handleEnum(
        ctx: IASTSimpleDeclaration,
        declSpecifier: IASTEnumerationSpecifier
    ): EnumDeclaration {
        val entries = mutableListOf<EnumConstantDeclaration>()
        val enum =
            newEnumDeclaration(
                name = declSpecifier.name.toString(),
                location = frontend.locationOf(ctx),
            )

        // Loop through its members
        for (enumerator in declSpecifier.enumerators) {
            val enumConst =
                newEnumConstantDeclaration(
                    enumerator.name.toString(),
                    frontend.codeOf(enumerator),
                    frontend.locationOf(enumerator),
                )

            // In C/C++, default enums are of type int
            enumConst.type = primitiveType("int")

            // We need to make them visible to the enclosing scope. However, we do NOT
            // want to add it to the AST of the enclosing scope, but to the AST of the
            // EnumDeclaration
            frontend.scopeManager.addDeclaration(enumConst, false)

            entries += enumConst
        }

        enum.entries = entries

        return enum
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
        allIncludes: HashMap<String, HashSet<String?>>
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
        // we need to reset the scope manager scope to global, to avoid spilling scope errors into
        // other translation units
        frontend.scopeManager.resetToGlobal(node)
        frontend.currentTU = node
        val problematicIncludes = HashMap<String?, HashSet<ProblemDeclaration>>()
        for (declaration in translationUnit.declarations) {
            if (declaration is CPPASTLinkageSpecification) {
                continue // do not care about these for now
            }
            val decl = handle(declaration) ?: continue
            (decl as? ProblemDeclaration)?.location?.let {
                val problems =
                    problematicIncludes.computeIfAbsent(it.artifactLocation.toString()) {
                        HashSet()
                    }
                problems.add(decl)
            }
        }

        if (frontend.config.addIncludesToGraph) {
            addIncludes(translationUnit, problematicIncludes, node)
        }

        return node
    }

    private fun addIncludes(
        translationUnit: IASTTranslationUnit,
        problematicIncludes: Map<String?, Set<ProblemDeclaration>>,
        node: TranslationUnitDeclaration
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
                if (problems != null) {
                    includeDeclaration.addProblems(problems)
                }
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
            val includeDeclaration = includeMap[key]
            for (s in value) {
                includeDeclaration?.addInclude(includeMap[s])
            }
        }
    }
}
