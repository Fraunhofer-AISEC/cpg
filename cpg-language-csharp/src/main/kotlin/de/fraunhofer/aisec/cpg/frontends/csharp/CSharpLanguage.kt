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
import de.fraunhofer.aisec.cpg.frontends.HasSuperClasses
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
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

class CSharpLanguage :
    Language<CSharpLanguageFrontend>(), HasImplicitReceiver, HasDefaultArguments, HasSuperClasses {
    override val receiverName = "this"
    override val fileExtensions = listOf("cs")
    override val namespaceDelimiter = "."
    override val superClassKeyword = "base"

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
            "int" to IntegerType("int", Integer.MAX_VALUE, this, NumericType.Modifier.SIGNED),
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
        )

    /**
     * Resolves the `base` [Reference] of a [MemberAccess] such as `base.Describe()` or
     * `base.field`.
     *
     * `base` is the current instance viewed as its base class, so two things have to happen here:
     * the reference has to point at the receiver of the enclosing method and its type has to become
     * the base class.
     *
     * This cannot be done while translating the `base`, because the base class may be declared in a
     * file that has not been parsed at that point. The frontend therefore leaves the reference
     * untyped and we type it here, once all records are known.
     *
     * C# spec:
     * [Base access](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12815-base-access)
     */
    override fun SymbolResolver.handleSuperExpression(
        memberExpression: MemberAccess,
        curClass: Record,
    ): Boolean {
        val base = memberExpression.base as? Reference ?: return false

        // `base` still refers to the same object as `this`, so it has to be connected to the
        // receiver of the enclosing method. This has to happen before the type is set, since a
        // reference observes the type of what it refers to.
        val function = scopeManager.currentFunction
        if (function is Method) {
            base.refersTo = function.receiver
        }

        // C# requires the base class to come first in a base list, which makes it the first entry.
        // There is not always one: a class without an explicit base class implicitly derives from
        // `System.Object`, and a base list may consist of interfaces only.
        // The syntax does not tell a base class and an interface apart, so a class that only
        // implements interfaces has one of them as its first entry and we pick that instead.
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
        // The `is` operator is a type check and therefore always yields a `bool`, no matter which
        // types its operands have.
        // See
        // https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/expressions#12123-the-is-operator
        return if (operatorCode == "is") {
            builtInTypes.getValue("bool")
        } else super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
    }
}
