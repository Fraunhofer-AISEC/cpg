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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.frontends.DeclarationContext
import de.fraunhofer.aisec.cpg.frontends.HasAccessControl
import de.fraunhofer.aisec.cpg.frontends.HasKeywordSemantics
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.KeywordSemantics
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

/** The TypeScript/JavaScript access modifier keywords, used to control member visibility. */
const val PUBLIC = "public"
const val PROTECTED = "protected"
const val PRIVATE = "private"

/** The keyword marking a class-level (rather than per-instance) member. */
const val STATIC = "static"

/**
 * A synthetic keyword standing in for a JavaScript/TypeScript *hard private* member, i.e. one whose
 * name starts with `#` (a `PrivateIdentifier`). Unlike TypeScript's compile-time `private`, this is
 * enforced at runtime and exists in plain JavaScript as well.
 */
const val HARD_PRIVATE = "#private"

/**
 * The JavaScript language.
 *
 * JavaScript itself has no access modifier *keywords*, but it does have genuine, runtime-enforced
 * access control via *hard private* (`#name`) class members, which map onto [Visibility.PRIVATE];
 * every other member is [Visibility.PUBLIC]. It therefore implements [HasAccessControl]. It also
 * has `static` class members, so it implements [HasKeywordSemantics] to interpret that keyword.
 * [TypeScriptLanguage] extends this with the compile-time `public`/`protected`/`private` access
 * specifiers.
 */
open class JavaScriptLanguage :
    Language<TypeScriptLanguageFrontend>(),
    HasShortCircuitOperators,
    HasKeywordSemantics,
    HasAccessControl {
    override val fileExtensions = listOf("js", "jsx")
    override val namespaceDelimiter = "."

    /**
     * Interprets a JavaScript declaration keyword into its canonical [KeywordSemantics]:
     * - [STATIC] on a record member marks it as a class-level (static) member;
     * - [HARD_PRIVATE] (a `#name` member) is a runtime-private member and maps onto
     *   [Visibility.PRIVATE].
     *
     * The compile-time access specifiers `public`/`protected`/`private` do not exist in plain
     * JavaScript and are only interpreted by [TypeScriptLanguage]. Any keyword this language does
     * not model yields empty [KeywordSemantics].
     */
    override fun interpretKeyword(keyword: String, context: DeclarationContext): KeywordSemantics {
        return when (keyword) {
            STATIC ->
                if (context == DeclarationContext.RECORD) KeywordSemantics(isStatic = true)
                else KeywordSemantics()
            HARD_PRIVATE -> KeywordSemantics(visibility = Visibility.PRIVATE)
            else -> KeywordSemantics()
        }
    }

    @DoNotPersist
    override val frontend: KClass<out TypeScriptLanguageFrontend> =
        TypeScriptLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&", "&&=", "??", "??=")
    override val disjunctiveOperators = listOf("||", "||=")

    /**
     * All operators which perform and assignment and an operation using lhs and rhs. See
     * https://tc39.es/ecma262/#sec-assignment-operators
     */
    override val compoundAssignmentOperators =
        setOf(
            "+=",
            "-=",
            "*=",
            "**=",
            "/=",
            "%=",
            "<<=",
            ">>=",
            ">>>=",
            "&=",
            "&&=",
            "|=",
            "||=",
            "^=",
            "??=",
        )

    /**
     * See
     * [Documentation](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Data_structures#primitive_values).
     */
    @DoNotPersist
    override val builtInTypes =
        mapOf(
            "boolean" to BooleanType("boolean", language = this),
            "number" to FloatingPointType("number", 64, this, NumericType.Modifier.SIGNED),
            "bigint" to IntegerType("bigint", null, this, NumericType.Modifier.SIGNED),
            "string" to StringType("string", this),
        )
}
