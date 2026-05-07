/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import kotlin.reflect.KClass

/** A simple PHP language model with support for modern PHP 8 syntax basics. */
open class PHPLanguage : Language<PHPLanguageFrontend>() {
    override val fileExtensions = listOf("php", "phtml")
    override val namespaceDelimiter = "\\"

    @DoNotPersist override val frontend: KClass<out PHPLanguageFrontend> = PHPLanguageFrontend::class

    @DoNotPersist
    override val builtInTypes: Map<String, Type> =
        mapOf(
            "bool" to BooleanType("bool", 1, this, NumericType.Modifier.NOT_APPLICABLE),
            "int" to IntegerType("int", 64, this, NumericType.Modifier.SIGNED),
            "float" to FloatingPointType("float", 64, this, NumericType.Modifier.SIGNED),
            "string" to StringType("string", this),
            "mixed" to IncompleteType("mixed", language = this),
            "void" to IncompleteType("void", language = this),
            "never" to IncompleteType("never", language = this),
            "null" to IncompleteType("null", language = this),
        )

    override val compoundAssignmentOperators =
        setOf(
            "+=",
            "-=",
            "*=",
            "/=",
            "%=",
            ".=",
            "&=",
            "|=",
            "^=",
            "<<=",
            ">>=",
            "??=",
        )
}
