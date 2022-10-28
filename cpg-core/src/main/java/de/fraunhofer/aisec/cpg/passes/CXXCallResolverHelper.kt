/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.CallResolver.Companion.LOGGER
import de.fraunhofer.aisec.cpg.passes.inference.inferFunction
import de.fraunhofer.aisec.cpg.passes.inference.inferMethod
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import java.util.HashMap
import java.util.regex.Pattern

fun CallResolver.handleNormalCallCXX(call: CallExpression) {
    if (scopeManager == null) {
        Util.errorWithFileLocation(
            call,
            LOGGER,
            "Could not handle normal call cpp: scopeManager is null"
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
        if (handleTemplateFunctionCalls(null, call, false)) {
            return
        } else {
            call.templateParametersEdges = null
        }
    }
    if (invocationCandidates.isEmpty()) {
        // If we don't find any candidate and our current language is c/c++ we check if there is
        // a candidate with an implicit cast
        invocationCandidates.addAll(resolveWithImplicitCastFunc(call))
    }

    if (invocationCandidates.isEmpty()) {
        // If we still have no candidates and our current language is c++ we create an inferred
        // FunctionDeclaration
        invocationCandidates.add(currentTU.inferFunction(call))
    }

    call.invokes = invocationCandidates
}

/**
 * @param callSignature Type signature of the CallExpression
 * @param functionSignature Type signature of the FunctionDeclaration
 * @return true if the CallExpression signature can be transformed into the FunctionDeclaration
 * signature by means of casting
 */
fun compatibleSignatures(callSignature: List<Type?>, functionSignature: List<Type>): Boolean {
    return if (callSignature.size == functionSignature.size) {
        for (i in callSignature.indices) {
            if (
                callSignature[i]!!.isPrimitive != functionSignature[i].isPrimitive &&
                    !TypeManager.getInstance().isSupertypeOf(functionSignature[i], callSignature[i])
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
 * @param call CallExpression
 * @param functionDeclaration FunctionDeclaration the CallExpression was resolved to
 * @return list containing the signature containing all argument types including the default
 * arguments
 */
fun getCallSignatureWithDefaults(
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
fun resolveWithImplicitCast(
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
                // to the same target type
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
 * Checks if the current casts are compatible with the casts necessary to match with a new
 * FunctionDeclaration. If a one argument would need to be cast in two different types it would be
 * modified to a cast to UnknownType
 *
 * @param implicitCasts current Cast
 * @param implicitCastTargets new Cast
 */
fun checkMostCommonImplicitCast(
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
fun applyImplicitCastToArguments(call: CallExpression, implicitCasts: List<CastExpression?>) {
    for (i in implicitCasts.indices) {
        implicitCasts[i]?.let { call.setArgument(i, it) }
    }
}

/**
 * @param call we want to find invocation targets for by performing implicit casts
 * @return list of invocation candidates by applying implicit casts
 */
fun CallResolver.resolveWithImplicitCastFunc(call: CallExpression): List<FunctionDeclaration> {
    if (scopeManager == null) {
        Util.errorWithFileLocation(
            call,
            LOGGER,
            "Could not resolve with implicit cast function: scopeManager is null"
        )
        return listOf()
    }
    val initialInvocationCandidates =
        listOf(*scopeManager!!.resolveFunctionStopScopeTraversalOnDefinition(call).toTypedArray())
    return resolveWithImplicitCast(call, initialInvocationCandidates)
}

/**
 * @param call we want to find invocation targets for by adding the default arguments to the
 * signature
 * @return list of invocation candidates that have matching signature when considering default
 * arguments
 */
fun CallResolver.resolveWithDefaultArgsFunc(call: CallExpression): List<FunctionDeclaration> {
    if (scopeManager == null) {
        Util.errorWithFileLocation(
            call,
            LOGGER,
            "Could not resolve with default args: scopeManager is null"
        )
        return listOf()
    }
    val invocationCandidates =
        scopeManager!!.resolveFunctionStopScopeTraversalOnDefinition(call).filter { f
            -> /*!f.isImplicit() &&*/
            call.signature.size < f.signatureTypes.size
        }
    return resolveWithDefaultArgs(call, invocationCandidates)
}

/**
 * Resolves a CallExpression to the potential target FunctionDeclarations by checking for omitted
 * arguments due to previously defined default arguments
 *
 * @param call CallExpression
 * @return List of FunctionDeclarations that are the target of the CallExpression (will be connected
 * with an invokes edge)
 */
fun resolveWithDefaultArgs(
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
 * @param call
 * @return FunctionDeclarations that are invocation candidates for the MethodCall call using C++
 * resolution techniques
 */
fun CallResolver.handleCXXMethodCall(
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
    // otherwise this will lead to false positives. This is a hotfix until we rework the call
    // resolver completely.
    if (call is MemberCallExpression) {
        invocationCandidates =
            invocationCandidates.filterIsInstance<MethodDeclaration>().toMutableList()
    }
    return invocationCandidates
}

fun getInvocationCandidatesFromRecordCXX(
    recordDeclaration: RecordDeclaration,
    call: CallExpression,
    namePattern: Pattern
): List<FunctionDeclaration> {
    val invocationCandidate =
        mutableListOf<FunctionDeclaration>(
            *recordDeclaration.methods
                .filter { m ->
                    namePattern.matcher(m.name).matches() && m.hasSignature(call.signature)
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
    return invocationCandidate
}

/**
 * @param constructExpression we want to find an invocation target for
 * @param signature of the ConstructExpression (without defaults)
 * @param recordDeclaration associated with the Object the ConstructExpression constructs
 * @return a ConstructDeclaration that matches with the signature of the ConstructExpression with
 * added default arguments. The default arguments are added to the arguments edge of the
 * ConstructExpression
 */
fun resolveConstructorWithDefaults(
    constructExpression: ConstructExpression,
    signature: List<Type?>,
    recordDeclaration: RecordDeclaration
): ConstructorDeclaration? {
    for (constructor in recordDeclaration.constructors) {
        if (/*!constructor.isImplicit() &&*/ signature.size < constructor.signatureTypes.size) {
            val workingSignature = getCallSignatureWithDefaults(constructExpression, constructor)
            if (constructor.hasSignature(workingSignature)) {
                return constructor
            }
        }
    }
    return null
}

/**
 * In C++ if there is a method that matches the name we are looking for, we have to stop searching
 * in the parents even if the signature of the method does not match
 *
 * @param recordDeclaration
 * @param name
 * @return true if there is no method in the recordDeclaration where the name of the method matches
 * with the provided name. false otherwise
 */
fun shouldContinueSearchInParent(recordDeclaration: RecordDeclaration?, name: String?): Boolean {
    val namePattern =
        Pattern.compile(
            "(" + Pattern.quote(recordDeclaration!!.name) + "\\.)?" + Pattern.quote(name)
        )
    val invocationCandidate =
        recordDeclaration.methods.filter { namePattern.matcher(it.name).matches() }
    return invocationCandidate.isEmpty()
}

/**
 * @param constructExpression we want to find an invocation target for
 * @param recordDeclaration associated with the Object the ConstructExpression constructs
 * @return a ConstructDeclaration that matches the signature of the ConstructExpression by applying
 * one or more implicit casts to the primitive type arguments of the ConstructExpressions. The
 * arguments are proxied through a CastExpression to the type required by the ConstructDeclaration.
 */
fun resolveConstructorWithImplicitCast(
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
        } else if (compatibleSignatures(workingSignature, constructorDeclaration.signatureTypes)) {
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
 * @param curClass class the invoked method must be part of.
 * @param templateCall call to instantiate and invoke a function template
 * @param applyInference if the resolution was unsuccessful and applyInference is true the call will
 * resolve to an instantiation/invocation of an inferred template
 * @return true if resolution was successful, false if not
 */
fun CallResolver.handleTemplateFunctionCalls(
    curClass: RecordDeclaration?,
    templateCall: CallExpression,
    applyInference: Boolean
): Boolean {
    if (scopeManager == null) {
        Util.errorWithFileLocation(
            templateCall,
            Pass.log,
            "Could not handle template function call: scopeManager is null"
        )
        return false
    }
    val instantiationCandidates = scopeManager!!.resolveFunctionTemplateDeclaration(templateCall)
    for (functionTemplateDeclaration in instantiationCandidates) {
        val initializationType = mutableMapOf<Node?, TemplateDeclaration.TemplateInitialization?>()
        val orderedInitializationSignature = mutableMapOf<Declaration, Int>()
        val explicitInstantiation = mutableListOf<ParameterizedType?>()
        if (
            (templateCall.templateParameters.size <= functionTemplateDeclaration.parameters.size) &&
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
                            getParameterizedSignaturesFromInitialization(initializationSignature),
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
                TemplateDeclaration.TemplateInitialization.EXPLICIT
            )
        }
        return true
    }
    return false
}

/**
 * Create an inferred FunctionTemplateDeclaration if a call to an FunctionTemplate could not be
 * resolved
 *
 * @param containingRecord
 * @param call
 * @return inferred FunctionTemplateDeclaration which can be invoked by the call
 */
fun CallResolver.createInferredFunctionTemplate(
    containingRecord: RecordDeclaration?,
    call: CallExpression
): FunctionTemplateDeclaration {
    val name = call.name
    val code = call.code
    val inferred = NodeBuilder.newFunctionTemplateDeclaration(name, call.language, code)
    inferred.isInferred = true

    val inferredRealization: FunctionDeclaration? =
        if (containingRecord != null) {
            containingRecord.addDeclaration(inferred)
            containingRecord.inferMethod(call)
        } else {
            currentTU.addDeclaration(inferred)
            currentTU.inferFunction(call)
        }

    inferred.addRealization(inferredRealization)

    var typeCounter = 0
    var nonTypeCounter = 0
    for (node in call.templateParameters) {
        if (node is TypeExpression) {
            // Template Parameter
            val inferredTypeIdentifier = "T$typeCounter"
            val typeParamDeclaration =
                inferred.startInference().inferTemplateParameter(inferredTypeIdentifier)
            typeCounter++
            inferred.addParameter(typeParamDeclaration)
        } else if (node is Expression) {
            val inferredNonTypeIdentifier = "N$nonTypeCounter"
            var paramVariableDeclaration =
                node.startInference().inferNonTypeTemplateParameter(inferredNonTypeIdentifier)

            paramVariableDeclaration.addPrevDFG(node)
            node.addNextDFG(paramVariableDeclaration)
            nonTypeCounter++
            inferred.addParameter(paramVariableDeclaration)
        }
    }
    return inferred
}

/**
 * Performs all necessary steps to make a CallExpression instantiate a template: 1. Set
 * TemplateInstantiation Edge from CallExpression to Template 2. Set Invokes Edge to all
 * realizations of the Template 3. Set return type of the CallExpression and checks if it uses a
 * ParameterizedType and therefore has to be instantiated 4. Set Template Parameters Edge from the
 * CallExpression to all Instantiation Values 5. Set DFG Edges from instantiation to
 * ParamVariableDeclaration in TemplateDeclaration
 *
 * @param templateCall call to instantiate and invoke a function template
 * @param functionTemplateDeclaration functionTemplate we have identified that should be
 * instantiated
 * @param function FunctionDeclaration representing the realization of the template
 * @param initializationSignature mapping containing the all elements of the signature of the
 * TemplateDeclaration as key and the Type/Expression the Parameter is initialized with.
 * @param initializationType mapping of the instantiation value to the instantiation type (depends
 * on resolution [TemplateDeclaration.TemplateInitialization]
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 */
fun applyTemplateInstantiation(
    templateCall: CallExpression,
    functionTemplateDeclaration: FunctionTemplateDeclaration?,
    function: FunctionDeclaration,
    initializationSignature: Map<Declaration?, Node?>,
    initializationType: Map<Node?, TemplateDeclaration.TemplateInitialization?>,
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
            (initializationSignature[parameterizedTypeResolution[returnType]] as TypeExpression?)
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
 * Computes the implicit casts that are necessary to reach the
 *
 * @param callSignature signature of the call we want to find invocation targets for by performing
 * implicit casts
 * @param arguments arguments of the call
 * @param functionSignature Types of the signature of the possible invocation candidate
 * @return List containing either null on the i-th position (if the type of i-th argument of the
 * call equals the type of the i-th argument of the FunctionDeclaration) or a CastExpression on the
 * i-th position (if the argument of the call can be cast to match the type of the argument at the
 * i-th position of the FunctionDeclaration). If the list is empty the signature of the
 * FunctionDeclaration cannot be reached through implicit casts
 */
fun signatureWithImplicitCastTransformation(
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
 * Gets all ParameterizedTypes from the initialization signature
 *
 * @param initialization mapping of the declaration of the template parameters to the explicit
 * values the template is instantiated with
 * @return mapping of the parameterized types to the corresponding TypeParamDeclaration in the
 * template
 */
fun getParameterizedSignaturesFromInitialization(
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
 * Creates a Mapping between the Parameters of the TemplateDeclaration and the Values provided * for
 * the instantiation of the template.
 *
 * The difference to [constructTemplateInitializationSignatureFromTemplateParameters] is that this
 * one also takes into account defaults and auto deductions
 *
 * Additionally, it fills the maps and lists mentioned below:
 *
 * @param functionTemplateDeclaration functionTemplate we have identified that should be
 * instantiated
 * @param templateCall callExpression that instantiates the template
 * @param instantiationType mapping of the instantiation value to the instantiation type (depends on
 * resolution [TemplateDeclaration.TemplateInitialization]
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
 * @return mapping containing the all elements of the signature of the TemplateDeclaration as key
 * and the Type/Expression the Parameter is initialized with. This function returns null if the
 * {ParamVariableDeclaration, TypeParamDeclaration} do not match the provided value for
 * initialization -&gt; initialization not possible
 */
fun getTemplateInitializationSignature(
    functionTemplateDeclaration: FunctionTemplateDeclaration,
    templateCall: CallExpression,
    instantiationType: MutableMap<Node?, TemplateDeclaration.TemplateInitialization?>,
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
        val typeExpression =
            NodeBuilder.newTypeExpression(deducedType.name, deducedType, templateCall.language)
        typeExpression.isImplicit = true
        if (
            currentArgumentType is ParameterizedType &&
                (signature[parameterizedTypeResolution[currentArgumentType]] == null ||
                    (instantiationType[
                        signature[parameterizedTypeResolution[currentArgumentType]]] ==
                        TemplateDeclaration.TemplateInitialization.DEFAULT))
        ) {
            signature[parameterizedTypeResolution[currentArgumentType]] = typeExpression
            instantiationType[typeExpression] =
                TemplateDeclaration.TemplateInitialization.AUTO_DEDUCTION
        }
    }
    return signature
}

/**
 * Creates a Mapping between the Parameters of the TemplateDeclaration and the Values provided for
 * the instantiation of the template (Only the ones that are in defined in the instantiation => no
 * defaults or implicit). Additionally, it fills the maps and lists mentioned below:
 *
 * @param functionTemplateDeclaration functionTemplate we have identified that should be
 * instantiated
 * @param templateCall callExpression that instantiates the template
 * @param instantiationType mapping of the instantiation value to the instantiation type (depends
 * * on resolution [TemplateDeclaration.TemplateInitialization]
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
 * @return mapping containing the all elements of the signature of the TemplateDeclaration as key
 * and the Type/Expression the Parameter is initialized with. This function returns null if the
 * {ParamVariableDeclaration, TypeParamDeclaration} do not match the provided value for
 * initialization -&gt; initialization not possible
 */
fun constructTemplateInitializationSignatureFromTemplateParameters(
    functionTemplateDeclaration: FunctionTemplateDeclaration,
    templateCall: CallExpression,
    instantiationType: MutableMap<Node?, TemplateDeclaration.TemplateInitialization?>,
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
                instantiationType[callParameter] =
                    TemplateDeclaration.TemplateInitialization.EXPLICIT
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
 * Checks if the provided call parameter can instantiate the required template parameter
 *
 * @param callParameterArg
 * @param templateParameter
 * @return If the TemplateParameter is an TypeParamDeclaration, the callParameter must be an
 * ObjectType => returns true If the TemplateParameter is a ParamVariableDeclaration, the
 * callParameterArg must be an Expression and its type must match the type of the
 * ParamVariableDeclaration (same type or subtype) => returns true Otherwise return false
 */
fun isInstantiated(callParameterArg: Node, templateParameter: Declaration?): Boolean {
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
 * Check if we are handling an implicit template parameter, if so set instantiationSignature,
 * instantiationType and orderedInitializationSignature maps accordingly
 *
 * @param functionTemplateDeclaration functionTemplate we have identified
 * @param index position of the templateParameter we are currently handling
 * @param instantiationSignature mapping of the Declaration representing a template parameter to the
 * value that initializes that template parameter
 * @param instantiationType mapping of the instantiation value to the instantiation type (depends on
 * resolution [TemplateDeclaration.TemplateInitialization]
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 */
fun handleImplicitTemplateParameter(
    functionTemplateDeclaration: FunctionTemplateDeclaration,
    index: Int,
    instantiationSignature: MutableMap<Declaration?, Node?>,
    instantiationType: MutableMap<Node?, TemplateDeclaration.TemplateInitialization?>,
    orderedInitializationSignature: MutableMap<Declaration, Int>
) {
    if ((functionTemplateDeclaration.parameters[index] as HasDefault<*>).default != null) {
        // If we have a default we fill it in
        var defaultNode = (functionTemplateDeclaration.parameters[index] as HasDefault<*>).default
        if (defaultNode is Type) {
            defaultNode =
                NodeBuilder.newTypeExpression(
                    defaultNode.name,
                    defaultNode,
                    functionTemplateDeclaration.language
                )
            defaultNode.isImplicit = true
        }
        instantiationSignature[functionTemplateDeclaration.parameters[index]] = defaultNode
        instantiationType[defaultNode] = TemplateDeclaration.TemplateInitialization.DEFAULT
        orderedInitializationSignature[functionTemplateDeclaration.parameters[index]] = index
    } else {
        // If there is no default, we don't have information on the parameter -> check
        // auto-deduction
        instantiationSignature[functionTemplateDeclaration.parameters[index]] = null
        instantiationType[null] = TemplateDeclaration.TemplateInitialization.UNKNOWN
        orderedInitializationSignature[functionTemplateDeclaration.parameters[index]] = index
    }
}

/**
 * @param function FunctionDeclaration realization of the template
 * @param parameterizedTypeResolution mapping of ParameterizedTypes to the TypeParamDeclarations
 * that define them, used to backwards resolve
 * @param initializationSignature mapping between the ParamDeclaration of the template and the
 * corresponding instantiations
 * @return List of Types representing the Signature of the FunctionDeclaration, but
 * ParameterizedTypes (which depend on the specific instantiation of the template) are resolved to
 * the values the Template is instantiated with.
 */
fun getCallSignature(
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

/**
 * @param functionDeclaration FunctionDeclaration realization of the template
 * @param functionDeclarationSignature Signature of the realization FunctionDeclaration, but
 * replacing the ParameterizedTypes with the ones provided in the instantiation
 * @param templateCallExpression CallExpression that instantiates the template
 * @param explicitInstantiation list of the explicitly instantiated type parameters
 * @return true if the instantiation of the template is compatible with the template declaration,
 * false otherwise
 */
fun checkArgumentValidity(
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
