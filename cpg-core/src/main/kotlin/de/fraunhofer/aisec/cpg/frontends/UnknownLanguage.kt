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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.types.Type
import kotlin.reflect.KClass

/**
 * Represents a language definition with no known implementation or specifics. The class is used as
 * a placeholder or to handle cases where the language is not explicitly defined or supported.
 */
object UnknownLanguage : Language<Nothing>() {
    override val fileExtensions: List<String>
        get() = listOf()

    override val namespaceDelimiter: String
        get() = ""

    override val frontend: KClass<out Nothing> = Nothing::class
    override val builtInTypes: Map<String, Type> = mapOf()
    override val compoundAssignmentOperators: Set<String> = setOf()
}

/**
 * Represents a composite language definition composed of multiple languages.
 *
 * @property languages A list of languages that are part of this composite language definition.
 */
class MultipleLanguages(val languages: Set<Language<*>>) : Language<Nothing>() {
    override val fileExtensions = languages.flatMap { it.fileExtensions }
    override val namespaceDelimiter: String = ""
    override val frontend: KClass<out Nothing> = Nothing::class
    override val builtInTypes: Map<String, Type> = mapOf()
    override val compoundAssignmentOperators: Set<String> = setOf()
}
