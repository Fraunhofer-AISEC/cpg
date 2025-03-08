/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass

class JVMLanguage(ctx: TranslationContext) : Language<JVMLanguageFrontend>(ctx) {
    override val fileExtensions: List<String>
        get() = listOf("class", "java", "jimple", "jar")

    override val namespaceDelimiter: String
        get() = "."

    override val frontend: KClass<out JVMLanguageFrontend>
        get() = JVMLanguageFrontend::class

    override val builtInTypes: Map<String, Type> =
        mapOf(
            "float" to FloatingPointType(ctx, "float", 32, this),
            "double" to FloatingPointType(ctx, "double", 64, this),
            "char" to IntegerType(ctx, "char", 8, this, NumericType.Modifier.UNSIGNED),
            "boolean" to BooleanType(ctx, "boolean", 1, this),
            "byte" to IntegerType(ctx, "byte", 8, this),
            "short" to IntegerType(ctx, "short", 16, this),
            "int" to IntegerType(ctx, "int", 32, this),
            "long" to IntegerType(ctx, "long", 64, this),
            "java.lang.String" to StringType(ctx, "java.lang.String", this),
            "java.lang.Class" to ObjectType(ctx, "java.lang.Class", listOf(), true, this),
        )

    override val compoundAssignmentOperators: Set<String>
        get() = setOf()
}
