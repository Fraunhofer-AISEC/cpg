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

import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The JavaScript language. */
open class JavaScriptLanguage : Language<TypeScriptLanguageFrontend>(), HasShortCircuitOperators {
    override val fileExtensions = listOf("js", "jsx")
    override val namespaceDelimiter = "."
    @Transient
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
    @Transient
    override val builtInTypes =
        mapOf(
            "boolean" to BooleanType("boolean", language = this),
            "number" to FloatingPointType("number", 64, this, NumericType.Modifier.SIGNED),
            "bigint" to IntegerType("bigint", null, this, NumericType.Modifier.SIGNED),
            "string" to StringType("string", this),
        )
}
