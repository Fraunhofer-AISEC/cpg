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

import de.fraunhofer.aisec.cpg.frontends.CastNotPossible
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.tryCast
import java.util.HashMap
import java.util.regex.Pattern

/**
 * In C++ if there is a method that matches the name we are looking for, we have to stop searching
 * in the parents even if the signature of the method does not match
 *
 * @param recordDeclaration
 * @param name
 * @return true if there is no method in the recordDeclaration where the name of the method matches
 *   with the provided name. false otherwise
 */
fun shouldContinueSearchInParent(recordDeclaration: RecordDeclaration?, name: String?): Boolean {
    val namePattern =
        Pattern.compile(
            "(" + Pattern.quote(recordDeclaration?.name.toString()) + "\\.)?" + Pattern.quote(name)
        )
    val invocationCandidate =
        recordDeclaration.methods.filter { namePattern.matcher(it.name.toString()).matches() }
    return invocationCandidate.isEmpty()
}

/**
 * Performs all necessary steps to make a CallExpression instantiate a template: 1. Set
 * TemplateInstantiation Edge from CallExpression to Template 2. Set Invokes Edge to all
 * realizations of the Template 3. Set return type of the CallExpression and checks if it uses a
 * ParameterizedType and therefore has to be instantiated 4. Set Template Parameters Edge from the
 * CallExpression to all Instantiation Values 5. Set DFG Edges from instantiation to
 * ParameterDeclaration in TemplateDeclaration
 *
 * @param templateCall call to instantiate and invoke a function template
 * @param functionTemplateDeclaration functionTemplate we have identified that should be
 *   instantiated
 * @param function FunctionDeclaration representing the realization of the template
 * @param initializationSignature mapping containing the all elements of the signature of the
 *   TemplateDeclaration as key and the Type/Expression the Parameter is initialized with.
 * @param initializationType mapping of the instantiation value to the instantiation type (depends
 *   on resolution [TemplateDeclaration.TemplateInitialization]
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 */
fun applyTemplateInstantiation(
    templateCall: CallExpression,
    functionTemplateDeclaration: FunctionTemplateDeclaration?,
    function: FunctionDeclaration,
    initializationSignature: Map<Declaration?, Node?>,
    initializationType: Map<Node?, TemplateDeclaration.TemplateInitialization?>,
    orderedInitializationSignature: Map<Declaration, Int>
): List<FunctionDeclaration> {
    val templateInstantiationParameters =
        mutableListOf<Node>(*orderedInitializationSignature.keys.toTypedArray())
    for ((key, value) in orderedInitializationSignature) {
        initializationSignature[key]?.let { templateInstantiationParameters[value] = it }
    }
    templateCall.templateInstantiation = functionTemplateDeclaration

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
    returnType?.let { templateCall.type = it }
    templateCall.updateTemplateParameters(initializationType, templateInstantiationParameters)

    // Apply changes to the call signature
    val templateFunctionSignature =
        getCallSignature(function, parameterizedTypeResolution, initializationSignature)
    val templateCallSignature = templateCall.signature
    val callSignatureImplicit =
        signatureWithImplicitCastTransformation(
            templateCall,
            templateCallSignature,
            templateCall.arguments,
            templateFunctionSignature,
        )
    for (i in callSignatureImplicit.indices) {
        val cast = callSignatureImplicit[i]
        if (cast != null) {
            templateCall.setArgument(i, cast)
        }
    }

    // Add DFG edges from the instantiation Expression to the ParameterDeclaration in the
    // Template.
    for ((declaration) in initializationSignature) {
        if (declaration is ParameterDeclaration) {
            initializationSignature[declaration]?.let { declaration.addPrevDFG(it) }
        }
    }

    return listOf(function)
}

