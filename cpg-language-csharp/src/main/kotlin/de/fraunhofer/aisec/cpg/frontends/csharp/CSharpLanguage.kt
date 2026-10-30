/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.frontends.HasDefaultArguments
import de.fraunhofer.aisec.cpg.frontends.HasImplicitReceiver
import de.fraunhofer.aisec.cpg.frontends.HasImportsFromRecords
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.HasSuperClasses
import de.fraunhofer.aisec.cpg.frontends.HasUnknownType
import de.fraunhofer.aisec.cpg.frontends.HasVisibilityModifiers
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Import
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.SymbolResolver.Companion.LOGGER
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

/** The C# access modifier keywords, used to control member visibility. */
const val PUBLIC = "public"
const val PROTECTED = "protected"
const val PRIVATE = "private"

/** The C# `internal` modifier, making a declaration visible within its whole assembly. */
const val INTERNAL = "internal"

/** The C# `static` modifier, marking a type-level (rather than per-instance) member. */
const val STATIC = "static"

/** The C# `const` modifier, marking a field as a constant, which is implicitly static. */
const val CONST = "const"

class CSharpLanguage :
    Language<CSharpLanguageFrontend>(),
    HasImplicitReceiver,
    HasDefaultArguments,
    HasSuperClasses,
    HasShortCircuitOperators,
    HasUnknownType,
    HasImportsFromRecords,
    HasVisibilityModifiers {
    override val receiverName = "this"
    override val fileExtensions = listOf("cs")
    override val namespaceDelimiter = "."
    override val superClassKeyword = "base"
    override val accessModifiers = setOf(PUBLIC, PROTECTED, PRIVATE, INTERNAL)

    /**
     * `&&`, as well as the pattern combinator `and`, only evaluate their right-hand side if the
     * left-hand side is `true`. A pattern such as `x is > 0 and < 100` only tests the right pattern
     * if the left one matched.
     *
     * See
     * [Documentation](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1213-conditional-logical-operators).
     */
    override val conjunctiveOperators = listOf("&&", "and")

    /**
     * `||`, the pattern combinator `or` and the null-coalescing operator `??` only evaluate their
     * right-hand side if the left-hand side is `false` (or `null`, for `??`).
     *
     * See
     * [Documentation](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#1213-conditional-logical-operators).
     */
    override val disjunctiveOperators = listOf("||", "or", "??")

    /**
     * Types that are not known when translating a declaration: `var` takes the type of its
     * initializer and a `dynamic` is only resolved at runtime.
     */
    override val unknownTypeString = listOf("var", "dynamic")

    @DoNotPersist
    override val frontend: KClass<out CSharpLanguageFrontend> = CSharpLanguageFrontend::class

    /**
     * See
     * [Documentation](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12214-assignment-operators).
     */
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", ">>>=", "&=", "|=", "^=", "??=")

    /**
     * See
     * [Documentation](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/built-in-types).
     */
    @Transient
    override val builtInTypes =
        mapOf<String, Type>(
            // Boolean type
            "bool" to BooleanType(typeName = "bool", language = this),
            // Integral Types:
            // https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/integral-numeric-types
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "uint" to IntegerType("uint", 32, this, NumericType.Modifier.UNSIGNED),
            "short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "ulong" to IntegerType("ulong", 64, this, NumericType.Modifier.UNSIGNED),
            "byte" to IntegerType("byte", 8, this, NumericType.Modifier.UNSIGNED),
            "sbyte" to IntegerType("sbyte", 8, this, NumericType.Modifier.SIGNED),
            // Floating-Point types:
            // https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/floating-point-numeric-types
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "decimal" to FloatingPointType("decimal", 128, this, NumericType.Modifier.SIGNED),
            // Char Type
            "char" to IntegerType("char", 16, this, NumericType.Modifier.UNSIGNED),
            // String Type
            "string" to StringType("string", this),
            "object" to ObjectType("object", listOf(), false, true, this),
            // `void` is not a real type, a method returning it returns nothing
            "void" to IncompleteType(language = this),
        )

    /**
     * Resolves the `base` [Reference] of a [MemberAccess] such as `base.Describe()` or
     * `base.field`.
     *
     * `base` is the current instance viewed as its base class, so two things have to happen here:
     * the reference has to point at the receiver of the enclosing method and its type has to become
     * the base class.
     *
     * The frontend leaves `base` untyped, since the base class may not be parsed yet. It is typed
     * here, once all records are known.
     *
     * C# spec:
     * [Base access](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12815-base-access)
     */
    override fun SymbolResolver.handleSuperExpression(
        memberExpression: MemberAccess,
        curClass: Record,
    ): Boolean {
        val base = memberExpression.base as? Reference ?: return false

        // `base` is the same object as `this`, so it refers to the receiver of the enclosing
        // method. This has to happen before the type is set.
        val function = scopeManager.currentFunction
        if (function is Method) {
            base.refersTo = function.receiver
        }

        // The base class is the first entry of the base list. A class without an explicit base
        // class
        // derives from `System.Object`, and the syntax does not tell a base class from an
        // interface, so for a class that only implements interfaces we pick the first interface.
        val target = curClass.superClasses.firstOrNull()?.root?.recordDeclaration
        if (target == null) {
            Util.warnWithFileLocation(
                memberExpression,
                LOGGER,
                "Cannot type a `base` access in {}, since it has no known base class",
                curClass.name,
            )
            return false
        }

        // "Cast" the receiver to the base class. We deliberately do not retype the receiver itself,
        // since it is shared with every `this` in the method and those keep referring to the
        // current class.
        val superType = target.toType()
        base.type = superType
        base.assignedTypes = mutableSetOf(superType)

        return true
    }

    override fun propagateTypeOfBinaryOperation(
        operatorCode: String?,
        lhsType: Type,
        rhsType: Type,
        hint: BinaryOperator?,
    ): Type {
        // `is` is a type check and always yields a `bool`, regardless of its operands. The pattern
        // combinators `and` and `or` yield a `bool` as well.
        return if (operatorCode == "is" || operatorCode == "and" || operatorCode == "or") {
            builtInTypes.getValue("bool")
        } else super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
    }

    /**
     * Projects the C# modifiers onto [declaration]. The access modifiers map onto the [Visibility]
     * as follows:
     * - `public`, `protected` and `private` map onto their namesakes.
     * - `internal` (visible within the assembly) maps onto [Visibility.PACKAGE].
     * - `protected internal` (visible to derived types or the assembly) is modeled as
     *   [Visibility.PACKAGE] only.
     * - `private protected` (visible to derived types of the assembly) is modeled as
     *   [Visibility.PROTECTED] only.
     *
     * Without an access modifier, C# applies a default that depends on where the declaration
     * appears: members of an interface are `public`, all other members are `private` and types of a
     * namespace are `internal`.
     *
     * `static` marks a member of a record, or a `using static` directive, as static. `const` fields
     * are implicitly static.
     *
     * C# spec:
     * [Declared accessibility](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/basic-concepts#752-declared-accessibility)
     */
    override fun applyModifiers(declaration: Declaration, scope: Scope?) {
        val modifiers = declaration.modifiers
        declaration.visibility =
            when {
                declaration is Import -> Visibility.UNKNOWN
                PUBLIC in modifiers -> Visibility.PUBLIC
                PROTECTED in modifiers && INTERNAL in modifiers -> Visibility.PACKAGE
                PRIVATE in modifiers && PROTECTED in modifiers -> Visibility.PROTECTED
                PROTECTED in modifiers -> Visibility.PROTECTED
                PRIVATE in modifiers -> Visibility.PRIVATE
                INTERNAL in modifiers -> Visibility.PACKAGE
                scope is RecordScope ->
                    if ((scope.astNode as? Record)?.kind == "interface") Visibility.PUBLIC
                    else Visibility.PRIVATE
                scope is NamespaceScope || scope is GlobalScope -> Visibility.PACKAGE
                else -> Visibility.UNKNOWN
            }

        if (
            (STATIC in modifiers || CONST in modifiers) &&
                (scope is RecordScope || declaration is Import)
        ) {
            declaration.isStatic = true
        }
    }
}
