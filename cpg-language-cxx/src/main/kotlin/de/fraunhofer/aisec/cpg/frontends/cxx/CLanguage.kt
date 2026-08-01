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
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.project.DetectionResult
import de.fraunhofer.aisec.cpg.project.Detector
import de.fraunhofer.aisec.cpg.project.TargetEnvironment
import java.nio.file.Path
import kotlin.reflect.KClass

const val CONST = "const"

/** The C/C++ storage-class specifier that marks internal linkage or a static member. */
const val STATIC = "static"

/** The C/C++ access specifier keywords, used inside records to control member visibility. */
const val PUBLIC = "public"
const val PROTECTED = "protected"
const val PRIVATE = "private"

/** The C language. */
open class CLanguage :
    Language<CXXLanguageFrontend>(),
    HasStructs,
    HasFunctionPointers,
    HasQualifier,
    HasElaboratedTypeSpecifier,
    HasShortCircuitOperators,
    HasGlobalVariables,
    HasGlobalFunctions,
    HasRedeclarations,
    HasKeywordSemantics,
    HasAccessControl,
    Detector {

    override fun detect(root: Path, environment: TargetEnvironment): DetectionResult? {
        return detectCxxProject(root)
    }

    override val fileExtensions = listOf("c", "h")
    override val namespaceDelimiter = "::"
    @DoNotPersist
    override val frontend: KClass<out CXXLanguageFrontend> = CXXLanguageFrontend::class
    override val qualifiers = listOf(CONST, "volatile", "restrict", "atomic")
    override val elaboratedTypeSpecifier = listOf("struct", "union", "enum")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    /**
     * Whether a bare, non-`extern`, non-initialized redeclaration of a global [Variable] is a valid
     * "tentative definition" ([ISO/IEC 9899:2011] §6.9.2) rather than an error. This holds for C,
     * but is overridden to `false` for C++, where a non-class-type variable at namespace/global
     * scope is already a full definition, even without an explicit initializer, so a second such
     * declaration would be an ODR violation rather than a redeclaration of the same object.
     */
    open val supportsTentativeDefinitions: Boolean = true

    /**
     * Determines whether [incoming] is a redeclaration of [existing] that should be merged into it,
     * rather than registered as a separate declaration.
     *
     * This only ever applies to two [Variable]s of the exact same concrete kind (e.g. two plain
     * global variables, or two `static` members of the same
     * [de.fraunhofer.aisec.cpg.graph.declarations.Record]) at global or namespace scope, and only
     * if at least one of them is "incomplete": either explicitly declared `extern`, or (in C only,
     * see [supportsTentativeDefinitions]) simply lacking an initializer, per C11's "tentative
     * definition" rules (§6.9.2). Two full definitions of the same symbol are deliberately left
     * unmerged, since that is an ODR violation rather than a legitimate redeclaration, and should
     * surface as an ambiguity during symbol resolution instead of being silently resolved.
     */
    override fun isRedeclaration(existing: Declaration, incoming: Declaration): Boolean {
        if (existing !is Variable || incoming !is Variable || existing::class != incoming::class) {
            return false
        }
        if (existing.scope !is GlobalScope && existing.scope !is NamespaceScope) {
            return false
        }
        if (existing.initializer != null && incoming.initializer != null) {
            // Two full definitions of the same global: an ODR violation. Leave both in place, so
            // that resolution surfaces the ambiguity instead of silently picking a winner.
            return false
        }
        if ("extern" in existing.modifiers || "extern" in incoming.modifiers) {
            return true
        }
        // Neither side carries `extern`: this is only a valid tentative-definition redeclaration
        // in C.
        return supportsTentativeDefinitions
    }

    /**
     * Merges [incoming] into [existing] after [isRedeclaration] determined that they refer to the
     * same object. [existing] is kept as the canonical declaration: it inherits [incoming]'s
     * initializer if it did not already have one of its own (i.e., if [incoming] turned out to be
     * the actual definition), and the union of both declarations' [Declaration.modifiers], with a
     * now-stale `extern` modifier removed once the declaration has become a definition. [incoming]
     * is discarded by the caller afterwards; its initializer is cleared here so it does not keep a
     * dangling reference to state that is now owned by [existing].
     */
    override fun mergeRedeclaration(existing: Declaration, incoming: Declaration) {
        if (existing !is Variable || incoming !is Variable) {
            return
        }
        if (existing.initializer == null && incoming.initializer != null) {
            existing.initializer = incoming.initializer
            incoming.initializer = null
        }
        existing.modifiers =
            (existing.modifiers + incoming.modifiers).let {
                if (existing.initializer != null) it - "extern" else it
            }
    }

    /**
     * Interprets a C/C++ declaration keyword into its canonical [KeywordSemantics], resolving the
     * notorious context-dependence of `static`:
     * - at file/namespace scope ([DeclarationContext.GLOBAL]) it grants *internal linkage*, i.e.
     *   the declaration is confined to its own translation unit ([Visibility.INTERNAL]);
     * - on a record member ([DeclarationContext.RECORD]) it makes the member *static*, i.e. bound
     *   to the record itself rather than to an instance;
     * - inside a function body ([DeclarationContext.LOCAL]) it only affects storage duration, which
     *   is irrelevant to symbol resolution, so it carries no canonical semantics.
     *
     * The access specifiers `public`/`protected`/`private` map onto the corresponding [Visibility]
     * regardless of context (they only ever occur on record members). Any other keyword yields
     * empty [KeywordSemantics].
     */
    override fun interpretKeyword(keyword: String, context: DeclarationContext): KeywordSemantics {
        return when (keyword) {
            STATIC ->
                when (context) {
                    DeclarationContext.GLOBAL -> KeywordSemantics(visibility = Visibility.INTERNAL)
                    DeclarationContext.RECORD -> KeywordSemantics(isStatic = true)
                    DeclarationContext.LOCAL -> KeywordSemantics()
                }
            PUBLIC -> KeywordSemantics(visibility = Visibility.PUBLIC)
            PROTECTED -> KeywordSemantics(visibility = Visibility.PROTECTED)
            PRIVATE -> KeywordSemantics(visibility = Visibility.PRIVATE)
            else -> KeywordSemantics()
        }
    }

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

        // In C, any pointer can be implicitly cast to void*
        if (
            type is PointerType &&
                targetType is PointerType &&
                targetType.elementType is IncompleteType
        ) {
            return ImplicitCast
        }

        return CastNotPossible
    }
}
