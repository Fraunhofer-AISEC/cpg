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
package de.fraunhofer.aisec.cpg.frontends.java

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The Java language. */
open class JavaLanguage :
    Language<JavaLanguageFrontend>(),
    // HasComplexCallResolution,
    HasClasses,
    HasSuperClasses,
    HasGenerics,
    HasQualifier,
    HasUnknownType,
    HasShortCircuitOperators {
    override val fileExtensions = listOf("java")
    override val namespaceDelimiter = "."
    @Transient override val frontend: KClass<out JavaLanguageFrontend> = JavaLanguageFrontend::class
    override val superClassKeyword = "super"
    override val qualifiers = listOf("final", "volatile")
    override val unknownTypeString = listOf("var")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    @Transient
    @JsonIgnore
    override val simpleTypes =
        mapOf(
            "boolean" to IntegerType("boolean", 1, this, NumericType.Modifier.SIGNED),
            "byte" to IntegerType("byte", 8, this, NumericType.Modifier.SIGNED),
            "char" to IntegerType("char", 16, this, NumericType.Modifier.SIGNED),
            "short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "String" to StringType("java.lang.String", this),
            "java.lang.String" to StringType("java.lang.String", this)
        )

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager,
    ): JavaLanguageFrontend {
        return JavaLanguageFrontend(this, config, scopeManager)
    }

    override val startCharacter = '<'
    override val endCharacter = '>'
}
