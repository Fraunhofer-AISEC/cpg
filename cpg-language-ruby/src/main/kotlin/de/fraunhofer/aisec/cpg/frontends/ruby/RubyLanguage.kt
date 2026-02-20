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
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import kotlin.reflect.KClass

/** The Ruby Language */
class RubyLanguage :
    Language<RubyLanguageFrontend>(),
    HasDefaultArguments,
    HasClasses,
    HasSuperClasses,
    HasShortCircuitOperators {
    override val fileExtensions = listOf("rb")
    override val namespaceDelimiter = "::"
    @Transient override val frontend: KClass<out RubyLanguageFrontend> = RubyLanguageFrontend::class
    override val superClassKeyword = "super"

    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    @Transient
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
}
