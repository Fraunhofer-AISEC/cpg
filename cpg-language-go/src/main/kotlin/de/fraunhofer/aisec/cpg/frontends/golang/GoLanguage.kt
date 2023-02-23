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
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.StringType

/** The Go language. */
open class GoLanguage : Language<GoLanguageFrontend>(), HasShortCircuitOperators, HasGenerics {
    override val fileExtensions = listOf("go")
    override val namespaceDelimiter = "."
    override val frontend = GoLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")
    override val startCharacter = '['
    override val endCharacter = ']'

    @Transient
    override val simpleTypes =
        mapOf(
            "int8" to IntegerType("int8", 8, this, NumericType.Modifier.SIGNED),
            "int16" to IntegerType("int16", 16, this, NumericType.Modifier.SIGNED),
            "int32" to IntegerType("int32", 32, this, NumericType.Modifier.SIGNED),
            "int64" to IntegerType("int64", 64, this, NumericType.Modifier.SIGNED),
            "uint8" to IntegerType("uint8", 8, this, NumericType.Modifier.UNSIGNED),
            "uint16" to IntegerType("uint16", 16, this, NumericType.Modifier.UNSIGNED),
            "uint32" to IntegerType("uint32", 32, this, NumericType.Modifier.UNSIGNED),
            "uint64" to IntegerType("uint64", 64, this, NumericType.Modifier.UNSIGNED),
            "float32" to FloatingPointType("float32", 32, this, NumericType.Modifier.SIGNED),
            "float64" to FloatingPointType("float64", 64, this, NumericType.Modifier.SIGNED),
            "complex32" to NumericType("complex32", 32, this, NumericType.Modifier.NOT_APPLICABLE),
            "complex64" to NumericType("complex54", 64, this, NumericType.Modifier.NOT_APPLICABLE),
            "string" to StringType("string", this)
        )

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager,
    ): GoLanguageFrontend {
        return GoLanguageFrontend(this, config, scopeManager)
    }
}
