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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.HasTemplates
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.HasDefault
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.duplicateLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.duplicateTypeExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionTemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodParameterIn
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newTypeExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newTypeParamDeclaration
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration.TemplateInitialization
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.*
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

/**
 * Resolves [CallExpression] and [NewExpression] targets.
 *
 * A [CallExpression] specifies the method that wants to be called via [CallExpression.name]. The
 * call target is a method of the same class the caller belongs to, so the name is resolved to the
 * appropriate [MethodDeclaration]. This pass also takes into consideration that a method might not
 * be present in the current class, but rather has its implementation in a superclass, and sets the
 * pointer accordingly.
 *
 * Constructor calls with [ConstructExpression] are resolved in such a way that their
 * [ConstructExpression.instantiates] points to the correct [RecordDeclaration]. Additionally, the
 * [ConstructExpression.constructor] is set to the according [ConstructorDeclaration].
 *
 * This pass should NOT use any DFG edges because they are computed / adjusted in a later stage.
 */
@DependsOn(VariableUsageResolver::class)
open class CallResolver : SymbolResolverPass() {
    protected val containingType = mutableMapOf<FunctionDeclaration, Type>()

    override fun cleanup() {
        containingType.clear()
    }

    override fun accept(translationResult: TranslationResult) {
        if (lang == null) {
            log.error("No language frontend specified. Can't resolve anything.")
            return
        }

        scopeManager = lang!!.scopeManager
        config = lang!!.config

        walker = ScopedWalker(scopeManager)
        walker.registerHandler { _, _, currNode -> walker.collectDeclarations(currNode) }
        walker.registerHandler { node, _ -> findRecords(node) }
        walker.registerHandler { node, _ -> findTemplates(node) }
        walker.registerHandler { currentClass, _, currentNode ->
            registerMethods(currentClass, currentNode)
        }

        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
        walker.clearCallbacks()
        walker.registerHandler { node, _ -> fixInitializers(node) }
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
        walker.clearCallbacks()
        walker.registerHandler { node, _ -> resolve(node) }
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    protected fun registerMethods(currentClass: RecordDeclaration?, currentNode: Node) {
        if (currentNode is MethodDeclaration && currentClass != null) {
            containingType[currentNode] = TypeParser.createFrom(currentClass.name, true)
        }
    }

    protected fun fixInitializers(node: Node) {
        if (node is VariableDeclaration) {
            // check if we have the corresponding class for this type
            val typeString = node.type.root.name
            if (typeString in recordMap) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer = newConstructExpression("()")
                    initializer.isImplicit = true
                    node.initializer = initializer
                    node.templateParameters?.let {
                        addImplicitTemplateParametersToCall(it, initializer)
                    }
                } else if (
                    currInitializer is CallExpression && currInitializer.name == typeString
                ) {
                    // This should actually be a construct expression, not a call!
                    val arguments = currInitializer.arguments
                    val signature = arguments.map(Node::code).joinToString(", ")
                    val initializer = newConstructExpression("($signature)")
                    initializer.arguments = mutableListOf(*arguments.toTypedArray())
                    initializer.isImplicit = true
                    node.initializer = initializer
                    currInitializer.disconnectFromGraph()
                }
            }
        }
    }

    /**
     * Handle calls in the form of `super.call()` or `ClassName.super.call() ` * , conforming to
     * JLS13 §15.12.1
     *
     * @param curClass The class containing the call
     * @param call The call to be resolved
     */
    protected fun handleSuperCall(curClass: RecordDeclaration, call: CallExpression) {
        // We need to connect this super reference to the receiver of this method
        val func = scopeManager!!.currentFunction
        if (func is MethodDeclaration) {
            (call.base as DeclaredReferenceExpression?)?.refersTo = func.receiver
        }
        var target: RecordDeclaration? = null
        if (call.base!!.name == "super") {

            // Direct superclass, either defined explicitly or java.lang.Object by default
            if (curClass.superClasses.isNotEmpty()) {
                target = recordMap[curClass.superClasses[0].root.typeName]
            } else {
                Util.warnWithFileLocation(
                    call,
                    LOGGER,
                    "super call without direct superclass! Expected java.lang.Object to be present at least!"
                )
            }
        } else {
            // BaseName.super.call(), might either be in order to specify an enclosing class or an
            // interface that is implemented
            target = handleSpecificSupertype(curClass, call)
        }
        if (target != null) {
            val superType = target.toType()
            // Explicitly set the type of the call's base to the super type
            call.base!!.type = superType
            // And set the possible subtypes, to ensure, that really only our super type is in there
            call.base!!.updatePossibleSubtypes(listOf(superType))
            handleMethodCall(target, call)
        }
    }

    protected fun handleSpecificSupertype(
        curClass: RecordDeclaration,
        call: CallExpression
    ): RecordDeclaration? {
        val baseName = call.base!!.name.substring(0, call.base!!.name.lastIndexOf(".super"))
        if (TypeParser.createFrom(baseName, true) in curClass.implementedInterfaces) {
            // Basename is an interface -> BaseName.super refers to BaseName itself
            return recordMap[baseName]
        } else {
            // BaseName refers to an enclosing class -> BaseName.super is BaseName's superclass
            val base = recordMap[baseName]
            if (base != null) {
                if (base.superClasses.isNotEmpty()) {
                    return recordMap[base.superClasses[0].root.typeName]
                } else {
                    Util.warnWithFileLocation(
                        call,
                        LOGGER,
                        "super call without direct superclass! Expected java.lang.Object to be present at least!"
                    )
                }
            }
        }
        return null
    }

    protected fun resolve(node: Node) {
        when (node) {
            is TranslationUnitDeclaration -> {
                currentTU = node
            }
            is ExplicitConstructorInvocation -> {
                resolveExplicitConstructorInvocation(node)
            }
            is ConstructExpression -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve
                // this call's signature, we need to make sure any call expression arguments are
                // fully
                // resolved
                resolveArguments(node)
                resolveConstructExpression(node)
            }
            is CallExpression -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve
                // this call's signature, we need to make sure any call expression arguments are
                // fully
                // resolved
                resolveArguments(node)
                handleCallExpression(scopeManager!!.currentRecord, node)
            }
        }
    }

    protected fun handleCallExpression(curClass: RecordDeclaration?, call: CallExpression) {
        if (
            call.language is JavaLanguageFrontend &&
                (call.base as? DeclaredReferenceExpression)
                    ?.name
                    ?.matches(Regex("(?<class>.+\\.)?super")) == true
        ) {
            handleSuperCall(curClass!!, call)
            return
        }
        if (call is MemberCallExpression) {
            val member = call.member
            if (!(member is HasType && (member as HasType).type is FunctionPointerType)) {
                // function pointers are handled by extra pass
                handleMethodCall(curClass, call)
            }
            return
        }
        if (call.instantiatesTemplate() && call.language is HasTemplates) {
            handleTemplateFunctionCalls(curClass, call, true)
            return
        }

        // we could be referring to a function pointer even though it is not a member call if the
        // usual function pointer syntax (*fp)() has been omitted: fp(). Looks like a normal call,
        // but it isn't
        val funcPointer =
            walker.getDeclarationForScope(call) { v ->
                v.type is FunctionPointerType && v.name == call.name
            }
        if (!funcPointer.isPresent) {
            // function pointers are handled by extra pass
            handleNormalCalls(curClass, call)
        }
    }

    /**
     * Checks if the provided call parameter can instantiate the required template parameter
     *
     * @param callParameterArg
     * @param templateParameter
     * @return If the TemplateParameter is an TypeParamDeclaration, the callParameter must be an
     * ObjectType => returns true If the TemplateParameter is a ParamVariableDeclaration, the
     * callParameterArg must be an Expression and its type must match the type of the
     * ParamVariableDeclaration (same type or subtype) => returns true Otherwise return false
     */
    protected fun isInstantiated(callParameterArg: Node, templateParameter: Declaration?): Boolean {
        var callParameter = callParameterArg
        if (callParameter is TypeExpression) {
            callParameter = callParameter.type
        }
        return if (callParameter is Type && templateParameter is TypeParamDeclaration) {
            callParameter is ObjectType
        } else if (callParameter is Expression && templateParameter is ParamVariableDeclaration) {
            callParameter.type == templateParameter.type ||
                TypeManager.getInstance().isSupertypeOf(templateParameter.type, callParameter.type)
        } else {
            false
        }
    }

    /**
     * Gets all ParameterizedTypes from the initialization signature
     *
     * @param initialization mapping of the declaration of the template parameters to the explicit
     * values the template is instantiated with
     * @return mapping of the parameterized types to the corresponding TypeParamDeclaration in the
     * template
     */
    protected fun getParameterizedSignaturesFromInitialization(
        initialization: Map<Declaration?, Node?>
    ): Map<ParameterizedType, TypeParamDeclaration> {
        val parameterizedSignature: MutableMap<ParameterizedType, TypeParamDeclaration> = HashMap()
        for (templateParam in initialization.keys) {
            if (templateParam is TypeParamDeclaration) {
                parameterizedSignature[templateParam.type as ParameterizedType] = templateParam
            }
        }
        return parameterizedSignature
    }

    /**
     * Check if we are handling an implicit template parameter, if so set instantiationSignature,
     * instantiationType and orderedInitializationSignature maps accordingly
     *
     * @param functionTemplateDeclaration functionTemplate we have identified
     * @param index position of the templateParameter we are currently handling
     * @param instantiationSignature mapping of the Declaration representing a template parameter to
     * the value that initializes that template parameter
     * @param instantiationType mapping of the instantiation value to the instantiation type
     * (depends on resolution [TemplateDeclaration.TemplateInitialization]
     * @param orderedInitializationSignature mapping of the ordering of the template parameters
     */
    protected fun handleImplicitTemplateParameter(
        functionTemplateDeclaration: FunctionTemplateDeclaration,
        index: Int,
        instantiationSignature: MutableMap<Declaration?, Node?>,
        instantiationType: MutableMap<Node?, TemplateInitialization?>,
        orderedInitializationSignature: MutableMap<Declaration, Int>
    ) {
        if ((functionTemplateDeclaration.parameters[index] as HasDefault<*>).default != null) {
            // If we have a default we fill it in
            var defaultNode =
                (functionTemplateDeclaration.parameters[index] as HasDefault<*>).default
            if (defaultNode is Type) {
                defaultNode = newTypeExpression(defaultNode.name, defaultNode)
                defaultNode.isImplicit = true
            }
            instantiationSignature[functionTemplateDeclaration.parameters[index]] = defaultNode
            instantiationType[defaultNode] = TemplateInitialization.DEFAULT
            orderedInitializationSignature[functionTemplateDeclaration.parameters[index]] = index
        } else {
            // If there is no default, we don't have information on the parameter -> check
            // auto-deduction
            instantiationSignature[functionTemplateDeclaration.parameters[index]] = null
            instantiationType[null] = TemplateInitialization.UNKNOWN
            orderedInitializationSignature[functionTemplateDeclaration.parameters[index]] = index
        }
    }

    /**
     * Creates a Mapping between the Parameters of the TemplateDeclaration and the Values provided
     * for the instantiation of the template (Only the ones that are in defined in the instantiation
     * -&gt; no defaults or implicit). Additionally, it fills the maps and lists mentioned below:
     *
     * @param functionTemplateDeclaration functionTemplate we have identified that should be
     * instantiated
     * @param templateCall callExpression that instantiates the template
     * @param instantiationType mapping of the instantiation value to the instantiation type
     * (depends
     * * on resolution [TemplateDeclaration.TemplateInitialization]
     * @param orderedInitializationSignature mapping of the ordering of the template parameters
     * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
     * @return mapping containing the all elements of the signature of the TemplateDeclaration as
     * key and the Type/Expression the Parameter is initialized with. This function returns null if
     * the {ParamVariableDeclaration, TypeParamDeclaration} do not match the provided value for
     * initialization -&gt; initialization not possible
     */
    protected fun constructTemplateInitializationSignatureFromTemplateParameters(
        functionTemplateDeclaration: FunctionTemplateDeclaration,
        templateCall: CallExpression,
        instantiationType: MutableMap<Node?, TemplateInitialization?>,
        orderedInitializationSignature: MutableMap<Declaration, Int>,
        explicitInstantiated: MutableList<ParameterizedType?>
    ): MutableMap<Declaration?, Node?>? {
        val instantiationSignature: MutableMap<Declaration?, Node?> = HashMap()
        for (i in functionTemplateDeclaration.parameters.indices) {
            if (i < templateCall.templateParameters.size) {
                val callParameter = templateCall.templateParameters[i]
                val templateParameter = functionTemplateDeclaration.parameters[i]
                if (isInstantiated(callParameter, templateParameter)) {
                    instantiationSignature[templateParameter] = callParameter
                    instantiationType[callParameter] = TemplateInitialization.EXPLICIT
                    if (templateParameter is TypeParamDeclaration) {
                        explicitInstantiated.add(templateParameter.type as ParameterizedType)
                    }
                    orderedInitializationSignature[templateParameter] = i
                } else {
                    // If both parameters do not match, we cannot instantiate the template
                    return null
                }
            } else {
                handleImplicitTemplateParameter(
                    functionTemplateDeclaration,
                    i,
                    instantiationSignature,
                    instantiationType,
                    orderedInitializationSignature
                )
            }
        }
        return instantiationSignature
    }

    /**
     * Creates a Mapping between the Parameters of the TemplateDeclaration and the Values provided *
     * for the instantiation of the template.
     *
     * The difference to
     * [CallResolver.constructTemplateInitializationSignatureFromTemplateParameters] is that this
     * one also takes into account defaults and auto deductions
     *
     * Additionally, it fills the maps and lists mentioned below:
     *
     * @param functionTemplateDeclaration functionTemplate we have identified that should be
     * instantiated
     * @param templateCall callExpression that instantiates the template
     * @param instantiationType mapping of the instantiation value to the instantiation type
     * (depends on resolution [TemplateDeclaration.TemplateInitialization]
     * @param orderedInitializationSignature mapping of the ordering of the template parameters
     * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
     * @return mapping containing the all elements of the signature of the TemplateDeclaration as
     * key and the Type/Expression the Parameter is initialized with. This function returns null if
     * the {ParamVariableDeclaration, TypeParamDeclaration} do not match the provided value for
     * initialization -&gt; initialization not possible
     */
    protected fun getTemplateInitializationSignature(
        functionTemplateDeclaration: FunctionTemplateDeclaration,
        templateCall: CallExpression,
        instantiationType: MutableMap<Node?, TemplateInitialization?>,
        orderedInitializationSignature: MutableMap<Declaration, Int>,
        explicitInstantiated: MutableList<ParameterizedType?>
    ): Map<Declaration?, Node?>? {
        // Construct Signature
        val signature =
            constructTemplateInitializationSignatureFromTemplateParameters(
                functionTemplateDeclaration,
                templateCall,
                instantiationType,
                orderedInitializationSignature,
                explicitInstantiated
            )
                ?: return null
        val parameterizedTypeResolution = getParameterizedSignaturesFromInitialization(signature)

        // Check for unresolved Parameters and try to deduce Type by looking at call arguments
        for (i in templateCall.arguments.indices) {
            val functionDeclaration = functionTemplateDeclaration.realization[0]
            val currentArgumentType = functionDeclaration.parameters[i].type
            val deducedType = templateCall.arguments[i].type
            val typeExpression = newTypeExpression(deducedType.name, deducedType)
            typeExpression.isImplicit = true
            if (
                currentArgumentType is ParameterizedType &&
                    (signature[parameterizedTypeResolution[currentArgumentType]] == null ||
                        (instantiationType[
                            signature[parameterizedTypeResolution[currentArgumentType]]] ==
                            TemplateInitialization.DEFAULT))
            ) {
                signature[parameterizedTypeResolution[currentArgumentType]] = typeExpression
                instantiationType[typeExpression] = TemplateInitialization.AUTO_DEDUCTION
            }
        }
        return signature
    }

    /**
     * @param curClass class the invoked method must be part of.
     * @param templateCall call to instantiate and invoke a function template
     * @param applyInference if the resolution was unsuccessful and applyInference is true the call
     * will resolve to an instantiation/invocation of an inferred template
     * @return true if resolution was successful, false if not
     */
    protected fun handleTemplateFunctionCalls(
        curClass: RecordDeclaration?,
        templateCall: CallExpression,
        applyInference: Boolean
    ): Boolean {
        if (scopeManager == null) {
            Util.errorWithFileLocation(
                templateCall,
                log,
                "Could not handle template function call: scopeManager is null"
            )
            return false
        }
        val instantiationCandidates =
            scopeManager!!.resolveFunctionTemplateDeclaration(templateCall)
        for (functionTemplateDeclaration in instantiationCandidates) {
            val initializationType = mutableMapOf<Node?, TemplateInitialization?>()
            val orderedInitializationSignature = mutableMapOf<Declaration, Int>()
            val explicitInstantiation = mutableListOf<ParameterizedType?>()
            if (
                (templateCall.templateParameters.size <=
                    functionTemplateDeclaration.parameters.size) &&
                    (templateCall.arguments.size <=
                        functionTemplateDeclaration.realization[0].parameters.size)
            ) {
                val initializationSignature =
                    getTemplateInitializationSignature(
                        functionTemplateDeclaration,
                        templateCall,
                        initializationType,
                        orderedInitializationSignature,
                        explicitInstantiation
                    )
                val function = functionTemplateDeclaration.realization[0]
                if (
                    initializationSignature != null &&
                        checkArgumentValidity(
                            function,
                            getCallSignature(
                                function,
                                getParameterizedSignaturesFromInitialization(
                                    initializationSignature
                                ),
                                initializationSignature
                            ),
                            templateCall,
                            explicitInstantiation
                        )
                ) {
                    // Valid Target -> Apply invocation
                    applyTemplateInstantiation(
                        templateCall,
                        functionTemplateDeclaration,
                        function,
                        initializationSignature,
                        initializationType,
                        orderedInitializationSignature
                    )
                    return true
                }
            }
        }
        if (applyInference) {
            // If we want to use an inferred functionTemplateDeclaration, this needs to be provided.
            // Otherwise, we could not resolve to a template and no modifications are made
            val functionTemplateDeclaration = createInferredFunctionTemplate(curClass, templateCall)
            templateCall.templateInstantiation = functionTemplateDeclaration
            templateCall.invokes = functionTemplateDeclaration.realization
            val edges = templateCall.templateParametersEdges ?: return false
            // Set instantiation propertyEdges
            for (instantiationParameter in edges) {
                instantiationParameter.addProperty(
                    Properties.INSTANTIATION,
                    TemplateInitialization.EXPLICIT
                )
            }
            return true
        }
        return false
    }

    /**
     * Performs all necessary steps to make a CallExpression instantiate a template: 1. Set
     * TemplateInstantiation Edge from CallExpression to Template 2. Set Invokes Edge to all
     * realizations of the Template 3. Set return type of the CallExpression and checks if it uses a
     * ParameterizedType and therefore has to be instantiated 4. Set Template Parameters Edge from
     * the CallExpression to all Instantiation Values 5. Set DFG Edges from instantiation to
     * ParamVariableDeclaration in TemplateDeclaration
     *
     * @param templateCall call to instantiate and invoke a function template
     * @param functionTemplateDeclaration functionTemplate we have identified that should be
     * instantiated
     * @param function FunctionDeclaration representing the realization of the template
     * @param initializationSignature mapping containing the all elements of the signature of the
     * TemplateDeclaration as key and the Type/Expression the Parameter is initialized with.
     * @param initializationType mapping of the instantiation value to the instantiation type
     * (depends on resolution [TemplateDeclaration.TemplateInitialization]
     * @param orderedInitializationSignature mapping of the ordering of the template parameters
     */
    protected fun applyTemplateInstantiation(
        templateCall: CallExpression,
        functionTemplateDeclaration: FunctionTemplateDeclaration?,
        function: FunctionDeclaration,
        initializationSignature: Map<Declaration?, Node?>,
        initializationType: Map<Node?, TemplateInitialization?>,
        orderedInitializationSignature: Map<Declaration, Int>
    ) {
        val templateInstantiationParameters =
            mutableListOf<Node>(*orderedInitializationSignature.keys.toTypedArray())
        for ((key, value) in orderedInitializationSignature) {
            templateInstantiationParameters[value] = initializationSignature[key]!!
        }
        templateCall.templateInstantiation = functionTemplateDeclaration
        templateCall.invokes = listOf(function)

        // TODO(oxisto): Support multiple return values
        // Set return Value of call if resolved
        var returnType: Type? = function.returnTypes[0]
        val parameterizedTypeResolution =
            getParameterizedSignaturesFromInitialization(initializationSignature)
        if (returnType is ParameterizedType) {
            returnType =
                (initializationSignature[parameterizedTypeResolution[returnType]]
                        as TypeExpression?)
                    ?.type
        }
        templateCall.type = returnType
        templateCall.updateTemplateParameters(initializationType, templateInstantiationParameters)

        // Apply changes to the call signature
        val templateFunctionSignature =
            getCallSignature(function, parameterizedTypeResolution, initializationSignature)
        val templateCallSignature = templateCall.signature
        val callSignatureImplicit =
            signatureWithImplicitCastTransformation(
                templateCallSignature,
                templateCall.arguments,
                templateFunctionSignature
            )
        for (i in callSignatureImplicit.indices) {
            val cast = callSignatureImplicit[i]
            if (cast != null) {
                templateCall.setArgument(i, cast)
            }
        }

        // Add DFG edges from the instantiation Expression to the ParamVariableDeclaration in the
        // Template.
        for ((declaration) in initializationSignature) {
            if (declaration is ParamVariableDeclaration) {
                declaration.addPrevDFG(initializationSignature[declaration]!!)
                initializationSignature[declaration]!!.addNextDFG(declaration)
            }
        }
    }

    /**
     * @param functionDeclaration FunctionDeclaration realization of the template
     * @param functionDeclarationSignature Signature of the realization FunctionDeclaration, but
     * replacing the ParameterizedTypes with the ones provided in the instantiation
     * @param templateCallExpression CallExpression that instantiates the template
     * @param explicitInstantiation list of the explicitly instantiated type parameters
     * @return true if the instantiation of the template is compatible with the template
     * declaration, false otherwise
     */
    protected fun checkArgumentValidity(
        functionDeclaration: FunctionDeclaration,
        functionDeclarationSignature: List<Type>,
        templateCallExpression: CallExpression,
        explicitInstantiation: List<ParameterizedType?>
    ): Boolean {
        if (templateCallExpression.arguments.size <= functionDeclaration.parameters.size) {
            val callArguments =
                mutableListOf<Expression?>(
                    *templateCallExpression.arguments.toTypedArray()
                ) // Use provided arguments
            callArguments.addAll(
                functionDeclaration.defaultParameters
                    .subList(callArguments.size, functionDeclaration.defaultParameters.size)
                    .filterNotNull()
            ) // Extend by defaults
            for (i in callArguments.indices) {
                val callArgument = callArguments[i] ?: return false
                if (
                    callArgument.type != functionDeclarationSignature[i] &&
                        !(callArgument.type.isPrimitive &&
                            functionDeclarationSignature[i].isPrimitive &&
                            functionDeclaration.parameters[i].type in explicitInstantiation)
                ) {
                    return false
                }
            }
            return true
        }
        return false
    }

    /**
     * @param function FunctionDeclaration realization of the template
     * @param parameterizedTypeResolution mapping of ParameterizedTypes to the TypeParamDeclarations
     * that define them, used to backwards resolve
     * @param initializationSignature mapping between the ParamDeclaration of the template and the
     * corresponding instantiations
     * @return List of Types representing the Signature of the FunctionDeclaration, but
     * ParameterizedTypes (which depend on the specific instantiation of the template) are resolved
     * to the values the Template is instantiated with.
     */
    protected fun getCallSignature(
        function: FunctionDeclaration,
        parameterizedTypeResolution: Map<ParameterizedType, TypeParamDeclaration>,
        initializationSignature: Map<Declaration?, Node?>
    ): List<Type> {
        val templateCallSignature = mutableListOf<Type>()
        for (argument in function.parameters) {
            if (argument.type is ParameterizedType) {
                var type: Type = UnknownType.getUnknownType()
                val typeParamDeclaration = parameterizedTypeResolution[argument.type]
                if (typeParamDeclaration != null) {
                    val node = initializationSignature[typeParamDeclaration]
                    if (node is TypeExpression) {
                        type = node.type
                    }
                }
                templateCallSignature.add(type)
            } else {
                templateCallSignature.add(argument.type)
            }
        }
        return templateCallSignature
    }

    protected fun resolveArguments(call: CallExpression) {
        val worklist: Deque<Node> = ArrayDeque()
        call.arguments.forEach { worklist.push(it) }
        while (!worklist.isEmpty()) {
            val curr = worklist.pop()
            if (curr is CallExpression) {
                resolve(curr)
            } else {
                val it = Strategy.AST_FORWARD(curr)
                while (it.hasNext()) {
                    val astChild = it.next()
                    if (astChild !is RecordDeclaration) {
                        worklist.push(astChild)
                    }
                }
            }
        }
    }

    /**
     * @param callSignature Type signature of the CallExpression
     * @param functionSignature Type signature of the FunctionDeclaration
     * @return true if the CallExpression signature can be transformed into the FunctionDeclaration
     * signature by means of casting
     */
    protected fun compatibleSignatures(
        callSignature: List<Type?>,
        functionSignature: List<Type>
    ): Boolean {
        return if (callSignature.size == functionSignature.size) {
            for (i in callSignature.indices) {
                if (
                    callSignature[i]!!.isPrimitive != functionSignature[i].isPrimitive &&
                        !TypeManager.getInstance()
                            .isSupertypeOf(functionSignature[i], callSignature[i])
                ) {
                    return false
                }
            }
            true
        } else {
            false
        }
    }

    /**
     * Computes the implicit casts that are necessary to reach the
     *
     * @param callSignature signature of the call we want to find invocation targets for by
     * performing implicit casts
     * @param arguments arguments of the call
     * @param functionSignature Types of the signature of the possible invocation candidate
     * @return List containing either null on the i-th position (if the type of i-th argument of the
     * call equals the type of the i-th argument of the FunctionDeclaration) or a CastExpression on
     * the i-th position (if the argument of the call can be cast to match the type of the argument
     * at the i-th position of the FunctionDeclaration). If the list is empty the signature of the
     * FunctionDeclaration cannot be reached through implicit casts
     */
    protected fun signatureWithImplicitCastTransformation(
        callSignature: List<Type?>,
        arguments: List<Expression?>,
        functionSignature: List<Type>
    ): MutableList<CastExpression?> {
        val implicitCasts = mutableListOf<CastExpression?>()
        if (callSignature.size != functionSignature.size) return implicitCasts

        for (i in callSignature.indices) {
            val callType = callSignature[i]
            val funcType = functionSignature[i]
            if (callType!!.isPrimitive && funcType.isPrimitive && callType != funcType) {
                val implicitCast = CastExpression()
                implicitCast.isImplicit = true
                implicitCast.castType = funcType
                implicitCast.expression = arguments[i]
                implicitCasts.add(implicitCast)
            } else {
                // If no cast is needed we add null to be able to access the function signature
                // list and
                // the implicit cast list with the same index.
                implicitCasts.add(null)
            }
        }
        return implicitCasts
    }

    /**
     * @param call CallExpression
     * @param functionDeclaration FunctionDeclaration the CallExpression was resolved to
     * @return list containing the signature containing all argument types including the default
     * arguments
     */
    protected fun getCallSignatureWithDefaults(
        call: CallExpression,
        functionDeclaration: FunctionDeclaration
    ): List<Type?> {
        val callSignature = mutableListOf(*call.signature.toTypedArray())
        if (call.signature.size < functionDeclaration.parameters.size) {
            callSignature.addAll(
                functionDeclaration.defaultParameterSignature.subList(
                    call.arguments.size,
                    functionDeclaration.defaultParameterSignature.size
                )
            )
        }
        return callSignature
    }

    /**
     * modifies: call arguments by applying implicit casts
     *
     * @param call we want to find invocation targets for by performing implicit casts
     * @return list of invocation candidates by applying implicit casts
     */
    protected fun resolveWithImplicitCast(
        call: CallExpression,
        initialInvocationCandidates: List<FunctionDeclaration>
    ): List<FunctionDeclaration> {

        // Output list for invocationTargets obtaining a valid signature by performing implicit
        // casts
        val invocationTargetsWithImplicitCast = mutableListOf<FunctionDeclaration>()
        val invocationTargetsWithImplicitCastAndDefaults = mutableListOf<FunctionDeclaration>()
        var implicitCasts: MutableList<CastExpression?>? = null

        // Iterate through all possible invocation candidates
        for (functionDeclaration in initialInvocationCandidates) {
            val callSignature = getCallSignatureWithDefaults(call, functionDeclaration)
            // Check if the signatures match by implicit casts
            if (compatibleSignatures(callSignature, functionDeclaration.signatureTypes)) {
                val implicitCastTargets =
                    signatureWithImplicitCastTransformation(
                        getCallSignatureWithDefaults(call, functionDeclaration),
                        call.arguments,
                        functionDeclaration.signatureTypes
                    )
                if (implicitCasts == null) {
                    implicitCasts = implicitCastTargets
                } else {
                    // Since we can have multiple possible invocation targets the cast must all be
                    // to the same
                    // target type
                    checkMostCommonImplicitCast(implicitCasts, implicitCastTargets)
                }
                if (compatibleSignatures(call.signature, functionDeclaration.signatureTypes)) {
                    invocationTargetsWithImplicitCast.add(functionDeclaration)
                } else {
                    invocationTargetsWithImplicitCastAndDefaults.add(functionDeclaration)
                }
            }
        }

        // Apply implicit casts to call arguments
        implicitCasts?.let { applyImplicitCastToArguments(call, it) }

        // Prior implicit casts without defaults
        return invocationTargetsWithImplicitCast.ifEmpty {
            invocationTargetsWithImplicitCastAndDefaults
        }
    }

    /**
     * @param call we want to find invocation targets for by performing implicit casts
     * @return list of invocation candidates by applying implicit casts
     */
    protected fun resolveWithImplicitCastFunc(call: CallExpression): List<FunctionDeclaration> {
        if (scopeManager == null) {
            Util.errorWithFileLocation(
                call,
                log,
                "Could not resolve implicit casts: scope manager is null"
            )
            return listOf()
        }
        val initialInvocationCandidates =
            listOf(
                *scopeManager!!.resolveFunctionStopScopeTraversalOnDefinition(call).toTypedArray()
            )
        return resolveWithImplicitCast(call, initialInvocationCandidates)
    }

    /**
     * Checks if the current casts are compatible with the casts necessary to match with a new
     * FunctionDeclaration. If a one argument would need to be cast in two different types it would
     * be modified to a cast to UnknownType
     *
     * @param implicitCasts current Cast
     * @param implicitCastTargets new Cast
     */
    protected fun checkMostCommonImplicitCast(
        implicitCasts: MutableList<CastExpression?>,
        implicitCastTargets: List<CastExpression?>
    ) {
        for (i in implicitCasts.indices) {
            val currentCast = implicitCasts[i]
            if (i < implicitCastTargets.size) {
                val otherCast = implicitCastTargets[i]
                if (currentCast != null && otherCast != null && currentCast != otherCast) {
                    // If we have multiple function targets with different implicit casts, we have
                    // an
                    // ambiguous call, and we can't have a single cast
                    val contradictoryCast = CastExpression()
                    contradictoryCast.isImplicit = true
                    contradictoryCast.castType = UnknownType.getUnknownType()
                    contradictoryCast.expression = currentCast.expression
                    implicitCasts[i] = contradictoryCast
                }
            }
        }
    }

    /**
     * Changes the arguments of the CallExpression to use the implicit casts instead
     *
     * @param call CallExpression
     * @param implicitCasts Casts
     */
    protected fun applyImplicitCastToArguments(
        call: CallExpression,
        implicitCasts: List<CastExpression?>
    ) {
        for (i in implicitCasts.indices) {
            implicitCasts[i]?.let { call.setArgument(i, it) }
        }
    }

    /**
     * Changes the arguments of the ConstructExpression to use the implicit casts instead
     *
     * @param constructExpression ConstructExpression
     * @param implicitCasts Casts
     */
    /*protected fun applyImplicitCastToArguments(
        constructExpression: ConstructExpression,
        implicitCasts: List<CastExpression?>?
    ) {
        if (implicitCasts != null) {
            for (i in implicitCasts.indices) {
                if (implicitCasts[i] != null) {
                    constructExpression.setArgument(i, implicitCasts[i]!!)
                }
            }
        }
    }*/

    /**
     * Resolves a CallExpression to the potential target FunctionDeclarations by checking for
     * omitted arguments due to previously defined default arguments
     *
     * @param call CallExpression
     * @return List of FunctionDeclarations that are the target of the CallExpression (will be
     * connected with an invokes edge)
     */
    protected fun resolveWithDefaultArgs(
        call: CallExpression,
        initialInvocationCandidates: List<FunctionDeclaration>
    ): List<FunctionDeclaration> {
        val invocationCandidatesDefaultArgs = mutableListOf<FunctionDeclaration>()
        for (functionDeclaration in initialInvocationCandidates) {
            if (
                functionDeclaration.hasSignature(
                    getCallSignatureWithDefaults(call, functionDeclaration)
                )
            ) {
                invocationCandidatesDefaultArgs.add(functionDeclaration)
            }
        }
        return invocationCandidatesDefaultArgs
    }

    /**
     * @param call we want to find invocation targets for by adding the default arguments to the
     * signature
     * @return list of invocation candidates that have matching signature when considering default
     * arguments
     */
    protected fun resolveWithDefaultArgsFunc(call: CallExpression): List<FunctionDeclaration> {
        if (scopeManager == null) {
            Util.errorWithFileLocation(
                call,
                log,
                "Could not resolve default arguments: scope manager is null"
            )
            return emptyList()
        }
        val invocationCandidates =
            scopeManager!!.resolveFunctionStopScopeTraversalOnDefinition(call).filter { f
                -> /*!f.isImplicit() &&*/
                call.signature.size < f.signatureTypes.size
            }
        return resolveWithDefaultArgs(call, invocationCandidates)
    }

    protected fun handleNormalCalls(curClass: RecordDeclaration?, call: CallExpression) {
        if (curClass == null) {
            // Handle function (not method) calls
            // C++ allows function overloading. Make sure we have at least the same number of
            // arguments
            if (call.language is CXXLanguageFrontend) {
                // Handle CXX normal call resolution externally, otherwise it leads to increased
                // complexity
                handleNormalCallCXX(curClass, call)
            } else {
                val invocationCandidates = scopeManager!!.resolveFunction(call).toMutableList()
                createInferredFunction(invocationCandidates, call)
                call.invokes = invocationCandidates
            }
        } else if (!handlePossibleStaticImport(call, curClass)) {
            handleMethodCall(curClass, call)
        }
    }

    protected fun handleNormalCallCXX(curClass: RecordDeclaration?, call: CallExpression) {
        if (scopeManager == null) {
            Util.errorWithFileLocation(
                call,
                log,
                "Could not handle normal CXX calls: scope manager is null"
            )
            return
        }
        val invocationCandidates =
            scopeManager!!
                .resolveFunctionStopScopeTraversalOnDefinition(call)
                .filter { it.hasSignature(call.signature) }
                .toMutableList()
        if (invocationCandidates.isEmpty()) {
            // Check for usage of default args
            invocationCandidates.addAll(resolveWithDefaultArgsFunc(call))
        }
        if (invocationCandidates.isEmpty()) {
            /*
             Check if the call can be resolved to a function template instantiation. If it can be resolver, we
             resolve the call. Otherwise, there won't be an inferred template, we will do an inferred
             FunctionDeclaration instead.
            */
            call.templateParametersEdges = mutableListOf()
            if (handleTemplateFunctionCalls(curClass, call, false)) {
                return
            } else {
                call.templateParametersEdges = null
            }
        }
        if (invocationCandidates.isEmpty()) {
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a
            // candidate with an implicit cast
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call))
        }
        createInferredFunction(invocationCandidates, call)
        call.invokes = invocationCandidates
    }

    protected fun createInferredFunction(
        invocationCandidates: MutableList<FunctionDeclaration>,
        call: CallExpression
    ) {
        if (invocationCandidates.isEmpty()) {
            // If we still have no candidates and our current language is c++ we create an inferred
            // FunctionDeclaration
            invocationCandidates.add(
                createInferredFunctionDeclaration(null, call.name, call.code, false, call.signature)
            )
        }
    }

    protected fun handleMethodCall(curClass: RecordDeclaration?, call: CallExpression) {
        val possibleContainingTypes = getPossibleContainingTypes(call, curClass)

        // Find overridden invokes
        var invocationCandidates =
            call.invokes
                .map { getOverridingCandidates(possibleContainingTypes, it) }
                .flatten()
                .toMutableList()

        // Find function targets
        if (invocationCandidates.isEmpty()) {
            invocationCandidates =
                if (call.language is CXXLanguageFrontend) {
                    handleCXXMethodCall(curClass, possibleContainingTypes, call).toMutableList()
                } else {
                    scopeManager!!.resolveFunction(call).toMutableList()
                }
        }

        // Find invokes by supertypes
        if (
            invocationCandidates.isEmpty() &&
                (call.language !is CXXLanguageFrontend || shouldSearchForInvokesInParent(call))
        ) {
            val nameParts =
                call.name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (nameParts.isNotEmpty()) {
                val records =
                    possibleContainingTypes.mapNotNull { recordMap[it.root.typeName] }.toSet()
                invocationCandidates =
                    getInvocationCandidatesFromParents(nameParts[nameParts.size - 1], call, records)
                        .toMutableList()
            }
        }
        createMethodDummies(invocationCandidates, possibleContainingTypes, call)
        call.invokes = invocationCandidates
    }

    /**
     * @param call
     * @return FunctionDeclarations that are invocation candidates for the MethodCall call using C++
     * resolution techniques
     */
    protected fun handleCXXMethodCall(
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>,
        call: CallExpression
    ): List<FunctionDeclaration> {
        var invocationCandidates = mutableListOf<FunctionDeclaration>()
        val records = possibleContainingTypes.mapNotNull { recordMap[it.root.typeName] }.toSet()
        for (record in records) {
            invocationCandidates.addAll(getInvocationCandidatesFromRecord(record, call.name, call))
        }
        if (invocationCandidates.isEmpty()) {
            // Check for usage of default args
            invocationCandidates.addAll(resolveWithDefaultArgsFunc(call))
        }
        if (invocationCandidates.isEmpty()) {
            if (handleTemplateFunctionCalls(curClass, call, false)) {
                return call.invokes
            } else {
                call.templateParametersEdges = null
            }
        }
        if (invocationCandidates.isEmpty()) {
            // Check for usage of implicit cast
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call))
        }

        // Make sure, that our invocation candidates for member call expressions are really METHODS,
        // otherwise this will lead to
        // false positives. This is a hotfix until we rework the call resolver completely.
        if (call is MemberCallExpression) {
            invocationCandidates =
                invocationCandidates.filterIsInstance<MethodDeclaration>().toMutableList()
        }
        return invocationCandidates
    }

    /**
     * Creates an inferred element for each RecordDeclaration if the invocationCandidates are empty
     *
     * @param invocationCandidates
     * @param possibleContainingTypes
     * @param call
     */
    protected fun createMethodDummies(
        invocationCandidates: MutableList<FunctionDeclaration>,
        possibleContainingTypes: Set<Type>,
        call: CallExpression
    ) {
        if (invocationCandidates.isEmpty()) {
            possibleContainingTypes
                .mapNotNull {
                    var record = recordMap[it.root.typeName]
                    if (record == null && config?.inferenceConfiguration?.inferRecords == true) {
                        record = inferRecordDeclaration(it)
                    }
                    record
                }
                .map {
                    createInferredFunctionDeclaration(
                        it,
                        call.name,
                        call.code,
                        false,
                        call.signature
                    )
                }
                .forEach { invocationCandidates.add(it) }
        }
    }

    /**
     * Infers a record declaration.
     *
     * TODO: Merge this with the (almost) same function in the VariableUsageResolver.
     *
     * @param type the object type representing a record that we want to infer.
     * @return the inferred record declaration.
     */
    protected fun inferRecordDeclaration(type: Type?): RecordDeclaration? {
        if (type is ObjectType) {
            log.debug(
                "Encountered an unknown record type ${type.typeName} during a call. We are going to infer that record",
            )

            // The kind is most likely a class, since this is a member call. However, in some
            // languages this might be still a struct (like Go), so we might need to fine-tune this
            // later.
            val declaration = newRecordDeclaration(type.typeName, "class", "")
            declaration.isInferred = true

            // update the type
            type.recordDeclaration = declaration

            // update the record map
            recordMap[type.getRoot().typeName] = declaration

            // add this record declaration to the current TU (this bypasses the scope manager)
            lang!!.currentTU.addDeclaration(declaration)
            return declaration
        } else {
            log.error(
                "Trying to infer a record declaration of a non-object type. Not sure what to do? Should we change the type?"
            )
        }
        return null
    }

    /**
     * In C++ search we don't search in the parent if there is a potential candidate with matching
     * name
     *
     * @param call
     * @return true if we should stop searching parent, false otherwise
     */
    protected fun shouldSearchForInvokesInParent(call: CallExpression): Boolean {
        if (scopeManager == null) {
            Util.errorWithFileLocation(
                call,
                log,
                "Could not search for invokes in parent: scopeManager is null"
            )
            return false
        }
        return scopeManager!!.resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty()
    }

    protected fun resolveConstructExpression(constructExpression: ConstructExpression) {
        val typeName = constructExpression.type.typeName
        val recordDeclaration = recordMap[typeName]
        constructExpression.instantiates = recordDeclaration
        for (template in templateList) {
            if (
                template is ClassTemplateDeclaration &&
                    recordDeclaration in template.realization &&
                    (constructExpression.templateParameters.size <= template.getParameters().size)
            ) {
                val defaultDifference =
                    template.getParameters().size - constructExpression.templateParameters.size
                if (defaultDifference <= template.getParameterDefaults().size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpression, template)

                    // Add missing defaults
                    val missingNewParams: List<Node> =
                        template
                            .getParameterDefaults()
                            .subList(
                                constructExpression.templateParameters.size,
                                template.getParameterDefaults().size
                            )
                    for (missingParam in missingNewParams) {
                        constructExpression.addTemplateParameter(
                            missingParam,
                            TemplateInitialization.DEFAULT
                        )
                    }
                    constructExpression.templateInstantiation = template
                    break
                }
            }
        }
        if (recordDeclaration != null) {
            val constructor = getConstructorDeclaration(constructExpression, recordDeclaration)
            constructExpression.constructor = constructor
        }
    }

    /**
     * Adds the resolved default template arguments recursively to the templateParameter list of the
     * ConstructExpression until a fixpoint is reached e.g. template&lt;class Type1, class Type2 =
     * Type1&gt;
     *
     * @param constructExpression
     * @param template
     */
    protected fun addRecursiveDefaultTemplateArgs(
        constructExpression: ConstructExpression,
        template: ClassTemplateDeclaration
    ) {
        var templateParameters: Int
        do {
            // Handle Explicit Template Arguments
            templateParameters = constructExpression.templateParameters.size
            val templateParametersExplicitInitialization: MutableMap<Node, Node> = HashMap()
            handleExplicitTemplateParameters(
                constructExpression,
                template,
                templateParametersExplicitInitialization
            )
            val templateParameterRealDefaultInitialization: MutableMap<Node, Node?> = HashMap()

            // Handle defaults of parameters
            handleDefaultTemplateParameters(template, templateParameterRealDefaultInitialization)

            // Add defaults to ConstructDeclaration
            applyMissingParams(
                template,
                constructExpression,
                templateParametersExplicitInitialization,
                templateParameterRealDefaultInitialization
            )
        } while (templateParameters != constructExpression.templateParameters.size)
    }

    /**
     * Apply missingParameters (either explicit or defaults) to the ConstructExpression and its type
     *
     * @param template Template which is instantiated by the ConstructExpression
     * @param constructExpression
     * @param templateParametersExplicitInitialization mapping of the template parameter to the
     * explicit instantiation
     * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
     * default (no recursive)
     */
    protected fun applyMissingParams(
        template: ClassTemplateDeclaration,
        constructExpression: ConstructExpression,
        templateParametersExplicitInitialization: Map<Node, Node>,
        templateParameterRealDefaultInitialization: Map<Node, Node?>
    ) {
        val missingParams: List<Node> =
            template.parameterDefaults.subList(
                constructExpression.templateParameters.size,
                template.parameterDefaults.size
            )
        for (m in missingParams) {
            var missingParam = m
            if (missingParam is DeclaredReferenceExpression) {
                missingParam = missingParam.refersTo!!
            }
            if (missingParam in templateParametersExplicitInitialization) {
                // If default is a previously defined template argument that has been explicitly
                // passed
                constructExpression.addTemplateParameter(
                    templateParametersExplicitInitialization[missingParam]!!,
                    TemplateInitialization.DEFAULT
                )
                // If template argument is a type add it as a generic to the type as well
                if (templateParametersExplicitInitialization[missingParam] is TypeExpression) {
                    (constructExpression.type as ObjectType).addGeneric(
                        (templateParametersExplicitInitialization[missingParam] as TypeExpression?)
                            ?.type
                    )
                }
            } else if (missingParam in templateParameterRealDefaultInitialization) {
                // Add default of template parameter to construct declaration
                constructExpression.addTemplateParameter(
                    templateParameterRealDefaultInitialization[missingParam]!!,
                    TemplateInitialization.DEFAULT
                )
                if (templateParametersExplicitInitialization[missingParam] is Type) {
                    (constructExpression.type as ObjectType).addGeneric(
                        (templateParametersExplicitInitialization[missingParam] as TypeExpression?)
                            ?.type
                    )
                }
            }
        }
    }

    /**
     * Matches declared template arguments to the explicit instantiation
     *
     * @param constructExpression containing the explicit instantiation
     * @param template containing declared template arguments
     * @param templateParametersExplicitInitialization mapping of the template parameter to the
     * explicit instantiation
     */
    protected fun handleExplicitTemplateParameters(
        constructExpression: ConstructExpression,
        template: ClassTemplateDeclaration,
        templateParametersExplicitInitialization: MutableMap<Node, Node>
    ) {
        for (i in constructExpression.templateParameters.indices) {
            val explicit = constructExpression.templateParameters[i]
            if (template.parameters[i] is TypeParamDeclaration) {
                templateParametersExplicitInitialization[
                    (template.parameters[i] as TypeParamDeclaration).type] = explicit
            } else if (template.parameters[i] is ParamVariableDeclaration) {
                templateParametersExplicitInitialization[template.parameters[i]] = explicit
            }
        }
    }

    /**
     * Matches declared template arguments to their defaults (without defaults of a previously
     * defined template argument)
     *
     * @param template containing template arguments
     * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
     * default (no recursive)
     */
    protected fun handleDefaultTemplateParameters(
        template: ClassTemplateDeclaration,
        templateParameterRealDefaultInitialization: MutableMap<Node, Node?>
    ) {
        val declaredTemplateTypes = mutableListOf<Type?>()
        val declaredNonTypeTemplate = mutableListOf<ParamVariableDeclaration>()
        val parametersWithDefaults = template.parametersWithDefaults
        for (declaration in template.parameters) {
            if (declaration is TypeParamDeclaration) {
                declaredTemplateTypes.add(declaration.type)
                if (
                    declaration.default !in declaredTemplateTypes &&
                        declaration in parametersWithDefaults
                ) {
                    templateParameterRealDefaultInitialization[declaration.type] =
                        declaration.default
                }
            } else if (declaration is ParamVariableDeclaration) {
                declaredNonTypeTemplate.add(declaration)
                if (
                    declaration in parametersWithDefaults &&
                        (declaration.default !is DeclaredReferenceExpression ||
                            (declaration.default as DeclaredReferenceExpression?)?.refersTo !in
                                declaredNonTypeTemplate)
                ) {
                    templateParameterRealDefaultInitialization[declaration] = declaration.default
                }
            }
        }
    }

    protected fun resolveExplicitConstructorInvocation(eci: ExplicitConstructorInvocation) {
        if (eci.containingClass != null) {
            val recordDeclaration = recordMap[eci.containingClass]
            val signature = eci.arguments.map { it.type }
            if (recordDeclaration != null) {
                val constructor =
                    getConstructorDeclarationForExplicitInvocation(signature, recordDeclaration)
                val invokes = mutableListOf<FunctionDeclaration>()
                invokes.add(constructor)
                eci.invokes = invokes
            }
        }
    }

    protected fun handlePossibleStaticImport(
        call: CallExpression?,
        curClass: RecordDeclaration?
    ): Boolean {
        if (call == null || curClass == null) {
            return false
        }
        val name = call.name.substring(call.name.lastIndexOf('.') + 1)
        val nameMatches =
            curClass.staticImports.filterIsInstance<FunctionDeclaration>().filter {
                it.name == name || it.name.endsWith(".$name")
            }
        return if (nameMatches.isEmpty()) {
            false
        } else {
            val invokes = mutableListOf<FunctionDeclaration>()
            val target = nameMatches.firstOrNull { it.hasSignature(call.signature) }
            if (target == null) {
                generateInferredStaticallyImportedMethods(call, name, invokes, curClass)
            } else {
                invokes.add(target)
            }
            call.invokes = invokes
            true
        }
    }

    protected fun generateInferredStaticallyImportedMethods(
        call: CallExpression,
        name: String,
        invokes: MutableList<FunctionDeclaration>,
        curClass: RecordDeclaration?
    ) {
        // We had an import for this method name, just not the correct signature. Let's just add
        // an inferred node to any class that might be affected
        if (curClass == null) {
            LOGGER.warn("Cannot generate inferred nodes for imports of a null class: $call")
            return
        }
        val containingRecords =
            curClass.staticImportStatements
                .filter { it.endsWith(".$name") }
                .map { it.substring(0, it.lastIndexOf('.')) }
                .mapNotNull { recordMap[it] }
        for (recordDeclaration in containingRecords) {
            val inferredMethod = newMethodDeclaration(name, "", true, recordDeclaration)
            inferredMethod.isInferred = true
            val params = Util.createInferredParameters(call.signature)
            inferredMethod.parameters = params
            recordDeclaration.addMethod(inferredMethod)
            curClass.staticImports.add(inferredMethod)
            invokes.add(inferredMethod)
        }
    }

    /**
     * Create an inferred FunctionTemplateDeclaration if a call to an FunctionTemplate could not be
     * resolved
     *
     * @param containingRecord
     * @param call
     * @return inferred FunctionTemplateDeclaration which can be invoked by the call
     */
    protected fun createInferredFunctionTemplate(
        containingRecord: RecordDeclaration?,
        call: CallExpression
    ): FunctionTemplateDeclaration {
        val name = call.name
        val code = call.code
        val inferred = newFunctionTemplateDeclaration(name, code)
        inferred.isInferred = true
        if (containingRecord != null) {
            containingRecord.addDeclaration(inferred)
        } else {
            currentTU.addDeclaration(inferred)
        }
        val inferredRealization =
            createInferredFunctionDeclaration(containingRecord, name, code, false, call.signature)
        inferred.addRealization(inferredRealization)
        var typeCounter = 0
        var nonTypeCounter = 0
        for (node in call.templateParameters) {
            if (node is TypeExpression) {
                // Template Parameter
                val inferredTypeIdentifier = "T$typeCounter"
                val typeParamDeclaration =
                    newTypeParamDeclaration(inferredTypeIdentifier, inferredTypeIdentifier)
                typeParamDeclaration.isInferred = true
                val parameterizedType = ParameterizedType(inferredTypeIdentifier)
                parameterizedType.isInferred = true
                typeParamDeclaration.type = parameterizedType
                TypeManager.getInstance().addTypeParameter(inferred, parameterizedType)
                typeCounter++
                inferred.addParameter(typeParamDeclaration)
            } else if (node is Expression) {
                // Non-Type Template Parameter
                val inferredNonTypeIdentifier = "N$nonTypeCounter"
                val paramVariableDeclaration =
                    newMethodParameterIn(
                        inferredNonTypeIdentifier,
                        node.type,
                        false,
                        inferredNonTypeIdentifier
                    )
                paramVariableDeclaration.isInferred = true
                paramVariableDeclaration.addPrevDFG(node)
                node.addNextDFG(paramVariableDeclaration)
                nonTypeCounter++
                inferred.addParameter(paramVariableDeclaration)
            }
        }
        return inferred
    }

    protected fun createInferredFunctionDeclaration(
        containingRecord: RecordDeclaration?,
        name: String?,
        code: String?,
        isStatic: Boolean,
        signature: List<Type?>?
    ): FunctionDeclaration {
        val parameters = Util.createInferredParameters(signature)
        return if (containingRecord != null) {
            val inferred = newMethodDeclaration(name, code, isStatic, containingRecord)
            inferred.isInferred = true
            inferred.parameters = parameters
            containingRecord.addMethod(inferred)

            // "upgrade" our struct to a class, if it was inferred by us, since we are calling
            // methods on
            // it
            if (
                config?.inferenceConfiguration?.inferRecords == true &&
                    containingRecord.isInferred &&
                    containingRecord.kind == "struct"
            ) {
                containingRecord.kind = "class"
            }
            log.debug(
                "Inferring a new method declaration ${inferred.name} with parameter types ${inferred.parameters.map { it.type.name }}"
            )
            inferred
        } else {
            // function declaration, not inside a class
            val inferred = newFunctionDeclaration(name!!, code)
            inferred.parameters = parameters
            inferred.isInferred = true
            currentTU.addDeclaration(inferred)
            inferred
        }
    }

    protected fun createInferredConstructor(
        containingRecord: RecordDeclaration,
        signature: List<Type?>
    ): ConstructorDeclaration {
        val inferred = newConstructorDeclaration(containingRecord.name, "", containingRecord)
        inferred.isInferred = true
        inferred.parameters = Util.createInferredParameters(signature)
        containingRecord.addConstructor(inferred)
        return inferred
    }

    protected fun getPossibleContainingTypes(node: Node?, curClass: RecordDeclaration?): Set<Type> {
        val possibleTypes = mutableSetOf<Type>()
        if (node is MemberCallExpression) {
            val base = node.base!!
            possibleTypes.add(base.type)
            possibleTypes.addAll(base.possibleSubTypes)
        } else if (node is StaticCallExpression) {
            if (node.targetRecord != null) {
                possibleTypes.add(TypeParser.createFrom(node.targetRecord, true))
            }
        } else if (curClass != null) {
            possibleTypes.add(TypeParser.createFrom(curClass.name, true))
        }
        return possibleTypes
    }

    protected fun getInvocationCandidatesFromRecord(
        recordDeclaration: RecordDeclaration?,
        name: String?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        val signature: List<Type?> = call.signature
        val namePattern =
            Pattern.compile(
                "(" + Pattern.quote(recordDeclaration!!.name) + "\\.)?" + Pattern.quote(name)
            )
        return if (call.language is CXXLanguageFrontend) {
            val invocationCandidate =
                mutableListOf<FunctionDeclaration>(
                    *recordDeclaration.methods
                        .filter { m ->
                            namePattern.matcher(m.name).matches() && m.hasSignature(signature)
                        }
                        .toTypedArray()
                )
            if (invocationCandidate.isEmpty()) {
                // Search for possible invocation with defaults args
                invocationCandidate.addAll(
                    resolveWithDefaultArgs(
                        call,
                        recordDeclaration.methods.filter { m ->
                            (namePattern.matcher(m.name).matches() /*&& !m.isImplicit()*/ &&
                                call.signature.size < m.signatureTypes.size)
                        }
                    )
                )
            }
            if (invocationCandidate.isEmpty()) {
                // Search for possible invocation with implicit cast
                invocationCandidate.addAll(
                    resolveWithImplicitCast(
                        call,
                        recordDeclaration.methods.filter { m ->
                            namePattern.matcher(m.name).matches() /*&& !m.isImplicit()*/
                        }
                    )
                )
            }
            invocationCandidate
        } else {
            recordDeclaration.methods.filter {
                namePattern.matcher(it.name).matches() && it.hasSignature(signature)
            }
        }
    }

    protected fun getInvocationCandidatesFromParents(
        name: String?,
        call: CallExpression,
        possibleTypes: Set<RecordDeclaration>
    ): List<FunctionDeclaration> {
        val workingPossibleTypes = mutableSetOf<RecordDeclaration>(*possibleTypes.toTypedArray())
        return if (possibleTypes.isEmpty()) {
            listOf()
        } else {
            val firstLevelCandidates =
                possibleTypes.map { getInvocationCandidatesFromRecord(it, name, call) }.flatten()

            // C++ does not allow overloading at different hierarchy levels. If we find a
            // FunctionDeclaration with the same name as the function in the CallExpression we have
            // to
            // stop the search in the parent even if the FunctionDeclaration does not match with the
            // signature of the CallExpression
            if (call.language is CXXLanguageFrontend) {
                workingPossibleTypes.removeIf { recordDeclaration ->
                    !shouldContinueSearchInParent(recordDeclaration, name)
                }
            }
            if (firstLevelCandidates.isEmpty() && !possibleTypes.isEmpty()) {
                workingPossibleTypes
                    .map { it.superTypeDeclarations }
                    .map { getInvocationCandidatesFromParents(name, call, it) }
                    .flatten()
            } else {
                firstLevelCandidates
            }
        }
    }

    /**
     * In C++ if there is a method that matches the name we are looking for, we have to stop
     * searching in the parents even if the signature of the method does not match
     *
     * @param recordDeclaration
     * @param name
     * @return true if there is no method in the recordDeclaration where the name of the method
     * matches with the provided name. false otherwise
     */
    protected fun shouldContinueSearchInParent(
        recordDeclaration: RecordDeclaration?,
        name: String?
    ): Boolean {
        val namePattern =
            Pattern.compile(
                "(" + Pattern.quote(recordDeclaration!!.name) + "\\.)?" + Pattern.quote(name)
            )
        val invocationCandidate =
            recordDeclaration.methods.filter { namePattern.matcher(it.name).matches() }
        return invocationCandidate.isEmpty()
    }

    protected fun getOverridingCandidates(
        possibleSubTypes: Set<Type?>,
        declaration: FunctionDeclaration
    ): Set<FunctionDeclaration> {
        return declaration.overriddenBy
            .filter { f -> containingType[f] in possibleSubTypes }
            .toSet()
    }

    /**
     * @param signature of the ConstructExpression
     * @param recordDeclaration matching the class the ConstructExpression wants to construct
     * @return ConstructorDeclaration that matches the provided signature
     */
    protected fun getConstructorDeclarationDirectMatch(
        signature: List<Type?>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration? {
        for (constructor in recordDeclaration.constructors) {
            if (constructor.hasSignature(signature)) {
                return constructor
            }
        }
        return null
    }

    /**
     * @param constructExpression we want to find an invocation target for
     * @param signature of the ConstructExpression (without defaults)
     * @param recordDeclaration associated with the Object the ConstructExpression constructs
     * @return a ConstructDeclaration that matches with the signature of the ConstructExpression
     * with added default arguments. The default arguments are added to the arguments edge of the
     * ConstructExpression
     */
    protected fun resolveConstructorWithDefaults(
        constructExpression: ConstructExpression,
        signature: List<Type?>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration? {
        for (constructor in recordDeclaration.constructors) {
            if (/*!constructor.isImplicit() &&*/ signature.size < constructor.signatureTypes.size) {
                val workingSignature =
                    getCallSignatureWithDefaults(constructExpression, constructor)
                if (constructor.hasSignature(workingSignature)) {
                    return constructor
                }
            }
        }
        return null
    }

    /**
     * @param constructExpression we want to find an invocation target for
     * @param recordDeclaration associated with the Object the ConstructExpression constructs
     * @return a ConstructDeclaration that matches the signature of the ConstructExpression by
     * applying one or more implicit casts to the primitive type arguments of the
     * ConstructExpressions. The arguments are proxied through a CastExpression to the type required
     * by the ConstructDeclaration.
     */
    protected fun resolveConstructorWithImplicitCast(
        constructExpression: ConstructExpression,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration? {
        for (constructorDeclaration in recordDeclaration.constructors) {
            val workingSignature = mutableListOf(*constructExpression.signature.toTypedArray())
            val defaultParameterSignature = constructorDeclaration.defaultParameterSignature
            if (constructExpression.arguments.size <= defaultParameterSignature.size) {
                workingSignature.addAll(
                    defaultParameterSignature.subList(
                        constructExpression.arguments.size,
                        defaultParameterSignature.size
                    )
                )
            }
            if (
                compatibleSignatures(
                    constructExpression.signature,
                    constructorDeclaration.signatureTypes
                )
            ) {
                val implicitCasts =
                    signatureWithImplicitCastTransformation(
                        constructExpression.signature,
                        constructExpression.arguments,
                        constructorDeclaration.signatureTypes
                    )
                applyImplicitCastToArguments(constructExpression, implicitCasts)
                return constructorDeclaration
            } else if (
                compatibleSignatures(workingSignature, constructorDeclaration.signatureTypes)
            ) {
                val implicitCasts =
                    signatureWithImplicitCastTransformation(
                        getCallSignatureWithDefaults(constructExpression, constructorDeclaration),
                        constructExpression.arguments,
                        constructorDeclaration.signatureTypes
                    )
                applyImplicitCastToArguments(constructExpression, implicitCasts)
                return constructorDeclaration
            }
        }
        return null
    }

    /**
     * @param constructExpression we want to find an invocation target for
     * @param recordDeclaration associated with the Object the ConstructExpression constructs
     * @return a ConstructDeclaration that is an invocation of the given ConstructExpression. If
     * there is no valid ConstructDeclaration we will create an implicit ConstructDeclaration that
     * matches the ConstructExpression.
     */
    protected fun getConstructorDeclaration(
        constructExpression: ConstructExpression,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration {
        val signature: List<Type?> = constructExpression.signature
        var constructorCandidate =
            getConstructorDeclarationDirectMatch(signature, recordDeclaration)
        if (constructorCandidate == null && constructExpression.language is CXXLanguageFrontend) {
            // Check for usage of default args
            constructorCandidate =
                resolveConstructorWithDefaults(constructExpression, signature, recordDeclaration)
        }
        if (constructorCandidate == null && constructExpression.language is CXXLanguageFrontend) {
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a candidate with an implicit cast
            constructorCandidate =
                resolveConstructorWithImplicitCast(constructExpression, recordDeclaration)
        }
        if (constructorCandidate == null) {
            // Create inferred node
            constructorCandidate = createInferredConstructor(recordDeclaration, signature)
        }
        return constructorCandidate
    }

    protected fun getConstructorDeclarationForExplicitInvocation(
        signature: List<Type?>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration {
        return recordDeclaration.constructors.firstOrNull { it.hasSignature(signature) }
            ?: createInferredConstructor(recordDeclaration, signature)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CallResolver::class.java)

        /**
         * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
         *
         * @param templateParams of the VariableDeclaration/NewExpression
         * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
         * ConstructExpression uses AST as well as the VariableDeclaration/NewExpression
         */
        fun addImplicitTemplateParametersToCall(
            templateParams: List<Node?>,
            constructExpression: ConstructExpression
        ) {
            for (node in templateParams) {
                if (node is TypeExpression) {
                    constructExpression.addTemplateParameter(duplicateTypeExpression(node, true))
                } else if (node is Literal<*>) {
                    constructExpression.addTemplateParameter(duplicateLiteral(node, true))
                }
            }
        }
    }
}