/**
 * Computes the implicit casts that are necessary to reach the
 *
 * @param callSignature signature of the call we want to find invocation targets for by performing
 *   implicit casts
 * @param arguments arguments of the call
 * @param functionSignature Types of the signature of the possible invocation candidate
 * @return List containing either null on the i-th position (if the type of i-th argument of the
 *   call equals the type of the i-th argument of the FunctionDeclaration) or a CastExpression on
 *   the i-th position (if the argument of the call can be cast to match the type of the argument at
 *   the i-th position of the FunctionDeclaration). If the list is empty the signature of the
 *   FunctionDeclaration cannot be reached through implicit casts
 */
fun signatureWithImplicitCastTransformation(
    call: CallExpression,
    callSignature: List<Type?>,
    arguments: List<Expression>,
    functionSignature: List<Type>,
): MutableList<CastExpression?> {
    val implicitCasts = mutableListOf<CastExpression?>()
    if (callSignature.size != functionSignature.size) return implicitCasts

    for (i in callSignature.indices) {
        val callType = callSignature[i]
        val funcType = functionSignature[i]
        if (callType?.isPrimitive == true && funcType.isPrimitive && callType != funcType) {
            val implicitCast = CastExpression()
            implicitCast.ctx = call.ctx
            implicitCast.isImplicit = true
            implicitCast.castType = funcType
            implicitCast.language = funcType.language
            implicitCast.expression = arguments[i]
            implicitCasts.add(implicitCast)
        } else {
            // If no cast is needed we add null to be able to access the function signature
            // list and the implicit cast list with the same index.
            implicitCasts.add(null)
        }
    }
    return implicitCasts
}

/**
 * Gets all ParameterizedTypes from the initialization signature
 *
 * @param initialization mapping of the declaration of the template parameters to the explicit
 *   values the template is instantiated with
 * @return mapping of the parameterized types to the corresponding TypeParameterDeclaration in the
 *   template
 */
