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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.CallResolver
import de.fraunhofer.aisec.cpg.passes.resolveWithImplicitCast
import java.util.regex.Pattern
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The C language. */
open class CLanguage :
    Language<CXXLanguageFrontend>(),
    HasComplexCallResolution,
    HasStructs,
    HasFunctionPointers,
    HasQualifier,
    HasElaboratedTypeSpecifier,
    HasShortCircuitOperators {
    override val fileExtensions = listOf("c", "h")
    override val namespaceDelimiter = "::"
    @Transient override val frontend: KClass<out CXXLanguageFrontend> = CXXLanguageFrontend::class
    override val qualifiers = listOf("const", "volatile", "restrict", "atomic")
    override val elaboratedTypeSpecifier = listOf("struct", "union", "enum")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    /**
     * All operators which perform and assignment and an operation using lhs and rhs. See
     * https://en.cppreference.com/w/c/language/operator_assignment
     */
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^=")

    @Transient
    @JsonIgnore
    override val builtInTypes: Map<String, Type> =
        mapOf(
            "boolean" to IntegerType("boolean", 1, this, NumericType.Modifier.SIGNED),
            "char" to IntegerType("char", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "byte" to IntegerType("byte", 8, this, NumericType.Modifier.SIGNED),
            "short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "short int" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "long int" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "long long" to IntegerType("long long int", 64, this, NumericType.Modifier.SIGNED),
            "long long int" to IntegerType("long long int", 64, this, NumericType.Modifier.SIGNED),
            "signed char" to IntegerType("signed char", 8, this, NumericType.Modifier.SIGNED),
            "signed byte" to IntegerType("byte", 8, this, NumericType.Modifier.SIGNED),
            "signed short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "signed short int" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "signed" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "signed int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "signed long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "signed long int" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "signed long long" to
                IntegerType("long long int", 64, this, NumericType.Modifier.SIGNED),
            "signed long long int" to
                IntegerType("long long int", 64, this, NumericType.Modifier.SIGNED),
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "unsigned char" to IntegerType("unsigned char", 8, this, NumericType.Modifier.UNSIGNED),
            "unsigned byte" to IntegerType("unsigned byte", 8, this, NumericType.Modifier.UNSIGNED),
            "unsigned short" to
                IntegerType("unsigned short", 16, this, NumericType.Modifier.UNSIGNED),
            "unsigned short int" to
                IntegerType("unsigned short", 16, this, NumericType.Modifier.UNSIGNED),
            "unsigned" to IntegerType("unsigned int", 32, this, NumericType.Modifier.UNSIGNED),
            "unsigned int" to IntegerType("unsigned int", 32, this, NumericType.Modifier.UNSIGNED),
            "unsigned long" to
                IntegerType("unsigned long", 64, this, NumericType.Modifier.UNSIGNED),
            "unsigned long int" to
                IntegerType("unsigned long", 64, this, NumericType.Modifier.UNSIGNED),
            "unsigned long long" to
                IntegerType("unsigned long long int", 64, this, NumericType.Modifier.UNSIGNED),
            "unsigned long long int" to
                IntegerType("unsigned long long int", 64, this, NumericType.Modifier.UNSIGNED)
        )

    override fun refineNormalCallResolution(
        call: CallExpression,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration
    ): List<FunctionDeclaration> {
        val invocationCandidates = ctx.scopeManager.resolveFunction(call).toMutableList()
        if (invocationCandidates.isEmpty()) {
            // Check for implicit casts
            invocationCandidates.addAll(resolveWithImplicitCastFunc(call, ctx))
        }
        return invocationCandidates
    }

    override fun refineMethodCallResolution(
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>,
        call: CallExpression,
        ctx: TranslationContext,
        currentTU: TranslationUnitDeclaration,
        callResolver: CallResolver
    ): List<FunctionDeclaration> = emptyList()

    override fun refineInvocationCandidatesFromRecord(
        recordDeclaration: RecordDeclaration,
        call: CallExpression,
        namePattern: Pattern,
        ctx: TranslationContext
    ): List<FunctionDeclaration> = emptyList()

    /**
     * @param call we want to find invocation targets for by performing implicit casts
     * @param scopeManager the scope manager used
     * @return list of invocation candidates by applying implicit casts
     */
    protected fun resolveWithImplicitCastFunc(
        call: CallExpression,
        ctx: TranslationContext,
    ): List<FunctionDeclaration> {
        val initialInvocationCandidates =
            listOf(
                *ctx.scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).toTypedArray()
            )
        return resolveWithImplicitCast(call, initialInvocationCandidates, ctx)
    }
}
