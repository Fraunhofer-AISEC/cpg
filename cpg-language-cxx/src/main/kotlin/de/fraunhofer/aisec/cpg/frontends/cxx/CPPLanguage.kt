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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import org.neo4j.ogm.annotation.Transient

/** The C++ language. */
open class CPPLanguage :
    CLanguage(),
    HasDefaultArguments,
    HasTemplates,
    HasComplexCallResolution,
    HasStructs,
    HasClasses,
    HasUnknownType,
    HasFunctionalCasts {
    override val fileExtensions = listOf("cpp", "cc", "cxx", "hpp", "hh")
    override val elaboratedTypeSpecifier = listOf("class", "struct", "union", "enum")
    override val unknownTypeString = listOf("auto")

    /**
     * The list of built-in types. See https://en.cppreference.com/w/cpp/language/types for a
     * reference. We only list equivalent types here and use the canonical form of integer values.
     */
    @Transient
    override val builtInTypes =
        mapOf(
            // Integer types
            "short int" to IntegerType("short int", 16, this, NumericType.Modifier.SIGNED),
            "unsigned short int" to
                IntegerType("unsigned short int", 16, this, NumericType.Modifier.UNSIGNED),
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "unsigned int" to IntegerType("unsigned int", 32, this, NumericType.Modifier.UNSIGNED),
            "long int" to IntegerType("long int", 64, this, NumericType.Modifier.SIGNED),
            "unsigned long int" to
                IntegerType("unsigned long int", 64, this, NumericType.Modifier.UNSIGNED),
            "long long int" to IntegerType("long long int", 64, this, NumericType.Modifier.SIGNED),
            "unsigned long long int" to
                IntegerType("unsigned long long int", 64, this, NumericType.Modifier.UNSIGNED),

            // Boolean type
            "bool" to BooleanType("bool"),

            // Character types
            "signed char" to IntegerType("signed char", 8, this, NumericType.Modifier.SIGNED),
            "unsigned char" to IntegerType("unsigned char", 8, this, NumericType.Modifier.UNSIGNED),
            "char" to IntegerType("char", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "wchar_t" to IntegerType("wchar_t", 32, this, NumericType.Modifier.NOT_APPLICABLE),
            "char8_t" to IntegerType("char8_t", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "char16_t" to IntegerType("char16_t", 16, this, NumericType.Modifier.NOT_APPLICABLE),
            "char32_t" to IntegerType("char32_t", 32, this, NumericType.Modifier.NOT_APPLICABLE),

            // Floating-point types
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "long double" to
                FloatingPointType("long double", 128, this, NumericType.Modifier.SIGNED),

            // Convenience types, defined in headers. They are not part of the language per se, but
            // part of the standard library. We therefore also consider them to be "built-in" types,
            // because we often don't parse all the headers which define them internally.
            "std::string" to StringType("std::string", this),
            "int8_t" to IntegerType("int8_t", 8, this, NumericType.Modifier.SIGNED),
            "int16_t" to IntegerType("int16_t", 16, this, NumericType.Modifier.SIGNED),
            "int32_t" to IntegerType("int32_t", 32, this, NumericType.Modifier.SIGNED),
            "int64_t" to IntegerType("int64_t", 64, this, NumericType.Modifier.SIGNED),
            "uint8_t" to IntegerType("uint8_t", 8, this, NumericType.Modifier.UNSIGNED),
            "uint16_t" to IntegerType("uint16_t", 16, this, NumericType.Modifier.UNSIGNED),
            "uint32_t" to IntegerType("uint32_t", 32, this, NumericType.Modifier.UNSIGNED),
            "uint64_t" to IntegerType("uint64_t", 64, this, NumericType.Modifier.UNSIGNED),

            // Other commonly used extension types
            "__int128" to IntegerType("__int128", 128, this, NumericType.Modifier.SIGNED),
        )

    /**
     * @param call
     * @return FunctionDeclarations that are invocation candidates for the MethodCall call using C++
     *   resolution techniques
     */
    override fun refineMethodCallResolution(
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>,
        call: CallExpression,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration,
        callResolver: SymbolResolver
    ): List<FunctionDeclaration> {
        var invocationCandidates = mutableListOf<FunctionDeclaration>()
        val records = possibleContainingTypes.mapNotNull { it.root.recordDeclaration }.toSet()
        for (record in records) {
            invocationCandidates.addAll(
                callResolver.getInvocationCandidatesFromRecord(record, call.name.localName, call)
            )
        }
        if (invocationCandidates.isEmpty()) {
            // Check for usage of default args
            invocationCandidates.addAll(resolveWithDefaultArgsFunc(call, ctx))
        }
        if (invocationCandidates.isEmpty()) {
            val (ok, candidates) =
                handleTemplateFunctionCalls(curClass, call, false, ctx, currentTU)
            if (ok) {
                return candidates
            }

            call.templateParameterEdges = null
        }
        if (invocationCandidates.isEmpty()) {
            // Check for usage of implicit cast
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call, ctx))
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
        name: String,
        ctx: TranslationContext
    ): List<FunctionDeclaration> {
        val invocationCandidate =
            mutableListOf<FunctionDeclaration>(
                *recordDeclaration.methods
                    .filter { m -> m.name.lastPartsMatch(name) && m.hasSignature(call.signature) }
                    .toTypedArray()
            )
        if (invocationCandidate.isEmpty()) {
            // Search for possible invocation with defaults args
            invocationCandidate.addAll(
                resolveWithDefaultArgs(
                    call,
                    recordDeclaration.methods.filter { m ->
                        m.name.lastPartsMatch(name) /*&& !m.isImplicit()*/ &&
                            call.signature.size < m.signatureTypes.size
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
                        m.name.lastPartsMatch(name) /*&& !m.isImplicit()*/
                    }
                )
            )
        }
        return invocationCandidate
    }

    override fun refineNormalCallResolution(
        call: CallExpression,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration
    ): List<FunctionDeclaration> {
        val invocationCandidates = ctx.scopeManager.resolveFunction(call).toMutableList()
        if (invocationCandidates.isEmpty()) {
            // Check for usage of default args
            invocationCandidates.addAll(resolveWithDefaultArgsFunc(call, ctx))
        }
        if (invocationCandidates.isEmpty()) {
            // Check if the call can be resolved to a function template instantiation. If it can be
            // resolver, we resolve the call. Otherwise, there won't be an inferred template, we
            // will do an inferred FunctionDeclaration instead.
            call.templateParameterEdges = mutableListOf()
            val (ok, candidates) = handleTemplateFunctionCalls(null, call, false, ctx, currentTU)
            if (ok) {
                return candidates
            }

            call.templateParameterEdges = null
        }
        if (invocationCandidates.isEmpty()) {
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a candidate with an implicit cast
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call, ctx))
        }

        return invocationCandidates
    }

    /**
     * @param call we want to find invocation targets for by adding the default arguments to the
     *   signature
     * @param scopeManager the scope manager used
     * @return list of invocation candidates that have matching signature when considering default
     *   arguments
     */
    private fun resolveWithDefaultArgsFunc(
        call: CallExpression,
        ctx: TranslationContext
    ): List<FunctionDeclaration> {
        val invocationCandidates =
            ctx.scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).filter {
                call.signature.size < it.signatureTypes.size
            }
        return resolveWithDefaultArgs(call, invocationCandidates)
    }

    override val startCharacter = '<'
    override val endCharacter = '>'

    /**
     * @param curClass class the invoked method must be part of.
     * @param templateCall call to instantiate and invoke a function template
     * @param applyInference if the resolution was unsuccessful and applyInference is true the call
     *   will resolve to an instantiation/invocation of an inferred template
     * @param scopeManager the scope manager used
     * @param currentTU The current translation unit
     * @return true if resolution was successful, false if not
     */
    override fun handleTemplateFunctionCalls(
        curClass: RecordDeclaration?,
        templateCall: CallExpression,
        applyInference: Boolean,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration
    ): Pair<Boolean, List<FunctionDeclaration>> {
        val instantiationCandidates =
            ctx.scopeManager.resolveFunctionTemplateDeclaration(templateCall)
        for (functionTemplateDeclaration in instantiationCandidates) {
            val initializationType =
                mutableMapOf<Node?, TemplateDeclaration.TemplateInitialization?>()
            val orderedInitializationSignature = mutableMapOf<Declaration, Int>()
            val explicitInstantiation = mutableListOf<ParameterizedType>()
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
                    val candidates =
                        applyTemplateInstantiation(
                            templateCall,
                            functionTemplateDeclaration,
                            function,
                            initializationSignature,
                            initializationType,
                            orderedInitializationSignature
                        )
                    return Pair(true, candidates)
                }
            }
        }
        if (applyInference) {
            val holder = curClass ?: currentTU

            // If we want to use an inferred functionTemplateDeclaration, this needs to be provided.
            // Otherwise, we could not resolve to a template and no modifications are made
            val functionTemplateDeclaration =
                holder.startInference(ctx)?.inferFunctionTemplate(templateCall)
            templateCall.templateInstantiation = functionTemplateDeclaration
            val edges = templateCall.templateParameterEdges
            // Set instantiation propertyEdges
            for (instantiationParameter in edges ?: listOf()) {
                instantiationParameter.addProperty(
                    Properties.INSTANTIATION,
                    TemplateDeclaration.TemplateInitialization.EXPLICIT
                )
            }

            if (functionTemplateDeclaration == null) {
                return Pair(false, listOf())
            }

            return Pair(true, functionTemplateDeclaration.realization)
        }

        return Pair(false, listOf())
    }
}
