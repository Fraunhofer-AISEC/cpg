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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit.IDependencyTree.IASTInclusionNode
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

class DeclarationHandler(lang: CXXLanguageFrontend) :
    Handler<Declaration?, IASTDeclaration, CXXLanguageFrontend>(Supplier { Declaration() }, lang) {

    init {
        map[CPPASTTemplateDeclaration::class.java] = HandlerInterface {
            handleTemplateDeclaration(it as CPPASTTemplateDeclaration)
        }
        map[CPPASTSimpleDeclaration::class.java] = HandlerInterface {
            handleSimpleDeclaration(it as CPPASTSimpleDeclaration)
        }
        map[CPPASTFunctionDefinition::class.java] = HandlerInterface {
            handleFunctionDefinition(it as CPPASTFunctionDefinition)
        }
        map[CPPASTProblemDeclaration::class.java] = HandlerInterface {
            handleProblem(it as CPPASTProblemDeclaration)
        }
        map[CPPASTNamespaceDefinition::class.java] = HandlerInterface {
            handleNamespace(it as CPPASTNamespaceDefinition)
        }
        map[CPPASTUsingDirective::class.java] = HandlerInterface {
            handleUsingDirective(it as CPPASTUsingDirective)
        }
    }

    private fun handleUsingDirective(using: CPPASTUsingDirective): Declaration {
        return NodeBuilder.newUsingDirective(using.rawSignature, using.qualifiedName.toString())
    }

    private fun handleNamespace(ctx: CPPASTNamespaceDefinition): Declaration {
        val fqn = lang.scopeManager.currentNamePrefixWithDelimiter + ctx.name.toString()
        val declaration = NodeBuilder.newNamespaceDeclaration(fqn, lang.getCodeFromRawNode(ctx))

        lang.scopeManager.addDeclaration(declaration)

        // enter the namespace scope
        lang.scopeManager.enterScope(declaration)

        // finally, handle all namespace declarations
        for (child in ctx.declarations) {
            handle(child)
        }

        lang.scopeManager.leaveScope(declaration)

        return declaration
    }

    private fun handleProblem(ctx: CPPASTProblemDeclaration): Declaration {
        val problem =
            NodeBuilder.newProblemDeclaration(
                ctx.containingFilename,
                ctx.problem.message,
                ctx.problem.fileLocation.toString()
            )

        lang.scopeManager.addDeclaration(problem)

        return problem
    }

    private fun handleFunctionDefinition(ctx: CPPASTFunctionDefinition): FunctionDeclaration {
        // Todo: A problem with cpp functions is that we cannot know if they may throw an exception
        // as
        // throw(...) is not compiler enforced (Problem for TryStatement)
        val functionDeclaration =
            lang.declaratorHandler.handle(ctx.declarator) as FunctionDeclaration
        val typeString = ctx.declarator.getTypeString(ctx.declSpecifier)
        functionDeclaration.setIsDefinition(true)
        functionDeclaration.type = TypeParser.createFrom(typeString, true, lang)

        // associated record declaration if this is a method or constructor
        val recordDeclaration =
            if (functionDeclaration is MethodDeclaration) functionDeclaration.recordDeclaration
            else null
        val outsideOfRecord =
            !(lang.scopeManager.currentScope is RecordScope ||
                lang.scopeManager.currentScope is TemplateScope)
        if (recordDeclaration != null) {
            if (outsideOfRecord) {
                // everything inside the method is within the scope of its record
                lang.scopeManager.enterScope(recordDeclaration)
            }

            // update the definition
            var candidates: List<MethodDeclaration> =
                if (functionDeclaration is ConstructorDeclaration) {
                    recordDeclaration.constructors
                } else {
                    recordDeclaration.methods
                }

            // look for the method or constructor
            candidates =
                candidates
                    .stream()
                    .filter { m: MethodDeclaration -> m.signature == functionDeclaration.signature }
                    .collect(Collectors.toList())
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
        lang.scopeManager.enterScope(functionDeclaration)
        functionDeclaration.type = TypeParser.createFrom(typeString, true, lang)
        if (ctx.body != null) {
            val bodyStatement = lang.statementHandler.handle(ctx.body)
            if (bodyStatement is CompoundStatement) {
                val statements = bodyStatement.statementEdges

                // get the last statement
                var lastStatement: Statement? = null
                if (statements.isNotEmpty()) {
                    lastStatement = statements[statements.size - 1].end
                }

                // add an implicit return statement, if there is none
                if (lastStatement !is ReturnStatement) {
                    val returnStatement = NodeBuilder.newReturnStatement("return;")
                    returnStatement.isImplicit = true
                    bodyStatement.addStatement(returnStatement)
                }
                functionDeclaration.body = bodyStatement
            }
        }
        lang.processAttributes(functionDeclaration, ctx)
        lang.scopeManager.leaveScope(functionDeclaration)
        if (recordDeclaration != null && outsideOfRecord) {
            lang.scopeManager.leaveScope(recordDeclaration)
        }

        // Check for declarations of the same function
        val declarationCandidates =
            lang.currentTU
                .declarations
                .stream()
                .filter { obj: Declaration? -> FunctionDeclaration::class.java.isInstance(obj) }
                .map { obj: Declaration? -> FunctionDeclaration::class.java.cast(obj) }
                .filter { f: FunctionDeclaration ->
                    !f.isDefinition && f.hasSameSignature(functionDeclaration)
                }
                .collect(Collectors.toList())
        for (declaration in declarationCandidates) {
            declaration.definition = functionDeclaration
            for (i in functionDeclaration.parameters.indices) {
                if (declaration.parameters[i].default != null) {
                    functionDeclaration.parameters[i].default = declaration.parameters[i].default
                }
            }
        }
        return functionDeclaration
    }

    private fun isTypedef(ctx: CPPASTSimpleDeclaration): Boolean {
        return if (ctx.rawSignature.contains("typedef")) {
            if (ctx.declSpecifier is CPPASTCompositeTypeSpecifier) {
                // we need to make a difference between structs that have typedefs and structs that
                // are
                // typedefs themselves
                ctx.declSpecifier.toString() == "struct" &&
                    ctx.rawSignature.trim().startsWith("typedef")
            } else {
                true
            }
        } else {
            false
        }
    }

    private fun handleTemplateDeclaration(ctx: CPPASTTemplateDeclaration): Declaration {
        val name = ctx.rawSignature.split("{").toTypedArray()[0].replace('\n', ' ').trim()

        val templateDeclaration: TemplateDeclaration =
            if (ctx.declaration is CPPASTFunctionDefinition) {
                NodeBuilder.newFunctionTemplateDeclaration(name, lang.getCodeFromRawNode(ctx))
            } else {
                NodeBuilder.newClassTemplateDeclaration(name, lang.getCodeFromRawNode(ctx))
            }

        templateDeclaration.location = lang.getLocationFromRawNode(ctx)
        lang.scopeManager.addDeclaration(templateDeclaration)
        lang.scopeManager.enterScope(templateDeclaration)
        addTemplateParameters(ctx, templateDeclaration)

        // Handle Template
        val innerDeclaration = lang.declarationHandler.handle(ctx.declaration)
        lang.scopeManager.leaveScope(templateDeclaration)
        if (templateDeclaration is FunctionTemplateDeclaration) {
            // Fix typeName
            templateDeclaration.name = templateDeclaration.getRealizationDeclarations()[0].name
        } else
            (innerDeclaration as? RecordDeclaration)?.let {
                fixTypeOfInnerDeclaration(templateDeclaration, it)
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
                    lang.declaratorHandler.handle(templateParameter) as TypeParamDeclaration
                val parameterizedType =
                    TypeManager.getInstance()
                        .createOrGetTypeParameter(
                            templateDeclaration,
                            templateParameter.name.toString()
                        )
                typeParamDeclaration.type = parameterizedType
                if (templateParameter.defaultType != null) {
                    val defaultType =
                        TypeParser.createFrom(
                            templateParameter.defaultType.declSpecifier.rawSignature,
                            false,
                            lang
                        )
                    typeParamDeclaration.default = defaultType
                }
                templateDeclaration.addParameter(typeParamDeclaration)
            } else if (templateParameter is CPPASTParameterDeclaration) {
                // Handle Value Parameters
                val nonTypeTemplateParamDeclaration =
                    lang.parameterDeclarationHandler.handle(
                        templateParameter as IASTParameterDeclaration
                    )
                if (templateParameter.declarator.initializer != null) {
                    val defaultExpression =
                        lang.initializerHandler.handle(templateParameter.declarator.initializer)
                    nonTypeTemplateParamDeclaration!!.default = defaultExpression
                    nonTypeTemplateParamDeclaration.addPrevDFG(defaultExpression!!)
                    defaultExpression.addNextDFG(nonTypeTemplateParamDeclaration)
                }
                templateDeclaration.addParameter(nonTypeTemplateParamDeclaration)
                lang.scopeManager.addDeclaration(nonTypeTemplateParamDeclaration)
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
            lang.scopeManager.addDeclaration(declaration)
        }
    }

    /**
     * Fixed the Types created by the innerDeclaration with the ParameterizedTypes of the
     * TemplateDeclaration
     *
     * @param templateDeclaration
     * @param innerDeclaration If RecordDeclaration
     */
    private fun fixTypeOfInnerDeclaration(
        templateDeclaration: TemplateDeclaration,
        innerDeclaration: Declaration
    ) {
        var type: Type
        type =
            if ((innerDeclaration as RecordDeclaration).getThis() == null) {
                TypeParser.createFrom(innerDeclaration.name, true)
            } else {
                innerDeclaration.getThis().type
            }
        val parameterizedTypes =
            TypeManager.getInstance().getAllParameterizedType(templateDeclaration)
        // Add ParameterizedTypes to type
        addParameterizedTypesToType(type, parameterizedTypes)

        // Add ParameterizedTypes to ConstructorDeclaration Type
        for (constructorDeclaration in innerDeclaration.constructors) {
            type = constructorDeclaration.type
            addParameterizedTypesToType(type, parameterizedTypes)
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
        }
    }

    private fun handleSimpleDeclaration(ctx: CPPASTSimpleDeclaration): Declaration {
        if (isTypedef(ctx)) {
            TypeManager.getInstance().handleTypedef(lang, ctx.rawSignature)
        }
        val sequence = DeclarationSequence()

        // check, whether the declaration specifier also contains declarations, i.e. class
        // definitions
        val declSpecifier = ctx.declSpecifier
        if (declSpecifier is CPPASTCompositeTypeSpecifier) {
            val declaration =
                lang.declaratorHandler.handle(ctx.declSpecifier as CPPASTCompositeTypeSpecifier)

            // handle typedef
            if (declaration!!.name.isEmpty() && ctx.rawSignature.trim().startsWith("typedef")) {
                // CDT didn't find out the name due to this thing being a typedef. We need to fix
                // this
                val endOfDeclaration = ctx.rawSignature.lastIndexOf('}')
                if (endOfDeclaration + 1 < ctx.rawSignature.length) {
                    val parts =
                        Util.splitLeavingParenthesisContents(
                            ctx.rawSignature.substring(endOfDeclaration + 1),
                            ","
                        )
                    val name =
                        parts
                            .stream()
                            .filter { p: String -> !p.contains("*") && !p.contains("[") }
                            .findFirst()
                    name.ifPresent { s: String -> declaration.name = s.replace(";", "") }
                }
            }
            lang.processAttributes(declaration, ctx)
            sequence.addDeclaration(declaration)
        } else if (declSpecifier is CPPASTElaboratedTypeSpecifier) {
            Util.warnWithFileLocation(
                lang,
                ctx,
                log,
                "Parsing elaborated type specifiers is not supported (yet)",
                declSpecifier.javaClass
            )
        }
        if (declSpecifier is CPPASTNamedTypeSpecifier && declSpecifier.name is CPPASTTemplateId) {
            handleTemplateUsage(declSpecifier, ctx, sequence)
        } else {
            for (declarator in ctx.declarators) {
                val typeString = declarator.getTypeString(ctx.declSpecifier)

                // make sure, the type manager knows about this type before parsing the declarator
                val result = TypeParser.createFrom(typeString, true, lang)

                val declaration = lang.declaratorHandler.handle(declarator) as ValueDeclaration

                declaration.type = result

                // process attributes
                lang.processAttributes(declaration, ctx)
                sequence.addDeclaration(declaration)
            }
        }

        return simplifySequence(sequence)
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
        ctx: CPPASTSimpleDeclaration,
        sequence: DeclarationSequence
    ) {
        val templateId = typeSpecifier.name as CPPASTTemplateId
        val type = TypeParser.createFrom(ctx.rawSignature, true)
        val templateParams: MutableList<Node?> = ArrayList()

        if (type.root !is ObjectType) {
            // we cannot continue in this case
            return
        }

        val objectType = type.root as ObjectType
        objectType.generics = emptyList()

        for (templateArgument in templateId.templateArguments) {
            if (templateArgument is CPPASTTypeId) {
                val genericInstantiation =
                    TypeParser.createFrom(templateArgument.getRawSignature(), true)
                objectType.addGeneric(genericInstantiation)
                templateParams.add(
                    NodeBuilder.newTypeExpression(genericInstantiation.name, genericInstantiation)
                )
            } else if (templateArgument is IASTExpression) {
                val expression = lang.expressionHandler.handle(templateArgument)
                templateParams.add(expression)
            }
        }
        for (declarator in ctx.declarators) {
            val declaration = lang.declaratorHandler.handle(declarator) as ValueDeclaration

            // Update Type
            declaration.type = type

            // Set TemplateParameters into VariableDeclaration
            if (declaration is VariableDeclaration) {
                declaration.templateParameters = templateParams
            }

            // process attributes
            lang.processAttributes(declaration, ctx)
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

    fun handleTranslationUnit(translationUnit: CPPASTTranslationUnit): TranslationUnitDeclaration {
        val node =
            NodeBuilder.newTranslationUnitDeclaration(
                translationUnit.filePath,
                translationUnit.rawSignature
            )

        // There might have been errors in the previous translation unit and in any case
        // we need to reset the scope manager scope to global, to avoid spilling scope errors into
        // other
        // translation units
        lang.scopeManager.resetToGlobal(node)
        lang.currentTU = node
        val problematicIncludes = HashMap<String?, HashSet<ProblemDeclaration>>()
        for (declaration in translationUnit.declarations) {
            if (declaration is CPPASTLinkageSpecification) {
                continue // do not care about these for now
            }
            val decl = handle(declaration) ?: continue
            if (decl is ProblemDeclaration) {
                val problems = problematicIncludes.computeIfAbsent(decl.filename) { HashSet() }
                problems.add(decl)
            }
        }

        // TODO: Remark CB: I am not quite sure, what the point of the code beyord this line is.
        // Probably needs to be refactored
        val addIncludesToGraph = true // todo move to config
        if (addIncludesToGraph) {

            // this tree is a bit problematic: If a file was already included before, it will not be
            // shown
            // connecting to other leaves.
            // I.e. if FileA includes FileB and FileC, and FileC also includes FileB, _no_
            // connection
            // between FileC and FileB will be shown.
            val dependencyTree = translationUnit.dependencyTree
            val allIncludes = HashMap<String, HashSet<String?>>()
            parseInclusions(dependencyTree.inclusions, allIncludes)

            //      for (Map.Entry<String, HashSet<String>> entry : allIncludes.entrySet()) {
            //        System.out.println(entry.getKey() + ":");
            //        for (String s : entry.getValue()) {
            //          System.out.println("\t" + s);
            //        }
            //      }
            if (allIncludes.size > 0) {
                // create all include nodes, potentially attach problemdecl
                val includesStrings = HashSet<String?>()
                val includeMap = HashMap<String?, IncludeDeclaration>()
                allIncludes.values.forEach(
                    Consumer { c: HashSet<String?>? -> includesStrings.addAll(c!!) }
                )
                for (includeString in includesStrings) {
                    val problems = problematicIncludes[includeString]
                    val includeDeclaration = NodeBuilder.newIncludeDeclaration(includeString ?: "")
                    if (problems != null) {
                        includeDeclaration.addProblems(problems)
                    }
                    includeMap[includeString] = includeDeclaration
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
        return node
    }

    companion object {
        /**
         * Returns a raw type string (that can be parsed by the [TypeParser] out of a cpp declarator
         * and associated declaration specifiers.
         *
         * @param declSpecifier the declaration specifier
         * @return the type string
         */
        fun IASTDeclarator.getTypeString(declSpecifier: IASTDeclSpecifier): String {
            // use the declaration specifier as basis
            val typeString = StringBuilder(declSpecifier.toString())

            // append names, pointer operators and array modifiers and such
            for (node in this.children) {
                if (node is IASTPointerOperator || node is IASTArrayModifier) {
                    typeString.append(node.rawSignature)
                }
            }
            return typeString.toString()
        }
    }
}
