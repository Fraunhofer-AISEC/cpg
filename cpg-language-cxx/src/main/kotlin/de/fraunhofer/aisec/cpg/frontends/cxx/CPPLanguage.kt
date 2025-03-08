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

import de.fraunhofer.aisec.cpg.CallResolutionResult
import de.fraunhofer.aisec.cpg.SignatureMatches
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.primitiveType
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.matchesSignature
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The C++ language. */
open class CPPLanguage(ctx: TranslationContext) :
    CLanguage(ctx),
    HasDefaultArguments,
    HasTemplates,
    HasStructs,
    HasClasses,
    HasUnknownType,
    HasFunctionStyleCasts,
    HasFunctionOverloading,
    HasOperatorOverloading,
    HasImplicitReceiver {
    override val fileExtensions = listOf("cpp", "cc", "cxx", "c++", "hpp", "hh")
    override val elaboratedTypeSpecifier = listOf("class", "struct", "union", "enum")
    override val unknownTypeString = listOf("auto")

    @Transient
    override val overloadedOperatorNames:
        Map<Pair<KClass<out HasOverloadedOperation>, String>, Symbol> =
        mapOf(
            // Arithmetic operators. See
            // https://en.cppreference.com/w/cpp/language/operator_arithmetic
            UnaryOperator::class of "+" to "operator+",
            UnaryOperator::class of "-" to "operator-",
            BinaryOperator::class of "+" to "operator+",
            BinaryOperator::class of "-" to "operator-",
            BinaryOperator::class of "*" to "operator*",
            BinaryOperator::class of "/" to "operator/",
            BinaryOperator::class of "%" to "operator%",
            UnaryOperator::class of "~" to "operator~",
            BinaryOperator::class of "&" to "operator&",
            BinaryOperator::class of "|" to "operator|",
            BinaryOperator::class of "^" to "operator^",
            BinaryOperator::class of "<<" to "operator<<",
            BinaryOperator::class of ">>" to "operator>>",

            // Increment/decrement operators. See
            // https://en.cppreference.com/w/cpp/language/operator_incdec
            UnaryOperator::class of "++" to "operator++",
            UnaryOperator::class of "--" to "operator--",

            // Comparison operators. See
            // https://en.cppreference.com/w/cpp/language/operator_comparison
            BinaryOperator::class of "==" to "operator==",
            BinaryOperator::class of "!=" to "operator!=",
            BinaryOperator::class of "<" to "operator<",
            BinaryOperator::class of ">" to "operator>",
            BinaryOperator::class of "<=" to "operator<=",
            BinaryOperator::class of "=>" to "operator=>",

            // Member access operators. See
            // https://en.cppreference.com/w/cpp/language/operator_member_access
            MemberExpression::class of "[]" to "operator[]",
            UnaryOperator::class of "*" to "operator*",
            UnaryOperator::class of "&" to "operator&",
            MemberExpression::class of "->" to "operator->",
            MemberExpression::class of "->*" to "operator->*",

            // Other operators. See https://en.cppreference.com/w/cpp/language/operator_other
            MemberCallExpression::class of "()" to "operator()",
            BinaryOperator::class of "," to "operator,",
        )

    /**
     * The list of built-in types. See https://en.cppreference.com/w/cpp/language/types for a
     * reference. We only list equivalent types here and use the canonical form of integer values.
     */
    @Transient
    override val builtInTypes =
        mapOf(
            // Integer types
            "short int" to IntegerType(ctx, "short int", 16, this, NumericType.Modifier.SIGNED),
            "unsigned short int" to
                IntegerType(ctx, "unsigned short int", 16, this, NumericType.Modifier.UNSIGNED),
            "int" to IntegerType(ctx, "int", 32, this, NumericType.Modifier.SIGNED),
            "unsigned int" to
                IntegerType(ctx, "unsigned int", 32, this, NumericType.Modifier.UNSIGNED),
            "long int" to IntegerType(ctx, "long int", 64, this, NumericType.Modifier.SIGNED),
            "unsigned long int" to
                IntegerType(ctx, "unsigned long int", 64, this, NumericType.Modifier.UNSIGNED),
            "long long int" to
                IntegerType(ctx, "long long int", 64, this, NumericType.Modifier.SIGNED),
            "unsigned long long int" to
                IntegerType(ctx, "unsigned long long int", 64, this, NumericType.Modifier.UNSIGNED),

            // Boolean type
            "bool" to BooleanType(ctx, "bool", language = this),

            // Character types
            "signed char" to IntegerType(ctx, "signed char", 8, this, NumericType.Modifier.SIGNED),
            "unsigned char" to
                IntegerType(ctx, "unsigned char", 8, this, NumericType.Modifier.UNSIGNED),
            "char" to IntegerType(ctx, "char", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "wchar_t" to IntegerType(ctx, "wchar_t", 32, this, NumericType.Modifier.NOT_APPLICABLE),
            "char8_t" to IntegerType(ctx, "char8_t", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "char16_t" to
                IntegerType(ctx, "char16_t", 16, this, NumericType.Modifier.NOT_APPLICABLE),
            "char32_t" to
                IntegerType(ctx, "char32_t", 32, this, NumericType.Modifier.NOT_APPLICABLE),

            // Floating-point types
            "float" to FloatingPointType(ctx, "float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType(ctx, "double", 64, this, NumericType.Modifier.SIGNED),
            "long double" to
                FloatingPointType(ctx, "long double", 128, this, NumericType.Modifier.SIGNED),

            // Convenience types, defined in headers. They are not part of the language per se, but
            // part of the standard library. We therefore also consider them to be "built-in" types,
            // because we often don't parse all the headers which define them internally.
            "std::string" to StringType(ctx, "std::string", this),
            "int8_t" to IntegerType(ctx, "int8_t", 8, this, NumericType.Modifier.SIGNED),
            "int16_t" to IntegerType(ctx, "int16_t", 16, this, NumericType.Modifier.SIGNED),
            "int32_t" to IntegerType(ctx, "int32_t", 32, this, NumericType.Modifier.SIGNED),
            "int64_t" to IntegerType(ctx, "int64_t", 64, this, NumericType.Modifier.SIGNED),
            "uint8_t" to IntegerType(ctx, "uint8_t", 8, this, NumericType.Modifier.UNSIGNED),
            "uint16_t" to IntegerType(ctx, "uint16_t", 16, this, NumericType.Modifier.UNSIGNED),
            "uint32_t" to IntegerType(ctx, "uint32_t", 32, this, NumericType.Modifier.UNSIGNED),
            "uint64_t" to IntegerType(ctx, "uint64_t", 64, this, NumericType.Modifier.UNSIGNED),

            // Other commonly used extension types
            "__int128" to IntegerType(ctx, "__int128", 128, this, NumericType.Modifier.SIGNED),
        )

    override fun tryCast(
        type: Type,
        targetType: Type,
        hint: HasType?,
        targetHint: HasType?,
    ): CastResult {
        val match = super.tryCast(type, targetType, hint, targetHint)
        if (match != CastNotPossible) {
            return match
        }

        // Another special rule is that if we have a (const) reference (e.g. const T&) in a function
        // call, this will match the type T because this means that the parameter is given by
        // reference rather than by value.
        if (
            targetType is ReferenceType &&
                targetType.elementType == type &&
                targetHint is ParameterDeclaration
        ) {
            return DirectMatch
        }

        // In C++, it is possible to have conversion constructors. We will not have full support for
        // them yet, but at least we should have some common cases here, such as const char* to
        // std::string
        if (
            type is PointerType &&
                type.elementType.typeName == "char" &&
                targetType.typeName == "std::string"
        ) {
            return DirectMatch
        }

        return CastNotPossible
    }

    override fun bestViableResolution(
        result: CallResolutionResult
    ): Pair<Set<FunctionDeclaration>, CallResolutionResult.SuccessKind> {
        // There is a sort of weird workaround in C++ to select a prefix vs. postfix operator for
        // increment and decrement operators. See
        // https://en.cppreference.com/w/cpp/language/operator_incdec. If it is a postfix, we need
        // to match for a function with a fake "int" parameter
        val expr = result.source
        if (
            expr is UnaryOperator &&
                (expr.operatorCode == "++" || expr.operatorCode == "--") &&
                expr.isPostfix
        ) {
            result.signatureResults =
                result.candidateFunctions
                    .map { Pair(it, it.matchesSignature(listOf(primitiveType("int")))) }
                    .filter { it.second is SignatureMatches }
                    .associate { it }
        }

        return super.bestViableResolution(result)
    }

    override val startCharacter = '<'
    override val endCharacter = '>'

    /**
     * @param curClass class the invoked method must be part of.
     * @param templateCall call to instantiate and invoke a function template
     * @param applyInference if the resolution was unsuccessful and applyInference is true the call
     *   will resolve to an instantiation/invocation of an inferred template
     * @param ctx the [TranslationContext] used
     * @param currentTU The current translation unit
     * @return true if resolution was successful, false if not
     */
    override fun handleTemplateFunctionCalls(
        curClass: RecordDeclaration?,
        templateCall: CallExpression,
        applyInference: Boolean,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration?,
        needsExactMatch: Boolean,
    ): Pair<Boolean, List<FunctionDeclaration>> {
        val instantiationCandidates =
            ctx.scopeManager.lookupSymbolByNodeNameOfType<FunctionTemplateDeclaration>(templateCall)
        for (functionTemplateDeclaration in instantiationCandidates) {
            val initializationType =
                mutableMapOf<Node?, TemplateDeclaration.TemplateInitialization?>()
            val orderedInitializationSignature = mutableMapOf<Declaration, Int>()
            val explicitInstantiation = mutableListOf<ParameterizedType>()
            if (
                (templateCall.templateArguments.size <=
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
                        explicitInstantiation,
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
                                initializationSignature,
                            ),
                            templateCall,
                            explicitInstantiation,
                            needsExactMatch,
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
                            orderedInitializationSignature,
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
                holder?.startInference(ctx)?.inferFunctionTemplate(templateCall)
            templateCall.templateInstantiation = functionTemplateDeclaration
            val edges = templateCall.templateArgumentEdges
            // Set instantiation propertyEdges
            for (edge in edges ?: listOf()) {
                edge.instantiation = TemplateDeclaration.TemplateInitialization.EXPLICIT
            }

            if (functionTemplateDeclaration == null) {
                return Pair(false, listOf())
            }

            return Pair(true, functionTemplateDeclaration.realization)
        }

        return Pair(false, listOf())
    }

    override val receiverName: String
        get() = "this"
}
