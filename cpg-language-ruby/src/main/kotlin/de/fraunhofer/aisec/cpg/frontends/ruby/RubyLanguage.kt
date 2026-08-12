/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.ruby

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

/** The Ruby Language */
open class RubyLanguage :
    Language<RubyLanguageFrontend>(),
    HasDefaultArguments,
    HasClasses,
    HasSuperClasses,
    HasShortCircuitOperators,
    HasVisibilityModifiers {
    override val fileExtensions = listOf("rb")
    override val namespaceDelimiter = "::"
    @DoNotPersist
    override val frontend: KClass<out RubyLanguageFrontend> = RubyLanguageFrontend::class
    override val superClassKeyword = "super"

    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    @DoNotPersist
    /** See [The RubySpec](https://github.com/ruby/spec) */
    override val builtInTypes =
        mapOf(
            // The bit width of the Integer type in Ruby is only limited by your memory
            "Integer" to IntegerType("Integer", null, this, NumericType.Modifier.SIGNED),
            "Float" to FloatingPointType("Float", 64, this, NumericType.Modifier.SIGNED),
            "String" to StringType("String", this),
            // The bit width of Booleans is not defined in the specification and
            // implementation-dependant
            "Boolean" to BooleanType("Boolean", null, this, NumericType.Modifier.NOT_APPLICABLE),
        )

    override val compoundAssignmentOperators =
        setOf(
            "+=", // Addition assignment
            "-=", // Subtraction assignment
            "*=", // Multiplication assignment
            "/=", // Division assignment
            "%=", // Modulo assignment
            "**=", // Exponentiation assignment
            "<<=", // Left shift assignment
            ">>=", // Right shift assignment
            "&=", // Bitwise AND assignment
            "|=", // Bitwise OR assignment
            "^=", // Bitwise XOR assignment
        )

    override fun SymbolResolver.handleSuperExpression(
        memberExpression: MemberAccess,
        curClass: Record,
    ): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Applies Ruby's method-visibility modifiers to [declaration]. Ruby controls method access via
     * the `public`/`protected`/`private` visibility modifiers, which flip the default visibility
     * for subsequently-defined methods (or, in the `private def ...` form, apply to a single
     * method). The raw keyword is kept losslessly in [Declaration.modifiers]; here it is projected
     * onto the canonical [Declaration.visibility], so passes such as the [SymbolResolver] can
     * reason about access control without knowing Ruby's concrete keywords.
     *
     * The keywords only ever affect record members and their meaning is context-independent, so the
     * [scope] is not relevant here.
     *
     * Note the mapping is intentionally lossy on Ruby's peculiar runtime semantics, but faithful to
     * the canonical access-control axis:
     * - `private` in Ruby means the method may only be called *without an explicit receiver* (i.e.
     *   implicitly on `self`). This is stricter than "declaring record only", but the closest
     *   canonical value is [Visibility.PRIVATE].
     * - `protected` in Ruby means the method may be called *with an explicit receiver* as long as
     *   that receiver is of the same class or a subclass. This maps to [Visibility.PROTECTED].
     * - `public` is the default and maps to [Visibility.PUBLIC].
     */
    override fun applyModifiers(declaration: Declaration, scope: Scope?) {
        when {
            PUBLIC in declaration.modifiers -> declaration.visibility = Visibility.PUBLIC
            PROTECTED in declaration.modifiers -> declaration.visibility = Visibility.PROTECTED
            PRIVATE in declaration.modifiers -> declaration.visibility = Visibility.PRIVATE
        }
    }

    companion object {
        const val PUBLIC = "public"
        const val PROTECTED = "protected"
        const val PRIVATE = "private"

        /** The Ruby method-visibility modifier keywords. */
        val visibilityModifiers = setOf(PUBLIC, PROTECTED, PRIVATE)
    }
}
