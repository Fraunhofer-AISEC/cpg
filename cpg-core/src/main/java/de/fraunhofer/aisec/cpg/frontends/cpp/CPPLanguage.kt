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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.inference.inferFunction
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.util.regex.Pattern

/** The C++ language. */
class CPPLanguage :
    CLanguage(), HasDefaultArguments, HasTemplates, HasComplexCallResolution, HasClasses {
    override val fileExtensions = listOf("cpp", "cc", "cxx", "hpp", "hh")
    override val elaboratedTypeSpecifier = listOf("class", "struct", "union", "enum")

    /**
     * @param call
     * @return FunctionDeclarations that are invocation candidates for the MethodCall call using C++
     * resolution techniques
     */
    override fun refineMethodCallResolution(
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>,
        call: CallExpression,
        scopeManager: ScopeManager,
        currentTU: TranslationUnitDeclaration,
        callResolver: CallResolver
    ): List<FunctionDeclaration> {
        var invocationCandidates = mutableListOf<FunctionDeclaration>()
        val records =
            possibleContainingTypes.mapNotNull { callResolver.recordMap[it.root.name] }.toSet()
        for (record in records) {
            invocationCandidates.addAll(
                callResolver.getInvocationCandidatesFromRecord(record, call.name.localName, call)
            )
        }
        if (invocationCandidates.isEmpty()) {
            // Check for usage of default args
            invocationCandidates.addAll(resolveWithDefaultArgsFunc(call, scopeManager))
        }
        if (invocationCandidates.isEmpty()) {
            if (handleTemplateFunctionCalls(curClass, call, false, scopeManager, currentTU)) {
                return call.invokes
            } else {
                call.templateParametersEdges = null
            }
        }
        if (invocationCandidates.isEmpty()) {
            // Check for usage of implicit cast
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call, scopeManager))
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

    override fun refineInvocationCandidatesFromRecord(
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

    override fun refineNormalCallResolution(
        call: CallExpression,
        scopeManager: ScopeManager,
        currentTU: TranslationUnitDeclaration
    ) {

        val invocationCandidates =
            scopeManager
                .resolveFunctionStopScopeTraversalOnDefinition(call)
                .filter { it.hasSignature(call.signature) }
                .toMutableList()
        if (invocationCandidates.isEmpty()) {
            // Check for usage of default args
            invocationCandidates.addAll(resolveWithDefaultArgsFunc(call, scopeManager))
        }
        if (invocationCandidates.isEmpty()) {
            /*
             Check if the call can be resolved to a function template instantiation. If it can be resolver, we
             resolve the call. Otherwise, there won't be an inferred template, we will do an inferred
             FunctionDeclaration instead.
            */
            call.templateParametersEdges = mutableListOf()
            if (handleTemplateFunctionCalls(null, call, false, scopeManager, currentTU)) {
                return
            } else {
                call.templateParametersEdges = null
            }
        }
        if (invocationCandidates.isEmpty()) {
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a candidate with an implicit cast
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call, scopeManager))
        }

        if (invocationCandidates.isEmpty()) {
            // If we still have no candidates and our current language is c++ we create an inferred
            // FunctionDeclaration
            invocationCandidates.add(currentTU.inferFunction(call))
        }

        call.invokes = invocationCandidates
    }

    /**
     * @param call we want to find invocation targets for by adding the default arguments to the
     * signature
     * @param scopeManager the scope manager used
     * @return list of invocation candidates that have matching signature when considering default
     * arguments
     */
    private fun resolveWithDefaultArgsFunc(
        call: CallExpression,
        scopeManager: ScopeManager
    ): List<FunctionDeclaration> {
        val invocationCandidates =
            scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).filter {
                call.signature.size < it.signatureTypes.size
            }
        return resolveWithDefaultArgs(call, invocationCandidates)
    }

    /**
     * @param curClass class the invoked method must be part of.
     * @param templateCall call to instantiate and invoke a function template
     * @param applyInference if the resolution was unsuccessful and applyInference is true the call
     * will resolve to an instantiation/invocation of an inferred template
     * @param scopeManager the scope manager used
     * @param currentTU The current translation unit
     * @return true if resolution was successful, false if not
     */
    override fun handleTemplateFunctionCalls(
        curClass: RecordDeclaration?,
        templateCall: CallExpression,
        applyInference: Boolean,
        scopeManager: ScopeManager,
        currentTU: TranslationUnitDeclaration
    ): Boolean {
        val instantiationCandidates = scopeManager.resolveFunctionTemplateDeclaration(templateCall)
        for (functionTemplateDeclaration in instantiationCandidates) {
            val initializationType =
                mutableMapOf<Node?, TemplateDeclaration.TemplateInitialization?>()
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
            val holder = curClass ?: currentTU

            // If we want to use an inferred functionTemplateDeclaration, this needs to be provided.
            // Otherwise, we could not resolve to a template and no modifications are made
            val functionTemplateDeclaration =
                holder.startInference().createInferredFunctionTemplate(templateCall)
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
     * @param call we want to find invocation targets for by performing implicit casts
     * @param scopeManager the scope manager used
     * @return list of invocation candidates by applying implicit casts
     */
    private fun resolveWithImplicitCastFunc(
        call: CallExpression,
        scopeManager: ScopeManager
    ): List<FunctionDeclaration> {
        val initialInvocationCandidates =
            listOf(*scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).toTypedArray())
        return resolveWithImplicitCast(call, initialInvocationCandidates)
    }
}
