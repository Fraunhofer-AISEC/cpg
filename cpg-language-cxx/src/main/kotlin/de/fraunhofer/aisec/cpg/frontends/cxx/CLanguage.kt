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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

const val CONST = "const"

/** The C language. */
open class CLanguage :
    Language<CXXLanguageFrontend>(),
    HasStructs,
    HasFunctionPointers,
    HasQualifier,
    HasElaboratedTypeSpecifier,
    HasShortCircuitOperators,
    HasGlobalVariables,
    HasGlobalFunctions {
    override val fileExtensions = listOf("c", "h")
    override val namespaceDelimiter = "::"
    @DoNotPersist
    override val frontend: KClass<out CXXLanguageFrontend> = CXXLanguageFrontend::class
    override val qualifiers = listOf(CONST, "volatile", "restrict", "atomic")
    override val elaboratedTypeSpecifier = listOf("struct", "union", "enum")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    val unaryOperators = listOf("--", "++", "-", "+", "*", "&", "~")

    /**
     * All operators which perform and assignment and an operation using lhs and rhs. See
     * https://en.cppreference.com/w/c/language/operator_assignment
     */
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^=")

    /**
     * The list of built-in types. See https://en.cppreference.com/w/c/language/arithmetic_types for
     * a reference. We only list equivalent types here and use the canonical form of integer values.
     */
    @DoNotPersist
    @JsonIgnore
    override val builtInTypes: Map<String, Type> =
        mapOf(
            // Integer types
            "char" to IntegerType("char", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "signed char" to IntegerType("signed char", 8, this, NumericType.Modifier.SIGNED),
            "unsigned char" to IntegerType("unsigned char", 8, this, NumericType.Modifier.UNSIGNED),
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

            // Floating-point types
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "long double" to
                FloatingPointType("long double", 128, this, NumericType.Modifier.SIGNED),

            // Convenience types, defined in headers such as <stddef.h> or <stdint.h>. They are not
            // part of the language per se, but part of the standard library. We therefore also
            // consider them to be "built-in" types, because we often don't parse all the headers
            // which define them internally.
            "bool" to IntegerType("bool", 1, this, NumericType.Modifier.SIGNED),
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

        // Numeric types can be cast implicitly
        if (type is NumericType && targetType is NumericType) {
            return ImplicitCast
        }

        // As a special rule, a non-nested pointer and array of the same type are completely
        // interchangeable
        if (type.root == targetType.root && type is PointerType && targetType is PointerType) {
            return ImplicitCast
        }

        return CastNotPossible
    }
}
