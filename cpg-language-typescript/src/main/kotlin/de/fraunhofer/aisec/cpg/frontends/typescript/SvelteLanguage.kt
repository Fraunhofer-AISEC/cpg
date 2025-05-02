/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.*
import kotlin.reflect.KClass
import org.neo4j.ogm.annotation.Transient

/** The Svelte language. */
class SvelteLanguage : Language<SvelteLanguageFrontend>() {

    override val fileExtensions = listOf("svelte")
    override val namespaceDelimiter = "." // Using JS/TS convention for now

    @Transient
    override val frontend: KClass<out SvelteLanguageFrontend> = SvelteLanguageFrontend::class

    // TODO: Define built-in types relevant for Svelte if any, beyond JS/TS
    @Transient override val builtInTypes: Map<String, Type> = mapOf()

    // TODO: Define compound assignment operators if Svelte has specific ones
    @Transient override val compoundAssignmentOperators = setOf<String>() // Inherit from JS/TS?
}
