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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope
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
     * [UsingDirective]. However, currently, no actual adjustment of available names / scopes is
     * done yet.
     */
    private fun handleUsingDirective(using: CPPASTUsingDirective): Declaration {
        return newUsingDirective(using.rawSignature, using.qualifiedName.toString())
    }

    /**
     * Translates a C++ [namespace](https://en.cppreference.com/w/cpp/language/namespace#Namespaces)
     * into a [NamespaceDeclaration].
     */
    private fun handleNamespace(ctx: CPPASTNamespaceDefinition): NamespaceDeclaration {
        // Build a FQN out of the current scope prefix
        val fqn = frontend.currentNamePrefixWithDelimiter + ctx.name.toString()
        val declaration = newNamespaceDeclaration(fqn, frontend.getCodeFromRawNode(ctx))

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
        declaration.type = type ?: UnknownType.getUnknownType(language)
        declaration.isDefinition = true

        // We also need to set the return type, based on the function type.
        declaration.returnTypes = type?.returnTypes ?: listOf(IncompleteType())

        // Store the reference to a record declaration, if we managed to find one. This is most
        // likely the case, if the function is defined within the class itself.
        val recordDeclaration = (declaration as? MethodDeclaration)?.recordDeclaration

        // We want to determine, whether we are currently outside a record. In this case, our
        // function
        // is either really a function or a method definition external to a class.
        val outsideOfRecord =
            !(frontend.scopeManager.currentScope is RecordScope ||
                frontend.scopeManager.currentScope is TemplateScope)

        if (recordDeclaration != null) {
            if (outsideOfRecord) {
                // everything inside the method is within the scope of its record
                frontend.scopeManager.enterScope(recordDeclaration)
            }

            // update the definition
            var candidates: List<MethodDeclaration> =
                if (declaration is ConstructorDeclaration) {
                    recordDeclaration.constructors
                } else {
                    recordDeclaration.methods
                }

            // look for the method or constructor
            candidates = candidates.filter { it.signature == declaration.signature }
            if (candidates.isEmpty() && frontend.scopeManager.currentScope !is TemplateScope) {
                log.warn(
                    "Could not find declaration of method {} in record {}",
                    declaration.name,
                    recordDeclaration.name
                )
            } else if (candidates.size > 1) {
                log.warn(
                    "Found more than one candidate to connect definition of method {} in record {} to its declaration. We will comply, but this is suspicious.",
                    declaration.name,
                    recordDeclaration.name
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
            if (bodyStatement is CompoundStatement) {
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
        if (recordDeclaration != null && outsideOfRecord) {
            frontend.scopeManager.leaveScope(recordDeclaration)
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
    val IASTSimpleDeclaration.isTypedef: Boolean
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
                newFunctionTemplateDeclaration(name, frontend.getCodeFromRawNode(ctx))
            } else {
                newClassTemplateDeclaration(name, frontend.getCodeFromRawNode(ctx))
            }

        templateDeclaration.location = frontend.getLocationFromRawNode(ctx)
        frontend.scopeManager.addDeclaration(templateDeclaration)
        frontend.scopeManager.enterScope(templateDeclaration)
        addTemplateParameters(ctx, templateDeclaration)

        // Handle Template
        val innerDeclaration = frontend.declarationHandler.handle(ctx.declaration)
        frontend.scopeManager.leaveScope(templateDeclaration)
        if (templateDeclaration is FunctionTemplateDeclaration) {
            // Fix typeName
            templateDeclaration.fullName =
                Name.parse(templateDeclaration.getRealizationDeclarations()[0].name, language)
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
                val typeParamDeclaration =
                    frontend.declaratorHandler.handle(templateParameter) as TypeParamDeclaration
                val parameterizedType =
                    TypeManager.getInstance()
                        .createOrGetTypeParameter(
                            templateDeclaration,
                            templateParameter.name.toString(),
                            language
                        )
                typeParamDeclaration.type = parameterizedType
                if (templateParameter.defaultType != null) {
                    val defaultType =
                        TypeParser.createFrom(
                            templateParameter.defaultType.declSpecifier.rawSignature,
                            false,
                            frontend
                        )
                    typeParamDeclaration.default = defaultType
                }
                templateDeclaration.addParameter(typeParamDeclaration)
            } else if (templateParameter is CPPASTParameterDeclaration) {
                // Handle Value Parameters
                val nonTypeTemplateParamDeclaration =
                    frontend.parameterDeclarationHandler.handle(
                        templateParameter as IASTParameterDeclaration
                    )
                if (nonTypeTemplateParamDeclaration is ParamVariableDeclaration) {
                    if (templateParameter.declarator.initializer != null) {
                        val defaultExpression =
                            frontend.initializerHandler.handle(
                                templateParameter.declarator.initializer
                            )
                        nonTypeTemplateParamDeclaration.default = defaultExpression
                        nonTypeTemplateParamDeclaration.addPrevDFG(defaultExpression!!)
                        defaultExpression.addNextDFG(nonTypeTemplateParamDeclaration)
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
        for (declaration in templateDeclaration.realizationDeclarations) {
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
        val parameterizedTypes =
            TypeManager.getInstance().getAllParameterizedType(templateDeclaration)

        // Loop through all the methods and adjust their receiver types
        for (method in (innerDeclaration as? RecordDeclaration)?.methods ?: listOf()) {
            // Add ParameterizedTypes to type
            method.receiver?.let { addParameterizedTypesToType(it.type, parameterizedTypes) }
        }

        // Add parameterizedTypes to ConstructorDeclaration type and adjust their receiver types
        for (constructor in innerDeclaration.constructors) {
            constructor.receiver?.let { addParameterizedTypesToType(it.type, parameterizedTypes) }

            // We need to add the type to (first) return type as well. The function type is somehow
            // magically updated then as well.
            constructor.returnTypes.firstOrNull()?.let {
                addParameterizedTypesToType(it, parameterizedTypes)
            }
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
    ) {
        if (type is ObjectType) {
            for (parameterizedType in parameterizedTypes) {
                type.addGeneric(parameterizedType)
            }
        } else if (type is PointerType) {
            addParameterizedTypesToType(type.elementType, parameterizedTypes)
        }
    }

    private fun handleSimpleDeclaration(ctx: IASTSimpleDeclaration): Declaration {
        var primaryDeclaration: Declaration? = null
        val sequence = DeclarationSequence()

        var useNameOfDeclarator = false

        // check, whether the declaration specifier also contains declarations, i.e. class
        // definitions
        val declSpecifier = ctx.declSpecifier
        when (declSpecifier) {
            is IASTCompositeTypeSpecifier -> {
                primaryDeclaration =
                    frontend.declaratorHandler.handle(
                        ctx.declSpecifier as IASTCompositeTypeSpecifier
                    )

                if (primaryDeclaration != null) {
                    // handle typedef
                    if (
                        primaryDeclaration.fullName.toString().isEmpty() &&
                            ctx.rawSignature.trim().startsWith("typedef")
                    ) {
                        // This is a special case, which is a common idiom in C, to typedef a
                        // unnamed struct into a type. For example `typedef struct { int a; } S`. In
                        // this case the record declaration actually has no name and only the
                        // typedef'd name is called S. However, to make things a little bit easier
                        // we also transfer the name to the record declaration.
                        ctx.declarators.firstOrNull()?.name?.toString()?.let {
                            primaryDeclaration?.fullName = Name.parse(it, language)
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

        if (
            !ctx.isTypedef &&
                declSpecifier is CPPASTNamedTypeSpecifier &&
                declSpecifier.name is CPPASTTemplateId
        ) {
            handleTemplateUsage(declSpecifier, ctx, sequence)
        } else {
            for (declarator in ctx.declarators) {
                // If a previous step informed us that we should take the name of the primary
                // declaration, we do so here. This most likely is the case of a typedef struct.
                val declSpecifierToUse =
                    if (useNameOfDeclarator && declSpecifier is IASTCompositeTypeSpecifier) {
                        val copy = declSpecifier.copy()
                        copy.name = CPPASTName(primaryDeclaration?.name?.toCharArray())
                        copy
                    } else {
                        declSpecifier
                    }

                // It is important, that we parse the type first, so that the type is known before
                // parsing the declaration.
                // This allows us to guess cast vs. call expression in
                // ExpressionHandler.handleUnaryExpression.
                var type = frontend.typeOf(declarator, declSpecifierToUse)

                if (ctx.isTypedef) {
                    // Handle typedefs.
                    val declaration = handleTypedef(declarator, ctx, type)

                    sequence.addDeclaration(declaration)
                } else {
                    val declaration =
                        frontend.declaratorHandler.handle(declarator) as? ValueDeclaration

                    // We need to reparse the type, if this is a constructor declaration, so that we
                    // can supply this as a hint to
                    // the typeOf
                    if (declaration is ConstructorDeclaration) {
                        type = frontend.typeOf(declarator, declSpecifierToUse, declaration)
                    }

                    // For function *declarations*, we need to update the return types based on the
                    // function type. For function *definitions*, this is done in
                    // [handleFunctionDefinition].
                    if (declaration is FunctionDeclaration) {
                        declaration.returnTypes =
                            (type as? FunctionType)?.returnTypes ?: listOf(IncompleteType())
                    }

                    if (declaration != null) {
                        // We also need to set the return type, based on the function type.
                        declaration.type = type

                        // process attributes
                        frontend.processAttributes(declaration, ctx)
                        sequence.addDeclaration(declaration)
                    }
                }
            }
        }

        return simplifySequence(sequence)
    }

    private fun handleTypedef(
        declarator: IASTDeclarator,
        ctx: IASTSimpleDeclaration,
        type: Type
    ): Declaration {
        val (nameDecl: IASTDeclarator, _) = declarator.realName()

        val declaration =
            TypeManager.getInstance()
                .createTypeAlias(
                    frontend,
                    frontend.getCodeFromRawNode(ctx),
                    type,
                    nameDecl.name.toString()
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
                location = frontend.getLocationFromRawNode(ctx),
            )

        // Loop through its members
        for (enumerator in declSpecifier.enumerators) {
            val enumConst =
                newEnumConstantDeclaration(
                    enumerator.name.toString(),
                    frontend.getCodeFromRawNode(enumerator),
                    frontend.getLocationFromRawNode(enumerator),
                )

            // In C/C++, default enums are of type int
            enumConst.type = parseType("int")

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
     * full sequence if it contains more than one element
     */
    private fun simplifySequence(sequence: DeclarationSequence): Declaration {
        return if (sequence.isSingle) {
            sequence.first()
        } else {
            sequence
        }
    }

    /**
     * Handles usage of Templates in SimpleDeclarations
     *
     * @param typeSpecifier
     * @param ctx
     * @param sequence
     */
    private fun handleTemplateUsage(
        typeSpecifier: CPPASTNamedTypeSpecifier,
        ctx: IASTSimpleDeclaration,
        sequence: DeclarationSequence
    ) {
        val templateId = typeSpecifier.name as CPPASTTemplateId
        val type = parseType(ctx.rawSignature)
        val templateParams: MutableList<Node?> = ArrayList()

        if (type.root !is ObjectType) {
            // we cannot continue in this case
            return
        }

        val objectType = type.root as ObjectType
        objectType.generics = emptyList()

        for (templateArgument in templateId.templateArguments) {
            if (templateArgument is CPPASTTypeId) {
                val genericInstantiation = parseType(templateArgument.getRawSignature())
                objectType.addGeneric(genericInstantiation)
                templateParams.add(
                    newTypeExpression(
                        genericInstantiation.name,
                        genericInstantiation,
                    )
                )
            } else if (templateArgument is IASTExpression) {
                val expression = frontend.expressionHandler.handle(templateArgument)
                templateParams.add(expression)
            }
        }
        for (declarator in ctx.declarators) {
            val declaration = frontend.declaratorHandler.handle(declarator) as ValueDeclaration

            // Update Type
            declaration.type = type

            // Set TemplateParameters into VariableDeclaration
            if (declaration is VariableDeclaration) {
                declaration.templateParameters = templateParams
            }

            // process attributes
            frontend.processAttributes(declaration, ctx)
            sequence.addDeclaration(declaration)
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
            newTranslationUnitDeclaration(
                translationUnit.filePath,
                translationUnit.rawSignature,
                translationUnit
            )

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

        if (allIncludes.size == 0) {
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
        for (incl in allIncludes[translationUnit.filePath]!!) {
            node.addDeclaration(includeMap[incl]!!)
        }
        allIncludes.remove(translationUnit.filePath)
        // attach to remaining nodes
        for ((key, value) in allIncludes) {
            val includeDeclaration = includeMap[key]
            for (s in value) {
                includeDeclaration!!.addInclude(includeMap[s])
            }
        }
    }
}
