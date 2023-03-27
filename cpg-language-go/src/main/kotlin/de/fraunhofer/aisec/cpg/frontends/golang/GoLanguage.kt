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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.HasGenerics
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.HasStructs
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.*
import org.neo4j.ogm.annotation.Transient

/** The Go language. */
class GoLanguage :
    Language<GoLanguageFrontend>(), HasShortCircuitOperators, HasGenerics, HasStructs {
    override val fileExtensions = listOf("go")
    override val namespaceDelimiter = "."
    @Transient override val frontend = GoLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")
    override val startCharacter = '['
    override val endCharacter = ']'

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
                    NumericType.Modifier.UNSIGNED
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
            "string" to StringType("string", this)
        )

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager,
    ): GoLanguageFrontend {
        return GoLanguageFrontend(this, config, scopeManager)
    }
}
