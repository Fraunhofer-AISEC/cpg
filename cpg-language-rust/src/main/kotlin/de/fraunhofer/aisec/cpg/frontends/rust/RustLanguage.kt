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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass

/**
 * The [Language] definition for Rust. It currently supports basic types and operators.
 *
 * More information can be found in the [Rust Reference](https://doc.rust-lang.org/reference/).
 */
class RustLanguage :
    Language<RustLanguageFrontend>(), HasShortCircuitOperators, HasDefaultArguments {
    override val fileExtensions = listOf("rs")
    override val namespaceDelimiter = "::"

    override val frontend: KClass<out RustLanguageFrontend> = RustLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=")

    /** See [Documentation](https://doc.rust-lang.org/reference/types.html). */
    override val builtInTypes =
        mapOf(
            "bool" to BooleanType(typeName = "bool", language = this),
            "i8" to IntegerType("i8", 8, this, NumericType.Modifier.SIGNED),
            "i16" to IntegerType("i16", 16, this, NumericType.Modifier.SIGNED),
            "i32" to IntegerType("i32", 32, this, NumericType.Modifier.SIGNED),
            "i64" to IntegerType("i64", 64, this, NumericType.Modifier.SIGNED),
            "i128" to IntegerType("i128", 128, this, NumericType.Modifier.SIGNED),
            "isize" to IntegerType("isize", null, this, NumericType.Modifier.SIGNED),
            "u8" to IntegerType("u8", 8, this, NumericType.Modifier.UNSIGNED),
            "u16" to IntegerType("u16", 16, this, NumericType.Modifier.UNSIGNED),
            "u32" to IntegerType("u32", 32, this, NumericType.Modifier.UNSIGNED),
            "u64" to IntegerType("u64", 64, this, NumericType.Modifier.UNSIGNED),
            "u128" to IntegerType("u128", 128, this, NumericType.Modifier.UNSIGNED),
            "usize" to IntegerType("usize", null, this, NumericType.Modifier.UNSIGNED),
            "f32" to FloatingPointType("f32", 32, this, NumericType.Modifier.NOT_APPLICABLE),
            "f64" to FloatingPointType("f64", 64, this, NumericType.Modifier.NOT_APPLICABLE),
            "str" to StringType("str", language = this, primitive = true),
            "String" to StringType("String", language = this, primitive = false),
            "char" to IntegerType("char", 32, this, NumericType.Modifier.UNSIGNED),
        )
}
