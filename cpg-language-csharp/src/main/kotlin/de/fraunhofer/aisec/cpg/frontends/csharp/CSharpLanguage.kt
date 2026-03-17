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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.graph.types.Type
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

class CSharpLanguage : Language<CSharpLanguageFrontend>() {
    override val fileExtensions = listOf("cs")
    override val namespaceDelimiter = "."

    @Transient
    override val frontend: KClass<out CSharpLanguageFrontend> = CSharpLanguageFrontend::class

    override val compoundAssignmentOperators = setOf<String>()

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
}
