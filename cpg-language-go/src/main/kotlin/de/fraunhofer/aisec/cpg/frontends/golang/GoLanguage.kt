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
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.primitiveType
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.project.DetectionResult
import de.fraunhofer.aisec.cpg.project.Detector
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import java.nio.file.Path
import kotlin.math.max

/** The Go language. */
open class GoLanguage :
    Language<GoLanguageFrontend>(),
    HasShortCircuitOperators,
    HasGenerics,
    HasStructs,
    HasFirstClassFunctions,
    HasAnonymousIdentifier,
    HasFunctionStyleCasts,
    HasVisibilityModifiers,
    Detector {

    override fun detect(root: Path, environment: TargetEnvironment): DetectionResult? {
        return detectGo(root, environment)
    }

    override val fileExtensions = listOf("go")
    override val namespaceDelimiter = "."
    @DoNotPersist override val frontend = GoLanguageFrontend::class
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
    @DoNotPersist
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

        // The same has to hold the other way round. A value whose type we could not determine --
        // the result of a two-value map lookup, say, whose type is only established later when the
        // data flow is known -- must not rule a candidate out. Not knowing the type is a gap on
        // our side, and treating it as a mismatch does not merely lose precision: the call stops
        // matching its target, a stub is inferred in its place, and with it every edge behind that
        // call.
        if (type is AutoType || type.root is AutoType) {
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

        // We accept all kind of numbers if the literal is part of the call expression.
        //
        // The same has to hold for a named untyped constant: `const bpTagData = 0x40` is untyped
        // and converts implicitly to the parameter's type, so passing it to a `uint8` parameter is
        // legal even though the declaration itself only carries the default type `int` of its
        // initializer. Without this, such a call does not match its target and a stub is inferred
        // instead.
        if (targetHint is Parameter && hint.isUntypedConstant) {
            return if (type is NumericType && targetType is NumericType) {
                DirectMatch
            } else {
                CastNotPossible
            }
        }

        // We additionally want to emulate the behavior of Go's interface system here
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

    /**
     * Go has no visibility keywords whatsoever, so — unlike most languages — this does not look at
     * [Declaration.modifiers] at all (the Go frontend uses those for an unrelated purpose, namely
     * marking embedded struct fields). Instead the visibility follows solely from the casing of the
     * declaration's identifier, see [visibilityOf].
     *
     * Only declarations living in a [NameScope] have export semantics: package-level funcs, vars,
     * consts and types, as well as struct fields and interface methods (a
     * [de.fraunhofer.aisec.cpg.graph.scopes.RecordScope] is a [NameScope] too). Block-scoped locals
     * are unreachable from another package by construction and are therefore left at
     * [Visibility.UNKNOWN] ("no restriction").
     */
    override fun applyModifiers(declaration: Declaration, scope: Scope?) {
        if (scope is NameScope) {
            declaration.visibility = visibilityOf(declaration.name.localName)
        }
    }

    override fun propagateTypeOfBinaryOperation(
        operatorCode: String?,
        lhsType: Type,
        rhsType: Type,
        hint: BinaryOperator?,
    ): Type {
        // Deal with literals. Numeric literals can also be used in simple arithmetic if the
        // underlying type is numeric.
        // There are two relevant sources of information: https://go.dev/ref/spec#Operators, and
        // https://go.dev/ref/spec#Constant_expressions which specify the following:
        // - If one operand is an untyped constant, the resulting type is the type of the other
        // operand
        // - Always Boolean for comparisons
        // - Always integer for shifts
        // - If both untyped operands are the same type, it's that one.
        // - If the untyped operands of a binary operation (other than a shift) are of different
        //   kinds, the result is of the operand's kind that appears later in this list: integer,
        //   rune, floating-point, complex
        return when {
            // Enforce bool for comparisons
            operatorCode in listOf("<", "=<", ">", "<=", "==") ->
                builtInTypes["bool"] ?: unknownType()
            // Two untyped literals for shift
            hint?.lhs is Literal<*> &&
                hint.rhs is Literal<*> &&
                operatorCode in listOf("<<", ">>") -> builtInTypes["int"] ?: unknownType()
            // A single (untyped) literal, so we take the other type
            hint?.lhs is Literal<*> &&
                lhsType is NumericType &&
                hint.rhs !is Literal<*> &&
                rhsType.underlyingType is NumericType -> rhsType
            hint?.rhs is Literal<*> &&
                rhsType is NumericType &&
                hint.lhs !is Literal<*> &&
                lhsType.underlyingType is NumericType -> lhsType
            // Two literals: If both are the same type, we take this type
            hint?.lhs is Literal<*> && hint.rhs is Literal<*> && lhsType == rhsType -> lhsType
            // Two literals of different type: Take the "higher one" in the list.
            hint?.lhs is Literal<*> &&
                hint.rhs is Literal<*> &&
                lhsType != rhsType &&
                lhsType is NumericType &&
                rhsType is NumericType ->
                max(
                        listOf("int", "rune", "float64", "complex").indexOf(lhsType.name.localName),
                        listOf("int", "rune", "float64", "complex").indexOf(rhsType.name.localName),
                    )
                    .let { index ->
                        when (index) {
                            0 -> builtInTypes["int"] ?: unknownType()
                            1 -> builtInTypes["rune"] ?: unknownType()
                            2 -> builtInTypes["float"] ?: unknownType()
                            3 -> builtInTypes["complex"] ?: unknownType()
                            else -> unknownType()
                        }
                    }
            // For all the rest, we take the default behavior
            else -> super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
        }
    }

    companion object {
        /**
         * Maps a Go identifier onto the canonical [Visibility]. Go has no access-control keywords:
         * the visibility of a declaration is derived solely from the casing of its identifier (see
         * https://go.dev/ref/spec#Exported_identifiers):
         * - an identifier whose first character is a Unicode upper-case letter is *exported* and is
         *   therefore reachable from other packages, which we model as [Visibility.PUBLIC];
         * - any other identifier is *unexported* and confined to its own package, which we model as
         *   [Visibility.PACKAGE] (module/package visibility, not tied to a record — Go has no
         *   record-level `private`/`protected` access control).
         *
         * The blank identifier `_` as well as empty/unnamed declarations carry no meaningful
         * visibility and are left as [Visibility.UNKNOWN] ("no restriction").
         *
         * This is the pure mapping only; [applyModifiers] is what decides *which* declarations it
         * is applied to.
         */
        internal fun visibilityOf(identifier: String): Visibility =
            when {
                // The blank identifier and unnamed declarations have no export semantics.
                identifier.isEmpty() || identifier == "_" -> Visibility.UNKNOWN
                // Go decides "exported" on the first *rune*, so we must look at the first full code
                // point rather than the first UTF-16 char to handle non-ASCII (e.g. "Über" vs
                // "über") identifiers correctly. `isUpperCase` returns false for uncased scripts,
                // which matches Go's "upper-case letter" rule.
                Character.isUpperCase(identifier.codePointAt(0)) -> Visibility.PUBLIC
                else -> Visibility.PACKAGE
            }
    }

    /**
     * In Go a package is a directory, and two packages in different directories may well carry the
     * same name. A vendored dependency that forks a standard library package is the common case:
     * `vendor/.../gofork/encoding/asn1` and the standard library's `encoding/asn1` are both named
     * `asn1` and declare the same symbols. A lookup by name therefore finds candidates from both,
     * which the default implementation reports as ambiguous and resolves to `null` -- leaving the
     * reference unresolved, and with it any member call that uses it as a base.
     *
     * Go resolves this unambiguously: an unqualified identifier refers to the declaration in the
     * package it is written in. We reproduce that by preferring candidates declared in the same
     * directory as the reference, and only fall back to the default behaviour if that does not
     * single one out.
     */
    override fun bestViableReferenceCandidate(ref: Reference): Declaration? {
        if (ref.candidates.size > 1) {
            val refPackage = ref.packageDirectory
            if (refPackage != null) {
                val samePackage =
                    ref.candidates.filterTo(mutableSetOf()) { it.packageDirectory == refPackage }
                if (samePackage.isNotEmpty()) {
                    ref.candidates = samePackage
                }
            }
        }

        return super.bestViableReferenceCandidate(ref)
    }
}

/** Marks a declaration that was declared in a `const` block. */
const val CONST_MODIFIER = "const"

/**
 * Marks a constant declared without an explicit type. Such a constant is *untyped* in Go and
 * converts implicitly to any type it is representable in.
 */
const val UNTYPED_CONST_MODIFIER = "untyped"

/**
 * Whether this expression is an untyped constant in the sense of https://go.dev/ref/spec#Constants,
 * i.e. a constant whose type is only determined by the context it is used in. That is the case for
 * a literal, and for a reference to a constant that was declared without an explicit type.
 */
private val HasType?.isUntypedConstant: Boolean
    get() =
        when (this) {
            is Literal<*> -> true
            is Reference -> {
                val target = this.refersTo
                target != null &&
                    CONST_MODIFIER in target.modifiers &&
                    UNTYPED_CONST_MODIFIER in target.modifiers
            }
            else -> false
        }

/**
 * The directory a node is declared in, which for Go is the same thing as the package it belongs to.
 * Returns `null` for nodes without a location, which therefore never match any package.
 */
private val de.fraunhofer.aisec.cpg.graph.Node.packageDirectory: String?
    get() =
        this.location
            ?.artifactLocation
            ?.uri
            ?.path
            ?.substringBeforeLast('/', missingDelimiterValue = "")
            ?.ifEmpty { null }
