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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.primitiveType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.unknownType
import org.neo4j.ogm.annotation.Transient

/** The Go language. */
class GoLanguage :
    Language<GoLanguageFrontend>(),
    HasShortCircuitOperators,
    HasGenerics,
    HasStructs,
    HasFirstClassFunctions,
    HasAnonymousIdentifier,
    HasFunctionStyleCasts {
    override val fileExtensions = listOf("go")
    override val namespaceDelimiter = "."
    @Transient override val frontend = GoLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")
    override val startCharacter = '['
    override val endCharacter = ']'

    /**
     * All operators which perform and assignment and an operation using lhs and rhs. See
     * https://go.dev/ref/spec#Operators_and_punctuation
     */
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&^=", "&=", "|=", "^=")

    /**
     * Go supports the normal `=` operator, as well as a short assignment operator, which also
     * declares the variable under certain circumstances. But both act as a simple assignment.
     */
    override val simpleAssignmentOperators = setOf("=", ":=")

    /** See [Documentation](https://pkg.go.dev/builtin). */
    @Transient
    override val builtInTypes =
        mapOf(
            // https://pkg.go.dev/builtin#any
            // TODO: Actually, this should be a type alias to interface{}
            "any" to ObjectType("any", listOf(), false, this),
            // https://pkg.go.dev/builtin#error
            // TODO: Actually, this is an interface{ Error() string } type.
            "error" to ObjectType("error", listOf(), false, this),
            // https://pkg.go.dev/builtin#bool
            "bool" to BooleanType("bool", language = this),
            // https://pkg.go.dev/builtin#int
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            // // https://pkg.go.dev/builtin#int8
            "int8" to IntegerType("int8", 8, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#int16
            "int16" to IntegerType("int16", 16, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#int32
            "int32" to IntegerType("int32", 32, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#int64
            "int64" to IntegerType("int64", 64, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#uint
            "uint" to IntegerType("uint", 32, this, NumericType.Modifier.UNSIGNED),
            // https://pkg.go.dev/builtin#uint8
            "uint8" to IntegerType("uint8", 8, this, NumericType.Modifier.UNSIGNED),
            // https://pkg.go.dev/builtin#uint16
            "uint16" to IntegerType("uint16", 16, this, NumericType.Modifier.UNSIGNED),
            // https://pkg.go.dev/builtin#uint32
            "uint32" to IntegerType("uint32", 32, this, NumericType.Modifier.UNSIGNED),
            // https://pkg.go.dev/builtin#uint64
            "uint64" to IntegerType("uint64", 64, this, NumericType.Modifier.UNSIGNED),
            // https://pkg.go.dev/builtin#uintptr
            "uintptr" to
                IntegerType(
                    "uintptr",
                    null /* depends on the architecture, so we don't know */,
                    this,
                    NumericType.Modifier.UNSIGNED,
                ),
            // https://pkg.go.dev/builtin#float32
            "float32" to FloatingPointType("float32", 32, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#float64
            "float64" to FloatingPointType("float64", 64, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#complex64
            "complex64" to NumericType("complex64", 64, this, NumericType.Modifier.NOT_APPLICABLE),
            // https://pkg.go.dev/builtin#complex128
            "complex128" to
                NumericType("complex128", 128, this, NumericType.Modifier.NOT_APPLICABLE),
            // https://pkg.go.dev/builtin#rune
            // TODO: Actually, this should be a type alias to int32
            "rune" to IntegerType("int32", 32, this, NumericType.Modifier.SIGNED),
            // https://pkg.go.dev/builtin#byte
            // TODO: Actually, this should be a type alias to uint8
            "byte" to IntegerType("uint8", 8, this, NumericType.Modifier.UNSIGNED),
            // https://pkg.go.dev/builtin#string
            "string" to StringType("string", this),
            // https://go.dev/ref/spec#Package_unsafe
            "unsafe.ArbitraryType" to ObjectType("unsafe.ArbitraryType", listOf(), false, this),
            // https://go.dev/ref/spec#Package_unsafe
            "unsafe.IntegerType" to ObjectType("unsafe.IntegerType", listOf(), false, this),
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

        if (
            type == targetType ||
                // "any" accepts any type
                targetType == primitiveType("any") ||
                // the unsafe.ArbitraryType is a fake type in the unsafe package, that also accepts
                // any type
                targetType == primitiveType("unsafe.ArbitraryType")
        ) {
            return DirectMatch
        }

        // This makes lambda expression works, as long as we have the dedicated a
        // FunctionPointerType
        if (type is FunctionPointerType && targetType.underlyingType is FunctionType) {
            return if (
                type == targetType.underlyingType?.reference(PointerType.PointerOrigin.POINTER)
            ) {
                DirectMatch
            } else {
                CastNotPossible
            }
        }

        // the unsafe.IntegerType is a fake type in the unsafe package, that accepts any integer
        // type
        if (type is IntegerType && targetType == primitiveType("unsafe.IntegerType")) {
            return DirectMatch
        }

        // If we encounter an auto type as part of the function declaration, we accept this as any
        // type
        if (
            (type is ObjectType && targetType is AutoType) ||
                (type is PointerType && type.isArray && targetType.root is AutoType)
        ) {
            return DirectMatch
        }

        // We accept the "nil" literal for the following super types:
        // - pointers
        // - interfaces
        // - maps
        // - slices (which we model also as a pointer type)
        // - channels
        // - function types
        if (hint.isNil) {
            return if (
                targetType is PointerType ||
                    targetType.isInterface ||
                    targetType.isMap ||
                    targetType.isChannel ||
                    targetType.underlyingType is FunctionType
            ) {
                DirectMatch
            } else {
                CastNotPossible
            }
        }

        // We accept all kind of numbers if the literal is part of the call expression
        if (targetHint is ParameterDeclaration && hint is Literal<*>) {
            return if (type is NumericType && targetType is NumericType) {
                DirectMatch
            } else {
                CastNotPossible
            }
        }

        // We additionally want to emulate the behaviour of Go's interface system here
        if (targetType.isInterface) {
            var b: CastResult = DirectMatch
            val target = (type.root as? ObjectType)?.recordDeclaration

            // Our target struct type needs to implement all the functions of the interface
            // TODO(oxisto): Differentiate on the receiver (pointer vs non-pointer)
            for (method in targetType.recordDeclaration?.methods ?: listOf()) {
                if (target?.methods?.firstOrNull { it.signature == method.signature } != null) {
                    b = CastNotPossible
                }
            }

            return b
        }

        return CastNotPossible
    }

    override fun propagateTypeOfBinaryOperation(operation: BinaryOperator): Type {
        if (operation.operatorCode == "==") {
            return super.propagateTypeOfBinaryOperation(operation)
        }

        // Deal with literals. Numeric literals can also be used in simple arithmetic of the
        // underlying type is numeric
        return when {
            operation.lhs is Literal<*> && (operation.lhs as Literal<*>).type is NumericType -> {
                val type = operation.rhs.type
                if (type is NumericType || type.underlyingType is NumericType) {
                    type
                } else {
                    unknownType()
                }
            }
            operation.rhs is Literal<*> && (operation.rhs as Literal<*>).type is NumericType -> {
                val type = operation.lhs.type
                if (type is NumericType || type.underlyingType is NumericType) {
                    type
                } else {
                    unknownType()
                }
            }
            else -> super.propagateTypeOfBinaryOperation(operation)
        }
    }
}