fun getParameterizedSignaturesFromInitialization(
    initialization: Map<Declaration?, Node?>
): Map<ParameterizedType, TypeParameterDeclaration> {
    val parameterizedSignature: MutableMap<ParameterizedType, TypeParameterDeclaration> = HashMap()
    for (templateParam in initialization.keys) {
        if (templateParam is TypeParameterDeclaration) {
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
 *   instantiated
 * @param templateCall callExpression that instantiates the template
 * @param instantiationType mapping of the instantiation value to the instantiation type (depends on
 *   resolution [TemplateDeclaration.TemplateInitialization]
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
 * @return mapping containing the all elements of the signature of the TemplateDeclaration as key
 *   and the Type/Expression the Parameter is initialized with. This function returns null if the
 *   {ParameterDeclaration, TypeParameterDeclaration} do not match the provided value for
 *   initialization -&gt; initialization not possible
 */
fun getTemplateInitializationSignature(
    functionTemplateDeclaration: FunctionTemplateDeclaration,
    templateCall: CallExpression,
    instantiationType: MutableMap<Node?, TemplateDeclaration.TemplateInitialization?>,
    orderedInitializationSignature: MutableMap<Declaration, Int>,
    explicitInstantiated: MutableList<ParameterizedType>
): Map<Declaration?, Node?>? {
    // Construct Signature
    val signature =
        constructTemplateInitializationSignatureFromTemplateParameters(
            functionTemplateDeclaration,
            templateCall,
            instantiationType,
            orderedInitializationSignature,
            explicitInstantiated
        ) ?: return null
    val parameterizedTypeResolution = getParameterizedSignaturesFromInitialization(signature)

    // Check for unresolved Parameters and try to deduce Type by looking at call arguments
    for (i in templateCall.arguments.indices) {
        val functionDeclaration = functionTemplateDeclaration.realization[0]
        val currentArgumentType =
            functionDeclaration.parameters[i]
                .type // TODO: Somehow, this should be the ParametrizedType but it's an ObjectType
        // with the same name. => The template logic fails.
        val deducedType = templateCall.arguments[i].type
        val typeExpression = templateCall.newTypeExpression(deducedType.name, deducedType)
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
 *   instantiated
 * @param templateCall callExpression that instantiates the template
 * @param instantiationType mapping of the instantiation value to the instantiation type (depends
 * * on resolution [TemplateDeclaration.TemplateInitialization]
 *
 * @param orderedInitializationSignature mapping of the ordering of the template parameters
 * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
 * @return mapping containing the all elements of the signature of the TemplateDeclaration as key
 *   and the Type/Expression the Parameter is initialized with. This function returns null if the
 *   {ParameterDeclaration, TypeParameterDeclaration} do not match the provided value for
 *   initialization -&gt; initialization not possible
 */
fun constructTemplateInitializationSignatureFromTemplateParameters(
    functionTemplateDeclaration: FunctionTemplateDeclaration,
    templateCall: CallExpression,
    instantiationType: MutableMap<Node?, TemplateDeclaration.TemplateInitialization?>,
    orderedInitializationSignature: MutableMap<Declaration, Int>,
    explicitInstantiated: MutableList<ParameterizedType>
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
                if (templateParameter is TypeParameterDeclaration) {
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
 * @return If the TemplateParameter is an TypeParameterDeclaration, the callParameter must be an
 *   ObjectType => returns true If the TemplateParameter is a ParameterDeclaration, the
 *   callParameterArg must be an Expression and its type must match the type of the
 *   ParameterDeclaration (same type or subtype) => returns true Otherwise return false
 */
fun isInstantiated(callParameterArg: Node, templateParameter: Declaration?): Boolean {
    var callParameter = callParameterArg
    if (callParameter is TypeExpression) {
        callParameter = callParameter.type
    }
    return if (callParameter is Type && templateParameter is TypeParameterDeclaration) {
        callParameter is ObjectType
    } else if (callParameter is Expression && templateParameter is ParameterDeclaration) {
        callParameter.type == templateParameter.type ||
            callParameter.type.tryCast(templateParameter.type) != CastNotPossible
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
 *   value that initializes that template parameter
 * @param instantiationType mapping of the instantiation value to the instantiation type (depends on
 *   resolution [TemplateDeclaration.TemplateInitialization]
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
                functionTemplateDeclaration.newTypeExpression(
                    defaultNode.name,
                    defaultNode,
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
 * @param parameterizedTypeResolution mapping of ParameterizedTypes to the TypeParameterDeclarations
 *   that define them, used to backwards resolve
 * @param initializationSignature mapping between the ParamDeclaration of the template and the
 *   corresponding instantiations
 * @return List of Types representing the Signature of the FunctionDeclaration, but
 *   ParameterizedTypes (which depend on the specific instantiation of the template) are resolved to
 *   the values the Template is instantiated with.
 */
fun getCallSignature(
    function: FunctionDeclaration,
    parameterizedTypeResolution: Map<ParameterizedType, TypeParameterDeclaration>,
    initializationSignature: Map<Declaration?, Node?>
): List<Type> {
    val templateCallSignature = mutableListOf<Type>()
    for (argument in function.parameters) {
        if (argument.type is ParameterizedType) {
            var type: Type = UnknownType.getUnknownType(function.language)
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
 *   replacing the ParameterizedTypes with the ones provided in the instantiation
 * @param templateCallExpression CallExpression that instantiates the template
 * @param explicitInstantiation list of the explicitly instantiated type parameters
 * @return true if the instantiation of the template is compatible with the template declaration,
 *   false otherwise
 */
fun checkArgumentValidity(
    functionDeclaration: FunctionDeclaration,
    functionDeclarationSignature: List<Type>,
    templateCallExpression: CallExpression,
    explicitInstantiation: List<ParameterizedType>
): Boolean {
    if (templateCallExpression.arguments.size <= functionDeclaration.parameters.size) {
        val callArguments =
            mutableListOf<Expression?>(
                *templateCallExpression.arguments.toTypedArray()
            ) // Use provided arguments
        callArguments.addAll(
            functionDeclaration.defaultParameters
                .subList(
                    callArguments.size,
                    functionDeclaration.defaultParameters.size
                ) // TODO: Could be replaced with functionDeclaration.parameters.size
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
